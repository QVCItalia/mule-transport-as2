package org.mule.transport.as2.transformers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;

public class ByteArraytoSMIME {

	/**
	 * Create a SMIME from a smime byte array or byte stream
	 * */
	public MimeMultipart transformMimeMessage(Object src, String contentType) throws TransformerException {
		
				
		try {	
	        if (src instanceof byte[])
	        {
	            byte[] bytes = (byte[]) src;
	            return new MimeMultipart(new InputStreamDataSource(new ByteArrayInputStream(bytes), contentType));

	        }
	        else if (src instanceof InputStream)
	        {
	        	return new MimeMultipart(new InputStreamDataSource((InputStream) src, contentType));
	        }
	        else
	        {
	            throw new TransformerException(CoreMessages.transformFailed("Byte Array", "SMIME"));
	            	
	        }
			
	        
		} 	catch (MessagingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new TransformerException(CoreMessages.transformFailed("Byte Array", "SMIME"));
			}
				
	}

	
	class InputStreamDataSource implements DataSource {

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
