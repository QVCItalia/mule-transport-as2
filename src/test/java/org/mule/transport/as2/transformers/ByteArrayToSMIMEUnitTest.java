package org.mule.transport.as2.transformers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mule.DefaultMuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

public class ByteArrayToSMIMEUnitTest extends AbstractMuleContextTestCase {

	private static MimeMultipart TEST_CASE1;
	private final String expectedContentType = "multipart/signed; protocol=\"application/pkcs7-signature\"; micalg=sha1;";
	
	@BeforeClass
	public static void setUp() {
		try {
			TEST_CASE1 = new MimeMultipart();
			
			MimeBodyPart payload = new MimeBodyPart();
		
			payload.setHeader("Content-Type", "application/XML");
			payload.setText("This is a test");
			
			MimeBodyPart signature = new MimeBodyPart();
			signature.setHeader("Content-Type", "application/pkcs7-signature; name=smime.p7s; smime-type=signed-data");
			signature.setHeader("Content-Transfer", "base64");
			signature.setText("Signature");
			
			TEST_CASE1.addBodyPart(payload);
			TEST_CASE1.addBodyPart(signature);
			
			
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void testByteArrayToSMIME() {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			TEST_CASE1.writeTo(out);
			ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());

			ByteArraytoSMIME transformer = new ByteArraytoSMIME();
						
			Object object = transformer.transformMimeMessage(in, null);
			assertNotNull(object);
			assertTrue(object instanceof MimeMultipart);
			
			MimeMultipart mime = (MimeMultipart) object;
			
			assertEquals(TEST_CASE1.getBodyPart(0).getContent(), mime.getBodyPart(0).getContent());
			assertEquals(TEST_CASE1.getBodyPart(1).getContent(), mime.getBodyPart(1).getContent());
			
						
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
	}
	
	@Test(expected=TransformerException.class)
	public void testNullToSMIME() throws TransformerException {

			ByteArraytoSMIME transformer = new ByteArraytoSMIME();
						
			Object object = transformer.transformMimeMessage(null, null);
			fail("Transformer Exception should be thrown");
			
	}
	
	
}
