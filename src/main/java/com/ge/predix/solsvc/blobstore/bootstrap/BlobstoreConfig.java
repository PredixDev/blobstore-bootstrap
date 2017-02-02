package com.ge.predix.solsvc.blobstore.bootstrap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("blobstore")
public class BlobstoreConfig{
	Log log = LogFactory.getLog(BlobstoreConfig.class);
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
	
	@Value("${predix.oauth.proxyHost:${null}}")
	private String proxyHost;
	
	@Value("${predix.oauth.proxyPort:8080}")
	private String proxyPort;
	
	public String getAccessKeyId() {
		return accessKeyId;
	}

	public void setAccessKeyId(String accessKeyId) {
		this.accessKeyId = accessKeyId;
	}

	public String getBucketName() {
		return bucketName;
	}

	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}

	public String getBlobstoreHost() {
		return blobstoreHost;
	}

	public void setBlobstoreHost(String blobstoreHost) {
		this.blobstoreHost = blobstoreHost;
	}

	public String getAccessKey() {
		return accessKey;
	}

	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getProxyHost() {
		return proxyHost;
	}

	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	public String getProxyPort() {
		return proxyPort;
	}
	
	public void setProxyPort(String proxyPort) {
		this.proxyPort = proxyPort;
	}
}
