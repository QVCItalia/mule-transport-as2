package org.mule.transport.as2.beans;

import java.io.Serializable;

public class PartnerConfiguration implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String partnerId;
	private String connectionMode;
	private String securityLevel;
	private String mdnRequest;
	
	public PartnerConfiguration(String partnerId, String connectionMode, String securityLevel, String mdnRequest) {
		this.partnerId = partnerId;
		this.connectionMode = connectionMode;
		this.securityLevel = securityLevel;
		this.mdnRequest = mdnRequest;
	}
	
	public String getPartnerId() {
		return partnerId;
	}
	public void setPartnerId(String partnerId) {
		this.partnerId = partnerId;
	}
	public String getConnectionMode() {
		return connectionMode;
	}
	public void setConnectionMode(String connectionMode) {
		this.connectionMode = connectionMode;
	}
	public String getSecurityLevel() {
		return securityLevel;
	}
	public void setSecurityLevel(String securityLevel) {
		this.securityLevel = securityLevel;
	}
	public String getMdnRequest() {
		return mdnRequest;
	}
	public void setMdnRequest(String mdnRequest) {
		this.mdnRequest = mdnRequest;
	}
	
	
}
