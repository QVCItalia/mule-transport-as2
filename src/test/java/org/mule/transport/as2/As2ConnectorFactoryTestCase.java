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
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class As2ConnectorFactoryTestCase extends AbstractMuleContextTestCase
{
    /* For general guidelines on writing transports see
       http://www.mulesoft.org/documentation/display/MULE3USER/Creating+Transports */

    @Test
    public void createFromFactory() throws Exception
    {
        InboundEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint(getEndpointURI());
        assertNotNull(endpoint);
        assertNotNull(endpoint.getConnector());
        assertTrue(endpoint.getConnector() instanceof As2Connector);
        assertEquals(getEndpointURI(), endpoint.getEndpointURI().getAddress());
    }

    public String getEndpointURI()
    {
        // TODO return a valid endpoint URI string for your transport
        // i.e. tcp://localhost:1234
        return "as2://127.0.0.1:8081/as2-receiver";
    }
}
