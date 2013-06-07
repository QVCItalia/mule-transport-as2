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
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMESignedGenerator;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;


/**
 * This Class is responsible for creating the AS2 Message which will be part
 * of the HTTP POST Body Request
 * 
 * */
public class As2MessBuilder {

	private final String keystorePath;
	private final String keystorePassword;
	private final String senderAlias;
	
	final String KEYSTORE_INSTANCE = "JKS";
	final String SIGN_ALGORITHM = "SHA1withRSA";
	final String PROVIDER_NAME = "BC";
	
	private static final String BOUNDARY_STRING_PATTERN = "boundary=.*";
	private String boundaryValue;
	
	public String getBoundaryValue() {
		return boundaryValue;
	}


	private KeyStore keystore;	
	private static As2MessBuilder as2MessBuilder;
	
	Logger log = Logger.getLogger(As2MessBuilder.class);
	
	private As2MessBuilder(String keystorePath, String keystorePassword, String senderAlias) throws Exception {
		
		this.keystorePath = keystorePath;
		this.keystorePassword = keystorePassword;
		this.senderAlias = senderAlias;
		
		try {
			log.debug("*** As2MessBuilder:  keystorePath: " + this.keystorePath);
			log.debug("*** As2MessBuilder:  keystorePassword: " + this.keystorePassword);
			log.debug("*** As2MessBuilder:  senderAlias: " + this.senderAlias);
			
			Security.addProvider(new BouncyCastleProvider());
	    	keystore = KeyStore.getInstance(KEYSTORE_INSTANCE);
	    	keystore.load(org.mule.util.IOUtils.getResourceAsStream(this.keystorePath, getClass()), (keystorePassword).toCharArray());
		
		} catch(Exception e) {
			log.error(e,e);
			throw new Exception("Exception creating As2MessBuilder");
		}
	}
	
	public static As2MessBuilder getAs2MessBuilder(String keystorePath, String keystorePassword, String senderAlias) throws Exception {
		if(as2MessBuilder == null) {
			as2MessBuilder = new As2MessBuilder(keystorePath, keystorePassword, senderAlias);
		}
		return as2MessBuilder;
	}
	
	/**
	 * Create an Input stream of the AS2 Message in the version Not Encrypted but Signed
	 * 
	 * */
    public InputStream createAS2Signed(final InputStream stream, final String fileName) 
    {
    				
		ByteArrayInputStream in = null;
		
        try {
        	/* Get the private key */
			PrivateKey privateKey = (PrivateKey) keystore.getKey(senderAlias, keystorePassword.toCharArray());
			X509Certificate cert = (X509Certificate) keystore.getCertificate(senderAlias);
        				
			/* Set up SMIME payload */
			MimeBodyPart payload = new MimeBodyPart();
			
			/* I need this step to make sure the file will be "attached" */
			saveInputStreamToFile(stream, fileName);
			payload.attachFile(new File(fileName));
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
			MimeMultipart smime = gen.generate(payload);
			
			/* Convert the MimeMultipart to an InputStream */
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			smime.writeTo(out);
			byte[] data = out.toByteArray();
			in = new ByteArrayInputStream(data);
			
			InputStream in2 = new ByteArrayInputStream(data);
			StringWriter writer = new StringWriter();
			IOUtils.copy(in2, writer);
			boundaryValue = getBoundary(writer.toString());
//			log.debug("DBG: boundary String is: " + boundaryValue);
			
//			boundaryValue="--123";
			
			/* Clean up */
			deleteTempFile(fileName);
			
        } catch (Exception e) {
        	log.error(e,e);
        }
        	
			return in;
        // Return plain text for testing in GEODIS
//        return new ByteArrayInputStream("TEST".getBytes());
        }
    
    
    /**
     * Save the input stream to a temp file
     */
    private void saveInputStreamToFile(InputStream inputStream, String fileName) {

    	File file = new File(fileName);
    	OutputStream outputStream;
		try {
			outputStream = new FileOutputStream(file);
	    	IOUtils.copy(inputStream, outputStream);
	    	outputStream.close();
	    	
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			log.error(e, e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error(e, e);
		}

    }
    
    
    /**
     * Delete temp file used to attach it to the AS2 message
     * */
    private void deleteTempFile(String fileName) {
    	
    	File file = new File(fileName);
    	file.delete();
    }
    
    
    /**
     * Find boundary string
     * */
    private String getBoundary(String smime) {
        log.debug("DBG: SMIME is: " + smime);
//    	String boundaryString = "";
//    	               
//        Pattern pattern = Pattern.compile(BOUNDARY_STRING_PATTERN);
//        Matcher matcher = pattern.matcher(smime);
//        
//        while (matcher.find()) {
//            boundaryString = matcher.group();
//	        }
			
        String [] lines = smime.split("\n");
        log.debug("DBG: boundary String is: " + lines[0].replace("\n", "").replace("\r", "").substring(2) + "<");
        return lines[0].replace("\n", "").replace("\r", "").substring(2);
    }
}
