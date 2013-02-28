package org.mule.transport.as2.transformers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Properties;

import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

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
import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transport.as2.transformers.MDNBuilder.MdnType;

public class SMIMEVerifierUnitTest extends AbstractMuleContextTestCase {

	private static MimeMultipart TEST_CASE1;
	
	private static SMIMEVerifier smimeVerifier;
	
	private final static String KEYSTORE_INSTANCE = "JKS";
	private final static String KEYSTORE_PATH = "src/test/resources/keystoreTest.jks";
	private final static String KEYSTORE_PASSWORD = "PASSWORD";
	private final String PARTNER_ALIAS = "mend";
	private final static String PRIVATE_KEY_ALIAS = "mule";
	private final static String SIGN_ALGORITHM = "SHA1withRSA";
	private final static String PROVIDER_NAME = "BC";
	
	
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
			payload.setHeader("Content-Type", "application/XML");
			payload.setHeader("Content-Transfer-Encoding", "binary");
			
			/* Set up signer */
			SMIMESignedGenerator gen = new SMIMESignedGenerator();			
			ContentSigner signer = new JcaContentSignerBuilder(SIGN_ALGORITHM).setProvider(PROVIDER_NAME).build(privateKey);
			DigestCalculatorProvider dcp =  new JcaDigestCalculatorProviderBuilder().setProvider(PROVIDER_NAME).build();	
			JcaSignerInfoGeneratorBuilder sigb = new JcaSignerInfoGeneratorBuilder(dcp);
			SignerInfoGenerator sig = sigb.build(signer, (X509Certificate)cert);
			gen.addSignerInfoGenerator(sig);
			
			/* Generate SMIME content */
			TEST_CASE1 = gen.generate(payload);
						
			/* Set up Transformer */			
			smimeVerifier = new SMIMEVerifier(KEYSTORE_PATH, KEYSTORE_PASSWORD);			
			
		} catch(Exception e) {
			e.printStackTrace();
			fail();
		}
		
	}
	
	
	@Test
	public void testSMIMEToPayloadValidSign(){
		
		try {
//			DefaultMuleMessage message = new DefaultMuleMessage(TEST_CASE1, muleContext);
			
			/* Kind of event handler */
//			MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.REQUEST_RESPONSE, getTestService());
			
			Object object = smimeVerifier.checkSMIME(TEST_CASE1, PARTNER_ALIAS);
			
			assertNotNull(object);
			assertTrue(object instanceof MdnType);
			MdnType actualObject = (MdnType) object;
			assertEquals(MdnType.PROCESSED, actualObject);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
	}
	
	/**
	 * Test the detection of an invalid signature
	 * @throws Exception 
	 * 
	 * */
	@Test
	public void testSMIMEToPayloadInvalidSign() throws Exception{

			
			/* Man in the middle attack */
			/* Payload substitution while keeping the original signature */
			MimeBodyPart attackPayload = new MimeBodyPart();
			attackPayload.attachFile(new File("src/test/resources/inputAttack.xml"));
			attackPayload.setHeader("content-type", "application/XML");
			attackPayload.setHeader("Content-Transfer-Encoding", "binary");
			TEST_CASE1.removeBodyPart(0);
			TEST_CASE1.addBodyPart(attackPayload, 0);
					
//			DefaultMuleMessage message = new DefaultMuleMessage(TEST_CASE1, muleContext);
//			
//			/* Kind of event handler */
//			MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.REQUEST_RESPONSE, getTestService());
			
			Object object = smimeVerifier.checkSMIME(TEST_CASE1, PARTNER_ALIAS);
			
			assertNotNull(object);
			assertTrue(object instanceof MdnType);
			MdnType actualObject = (MdnType) object;
			assertEquals(MdnType.INTEGRITY_CHECK_FAILED, actualObject);
	}
	
	@Test
	public void testNullToPayload() throws TransformerException {
						
			Object object = smimeVerifier.checkSMIME(null, null);
			assertNotNull(object);
			assertTrue(object instanceof MdnType);
			MdnType actualObject = (MdnType) object;
			assertEquals(MdnType.UNEXPECTED_PROCESSING_ERROR, actualObject);
			
	}
	
}
