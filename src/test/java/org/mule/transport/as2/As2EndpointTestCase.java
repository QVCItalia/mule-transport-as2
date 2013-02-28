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

import org.mule.api.endpoint.EndpointURI;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class As2EndpointTestCase extends AbstractMuleTestCase
{
    /* For general guidelines on writing transports see
       http://www.mulesoft.org/documentation/display/MULE3USER/Creating+Transports */

    @Test
    public void validEndpointURI() throws Exception
    {
        // TODO test creating and asserting Endpoint values eg

        EndpointURI url = new MuleEndpointURI("http://localhost:8081", new DefaultMuleContextFactory().createMuleContext());
        assertEquals("http", url.getScheme());
        assertNull(url.getEndpointName());
        assertEquals(8081, url.getPort());
        assertEquals("localhost", url.getHost());
        assertEquals(0, url.getParams().size());

    }
}
