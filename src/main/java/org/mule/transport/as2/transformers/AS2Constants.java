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

    public static final String HEADER_VERSION = "as2-version";
    public static final String HEADER_FROM = "as2-from";
    public static final String HEADER_TO = "as2-to";
    public static final String HEADER_MESSAGE_ID = "message-id";
    public static final String HEADER_SUBJECT = "subject";
    public static final String HEADER_DATE = "date";
    public static final String HEADER_MDN = "Disposition-notification-to";
    public static final String HEADER_RECEIVER_RESPONSE = "Receipt-Delivery-Option";    
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
    public static final String HEADER_CONTENT_DISPOSITION = "content-disposition";
    public static final String HEADER_EDIINT_FEATURES = "ediint-features";
    public static final String HEADER_DISPOSITION_NOTIFICATION_TO = "disposition-notification-to";
    public static final String HEADER_SIMPLE_FROM = "from";
    public static final String HEADER_MIME_VERSION = "mime-version";
    public static final String HEADER_RECIPIENT_ADDRESS = "recipient-address";
    
    public static final String HEADER_MDN_CONTENT_TYPE = "multipart/report; report-type=disposition-notification; boundary=\"";
    public static final String HEADER_AS2_MULTIPART_SIGNED = "multipart/signed; protocol=\"application/pkcs7-signature\"; micalg=sha1; boundary=\"";
    public static final String HEADER_TEXT_PLAIN = "text/plain"; 
    public static final String HEADER_7_BIT = "7bit";
    public static final String HEADER_ATTACHMENT_VALUE = "attachment; filename=\"smime.p7m\"";
    public static final String HEADER_EDIINT_FEATURES_VALUE = "multiple-attachments, CEM";
    public static final String HEADER_AS2_VERSION_VALUE = "1.2";
    public static final String HEADER_MIME_VERSION_VALUE = "1.0";
    
}


