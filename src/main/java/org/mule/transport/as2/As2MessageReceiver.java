/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.as2;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.resource.spi.work.Work;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.io.IOUtils;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.execution.ExecutionCallback;
import org.mule.api.execution.ExecutionTemplate;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.Connector;
import org.mule.api.transport.MessageReceiver;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;
import org.mule.transport.as2.transformers.AS2Constants;
import org.mule.transport.as2.transformers.MDNBuilder;
import org.mule.transport.as2.transformers.MDNBuilder.MdnType;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpMessageReceiver;
import org.mule.transport.http.HttpRequest;
import org.mule.transport.http.HttpResponse;
import org.mule.transport.http.HttpServerConnection;
import org.mule.transport.http.RequestLine;
import org.mule.transport.http.i18n.HttpMessages;
import org.mule.util.MapUtils;

import com.sun.mail.util.BASE64DecoderStream;

/**
 * <code>As2MessageReceiver</code> TODO document
 */
public class As2MessageReceiver extends  HttpMessageReceiver 
{
	private String FILE_NAME = "fileName";
	private String FILE_NAME_PATTERN = "filename=.*";
	
	private MDNBuilder mdnBuilder;
	private As2Connector as2Connector;

    public As2MessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint) throws CreateException
    {
        super(connector, flowConstruct, endpoint);
        
        try {
        	
        	as2Connector = (As2Connector) connector;
			mdnBuilder = new MDNBuilder(as2Connector.getKeystorePath(), as2Connector.getKeystorePassword());
			
        } catch (TransformerException e) {
			// TODO Auto-generated catch block
			logger.error(e, e);
			throw new CreateException(CoreMessages.failedToCreate("AS2 Message Receiver"), null);
		}
    }

    @Override
    protected void initializeMessageFactory() throws InitialisationException
    {
    	As2MuleMessageFactory factory;
        try
        {
            factory = (As2MuleMessageFactory) super.createMuleMessageFactory();

            boolean enableCookies = MapUtils.getBooleanValue(endpoint.getProperties(),
                HttpConnector.HTTP_ENABLE_COOKIES_PROPERTY, ((HttpConnector) connector).isEnableCookies());
            factory.setEnableCookies(enableCookies);

            String cookieSpec = MapUtils.getString(endpoint.getProperties(),
                HttpConnector.HTTP_COOKIE_SPEC_PROPERTY, ((HttpConnector) connector).getCookieSpec());
            factory.setCookieSpec(cookieSpec);

            factory.setExchangePattern(endpoint.getExchangePattern());

            muleMessageFactory = factory;

        }
        catch (CreateException ce)
        {
            Message message = MessageFactory.createStaticMessage(ce.getMessage());
            throw new InitialisationException(message, ce, this);
        }
    }   
    
    @Override
    protected Work createWork(Socket socket) throws IOException
    {
    	return new As2Worker(socket);
    }
    
    protected class As2Worker extends HttpWorker {
    	private HttpServerConnection conn;
    	private String remoteClientAddress;
	
		public As2Worker(Socket socket) throws IOException {
			super(socket);
			
            String encoding = endpoint.getEncoding();
            if (encoding == null)
            {
                encoding = connector.getMuleContext().getConfiguration().getDefaultEncoding();
            }

            conn = new HttpServerConnection(socket, encoding, (HttpConnector) connector);

            final SocketAddress clientAddress = socket.getRemoteSocketAddress();
            if (clientAddress != null)
            {
                remoteClientAddress = clientAddress.toString();
            }
		}
	
	
		/**
		 * Server the HTTP Request 
		 */
		protected HttpResponse doRequest(HttpRequest request) throws IOException, MuleException
		{
			sendExpect100(request);

			HttpResponse response = null;
			MuleEvent returnEvent = null;
			boolean isSecLevelCorrect = false;

			MuleMessage mdnMessage = null;
			Object tempResponse;

			//PartnerConfiguration configuration = new PartnerConfiguration(as2Connector.getPartnerId(), as2Connector.getMode(), as2Connector.getSecurityLevel(), as2Connector.getMdnRequest());
									
			
			/* 1) Create the mule message which contains a MIME */
			final MuleMessage message = createMuleMessage(request);

			/* Copy the incoming mule message containing the MIME */
			final MuleMessage smimeMessage = new DefaultMuleMessage(message);

			MimeBodyPart payload = null;
			try {
				/* Payload is always body part 0 */
				payload = (MimeBodyPart) ((MimeMultipart)message.getPayload()).getBodyPart(0);
				logger.debug("DBG: payload is " + IOUtils.toString((InputStream) payload.getContent()));
				
				MimeBodyPart signature = (MimeBodyPart)((MimeMultipart)message.getPayload()).getBodyPart(1);
				logger.debug("DBG: signature is: " + signature.getContent().getClass());
				
				BASE64DecoderStream base64DecoderStream = (BASE64DecoderStream) signature.getContent();

				byte[] byteArray = IOUtils.toByteArray(base64DecoderStream);
				byte[] encodeBase64 = Base64.encodeBase64(byteArray);
		      
				logger.debug("DBG: signature is: " + new String(encodeBase64, "UTF-8"));
				
			} catch (javax.mail.MessagingException e2) {
			// TODO Auto-generated catch block
				logger.error(e2, e2);
			} catch (Exception e) {
				logger.error(e, e);
			} 
			
            MuleEvent event = new DefaultMuleEvent(message, (InboundEndpoint) endpoint, flowConstruct);
 	  	
			/* 2) Create MDN accordingly to the SMIME check */
			mdnMessage = mdnBuilder.createMDN(smimeMessage, as2Connector.getPartnerId());
			
			/* If the MdnType is not PROCESSED no Mule Message has routed and the MDN is sent back immediately */
			if (!(mdnMessage.getProperty("mdnType", PropertyScope.INBOUND) == MdnType.PROCESSED)) {
//				MuleEvent event = new DefaultMuleEvent(message, (InboundEndpoint) endpoint, flowConstruct);
				return transformResponse(mdnMessage, event);
			}
			
			
			tempResponse = mdnMessage;

        
			/* Set the message   of the outgoing message with the body of the incoming http request */
			try {
				message.setPayload(IOUtils.toString((InputStream) payload.getContent()));
			} catch (javax.mail.MessagingException e1) {
				// TODO Auto-generated catch block
				logger.error(e1, e1);
			}

			
			String path = message.getInboundProperty(HttpConnector.HTTP_REQUEST_PROPERTY);
      
			int i = path.indexOf('?');
	        if (i > -1)
	        {
	        	path = path.substring(0, i);
	        }
	
	        message.setProperty(HttpConnector.HTTP_REQUEST_PATH_PROPERTY, path, PropertyScope.INBOUND);
	        setFileNameProperty(payload, message);
			
	        if (logger.isDebugEnabled())
	        {
	        	logger.debug(message.getInboundProperty(HttpConnector.HTTP_REQUEST_PROPERTY));
	        }
	
	        // determine if the request path on this request denotes a different receiver
	        final MessageReceiver receiver = getTargetReceiver(message, endpoint);


	        // the response only needs to be transformed explicitly if
	        // A) the request was not served or B) a null result was returned
	        if (receiver != null)
	        {
	        	String contextPath = HttpConnector.normalizeUrl(receiver.getEndpointURI().getPath());
              	message.setProperty(HttpConnector.HTTP_CONTEXT_PATH_PROPERTY,
                                         contextPath,
                                         PropertyScope.INBOUND);

              	message.setProperty(HttpConnector.HTTP_CONTEXT_URI_PROPERTY,
                                  receiver.getEndpointURI().getAddress(),
                                  PropertyScope.INBOUND);

              	message.setProperty(HttpConnector.HTTP_RELATIVE_PATH_PROPERTY,
                                  processRelativePath(contextPath, path),
                                  PropertyScope.INBOUND);

              	ExecutionTemplate<MuleEvent> executionTemplate = createExecutionTemplate();


              	/* Route the message into the flow */
              	try {
					returnEvent = executionTemplate.execute(new ExecutionCallback<MuleEvent>()
					{
					    @Override
					    public MuleEvent process() throws Exception
					    {
					        preRouteMessage(message);
					        return receiver.routeMessage(message);
					    }
					});
              		} catch (Exception e) {
              			// TODO Auto-generated catch block
//              			e.printStackTrace();
              			logger.error(e, e);
              			// Exception in the flow -> send MDN UNEXPECTED_PROCESSING_ERROR
						return transformResponse(mdnBuilder.createErroredMDN(smimeMessage, as2Connector.getPartnerId()), event);
		
              		}
      
                  
              	  /* exchangePattern="one-way" returnEvent == null otherwise is the latest message of the flow */
                  /* Maybe inspecting the message is not necessary */
//                  MuleMessage returnMessage = returnEvent == null ? null : returnEvent.getMessage();	
              		
            	              	
	              // This removes the need for users to explicitly adding the response transformer
	              // ObjectToHttpResponse in their config
	              if (tempResponse instanceof HttpResponse)
	              {
	                  response = (HttpResponse) tempResponse;
	              }
	              else
	              {
	              	response = transformResponse(tempResponse, returnEvent);
	              }

//	              response.setupKeepAliveFromRequestVersion(request.getRequestLine().getHttpVersion());
	              HttpConnector httpConnector = (HttpConnector) connector;
	              response.disableKeepAlive(!httpConnector.isKeepAlive());

	              Header connectionHeader = request.getFirstHeader("Connection");
	              if (connectionHeader != null)
	              {
	                  String value = connectionHeader.getValue();
	                  boolean endpointOverride = getEndpointKeepAliveValue(endpoint);
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
	          }
	          else
	          {
	              EndpointURI uri = endpoint.getEndpointURI();
	              String failedPath = String.format("%s://%s:%d%s",
	                                                uri.getScheme(), uri.getHost(), uri.getPort(),
	                                                message.getInboundProperty(HttpConnector.HTTP_REQUEST_PATH_PROPERTY));
	              response = buildFailureResponse(request.getRequestLine().getHttpVersion(), HttpConstants.SC_NOT_FOUND,
	                                              HttpMessages.cannotBindToAddress(failedPath).toString());
	          }
          
          return response;
      }
  
			    /**
			    * Check if endpoint has a keep-alive property configured. Note the translation from
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
   
		     private void sendExpect100(HttpRequest request) throws MuleException, IOException
		     {
		         RequestLine requestLine = request.getRequestLine();
		
		         // respond with status code 100, for Expect handshake
		         // according to rfc 2616 and http 1.1
		         // the processing will continue and the request will be fully
		         // read immediately after
		         HttpVersion requestVersion = requestLine.getHttpVersion();
		         if (HttpVersion.HTTP_1_1.equals(requestVersion))
		         {
		             Header expectHeader = request.getFirstHeader(HttpConstants.HEADER_EXPECT);
		             if (expectHeader != null)
		             {
		                 String expectHeaderValue = expectHeader.getValue();
		                 if (HttpConstants.HEADER_EXPECT_CONTINUE_REQUEST_VALUE.equals(expectHeaderValue))
		                 {
		                     HttpResponse expected = new HttpResponse();
		                     expected.setStatusLine(requestLine.getHttpVersion(), HttpConstants.SC_CONTINUE);
		                     final DefaultMuleEvent event = new DefaultMuleEvent(new DefaultMuleMessage(expected,
		                         connector.getMuleContext()), (InboundEndpoint) endpoint, flowConstruct);
		                     RequestContext.setEvent(event);
		                     conn.writeResponse(transformResponse(expected, event));
		                 }
		             }
		         }
		     }


    }
    
    
    /**
     * Set "fileName" INBOUND Property with the filename in the AS2 smime
     * 
     * */
    private void setFileNameProperty(MimeBodyPart bodyPart, MuleMessage message) {
    	String fileName = "";
    	try {
			String contentDisposition = bodyPart.getHeader(AS2Constants.HEADER_CONTENT_DISPOSITION)[0];
			Pattern p = Pattern.compile(FILE_NAME_PATTERN);
			Matcher m = p.matcher(contentDisposition);
			if (m.find()) {
				String fileNameString = m.group();
				fileName = fileNameString.replace("filename=", "");
			}
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			logger.error(e, e);
		}
    	
    	message.setProperty(FILE_NAME, fileName, PropertyScope.INBOUND);
    }
}
