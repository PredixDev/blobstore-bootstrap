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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

	/**
     * 
     */
    Log log = LogFactory.getLog(BlobstoreClientImpl.class);

    /**
     * Instance of BlobStore
     */
    private AmazonS3Client s3Client;

   @Autowired
   private BlobstoreConfig blobstoreConfig;

    /**
     * Serverside Encryption
     */
    private boolean enableSSE;

    /**
     * 
     */
    public static final String APPLICATION_OCTET_STREAM = "application/octet-stream"; //$NON-NLS-1$

    /**
     *  -
     */
    @PostConstruct
    public void init(){
    	
        ClientConfiguration config = new ClientConfiguration();
        config.setProtocol(Protocol.HTTPS);
        if (this.blobstoreConfig.getProxyHost() != null && !"".equals(this.blobstoreConfig.getProxyHost())) { //$NON-NLS-1$
			this.log.info("Connnecting with proxy"); //$NON-NLS-1$
			if (this.blobstoreConfig.getProxyHost() != null) {
				config.withProxyHost(this.blobstoreConfig.getProxyHost());
			}
			if (this.blobstoreConfig.getProxyPort() != null) {
				config.withProxyPort(Integer.parseInt(this.blobstoreConfig.getProxyPort()));
			}
		}
        BasicAWSCredentials creds = new BasicAWSCredentials(this.blobstoreConfig.getAccessKey(), this.blobstoreConfig.getAccessKey());
        this.s3Client = new AmazonS3Client(creds, config);
        this.s3Client.setEndpoint(this.blobstoreConfig.getUrl());

    }
   

    /**
     * Adds a new Blob to the binded bucket in the Object Store
     *
     * @param obj S3Object to be added
     */
    @Override
	public String saveBlob(S3Object obj) {
        if (obj == null) {
            this.log.error("put(): Empty file provided"); //$NON-NLS-1$
            throw new RuntimeException("File is null"); //$NON-NLS-1$
        }
        List<PartETag> partETags = new ArrayList<>();
        String bucket = this.blobstoreConfig.getBucketName();
        InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucket, obj.getKey());
        InitiateMultipartUploadResult initResponse = this.s3Client.initiateMultipartUpload(initRequest);
        try (InputStream is = obj.getObjectContent();){

            int i = 1;
            int currentPartSize = 0;
            ByteArrayOutputStream tempBuffer = new ByteArrayOutputStream();
            int byteValue;
            while ((byteValue = is.read()) != -1) {
                tempBuffer.write(byteValue);
                currentPartSize = tempBuffer.size();
                if (currentPartSize == (50 * 1024 * 1024)) //make this a const
                {
                    byte[] b = tempBuffer.toByteArray();
                    ByteArrayInputStream byteStream = new ByteArrayInputStream(b);

                    UploadPartRequest uploadPartRequest = new UploadPartRequest()
                            .withBucketName(bucket).withKey(obj.getKey())
                            .withUploadId(initResponse.getUploadId()).withPartNumber(i++)
                            .withInputStream(byteStream)
                            .withPartSize(currentPartSize);
                    partETags.add(this.s3Client.uploadPart(uploadPartRequest).getPartETag());

                    tempBuffer.reset();
                }
            }
            this.log.info("currentPartSize: " + currentPartSize); //$NON-NLS-1$
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(currentPartSize);
            if (this.enableSSE) {
                objectMetadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
            }
            obj.setObjectMetadata(objectMetadata);

            if (i == 1 && currentPartSize < (5 * 1024 * 1024)) // make this a const
            {
                this.s3Client.abortMultipartUpload(new AbortMultipartUploadRequest(
                        bucket, obj.getKey(), initResponse.getUploadId()
                ));

                byte[] b = tempBuffer.toByteArray();
                ByteArrayInputStream byteStream = new ByteArrayInputStream(b);
                objectMetadata.setContentType(getContentType(b));
                if (this.enableSSE) {
                    objectMetadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
                }
                obj.setObjectMetadata(objectMetadata);

                PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, obj.getKey(), byteStream, obj.getObjectMetadata());
                this.s3Client.putObject(putObjectRequest);

                ObjectMetadata meta = this.s3Client.getObjectMetadata(bucket, obj.getKey());
                Map<String, Object> headers = meta.getRawMetadata();
                for (Map.Entry<String, Object> entry : headers.entrySet()) {
                    this.log.info("Object Metadata -- " + entry.getKey() + ": " + entry.getValue().toString()); //$NON-NLS-1$ //$NON-NLS-2$
                }

                return initResponse.getUploadId();
            }

            if (currentPartSize > 0 && currentPartSize <= (50 * 1024 * 1024)) // make this a const
            {
                byte[] b = tempBuffer.toByteArray();
                ByteArrayInputStream byteStream = new ByteArrayInputStream(b);

                this.log.info("currentPartSize: " + currentPartSize); //$NON-NLS-1$
                this.log.info("byteArray: " + b); //$NON-NLS-1$

                UploadPartRequest uploadPartRequest = new UploadPartRequest()
                        .withBucketName(bucket).withKey(obj.getKey())
                        .withUploadId(initResponse.getUploadId()).withPartNumber(i)
                        .withInputStream(byteStream)
                        .withPartSize(currentPartSize);
                partETags.add(this.s3Client.uploadPart(uploadPartRequest).getPartETag());
            }


            CompleteMultipartUploadRequest completeMultipartUploadRequest = new CompleteMultipartUploadRequest()
                    .withBucketName(bucket)
                    .withPartETags(partETags)
                    .withUploadId(initResponse.getUploadId())
                    .withKey(obj.getKey());

            this.s3Client.completeMultipartUpload(completeMultipartUploadRequest);
            return initResponse.getUploadId();
        } catch (Exception e) {
            this.log.error("put(): Exception occurred in put(): " + e.getMessage()); //$NON-NLS-1$
            this.s3Client.abortMultipartUpload(new AbortMultipartUploadRequest(
                    bucket, obj.getKey(), initResponse.getUploadId()
            ));
            throw new RuntimeException("put(): Exception occurred in put(): ",e); //$NON-NLS-1$
        }
    }

    private String getContentType(byte[] b) {
        String contentType;
        try {
            contentType = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(b));
        } catch (IOException e) {
            if (this.log.isDebugEnabled()) {
                this.log.debug("unable to determine content type", e); //$NON-NLS-1$
            }
            return APPLICATION_OCTET_STREAM;
        }
        if (this.log.isDebugEnabled()) {
            this.log.debug("content type inferred is"+contentType); //$NON-NLS-1$
        }
        return contentType;
    }

 
    @Override
	public DataFile getBlob(String fileName) {

    	try (S3Object object = this.s3Client
				.getObject(new GetObjectRequest(this.blobstoreConfig.getBucketName(), fileName));
				InputStream objectData = object.getObjectContent();) {
			DataFile dataFile = new DataFile();

			dataFile.setFile(IOUtils.toByteArray(objectData));
			dataFile.setName(fileName);
			return dataFile;

		} catch (Exception e) {
			this.log.error("Exception Occurred in get(): " + e.getMessage()); //$NON-NLS-1$
			throw new RuntimeException("Exception Occurred in get(): ", e); //$NON-NLS-1$
		}
    }

