/*******************************************************************************
 * Copyright 2016 General Electric Company.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.ge.predix.solsvc.blobstore.bootstrap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.ge.predix.entity.datafile.DataFile;
import com.ge.predix.solsvc.blobstore.bootstrap.api.BlobstoreClient;

/**
 * This is Core Object Store Service Class. Has methods for operating on the
 * Object Store.
 *
 * @since Feb 2015
 */
@Component
@ImportResource({ "classpath*:META-INF/spring/blobstore-bootstrap-scan-context.xml" })
@Profile("blobstore")
public class BlobstoreClientImpl implements BlobstoreClient {

	private Logger log = LoggerFactory.getLogger(BlobstoreClientImpl.class);

	/**
	 * Instance of BlobStore
	 */
	// @Autowired
	private AmazonS3Client s3Client = null;

	@Autowired
	private BlobstoreConfig blobstoreConfig;

	/**
	 * 
	 */
	public static final String APPLICATION_OCTET_STREAM = "application/octet-stream"; //$NON-NLS-1$

	/**
	 * -
	 */
	@SuppressWarnings({ "nls" })
	@PostConstruct
	public void postConstruct() {
		if (this.blobstoreConfig.getProxyHost() != null
				&& !"localhost".equals(this.blobstoreConfig.getProxyHost())) {
			ClientConfiguration cf = new ClientConfiguration();
			if (this.blobstoreConfig.getProxyHost() != null) {
				cf.setProxyHost(this.blobstoreConfig.getProxyHost());
			}
			cf.setProtocol(Protocol.HTTP);
			cf.setProxyHost(this.blobstoreConfig.getProxyHost());
			if (this.blobstoreConfig.getProxyPort() != null ) {
				cf.setProxyPort(Integer.parseInt(this.blobstoreConfig.getProxyPort()));
			}
			this.s3Client = new AmazonS3Client(
					new BasicAWSCredentials(this.blobstoreConfig.getAccessKeyId(), this.blobstoreConfig.getAccessKey()),
					cf);
		} else {
			this.s3Client = new AmazonS3Client(new BasicAWSCredentials(this.blobstoreConfig.getAccessKeyId(),
					this.blobstoreConfig.getAccessKey()));
		}

		this.s3Client.setEndpoint(this.blobstoreConfig.getUrl());
	}

