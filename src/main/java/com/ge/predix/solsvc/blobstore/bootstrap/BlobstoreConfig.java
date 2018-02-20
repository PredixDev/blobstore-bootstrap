package com.ge.predix.solsvc.blobstore.bootstrap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 
 * @author 212546387 -
 */
@Configuration
@Profile("blobstore")
public class BlobstoreConfig{
	@Value("${predix.blobstore.access.keyid}")
	private String accessKeyId;
	
	@Value("${predix.blobstore.bucket.name}")
	private String bucketName;
	
	@Value("${predix.blobstore.host}")
	private String blobstoreHost;
	
	@Value("${predix.blobstore.secret.access.key}")
	private String accessKey;
	
	@Value("${predix.blobstore.url}")
	private String url;
	
	@Value("${predix.oauth.proxyHost:#{null}}")
	private String proxyHost;
	
	@Value("${predix.oauth.proxyPort:8080}")
	private String proxyPort;
	
	@Value("${predix.oauth.certLocation:#{null}}")
	private String oauthCertLocation;
	
	@Value("${predix.oauth.certPassword:#{null}}")
	private String oauthCertPassword;
	
	/**
	 * @return -
	 */
	public String getAccessKeyId() {
		return this.accessKeyId;
	}

	/**
	 * @param accessKeyId -
	 */
	public void setAccessKeyId(String accessKeyId) {
		this.accessKeyId = accessKeyId;
	}

	/**
	 * @return -
	 */
	public String getBucketName() {
		return this.bucketName;
	}

	/**
	 * @param bucketName -
	 */
	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}

	/**
	 * @return -
	 */
	public String getBlobstoreHost() {
		return this.blobstoreHost;
	}

	/**
	 * @param blobstoreHost -
	 */
	public void setBlobstoreHost(String blobstoreHost) {
		this.blobstoreHost = blobstoreHost;
	}

	/**
	 * @return -
	 */
	public String getAccessKey() {
		return this.accessKey;
	}

	/**
	 * @param accessKey -
	 */
	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	/**
	 * @return -
	 */
	public String getUrl() {
		return this.url;
	}

	/**
	 * @param url -
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * @return -
	 */
	public String getProxyHost() {
		return this.proxyHost;
	}

	/**
	 * @param proxyHost -
	 */
	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	/**
	 * @return -
	 */
	public String getProxyPort() {
		return this.proxyPort;
	}
	
	/**
	 * @param proxyPort -
	 */
	public void setProxyPort(String proxyPort) {
		this.proxyPort = proxyPort;
	}

	/**
	 * @return -
	 */
	public String getOauthCertLocation() {
		return this.oauthCertLocation;
	}

	/**
	 * @param oauthCertLocation -
	 */
	public void setOauthCertLocation(String oauthCertLocation) {
		this.oauthCertLocation = oauthCertLocation;
	}

	/**
	 * @return -
	 */
	public String getOauthCertPassword() {
		return this.oauthCertPassword;
	}

	/**
	 * @param oauthCertPassword -
	 */
	public void setOauthCertPassword(String oauthCertPassword) {
		this.oauthCertPassword = oauthCertPassword;
	}
}
