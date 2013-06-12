package org.mule.transport.as2;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.io.IOUtils;
import org.mule.DefaultMuleMessage;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.WorkManager;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.config.ExceptionHelper;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.transport.NullPayload;
import org.mule.transport.as2.transformers.MDNBuilder;
import org.mule.transport.as2.transformers.MDNBuilder.MdnType;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpMessageProcessTemplate;
import org.mule.transport.http.HttpMessageReceiver;
import org.mule.transport.http.HttpRequest;
import org.mule.transport.http.HttpResponse;
import org.mule.transport.http.HttpServerConnection;

public class As2MessageProcessTemplate extends HttpMessageProcessTemplate{

    private final HttpServerConnection httpServerConnection;
    private HttpRequest request;
    private Long remainingRequestInCurrentPeriod;
    private Long maximumRequestAllowedPerPeriod;
    private Long timeUntilNextPeriodInMillis;
    private boolean failureSendingResponse;
	
	private As2Connector as2Connector;
	private MDNBuilder mdnBuilder;
	private MuleMessage mdnMessage;
	private MuleMessage smimeMessage;
	
	public As2MessageProcessTemplate(final HttpMessageReceiver messageReceiver, final HttpServerConnection httpServerConnection, final WorkManager flowExecutionWorkManager, As2Connector as2Connector) {
		super(messageReceiver, httpServerConnection, flowExecutionWorkManager);
		
		this.httpServerConnection = httpServerConnection;
		this.as2Connector = as2Connector;
		try {
			mdnBuilder = new MDNBuilder(as2Connector.getKeystorePath(), as2Connector.getKeystorePassword());
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			logger.error(e,e);
//			throw new MuleException(CoreMessages.fa\iledToCreate("MDNBuilder"));
		}
		logger.debug("DBG: inside As2MessageProcessTemplate");
	}
	
	
	/* Create the outgoing message from the connector */
    protected MuleMessage createMessageFromSource(Object message) throws MuleException {
    	
    	logger.debug("DBG: inside AS2 createMessageFromSource");
    	
    	/* Create the HTTP message */
        MuleMessage muleMessage = super.createMessageFromSource(message);
        
        /* Incoming SMIME */
        smimeMessage = new DefaultMuleMessage(muleMessage);
		MimeBodyPart payload = null;
		
		try {
			
			payload = (MimeBodyPart) ((MimeMultipart)smimeMessage.getPayload()).getBodyPart(0);
			
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			logger.error(e, e);
		}
		
		
		/* 2) Create MDN accordingly to the SMIME check */
		mdnMessage = mdnBuilder.createMDN(smimeMessage, as2Connector.getPartnerId());
		
		
		//MuleEvent event = new DefaultMuleEvent(message, (InboundEndpoint) endpoint, flowConstruct);
		/* If the MdnType is not PROCESSED no Mule Message has routed and the MDN is sent back immediately */
		if (!(mdnMessage.getProperty("mdnType", PropertyScope.INBOUND) == MdnType.PROCESSED)) {
			/* TODO: Launch an Exception or something like that and quit processing */
			throw new DefaultMuleException(MessageFactory.createStaticMessage("MDN is not Processed"));
		}
		
				
        /* Set the body of the outgoing message with the plain text of the incoming SMIME */
		try {
			String fileName = payload.getFileName();
			logger.debug("DBG: fileName is: " + fileName);
			muleMessage.setPayload(IOUtils.toString((InputStream) payload.getContent()));	
			/* Set fileName INBOUND PROPERTY*/
			muleMessage.setProperty("fileName", fileName, PropertyScope.INBOUND);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error(e, e);
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			logger.error(e, e);
		}
		
   
        return muleMessage;
    }
    
    
    /* *
     * Create the HTTP response back to the requestor 
     * 
     * */
    @Override
    public void sendResponseToClient(MuleEvent responseMuleEvent) throws MuleException
    {
    	    	
        try
        {
        	       	       	
            if (logger.isTraceEnabled())
            {
                logger.trace("Sending http response");
            }
            
            
        	/* TODO: it could be moved */
			request = getHttpServerConnection().readRequest();
            
            
            /* We don't care about the message at the end of the flow */
            //MuleMessage returnMessage = responseMuleEvent == null ? null : responseMuleEvent.getMessage();

			MuleMessage returnMessage = mdnMessage;
            
            Object tempResponse;
            if (returnMessage != null)
            {
                tempResponse = returnMessage.getPayload();
            }
            else
            {
                tempResponse = NullPayload.getInstance();
            }
            // This removes the need for users to explicitly adding the response transformer
            // ObjectToHttpResponse in their config
            
                        
            HttpResponse response;
            if (tempResponse instanceof HttpResponse)
            {
                response = (HttpResponse) tempResponse;
            }
            else
            {
                response = transformResponse(returnMessage);
            }

            //response.setupKeepAliveFromRequestVersion(request.getRequestLine().getHttpVersion());
            As2Connector as2Connector = (As2Connector) getMessageReceiver().getEndpoint().getConnector();
            response.disableKeepAlive(!as2Connector.isKeepAlive());

            Header connectionHeader = request.getFirstHeader("Connection");
            if (connectionHeader != null)
            {
                String value = connectionHeader.getValue();
                boolean endpointOverride = getEndpointKeepAliveValue(getMessageReceiver().getEndpoint());
                if ("keep-alive".equalsIgnoreCase(value) && endpointOverride)
                {
                    response.setKeepAlive(true);

                    if (response.getHttpVersion().equals(HttpVersion.HTTP_1_0))
                    {
                        connectionHeader = new Header(HttpConstants.HEADER_CONNECTION, "Keep-Alive");
                        response.setHeader(connectionHeader);
                    }
                }
                else if ("close".equalsIgnoreCase(value))
                {
                    response.setKeepAlive(false);
                }
            }

                        
            
            try
            {
                httpServerConnection.writeResponse(response,getThrottlingHeaders());
            }
            catch (Exception e)
            {
                failureSendingResponse = true;
            }
            if (logger.isTraceEnabled())
            {
                logger.trace("HTTP response sent successfully");
            }
              
        
        }
        catch (Exception e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Exception while sending http response");
                logger.debug(e);
            }
            //throw new MessagingException(responseMuleEvent,e);
        }
    }

    
    /**
     * It handles whenever an exception is thrown 
     * 
     * */
    @Override
    public void afterFailureProcessingFlow(MuleException messagingException) throws MuleException
    {
    	
    	logger.debug("DBG: inside " + getClass() + ".afterFailureProcessingFlow()");
    	if (!(mdnMessage.getProperty("mdnType", PropertyScope.INBOUND) == MdnType.PROCESSED)) {
    		
    		/* An exception has been thrown by createMessageFromSource() because the signature is not valid */
    		HttpResponse response = transformResponse(mdnMessage);
    		
    		try
            {
                httpServerConnection.writeResponse(response, getThrottlingHeaders());
            }
            catch (Exception e)
            {
                failureSendingResponse = true;
            }
    	}
    	else {
    		/* An exception has happened during the flow */
    		logger.debug("DBG: sending errored MDN");
        	sendMDNErrorToClient();
    	}


    }
    
      
    
    /**
     * Check if the endpoint has a keep-alive property configured. Note the translation from
     * keep-alive in the schema to keepAlive here.
     */
    private boolean getEndpointKeepAliveValue(ImmutableEndpoint ep)
    {
        String value = (String) ep.getProperty("keepAlive");
        if (value != null)
        {
            return Boolean.parseBoolean(value);
        }
        return true;
    }

    
    private Map<String,String> getThrottlingHeaders()
    {
        Map<String, String> throttlingHeaders = new HashMap<String, String>();
        addToMapIfNotNull(throttlingHeaders, X_RATE_LIMIT_LIMIT_HEADER,this.remainingRequestInCurrentPeriod);
        addToMapIfNotNull(throttlingHeaders, X_RATE_LIMIT_REMAINING_HEADER,this.maximumRequestAllowedPerPeriod);
        addToMapIfNotNull(throttlingHeaders, X_RATE_LIMIT_RESET_HEADER,this.timeUntilNextPeriodInMillis);
        return throttlingHeaders;
    }
    
    
    private void addToMapIfNotNull(Map<String,String> map, String key, Long value)
    {
        if (value != null)
        {
            map.put(key, String.valueOf(value));
        }
    }
    
    
    /**
     * Send and UNEXPECTED_PROCESSING ERROR MDN back to the sender
     * */
    private void sendMDNErrorToClient() {
    	
        HttpResponse httpResponse;
        
		try {
			httpResponse = transformResponse(mdnBuilder.createErroredMDN(smimeMessage, as2Connector.getPartnerId()));
	        httpServerConnection.writeResponse(httpResponse, getThrottlingHeaders());
		
		} catch (MuleException e) {
			
			logger.error(e, e);
			
		} catch (IOException e) {
			
			logger.error(e, e);
			
		}
    }
    

}
