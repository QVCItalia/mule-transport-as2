/**
 * Mule AS2 Cloud Connector
 *
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.as2.transformers;

/**
 * Constants use in AS2
 */
public class AS2Constants
{

    public static final String HEADER_VERSION = "AS2-Version";
    public static final String HEADER_FROM = "AS2-From";
    public static final String HEADER_TO = "AS2-To";
    public static final String HEADER_MESSAGE_ID = "Message-Id";
    public static final String HEADER_SUBJECT = "Subject";
    public static final String HEADER_DATE = "Date";
    public static final String HEADER_MDN = "Disposition-notification-to";
    public static final String HEADER_RECEIVER_RESPONSE = "Receipt-Delivery-Option";    
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
    public static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    
    public static final String HEADER_MDN_CONTENT_TYPE = "multipart/report; report-type=disposition-notification; boundary=\"";
    public static final String HEADER_AS2_MULTIPART_SIGNED = "multipart/signed; protocol=\"application/pkcs7-signature\"; micalg=sha1";
    public static final String HEADER_TEXT_PLAIN = "text/plain"; 
    public static final String HEADER_7_BIT = "7bit";
    
}


