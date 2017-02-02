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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
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
@ActiveProfiles(profiles = { "local", "blobstore" })
@Ignore
public class BlobstoreClientIT {

	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(BlobstoreClientIT.class);

	private static final String TEST_FILE = "src/test/resources/sample-test.csv"; //$NON-NLS-1$

	@Autowired
	private BlobstoreClient blobstoreClient;

	/**
	 * -
	 */
	@SuppressWarnings("nls")
	@Test
	public void testBlobstorePut() {
		DataFile data = new DataFile();
		data.setName(TEST_FILE.substring(TEST_FILE.lastIndexOf("/") + 1));
		try {
			data.setFile(IOUtils.toByteArray(new FileSystemResource(TEST_FILE).getInputStream()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		Map<String, String> userObjectMetaData = new HashMap<String, String>();
		this.blobstoreClient.saveBlob(data, userObjectMetaData);

	}

}
