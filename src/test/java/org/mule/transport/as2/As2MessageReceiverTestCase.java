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

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.api.transport.MessageReceiver;
import org.mule.transport.AbstractMessageReceiverTestCase;

import com.mockobjects.dynamic.Mock;

import org.junit.Test;

public class As2MessageReceiverTestCase extends AbstractMessageReceiverTestCase
{
    /* For general guidelines on writing transports see
       http://www.mulesoft.org/documentation/display/MULE3USER/Creating+Transports */

    @Override
    public MessageReceiver getMessageReceiver() throws Exception
    {
        Mock mockService = new Mock(Service.class);
        mockService.expectAndReturn("getResponseTransformer", null);
        return new As2MessageReceiver(createConnector(), (Service) mockService.proxy(), endpoint);
    }

    @Override
    public InboundEndpoint getEndpoint() throws Exception
    {
        // TODO return a valid endpoint i.e.
        // return new MuleEndpoint("tcp://localhost:1234", true)
    	return muleContext.getEndpointFactory().getInboundEndpoint("as2://127.0.0.1:8081/as2-receiver");
    }
    
    private Connector createConnector() throws Exception
    {
        /* IMPLEMENTATION NOTE: Create and initialise an instance of your
           connector here. Do not actually call the connect method. */

        As2Connector connector = new As2Connector(muleContext);
        connector.setName("TestAS2Connector");
        connector.setPartnerId("mend");
        connector.setKeystorePath("src/test/resources/keystoreTest.jks");
        connector.setKeystorePassword("PASSWORD");
        return connector;
    }
}
