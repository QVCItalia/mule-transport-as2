package org.mule.transport.as2.transformers;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Iterator;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.apache.log4j.Logger;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignerDigestMismatchException;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMESignedParser;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.mule.api.annotations.ContainsTransformerMethods;
import org.mule.api.annotations.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.as2.transformers.MDNBuilder.MdnType;
import org.mule.util.IOUtils;

@ContainsTransformerMethods
public class SMIMEVerifier 
{
		
	Logger log = Logger.getLogger(SMIMEVerifier.class);
	
	private String KEYSTORE_INSTANCE = "JKS";
	private String SIGN_ALGORITHM = "SHA1withRSA";
	private String PROVIDER_NAME = "BC";
	
	private String KEYSTORE_PATH = null;
	private String KEYSTORE_PASSWORD = null;
	
	private KeyStore keystore;

	public void setKEYSTORE_PATH(String kEYSTORE_PATH) {
		this.KEYSTORE_PATH = kEYSTORE_PATH;
	}

	public void setKEYSTORE_PASSWORD(String kEYSTORE_PASSWORD) {
		this.KEYSTORE_PASSWORD = kEYSTORE_PASSWORD;
	}

	
	public SMIMEVerifier(String KEYSTORE_PATH, String KEYSTORE_PASSWORD) throws TransformerException {
		
		try {
			this.KEYSTORE_PATH = KEYSTORE_PATH;
			this.KEYSTORE_PASSWORD = KEYSTORE_PASSWORD;

			/* Load up Keystore */
	    	Security.addProvider(new BouncyCastleProvider());	
			keystore = KeyStore.getInstance(KEYSTORE_INSTANCE);	
			keystore.load(IOUtils.getResourceAsStream(KEYSTORE_PATH, getClass()), (KEYSTORE_PASSWORD).toCharArray());
			
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new TransformerException(CoreMessages.failedToCreate(getName()));
		}
	}
	

	public MdnType checkSMIME(MimeMultipart smime, String alias)  {
		
		try {
							
			if (!verifySignature(smime, alias)) {
				return MdnType.AUTHENTIFICATION_FAILED;
			}

		} catch (CMSException e) {
			return MdnType.INTEGRITY_CHECK_FAILED;					
		
		} catch (Exception e){
			e.printStackTrace();
			return MdnType.UNEXPECTED_PROCESSING_ERROR;
		} 
		
		/* Everything is fine */
		return MdnType.PROCESSED;

	}
	
		
	
	/**
	 * Verify the correctness of the signature in the SMIME message
	 * @throws CMSException 
	 * @throws TransformerException 
	 * @throws InvalidSignatureException 
	 * @throws CMSSignerDigestMismatchException 
	 * 
	 * */
	private boolean verifySignature(MimeMultipart smime, String alias) throws CMSException, TransformerException  {
				
		try {

			/* Get Certificate */
			X509Certificate cert = (X509Certificate) keystore.getCertificate(alias);
			
			/* Set SMIME Parser */
			SMIMESignedParser s = new SMIMESignedParser(smime);
			ContentVerifierProvider verifier = new JcaContentVerifierProviderBuilder().setProvider(new BouncyCastleProvider()).build(cert);		
			SignerInformationVerifier signVerifier = new SignerInformationVerifier(verifier, new BcDigestCalculatorProvider());
			
			SignerInformationStore signers = s.getSignerInfos();
			Collection<SignerInformation> c = signers.getSigners();
			Iterator<SignerInformation> it = c.iterator();
			
			/* For each signer check the signature */
			while (it.hasNext()) {
				SignerInformation signer = it.next();
				
				/* If one of the signatures is not verified verification fails */
				if (!signer.verify(signVerifier)) {
					return false;
				}
	
			}
		} catch (CMSException e){
			e.printStackTrace();
			throw e;
		
		}  catch (Exception e) {
			e.printStackTrace();
			throw new TransformerException(CoreMessages.failedToCreate(getName()));
		}
		
				
		/* Verification ends successfully */
		return true;
	}
	
	private String getName(){
		return "SMIMEToPayload";
	}
	
	

}
