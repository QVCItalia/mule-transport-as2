package org.mule.transport.as2.transformers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Properties;

import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMESignedGenerator;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transport.as2.transformers.MDNBuilder.MdnType;

public class MDNBuilderUnitTest extends AbstractMuleContextTestCase{
	
	private final static String KEYSTORE_INSTANCE = "JKS";
	private final static String KEYSTORE_PATH = "src/test/resources/keystoreTest.jks";
	private final static String KEYSTORE_PASSWORD = "PASSWORD";
	private final static String PARTNER_ALIAS = "mend";
	private final static String PRIVATE_KEY_ALIAS = "mule";
	private final static String MESSAGE_ID = "test_message_123";
	
	private final static String SIGN_ALGORITHM = "SHA1withRSA";
	private final static String PROVIDER_NAME = "BC";
	
	private static MDNBuilder mdnBuilder;
	
	private static MimeMultipart TEST_CASE1;
	
	private static MimeBodyPart TEST_CASE1_TEXT;
	private static MimeBodyPart TEST_CASE1_DISPOSITION;
	
	private static MimeBodyPart TEST_CASE2_TEXT;
	private static MimeBodyPart TEST_CASE2_DISPOSITION;
	
	private static MimeMultipart TEST_CASE3;

	@BeforeClass
	public static void setUp() {

		
		try {	
        	/* Set up Keys */
        	Security.addProvider(new BouncyCastleProvider());
        	
        	KeyStore keystore = KeyStore.getInstance(KEYSTORE_INSTANCE);
        	keystore.load(new FileInputStream(KEYSTORE_PATH), KEYSTORE_PASSWORD.toCharArray());
        	PrivateKey privateKey = (PrivateKey) keystore.getKey(PRIVATE_KEY_ALIAS, KEYSTORE_PASSWORD.toCharArray());
			X509Certificate cert = (X509Certificate) keystore.getCertificate(PRIVATE_KEY_ALIAS);

        			
			/* Set up MIME Message */
			Properties props = new Properties();
			Session session = Session.getInstance(props, null);					
			final MimeMessage message = new MimeMessage(session);
			
			/* Set up SMIME payload */
			MimeBodyPart payload = new MimeBodyPart();
			payload.attachFile(new File("src/test/resources/input.xml"));
			payload.setHeader(AS2Constants.HEADER_CONTENT_TYPE, "application/XML");
			payload.setHeader(AS2Constants.HEADER_CONTENT_TRANSFER_ENCODING, "binary");
			
			/* Set up signer */
			SMIMESignedGenerator gen = new SMIMESignedGenerator();			
			ContentSigner signer = new JcaContentSignerBuilder(SIGN_ALGORITHM).setProvider(PROVIDER_NAME).build(privateKey);
			DigestCalculatorProvider dcp =  new JcaDigestCalculatorProviderBuilder().setProvider(PROVIDER_NAME).build();	
			JcaSignerInfoGeneratorBuilder sigb = new JcaSignerInfoGeneratorBuilder(dcp);
			SignerInfoGenerator sig = sigb.build(signer, (X509Certificate)cert);
			gen.addSignerInfoGenerator(sig);
			
			/* Generate SMIME content */
			TEST_CASE1 = gen.generate(payload);
			
			
			mdnBuilder = new MDNBuilder(KEYSTORE_PATH, KEYSTORE_PASSWORD);
			/* Expected processed MDN */
			TEST_CASE1_TEXT = new MimeBodyPart();
			TEST_CASE1_TEXT.setContent(new String("File received correctly"), "text/plain");
			TEST_CASE1_TEXT.setHeader(AS2Constants.HEADER_CONTENT_TYPE, "text/plain");
			TEST_CASE1_TEXT.setHeader(AS2Constants.HEADER_CONTENT_TRANSFER_ENCODING, "7bit");
			
			TEST_CASE1_DISPOSITION = new MimeBodyPart();			
			TEST_CASE1_DISPOSITION.setContent(new String("Original-Recipient: rfc822; " + PARTNER_ALIAS + "\r\n" +
										"Final-Recipient: rfc822; " + PARTNER_ALIAS + "\r\n" +
										"Original-Message-ID: " + MESSAGE_ID + "\r\n" +
										"Disposition: automatic-action/MDN-sent-automatically; processed"), "message/disposition-notification");
			
			TEST_CASE1_DISPOSITION.setHeader(AS2Constants.HEADER_CONTENT_TYPE, "message/disposition-notification");	
			TEST_CASE1_DISPOSITION.addHeader(AS2Constants.HEADER_CONTENT_TRANSFER_ENCODING, "7bit");
			
			/* Expected Errored MDN */
			TEST_CASE2_TEXT = new MimeBodyPart();
			TEST_CASE2_TEXT.setContent(new String("An exception occurred server side"), "text/plain");
			TEST_CASE2_TEXT.setHeader(AS2Constants.HEADER_CONTENT_TYPE, "text/plain");
			TEST_CASE2_TEXT.setHeader(AS2Constants.HEADER_CONTENT_TRANSFER_ENCODING, "7bit");
			
			TEST_CASE2_DISPOSITION = new MimeBodyPart();			
			TEST_CASE2_DISPOSITION.setContent(new String("Original-Recipient: rfc822; " + PARTNER_ALIAS + "\r\n" +
										"Final-Recipient: rfc822; "+ PARTNER_ALIAS + "\r\n" +
										"Original-Message-ID: " + "test_message_123" + "\r\n" +
										"Disposition: automatic-action/MDN-sent-automatically; processed/error: unexpected-processing-error"), "message/disposition-notification");
			
			TEST_CASE2_DISPOSITION.setHeader(AS2Constants.HEADER_CONTENT_TYPE, "message/disposition-notification");	
			TEST_CASE2_DISPOSITION.addHeader(AS2Constants.HEADER_CONTENT_TRANSFER_ENCODING, "7bit");
			
			TEST_CASE3 = new MimeMultipart();
			TEST_CASE3.addBodyPart(TEST_CASE1_TEXT);
			TEST_CASE3.addBodyPart(TEST_CASE1_DISPOSITION);
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
	}
	
	
	/**
	 * Test creation of processed MDN based on isValidSignature session variable
	 * 
	 * */
	@Test
	public void testProcessedMDN() {
		
		try {
			
			DefaultMuleMessage message = new DefaultMuleMessage(TEST_CASE1, muleContext);
			message.setProperty(AS2Constants.HEADER_FROM, PRIVATE_KEY_ALIAS, PropertyScope.INBOUND);
			message.setProperty(AS2Constants.HEADER_TO, PARTNER_ALIAS, PropertyScope.INBOUND);
			message.setProperty(AS2Constants.HEADER_MESSAGE_ID, MESSAGE_ID, PropertyScope.INBOUND);
		
			Object actualObject = mdnBuilder.createMDN(message, PARTNER_ALIAS);
			
			assertNotNull(actualObject);
			assertTrue(actualObject instanceof MuleMessage);
			MuleMessage actualMuleMessage = (MuleMessage) actualObject;
			InputStream mdnStream = new ByteArrayInputStream(actualMuleMessage.getPayloadAsBytes());
			
			MimeMultipart actualMime = new MimeMultipart(new InputStreamDataSource((InputStream) mdnStream,"multipart/report; report-type=disposition-notification"));
						
			MimeBodyPart actualTextBodyPart = (MimeBodyPart) actualMime.getBodyPart(0);
			assertEquals(TEST_CASE1_TEXT.getHeader(AS2Constants.HEADER_CONTENT_TYPE)[0], actualTextBodyPart.getHeader("Content-Type")[0]);
			assertEquals(TEST_CASE1_TEXT.getHeader("Content-Transfer-Encoding")[0], actualTextBodyPart.getHeader("Content-Transfer-Encoding")[0]);
			assertEquals(TEST_CASE1_TEXT.getContent(), actualTextBodyPart.getContent());
			
			MimeBodyPart actualDispositionBodyPart = (MimeBodyPart) actualMime.getBodyPart(1);
			assertEquals(TEST_CASE1_DISPOSITION.getHeader("Content-Type")[0], actualDispositionBodyPart.getHeader("Content-Type")[0]);
			assertEquals(TEST_CASE1_DISPOSITION.getHeader("Content-Transfer-Encoding")[0], actualDispositionBodyPart.getHeader("Content-Transfer-Encoding")[0]);
			
			StringWriter writer = new StringWriter();
			IOUtils.copy((InputStream) actualDispositionBodyPart.getContent(), writer, "UTF-8");			
			assertEquals(TEST_CASE1_DISPOSITION.getContent(), writer.toString());

						
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
	}
	
	/**
	 * Test creation of errored MDN 
	 * @throws TransformerException 
	 * 
	 * */
	@Test(expected=TransformerException.class)
	public void testErroredMDN() throws TransformerException {
		
		
			DefaultMuleMessage message = new DefaultMuleMessage("", muleContext);		
			Object actualObject = mdnBuilder.createMDN(message, null);
			
			fail("A Transformer Exception should be thrown");
	
	}
	
	@Test
	public void testMDNCreationFromResponse(){
		
		try {
			
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			TEST_CASE3.writeTo(out);
			ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
			
			
			Object object = MDNBuilder.createMDNFromResponse(in, "");
			
			assertNotNull(object);
			assertTrue(object instanceof MimeMultipart);
			MimeMultipart actualMdn = (MimeMultipart) object;
			
			MimeBodyPart actualTextBodyPart = (MimeBodyPart) actualMdn.getBodyPart(0);
			assertEquals(TEST_CASE1_TEXT.getHeader(AS2Constants.HEADER_CONTENT_TYPE)[0], actualTextBodyPart.getHeader("Content-Type")[0]);
			assertEquals(TEST_CASE1_TEXT.getHeader("Content-Transfer-Encoding")[0], actualTextBodyPart.getHeader("Content-Transfer-Encoding")[0]);
			assertEquals(TEST_CASE1_TEXT.getContent(), actualTextBodyPart.getContent());
			
			MimeBodyPart actualDispositionBodyPart = (MimeBodyPart) actualMdn.getBodyPart(1);
			assertEquals(TEST_CASE1_DISPOSITION.getHeader("Content-Type")[0], actualDispositionBodyPart.getHeader("Content-Type")[0]);
			assertEquals(TEST_CASE1_DISPOSITION.getHeader("Content-Transfer-Encoding")[0], actualDispositionBodyPart.getHeader("Content-Transfer-Encoding")[0]);
			
			StringWriter writer = new StringWriter();
			IOUtils.copy((InputStream) actualDispositionBodyPart.getContent(), writer, "UTF-8");			
			assertEquals(TEST_CASE1_DISPOSITION.getContent(), writer.toString());
			
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MessagingException e) {
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
