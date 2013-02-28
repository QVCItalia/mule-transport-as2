package org.mule.transport.as2.transformers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.CoreMessages;


/**
 * This class is responsible for everything concerning MDN creation. It could be an outgoing MDN or an incoming MDN
 * as well as different kind of it.
 * 
 * */
public class MDNBuilder  {
	
	private static final String ORIGINAL_RECIPIENT = "Original-Recipient: rfc822; ";
	private static final String FINAL_RECIPIENT = "Final-Recipient: rfc822; ";
	private static final String ORIGINAL_MESSAGE_ID = "Original-Message-ID: ";
	private static final String DISPOSITION = "Disposition: automatic-action/MDN-sent-automatically; ";
	private static final String DISP_CONTENT_TYPE = "message/disposition-notification";
	
	private static final String BOUNDARY_STRING_PATTERN = "boundary=.*";

	private final SMIMEVerifier smimeVerifier;
	
	public enum MdnType {	
		
		AUTHENTIFICATION_FAILED("processed/error: authentication-failed", "Signature is not verified"), 						
		DECOMPRESSION_FAILED("processed/error: decompression-failed", "Error during decompression"),
		DECRYPTION_FAILED("processed/decryption-failed", "Error durin decryption"),
		INSUFFICIENT_MESSAGE_SECURITY("insufficient-message-security", "Message security is not matching the partner configuration"),
		INTEGRITY_CHECK_FAILED("processed/error: integrity-check-failed", "Message Digest value doesn't match calculated value"),
		UNKNOWN_TRADING_PARTNER("processed/error: unknown-trading-partner", "Unknown trading partner"),		
		UNEXPECTED_PROCESSING_ERROR("processed/error: unexpected-processing-error", "An exception occurred server side"),
		PROCESSED("processed", "File received correctly");
		
		private final String mdnDispModExtension;
		private final String mdnMessage;
		
		MdnType(String mdnDispModExtension, String mdnMessage) {
			this.mdnDispModExtension = mdnDispModExtension;
			this.mdnMessage = mdnMessage;
			}
		
		private String getDispModExtension() {
			return mdnDispModExtension;
		} 
		
