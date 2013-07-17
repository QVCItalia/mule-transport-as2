package org.mule.transport.as2.transformers;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.xml.bind.DatatypeConverter;


import org.apache.commons.codec.binary.Hex;
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
		log.debug("DBG: inside " + getClass() + ".SMIMEVerifier()");
		try {
			this.KEYSTORE_PATH = KEYSTORE_PATH;
			this.KEYSTORE_PASSWORD = KEYSTORE_PASSWORD;

			/* Load up Keystore */
	    	Security.addProvider(new BouncyCastleProvider());	
			keystore = KeyStore.getInstance(KEYSTORE_INSTANCE);	
			keystore.load(IOUtils.getResourceAsStream(KEYSTORE_PATH, getClass()), (KEYSTORE_PASSWORD).toCharArray());
			
		} catch (Exception e) {
			log.error(e, e);
			throw new TransformerException(CoreMessages.failedToCreate(getName()));
		}
	}
	

	public synchronized MdnType checkSMIME(MimeMultipart smime, String alias)  {
		log.debug("DBG: inside " + getClass() + ".checkSMIME()");
		
		log.debug("DBG: alias is: " + alias);

		
		
		try {
							
			if (!verifySignature(smime, alias)) {
				log.debug("MdnType is: AUTHENTIFICATION_FAILED");
				return MdnType.AUTHENTIFICATION_FAILED;
			}

		} catch (CMSException e) {
			log.debug("MdnType is: INTEGRITY_CHECK_FAILED");
			return MdnType.INTEGRITY_CHECK_FAILED;	
		
		} catch (Exception e){
			log.error(e,e);
			log.debug("MdnType is UNEXPECTED_PROCESSING_ERROR");
			return MdnType.UNEXPECTED_PROCESSING_ERROR;
		} 
		
		/* Everything is fine */
		log.debug("MdnType is: PROCESSED");
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
	private synchronized boolean verifySignature(MimeMultipart smime, String alias) throws CMSException, TransformerException  {
						
		try {

			/* Get Certificate */
			X509Certificate cert = (X509Certificate) keystore.getCertificate(alias);
			log.debug("DBG: local certificate information: ");
			log.debug("DBG: Issuer: " + cert.getIssuerX500Principal().getName());
			log.debug("DBG: Serial number: " + Long.toHexString(cert.getSerialNumber().longValue()));
			log.debug("DBG: Digest Alg " + cert.getSigAlgOID());
			
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
				log.debug("DBG: signer Issuer: " + signer.getSID().getIssuerAsString());
				log.debug("DBG: signer Serial Number: " + Long.toHexString(signer.getSID().getSerialNumber().longValue()));
				log.debug("DBG: signer Digest Alg " + signer.getDigestAlgOID());
				/* If one of the signatures is not verified verification fails */
				if (!signer.verify(signVerifier)) {
					log.debug("DBG: signature is not verified");
					return false;
				}
	
			}
		} catch (CMSException e){
			log.error(e, e);
			throw e;
		
		}  catch (KeyStoreException e) {
			log.error(e, e);
			throw new TransformerException(CoreMessages.failedToCreate(getName()));
		}  catch (Exception e) {
			log.error(e, e);
			throw new TransformerException(CoreMessages.failedToCreate(getName()));
		}
		
				
		/* Verification ends successfully */
		return true;
	}
	
	private String getName(){
		return "SMIMEToPayload";
	}
	
	

	

}
