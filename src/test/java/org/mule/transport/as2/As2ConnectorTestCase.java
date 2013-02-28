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

import static org.junit.Assert.*;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.api.transport.MessageDispatcherFactory;
import org.mule.api.transport.MessageRequesterFactory;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.transport.AbstractConnectorTestCase;

import org.junit.Test;

public class As2ConnectorTestCase extends AbstractConnectorTestCase
{
    /* For general guidelines on writing transports see
       http://www.mulesoft.org/documentation/display/MULE3USER/Creating+Transports */

    @Override
    public Connector createConnector() throws Exception
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

    @Override
    public String getTestEndpointURI()
    {
        // TODO Return a valid endpoint for your transport here
    	return "as2://127.0.0.1:8081/as2-receiver";
    }

    @Override
    public Object getValidMessage() throws Exception
    {
        // TODO Return an valid message for your transport
        throw new UnsupportedOperationException("getValidMessage");
    }

    @Test
    public void customProperties() throws Exception
    {
        // TODO test setting and retrieving any custom properties on the
        // Connector as necessary
    }
    
    @Test
    public void testConnectorMessageDispatcherFactory() throws Exception
    {
        Connector connector = getConnectorAndAssert();

        MessageDispatcherFactory factory = connector.getDispatcherFactory();
        assertNotNull(factory);
    }
    
    @Test
    public void testConnectorListenerSupport() throws Exception
    {
        Connector connector = getConnectorAndAssert();

        Service service = getTestService("anApple", Apple.class);

        InboundEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint(getTestEndpointURI());

        try
        {
            connector.registerListener(null, null, service);
            fail("cannot register null");
        }
        catch (Exception e)
        {
            // expected
        }

        try
        {
            connector.registerListener(endpoint, null, service);
            fail("cannot register null");
        }
        catch (Exception e)
        {
            // expected
        }

        try
        {
            connector.registerListener(null, getSensingNullMessageProcessor(), service);
            fail("cannot register null");
        }
        catch (Exception e)
        {
            // expected
        }

        connector.registerListener(endpoint, getSensingNullMessageProcessor(), service);

        // this should work
        connector.unregisterListener(endpoint, service);
        // so should this
        try
        {
            connector.unregisterListener(null, service);
            fail("cannot unregister null");
        }
        catch (Exception e)
        {
            // expected
        }
        try
        {
            connector.unregisterListener(null, service);
            fail("cannot unregister null");
        }
        catch (Exception e)
        {
            // expected
        }

        try
        {
            connector.unregisterListener(null, service);
            fail("cannot unregister null");
        }
        catch (Exception e)
        {
            // expected
        }
        connector.unregisterListener(endpoint, service);
        muleContext.getRegistry().unregisterService(service.getName());
    }
    
    @Test
    public void testConnectorMessageRequesterFactory() throws Exception
    {
        Connector connector = getConnectorAndAssert();

        MessageRequesterFactory factory = connector.getRequesterFactory();
        assertNull(factory);
    }
}
