/*
 * Copyright (c) 2015 General Electric Company. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * General Electric Company. The software may be used and/or copied only
 * with the written permission of General Electric Company or in accordance
 * with the terms and conditions stipulated in the agreement/contract
 * under which the software has been supplied.
 */

package com.ge.predix.solsvc.fdh.handler.blobstore;

import java.io.CharArrayReader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.FileSystemResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.s3.model.S3Object;
import com.ge.predix.entity.datafile.DataFile;
import com.ge.predix.solsvc.blobstore.bootstrap.api.BlobstoreClient;

/**
 * 
 * 
 * @author 212438846
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath*:META-INF/spring/TEST-blobstore-bootstrap-properties-context.xml" })
//@IntegrationTest({ "server.port=0" })
@ComponentScan(basePackages = { "com.ge.predix.solsvc" })
@ActiveProfiles("blobstore")
public class BlobstoreClientIT {
	private static Logger log = LoggerFactory.getLogger(BlobstoreClientIT.class);

	private static final String TEST_FILE = "src/test/resources/sample-test.csv"; //$NON-NLS-1$

	@Autowired
	private BlobstoreClient blobstoreClient;

	
	/**
	 * -
	 */
	@Test
	public void testBlobstorePut() {
		
		DataFile data = new DataFile();
		String fileName = TEST_FILE.substring(TEST_FILE.lastIndexOf("/") + 1); //$NON-NLS-1$
		data.setName(fileName);
		try (FileReader reader = new FileReader(TEST_FILE);
				S3Object obj = new S3Object();) {
			String contents = IOUtils.toString(reader);
			data.setFile(IOUtils.toByteArray(new FileSystemResource(TEST_FILE).getInputStream()));
			
			obj.setKey(fileName);
            obj.setObjectContent(new FileSystemResource(TEST_FILE).getInputStream());
            
			this.blobstoreClient.saveBlob(obj);
			log.info(fileName+" saved successfully"); //$NON-NLS-1$
			DataFile retData = this.blobstoreClient.getBlob(fileName);
			String retContent = IOUtils.toString(new CharArrayReader((char[])retData.getFile()));		
			Assert.assertEquals(contents, retContent);
			log.info("returned content : "+retContent); //$NON-NLS-1$
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		
		
	}

}
