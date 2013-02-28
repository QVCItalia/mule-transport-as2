/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.as2.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.as2.As2Connector;
import org.mule.transport.as2.transformers.AS2ObjectToHttpMethodRequest;
import org.mule.transport.file.ExpressionFilenameParser;
import org.mule.transport.http.transformers.ObjectToHttpClientMethodRequest;

/**
 * Registers a Bean Definition Parser for handling <code><as2:connector></code> elements
 * and supporting endpoint elements.
 */
public class As2NamespaceHandler extends AbstractMuleNamespaceHandler
{
    public void init()
    {
        /* This creates handlers for 'endpoint', 'outbound-endpoint' and 'inbound-endpoint' elements.
           The defaults are sufficient unless you have endpoint styles different from the Mule standard ones
           The URIBuilder as constants for common required attributes, but you can also pass in a user-defined String[].
         */
        registerStandardTransportEndpoints(As2Connector.AS2, URIBuilder.PATH_ATTRIBUTES);

        /* This will create the handler for your custom 'connector' element.  You will need to add handlers for any other
           xml elements you define.  For more information see:
           http://www.mulesoft.org/documentation/display/MULE3USER/Creating+a+Custom+XML+Namespace
        */
        registerConnectorDefinitionParser(As2Connector.class);
        
        registerBeanDefinitionParser("as2object-to-http-request-transformer", new MessageProcessorDefinitionParser(AS2ObjectToHttpMethodRequest.class));
        
        registerBeanDefinitionParser("expression-filename-parser", new ChildDefinitionParser("filenameParser", ExpressionFilenameParser.class));


    }
}
