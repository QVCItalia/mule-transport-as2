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

import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;

import javax.mail.internet.MimeMultipart;

import org.apache.commons.httpclient.Header;
import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.transport.AbstractMuleMessageFactoryTestCase;
import org.mule.transport.http.HttpRequest;
import org.mule.transport.http.RequestLine;

public class As2MuleMessageFactoryTestCase extends AbstractMuleMessageFactoryTestCase
{
    /* For general guidelines on writing transports see
       http://www.mulesoft.org/documentation/display/MULE3USER/Creating+Transports */

    @Override
    protected MuleMessageFactory doCreateMuleMessageFactory()
    {
        return new As2MuleMessageFactory(muleContext);
    }

    @Override
    protected Object getValidTransportMessage() throws Exception
    {
        ArrayList<Header> headers = new ArrayList<Header>();
        headers.add(new Header("as2-version", "1.2"));
        headers.add(new Header("content-type", "application/XML"));
        
    	HttpRequest as2Request = new HttpRequest(RequestLine.parseLine("POST /as-receiver HTTP/1.1"), headers.toArray(new Header[headers.size()]), new ByteArrayInputStream("Body".getBytes()), "UTF-8");
    	
    	return as2Request;
    }
    
    @Test
    public void testValidPayload() throws Exception
    {
        MuleMessageFactory factory = createMuleMessageFactory();
    
        Object payload = getValidTransportMessage();
        MuleMessage message = factory.create(payload, encoding);
        assertNotNull(message);
//        assertEquals(payload, message.getPayload());
    }
    
    protected Object getUnsupportedTransportMessage()
    {
        return MimeMultipart.class;
    }
}
