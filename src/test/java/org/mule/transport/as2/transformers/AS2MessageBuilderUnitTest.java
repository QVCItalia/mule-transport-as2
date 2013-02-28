package org.mule.transport.as2.transformers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mule.tck.junit4.AbstractMuleTestCase;

import static org.junit.Assert.*;
import static org.junit.Assert.fail;


public class AS2MessageBuilderUnitTest extends AbstractMuleTestCase{
	
	static As2MessBuilder as2MessBuilder;
	
	String TEST_CASE1 = "<?xml version=\"1.0\"?><note>" 
						+ "<to>Tove</to>" 
						+ "<from>Jani</from>"
						+ "<heading>Reminder</heading>"
						+ "<body>Don't forget me this weekend!</body>"
						+ "</note>";
	
	String TEST_CASE1_FILENAME = "test.xml";
	String TEST_CASE1_CONTENT_DISPOSITION_STRING = "attachment; filename=" + TEST_CASE1_FILENAME;
	
	private final String KEYSTORE_INSTANCE = "JKS";
	private final static String KEYSTORE_PATH = "src/test/resources/keystoreTest.jks";
	private final static String KEYSTORE_PASSWORD = "PASSWORD";
	private final String PARTNER_ALIAS = "mend";
	private final static String PRIVATE_KEY_ALIAS = "mule";
	private final String MESSAGE_ID = "test_message_123";
	
	private final String SIGN_ALGORITHM = "SHA1withRSA";
	private final String PROVIDER_NAME = "BC";
	
	@BeforeClass
	public static void setUp() {
		try {
			as2MessBuilder = As2MessBuilder.getAs2MessBuilder(KEYSTORE_PATH, KEYSTORE_PASSWORD, PRIVATE_KEY_ALIAS);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail("Exception during set up");
		}
		
	}
	
	@Test
	public void testAS2NoEncrSigned() {
		
		Object object = as2MessBuilder.createAS2Signed(new ByteArrayInputStream(TEST_CASE1.getBytes()), TEST_CASE1_FILENAME);
		
		assertNotNull(object);
		assertTrue(object instanceof InputStream);
		InputStream actualMimeStream = (InputStream) object;
		
		try {
			MimeMultipart actualObject = new MimeMultipart(new InputStreamDataSource(actualMimeStream, ""));
			
			assertEquals(2, actualObject.getCount());
			assertTrue(actualObject.getBodyPart(0) instanceof MimeBodyPart);
			
			MimeBodyPart actualPayload = (MimeBodyPart) actualObject.getBodyPart(0);
			String actualStringPayload = IOUtils.toString((InputStream) actualPayload.getContent());
			assertEquals(TEST_CASE1, actualStringPayload);
			assertEquals(TEST_CASE1_CONTENT_DISPOSITION_STRING, actualPayload.getHeader(AS2Constants.HEADER_CONTENT_DISPOSITION)[0]);
			
			
			
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail("");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
			return "multipart/mixed";
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