//    public AccessControlList getObjectACL()

    /**
     * Gets the list of available Blobs for the binded bucket from the BlobStore.
     *
     * @return List of String Blobs
     */
    @Override
    public List<String> getAvailableBlobs() {
        List<String> objs = new ArrayList<>();
        try {
            // Get the List from BlobStore
            ObjectListing objectList = this.s3Client.listObjects(this.blobstoreConfig.getBucketName());

            for (S3ObjectSummary objectSummary :
                    objectList.getObjectSummaries()) {

                objs.add(objectSummary.getKey());
            }

        } catch (Exception e) {
            this.log.error("Exception occurred in get(): " + e.getMessage()); //$NON-NLS-1$
            throw e;
        }

        return objs;
    }


    /**
     * Delete the Blob from the binded bucket
     *
     * @param fileName String of file to be removed
     */
    @Override
    public void deleteBlob(String fileName) {
        try {
            this.s3Client.deleteObject(this.blobstoreConfig.getBucketName(), fileName);
            if (this.log.isDebugEnabled())
                this.log.debug("delete(): Successfully deleted the file = " + fileName); //$NON-NLS-1$
        } catch (Exception e) {
            this.log.error("delete(): Exception Occurred in delete(): " + e.getMessage()); //$NON-NLS-1$
            throw e;
        }
    }


    /**
	 * Gets the list of available Blobs for the binded bucket from the
	 * BlobStore.
	 *
	 * @return List of DataFile Blobs
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
					throw new RuntimeException("unable to close object object=" + obj + " throwing original exception", //$NON-NLS-1$ //$NON-NLS-2$
							e);
				}
			}
		}
	}
}
