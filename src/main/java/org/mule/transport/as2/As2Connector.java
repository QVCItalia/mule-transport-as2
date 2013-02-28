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

import java.io.UnsupportedEncodingException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.transport.file.ExpressionFilenameParser;
import org.mule.transport.file.FilenameParser;
import org.mule.transport.http.HttpConnector;

/**
 * <code>As2Connector</code> TODO document
 */
public class As2Connector extends HttpConnector
{
    /* This constant defines the main transport protocol identifier */
    public static final String AS2 = "as2";

    private String senderId;
    private String keystorePath;
    private String keystorePassword;
    private String partnerId;
    
    private FilenameParser filenameParser;
       
    public As2Connector(MuleContext context)
    {
        super(context);
        filenameParser = new ExpressionFilenameParser();
    }
       

    public String getProtocol()
    {
        return AS2;
    }

    public String getSenderId() {
    	return senderId;
    }
    
    public void setSenderId(String senderId) {
    	this.senderId = senderId;
    }
    
	public String getKeystorePath() {
		return keystorePath;
	}


	public void setKeystorePath(String keystorePath) {
		this.keystorePath = keystorePath;
	}


	public String getKeystorePassword() {
		return keystorePassword;
	}


	public void setKeystorePassword(String keystorePassword) {
		this.keystorePassword = keystorePassword;
	}


	public String getPartnerId() {
		return partnerId;
	}


	public void setPartnerId(String partnerId) {
		this.partnerId = partnerId;
	}
	
	
	public FilenameParser getFilenameParser() {
		return filenameParser;
	}


	public void setFilenameParser(FilenameParser filenameParser) {
		
		this.filenameParser = filenameParser;		
		if (filenameParser != null) {
			filenameParser.setMuleContext(muleContext);
		}
	}


	protected void setupClientAuthorizationLocal(MuleEvent event, HttpMethod httpMethod, HttpClient client, ImmutableEndpoint endpoint) throws UnsupportedEncodingException {
		this.setupClientAuthorization(event, httpMethod, client, endpoint);
	}
	
	
	protected HttpClient doClientConnectLocal() throws Exception {
		return this.doClientConnect();
	}
	
	@Override
	protected void doInitialise() throws InitialisationException {
		super.doInitialise();
		
        if (filenameParser != null)
        {
            filenameParser.setMuleContext(muleContext);
        }
		
	}

}
