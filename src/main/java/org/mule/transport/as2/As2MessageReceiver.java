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
import org.mule.execution.MessageProcessContext;
import org.mule.transport.ConnectException;
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

	public As2MessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint) throws CreateException {
		super(connector, flowConstruct, endpoint);
		// TODO Auto-generated constructor stub
	}
	
    @Override
    protected void doConnect() throws ConnectException
    {
        ((As2Connector) connector).connect(endpoint.getEndpointURI());
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        ((As2Connector) connector).disconnect(endpoint.getEndpointURI());
    }

    MessageProcessContext createMessageContext(HttpServerConnection httpServerConnection)
    {
        return new As2MessageProcessTemplate(this,httpServerConnection,getWorkManager(), (As2Connector) connector);
    }

    void processRequest(HttpServerConnection httpServerConnection) throws InterruptedException, MuleException
    {
    	logger.debug("DBG: inside AS2 process Request");
        As2MessageProcessTemplate messageContext = (As2MessageProcessTemplate) createMessageContext(httpServerConnection);
        processMessage(messageContext,messageContext);
        messageContext.awaitTermination();
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
}
