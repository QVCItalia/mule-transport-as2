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
import java.net.URI;
import java.util.List;

import javax.mail.internet.MimeMultipart;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.lang.RandomStringUtils;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.DispatchException;
import org.mule.transformer.TransformerChain;
import org.mule.transport.as2.transformers.AS2Constants;
import org.mule.transport.as2.transformers.AS2ObjectToHttpMethodRequest;
import org.mule.transport.as2.transformers.MDNBuilder;
import org.mule.transport.as2.transformers.MDNBuilder.MdnType;
import org.mule.transport.http.HttpClientMessageDispatcher;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

/**
 * <code>As2MessageDispatcher</code> TODO document
 */
public class As2MessageDispatcher extends HttpClientMessageDispatcher
{

	private As2Connector as2Connector;
    private Transformer sendTransformer;
    private volatile HttpClient client = null;
    
    public static final int ERROR_STATUS_CODE_RANGE_START = 400;
    public static final int REDIRECT_STATUS_CODE_RANGE_START = 300;

	
    public As2MessageDispatcher(OutboundEndpoint endpoint)
    {    	
        super(endpoint);
        this.as2Connector =  (As2Connector) endpoint.getConnector();
        List<Transformer> ts = as2Connector.getDefaultOutboundTransformers(null);
        if (ts.size() == 1)
        {
            this.sendTransformer = ts.get(0);
        }
        else if (ts.size() == 0)
        {
	        	AS2ObjectToHttpMethodRequest transformer = new AS2ObjectToHttpMethodRequest();
	        	this.sendTransformer = transformer;
	            this.sendTransformer.setMuleContext(as2Connector.getMuleContext());
	            this.sendTransformer.setEndpoint(endpoint);
        }
        else
        {
            this.sendTransformer = new TransformerChain(ts);
        }
    }

    
    @Override
    protected void doConnect() throws Exception
    {
        if (client == null)
        {
            client = as2Connector.doClientConnectLocal();
        }
    }


    @Override
    protected void doDispatch(MuleEvent event) throws Exception
    {
        HttpMethod httpMethod = getMethod(event);
        as2Connector.setupClientAuthorizationLocal(event, httpMethod, client, endpoint);

        try
        {
            execute(event, httpMethod);

            if (returnException(event, httpMethod))
            {
                logger.error(httpMethod.getResponseBodyAsString());

                Exception cause = new Exception(String.format("Http call returned a status of: %1d %1s", httpMethod.getStatusCode(), httpMethod.getStatusText()));
                throw new DispatchException(event, getEndpoint(), cause);
            }
            else if (httpMethod.getStatusCode() >= REDIRECT_STATUS_CODE_RANGE_START)
            {
                if (logger.isInfoEnabled())
                {
                    logger.info("Received a redirect response code: " + httpMethod.getStatusCode() + " " + httpMethod.getStatusText());
                }
            }
            else {
	            /* Check the incoming synch MDN */
	            MimeMultipart mdn = MDNBuilder.createMDNFromResponse(httpMethod.getResponseBodyAsStream(), "multipart/report");
	            if (MDNBuilder.identifyMdnType(mdn) != MdnType.PROCESSED) {
	            	throw new Exception("MDN is not of type PROCESSED");
	            }
            }
        }
        finally
        {
            httpMethod.releaseConnection();
        }
    }

    protected HttpMethod execute(MuleEvent event, HttpMethod httpMethod) throws Exception
    {
        // TODO set connection timeout buffer etc
        try
        {
        	String endpointAddress = endpoint.getEndpointURI().getAddress();
        	URI uri = new URI(endpointAddress.replaceFirst("as2", "http"));
        	
            this.processCookies(event);
            this.processMuleSession(event, httpMethod);
            this.setAS2Headers(event, httpMethod);

            // TODO can we use the return code for better reporting?
            client.executeMethod(getHostConfig(uri), httpMethod);

            return httpMethod;
        }
        catch (IOException e)
        {
            // TODO employ dispatcher reconnection strategy at this point
            throw new DispatchException(event, getEndpoint(), e);
        }
        catch (Exception e)
        {
            throw new DispatchException(event, getEndpoint(), e);
        }

    }

    private void processMuleSession(MuleEvent event, HttpMethod httpMethod)
    {
        httpMethod.setRequestHeader(new Header(HttpConstants.HEADER_MULE_SESSION, event.getMessage().<String>getOutboundProperty(MuleProperties.MULE_SESSION_PROPERTY)));
    }
    
    /**
     * Set AS2 Specific Headers
     * */
    private void setAS2Headers(MuleEvent event, HttpMethod httpMethod) {
    	String asTo = as2Connector.getPartnerId();
    	String asFrom = as2Connector.getSenderId();
    	String subject = (String) endpoint.getProperty("subject");
    	
    	httpMethod.setRequestHeader(new Header(AS2Constants.HEADER_TO, asTo));
    	httpMethod.setRequestHeader(new Header(AS2Constants.HEADER_FROM, asFrom));
    	httpMethod.setRequestHeader(new Header(AS2Constants.HEADER_MESSAGE_ID, "<AS2_"+RandomStringUtils.randomAlphanumeric(4) + "@" + asFrom + "_" + asTo + ">"));
    	httpMethod.setRequestHeader(new Header(AS2Constants.HEADER_CONTENT_TYPE, AS2Constants.HEADER_AS2_MULTIPART_SIGNED));
    	if (subject != null) {
    		httpMethod.setRequestHeader(new Header(AS2Constants.HEADER_SUBJECT, subject));
    	}
    }


    protected HttpMethod getMethod(MuleEvent event) throws TransformerException
    {

        // Configure timeout. This is done here because MuleEvent.getTimeout() takes
        // precedence and is not available before send/dispatch.
        // Given that dispatchers are borrowed from a thread pool mutating client
        // here is ok even though it is not ideal.
        client.getHttpConnectionManager().getParams().setConnectionTimeout(endpoint.getResponseTimeout());
        client.getHttpConnectionManager().getParams().setSoTimeout(endpoint.getResponseTimeout());

        MuleMessage msg = event.getMessage();
        setPropertyFromEndpoint(event, msg, HttpConnector.HTTP_CUSTOM_HEADERS_MAP_PROPERTY);

        HttpMethod httpMethod;
        Object body = event.getMessage().getPayload();
        
        if (body instanceof HttpMethod)
        {
            httpMethod = (HttpMethod) body;
        }
        else
        {
            httpMethod = (HttpMethod) sendTransformer.transform(msg);
        }

        httpMethod.setFollowRedirects("true".equalsIgnoreCase((String)endpoint.getProperty("followRedirects")));
        return httpMethod;
    }

 

}