	/**
	 * Adds a new Blob to the binded bucket in the Object Store
	 *
	 * @param obj
	 *            S3Object to be added
	 */
	@SuppressWarnings("nls")
	@Override
	public String saveBlob(DataFile data, Map<String, String> userObjectMetaData) {
		this.log.info("AccessKeyId : " + this.blobstoreConfig.getAccessKeyId());
		this.log.info("AccessKey : " + this.blobstoreConfig.getAccessKey());
		this.log.info("Bucket Name : " + this.blobstoreConfig.getBucketName());
		InitiateMultipartUploadResult initResponse = null;
		S3Object obj = null;
		InputStream is = null;
		try {
			obj = new S3Object();
			obj.setKey(data.getName());

			is = IOUtils.toInputStream(new String((byte[]) data.getFile()));

			List<PartETag> partETags = new ArrayList<>();
			this.log.info("Creating Request for bucket : " + this.blobstoreConfig.getBucketName());
			InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(
					this.blobstoreConfig.getBucketName(), obj.getKey());
			try {
				initResponse = this.s3Client.initiateMultipartUpload(initRequest);
				int i = 1;
				int currentPartSize = 0;
				ByteArrayOutputStream tempBuffer = new ByteArrayOutputStream();
				int byteValue;
				while ((byteValue = is.read()) != -1) {
					tempBuffer.write(byteValue);
					currentPartSize = tempBuffer.size();
					if (currentPartSize == (50 * 1024 * 1024)) // make this a
																// const
					{
						byte[] b = tempBuffer.toByteArray();
						ByteArrayInputStream byteStream = new ByteArrayInputStream(b);

						UploadPartRequest uploadPartRequest = new UploadPartRequest()
								.withBucketName(this.blobstoreConfig.getBucketName()).withKey(obj.getKey())
								.withUploadId(initResponse.getUploadId()).withPartNumber(i++)
								.withInputStream(byteStream).withPartSize(currentPartSize);
						partETags.add(this.s3Client.uploadPart(uploadPartRequest).getPartETag());

						tempBuffer.reset();
					}
				}
				this.log.info("currentPartSize: " + currentPartSize);
				ObjectMetadata objectMetadata = new ObjectMetadata();
				objectMetadata.setContentLength(currentPartSize);
				objectMetadata.setUserMetadata(userObjectMetaData);
				obj.setObjectMetadata(objectMetadata);

				if (i == 1 && currentPartSize < (5 * 1024 * 1024)) // make this
																	// a const
				{
					this.s3Client.abortMultipartUpload(new AbortMultipartUploadRequest(
							this.blobstoreConfig.getBucketName(), obj.getKey(), initResponse.getUploadId()));

					byte[] b = tempBuffer.toByteArray();
					ByteArrayInputStream byteStream = new ByteArrayInputStream(b);
					objectMetadata.setContentType(getContentType(b));
					obj.setObjectMetadata(objectMetadata);

					PutObjectRequest putObjectRequest = new PutObjectRequest(this.blobstoreConfig.getBucketName(),
							obj.getKey(), byteStream, obj.getObjectMetadata());
					PutObjectResult result = this.s3Client.putObject(putObjectRequest);
					this.log.info("ETag : " + result.getETag());
					this.s3Client.getResourceUrl(this.blobstoreConfig.getBucketName(), obj.getKey());
					return initResponse.getUploadId();
				}

				if (currentPartSize > 0 && currentPartSize <= (50 * 1024 * 1024)) // make
																					// this
																					// a
																					// const
				{
					byte[] b = tempBuffer.toByteArray();
					ByteArrayInputStream byteStream = new ByteArrayInputStream(b);

					this.log.info("currentPartSize: " + currentPartSize);
					this.log.info("byteArray: " + b);

					UploadPartRequest uploadPartRequest = new UploadPartRequest()
							.withBucketName(this.blobstoreConfig.getBucketName()).withKey(obj.getKey())
							.withUploadId(initResponse.getUploadId()).withPartNumber(i).withInputStream(byteStream)
							.withPartSize(currentPartSize);
					partETags.add(this.s3Client.uploadPart(uploadPartRequest).getPartETag());
				}
			} catch (IOException e) {
				this.s3Client.abortMultipartUpload(new AbortMultipartUploadRequest(this.blobstoreConfig.getBucketName(),
						obj.getKey(), initResponse.getUploadId()));
				throw new RuntimeException(e);
			}
			CompleteMultipartUploadRequest completeMultipartUploadRequest = new CompleteMultipartUploadRequest()
					.withBucketName(this.blobstoreConfig.getBucketName()).withPartETags(partETags)
					.withUploadId(initResponse.getUploadId()).withKey(obj.getKey());

			this.s3Client.completeMultipartUpload(completeMultipartUploadRequest);
			return initResponse.getUploadId();
		} finally {
			if (obj != null) {
				try {
					obj.close();
				} catch (IOException e) {
					throw new RuntimeException("unable to close object obj=" + obj);
				}
			}
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					throw new RuntimeException("unable to close object obj=" + obj);
				}
			}
		}
	}

	@SuppressWarnings("nls")
	private String getContentType(byte[] b) {
		String contentType;
		try {
			contentType = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(b));
		} catch (IOException e) {
			if (this.log.isDebugEnabled()) {
				this.log.debug("unable to determine content type", e);
			}
			return APPLICATION_OCTET_STREAM;
		}
		if (this.log.isDebugEnabled()) {
			this.log.debug("content type inferred is" + contentType);
		}
		return contentType;
	}

	/**
	 * Get the Blob from the binded bucket
	 *
	 * @param fileName
	 *            String
	 */
	@SuppressWarnings("nls")
	@Override
	public DataFile getBlob(String fileName) {
		S3Object object = null;
		InputStream objectData = null;
		try {
			DataFile dataFile = new DataFile();
			object = this.s3Client.getObject(new GetObjectRequest(this.blobstoreConfig.getBucketName(), fileName));
			objectData = object.getObjectContent();
			dataFile.setFile(IOUtils.toByteArray(objectData));
			dataFile.setName(object.getKey());
			return dataFile;
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (object != null) {
				try {
					object.close();
				} catch (IOException e) {
					throw new RuntimeException("unable to close object object=" + object);
				}
			}
			if (objectData != null) {
				try {
					objectData.close();
				} catch (IOException e) {
					throw new RuntimeException("unable to close objectData objectData=" + objectData);
				}
			}

		}
	}

	// public AccessControlList getObjectACL()

	@SuppressWarnings("nls")
	/**
	 * Gets the list of available Blobs for the binded bucket from the
	 * BlobStore.
	 *
	 * @return List<BlobFile> List of Blobs
	 */
	@Override
	public List<DataFile> getBlob() {
		S3Object obj = null;
		try {
			List<DataFile> objs = new ArrayList<DataFile>();
			// Get the List from BlobStore
			ObjectListing objectList = this.s3Client.listObjects(this.blobstoreConfig.getBucketName());

			for (S3ObjectSummary objectSummary : objectList.getObjectSummaries()) {
				obj = this.s3Client
						.getObject(new GetObjectRequest(this.blobstoreConfig.getBucketName(), objectSummary.getKey()));
				DataFile data = new DataFile();
				data.setFile(IOUtils.toByteArray(obj.getObjectContent()));
				objs.add(data);
			}
			return objs;
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (obj != null) {
				try {
					obj.close();
				} catch (IOException e) {
					throw new RuntimeException("unable to close object object=" + obj + " throwing original exception",
							e);
				}
			}
		}
	}

	/**
	 * Delete the Blob from the binded bucket
	 *
	 * @param fileName
	 *            String of file to be removed
	 */
	@SuppressWarnings("nls")
	@Override
	public void deleteBlob(String fileName) {
		this.s3Client.deleteObject(this.blobstoreConfig.getBucketName(), fileName);
		if (this.log.isDebugEnabled())
			this.log.debug("delete(): Successfully deleted the file = " + fileName);
	}
}
