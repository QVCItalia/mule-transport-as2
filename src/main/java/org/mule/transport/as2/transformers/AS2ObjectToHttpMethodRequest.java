package org.mule.transport.as2.transformers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.OutputHandler;
import org.mule.api.transport.PropertyScope;
import org.mule.transport.NullPayload;
import org.mule.transport.as2.As2Connector;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.StreamPayloadRequestEntity;
import org.mule.transport.http.i18n.HttpMessages;
import org.mule.transport.http.transformers.ObjectToHttpClientMethodRequest;
import org.mule.util.SerializationUtils;

public class AS2ObjectToHttpMethodRequest extends ObjectToHttpClientMethodRequest{

	private As2Connector connector = null;
    private As2MessBuilder as2MessageBuilder = null;
	
	
	protected URI getURI(MuleMessage message) throws URISyntaxException, TransformerException
    {
        String endpointAddress = message.getOutboundProperty(MuleProperties.MULE_ENDPOINT_PROPERTY, null);
        if (endpointAddress == null)
        {
            throw new TransformerException(
                HttpMessages.eventPropertyNotSetCannotProcessRequest(MuleProperties.MULE_ENDPOINT_PROPERTY),
                this);
        }
        return new URI(endpointAddress.replaceFirst("as2", "http"));
    }
	
	protected void setupEntityMethod(Object src, String encoding, MuleMessage msg, EntityEnclosingMethod postMethod) throws UnsupportedEncodingException, TransformerException
	    {
			// Dont set a POST payload if the body is a Null Payload.
	        // This way client calls can control if a POST body is posted explicitly
	        if (!(msg.getPayload() instanceof NullPayload))
	        {
	            String outboundMimeType = (String) msg.getProperty(HttpConstants.HEADER_CONTENT_TYPE, PropertyScope.OUTBOUND);
	            if (outboundMimeType == null)
	            {
	                outboundMimeType = (getEndpoint() != null ? getEndpoint().getMimeType() : null);
	            }
	            if (outboundMimeType == null)
	            {
	                outboundMimeType = HttpConstants.DEFAULT_CONTENT_TYPE;
	                logger.info("Content-Type not set on outgoing request, defaulting to: " + outboundMimeType);
	            }

	            if (encoding != null && !"UTF-8".equals(encoding.toUpperCase())
	                && outboundMimeType.indexOf("charset") == -1)
	            {
	                outboundMimeType += "; charset=" + encoding;
	            }

	            // Ensure that we have a cached representation of the message if we're
	            // using HTTP 1.0
	            final String httpVersion = msg.getOutboundProperty(HttpConnector.HTTP_VERSION_PROPERTY,
	                HttpConstants.HTTP11);
	            if (HttpConstants.HTTP10.equals(httpVersion))
	            {
	                try
	                {
	                    src = msg.getPayloadAsBytes();
	                }
	                catch (final Exception e)
	                {
	                    throw new TransformerException(this, e);
	                }
	            }

	            if (msg.getOutboundAttachmentNames() != null && msg.getOutboundAttachmentNames().size() > 0)
	            {
	                try
	                {
	                    postMethod.setRequestEntity(createMultiPart(msg, postMethod));
	                    return;
	                }
	                catch (final Exception e)
	                {
	                    throw new TransformerException(this, e);
	                }
	            }
	            /* TODO place try catch in a better way */
	            
	            /* Create file name from fileNamePattern */
	            if (connector == null) {
	            	connector = (As2Connector) getEndpoint().getConnector();
	            }
	            
	            String fileNamePattern = (String) endpoint.getProperty("fileName");
	            String fileName = connector.getFilenameParser().getFilename(msg, fileNamePattern);
	            
	            if (as2MessageBuilder == null) {
		            try {
						as2MessageBuilder = As2MessBuilder.getAs2MessBuilder(connector.getKeystorePath(), connector.getKeystorePassword(), connector.getSenderId());
					} catch (Exception e) {
						// TODO Auto-generated catch block
						logger.error("Exception creating As2MessBuilder");
						throw new TransformerException(this, e);
					}
	            }

	            /* Set the POST request entity with the AS2 Mime Multipart */
	            if (src instanceof String)
	            {
	                postMethod.setRequestEntity(new StringRequestEntity(src.toString(), outboundMimeType, encoding));
	                return;
	            }

	            if (src instanceof InputStream)
	            {
	            	src = as2MessageBuilder.createAS2Signed((InputStream) src, fileName);
	                postMethod.setRequestEntity(new InputStreamRequestEntity((InputStream) src, outboundMimeType));
	            }
	            else if (src instanceof byte[])
	            {
	            	src = as2MessageBuilder.createAS2Signed(new ByteArrayInputStream((byte[]) src), fileName);
	                postMethod.setRequestEntity(new InputStreamRequestEntity((InputStream) src, outboundMimeType));
	            }
	            else if (src instanceof OutputHandler)
	            {
	                final MuleEvent event = RequestContext.getEvent();
	                postMethod.setRequestEntity(new StreamPayloadRequestEntity((OutputHandler) src, event));
	            }
	            else
	            {
	                final byte[] buffer = SerializationUtils.serialize((Serializable) src);
	            	src = as2MessageBuilder.createAS2Signed(new ByteArrayInputStream((byte[]) buffer), fileName);
	                postMethod.setRequestEntity(new InputStreamRequestEntity((InputStream) src, outboundMimeType));
	            }
	        }
	        else if (msg.getOutboundAttachmentNames() != null && msg.getOutboundAttachmentNames().size() > 0)
	        {
	            try
	            {
	                postMethod.setRequestEntity(createMultiPart(msg, postMethod));
	            }
	            catch (Exception e)
	            {
	                throw new TransformerException(this, e);
	            }
	        }
	    }
	


}