		private String getMdnMessage() {
			return mdnMessage;
		}
		
	};
	
	
	public MDNBuilder(String keystorePath, String keystorePassword) throws TransformerException {
		
		smimeVerifier = new SMIMEVerifier(keystorePath, keystorePassword);
		
	}
		
	
	/**
	 * Create a different MDN based on the incoming mule message
	 * 
	 * */
	public MuleMessage createMDN(MuleMessage message, String partnerId) throws TransformerException {
		
		MimeMultipart mdn = new MimeMultipart();

		try {
			MdnType mdnType = null;
				
			if(!(message.getPayload() instanceof MimeMultipart)) {
				throw new TransformerException(CoreMessages.failedToCreate("MDN Message"));
			} 	
				MimeMultipart mime = (MimeMultipart) message.getPayload();
			
			if (mime.getCount() == 1) {
				/* It is just a plain text so it is processed automatically */
				mdnType = MdnType.PROCESSED;
			}	
			else {
				/* Determine the MDN type to send based on the correctness of the SMIME */
				mdnType = smimeVerifier.checkSMIME(mime, partnerId);
					
			}
			mdn.addBodyPart(createDescrPart(mdnType));
			mdn.addBodyPart(createDispositionPart((String) message.getProperty("message-id", PropertyScope.INBOUND), (String) message.getProperty("as2-to", PropertyScope.INBOUND), mdnType));
		
			String BOUNDARY_STRING = "----=_Part_" + RandomStringUtils.randomAlphanumeric(20);
			
			/* Set HTTP Headers */
			message.setProperty(AS2Constants.HEADER_CONTENT_TYPE, "multipart/report; report-type=disposition-notification; boundary=\"" + BOUNDARY_STRING + "\"", PropertyScope.OUTBOUND);
			message.setProperty(AS2Constants.HEADER_TO, message.getProperty(AS2Constants.HEADER_FROM, PropertyScope.INBOUND), PropertyScope.OUTBOUND);
			message.setProperty(AS2Constants.HEADER_MESSAGE_ID,"_" + RandomStringUtils.randomAlphanumeric(4), PropertyScope.OUTBOUND);
			message.setProperty("mime-version", "1.0", PropertyScope.OUTBOUND);
			message.setProperty(AS2Constants.HEADER_FROM, message.getProperty(AS2Constants.HEADER_TO, PropertyScope.INBOUND), PropertyScope.OUTBOUND);
		
			message.setProperty("mdnType", mdnType, PropertyScope.INBOUND);
			
			message.setProperty(AS2Constants.HEADER_CONTENT_TYPE, "multipart/report; report-type=disposition-notification;" + getBoundary(mdn.getContentType()), PropertyScope.OUTBOUND);
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			mdn.writeTo(baos);
			message.setPayload(baos.toByteArray());
			
		} catch (MessagingException e) {
			throw new TransformerException(CoreMessages.failedToCreate("MDN Message"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 				
			
		
		return message;
	}
	
	
	/* Generate Description Body part of the MDN */
	private MimeBodyPart createDescrPart(MdnType mdnType) throws MessagingException {

		MimeBodyPart descrBodyPart = new MimeBodyPart();
		descrBodyPart.setContent(mdnType.getMdnMessage(), AS2Constants.HEADER_TEXT_PLAIN);
		descrBodyPart.setHeader(AS2Constants.HEADER_CONTENT_TYPE, AS2Constants.HEADER_TEXT_PLAIN);
		descrBodyPart.setHeader(AS2Constants.HEADER_CONTENT_TRANSFER_ENCODING, AS2Constants.HEADER_7_BIT);
		
		return descrBodyPart;
	}
	
	
	/* Generate Disposition Body part of the MDN */
	private MimeBodyPart createDispositionPart(String originalMessageId, String originalRecipient, MdnType mdnType) throws MessagingException {
		
		MimeBodyPart mimeDisposition = new MimeBodyPart();			
		mimeDisposition.setContent(	ORIGINAL_RECIPIENT + originalRecipient + "\r\n" +
									FINAL_RECIPIENT + originalRecipient + "\r\n" +
									ORIGINAL_MESSAGE_ID + originalMessageId + "\r\n" +
									DISPOSITION + mdnType.getDispModExtension(), DISP_CONTENT_TYPE);
		
		mimeDisposition.setHeader(AS2Constants.HEADER_CONTENT_TYPE, "message/disposition-notification");	
		mimeDisposition.addHeader(AS2Constants.HEADER_CONTENT_TRANSFER_ENCODING, AS2Constants.HEADER_7_BIT);
		
		return mimeDisposition;
		
	}
	
	
    /**
     * Find boundary string
     * */
    private String getBoundary(String contentType) {
        String boundaryString = "";
    	               
        Pattern pattern = Pattern.compile(BOUNDARY_STRING_PATTERN);
        Matcher matcher = pattern.matcher(contentType);
        
        while (matcher.find()) {
            boundaryString = matcher.group();
	        }
			
        return boundaryString;
    }
    
    /**
     * Identify MDN Type of a MDN message
     * @throws MessagingException 
     * @throws IOException 
     * 
     * */
    public static MdnType identifyMdnType(MimeMultipart mdn) throws Exception {    	
    	
    	MimeBodyPart mdnDispBodyPart = (MimeBodyPart) mdn.getBodyPart(1);
    	String mdnDispContentAsString = IOUtils.toString((InputStream) mdnDispBodyPart.getContent());
    	
    	for(MdnType tmpMdnType: MdnType.values()) {
    		/* TODO maybe this check should be improved */
    		if (mdnDispContentAsString.contains(tmpMdnType.getDispModExtension())) {
    			return tmpMdnType;
    		}
    	}
    	
    	throw new Exception("MDN Type not recognized");
    }
    
    /**
     * Create a MDN from an input stream 
     * 
     * */
    public static  MimeMultipart createMDNFromResponse(InputStream mdnInputStream, String contentType) throws MessagingException {
    	MimeMultipart mdn = null;
    	mdn = new MimeMultipart(new InputStreamDataSource(mdnInputStream, contentType));
    	return mdn;
    }
    
    /**
     * Wrapper class of an InputStream
     * 
     * */
    static class InputStreamDataSource implements DataSource {

		private InputStream inputStream;
		private String contentType;
		
		public InputStreamDataSource(InputStream inputStream, String contentType) {
			this.inputStream = inputStream;
			this.contentType = contentType;
		}
		
		@Override
		public String getContentType() {
			// TODO Auto-generated method stub
			return "multipart/signed; protocol=\"application/pkcs7-signature\"; micalg=sha1;";
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return inputStream;
		}

		@Override
		public String getName() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public OutputStream getOutputStream() throws IOException {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
    
    

}
