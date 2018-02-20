package com.ge.predix.solsvc.blobstore.bootstrap.api;

import java.util.List;

import com.amazonaws.services.s3.model.S3Object;
import com.ge.predix.entity.datafile.DataFile;

/**
 * 
 * @author predix -
 */
public interface BlobstoreClient {

	/**
	 * @param object - 
	 * @return String
	 */
	public String saveBlob(S3Object object);

	/**
	 * @param fileName -
	 * @return -
	 */
	public DataFile getBlob(String fileName);

	/**
	 * @return -
	 */
	public List<DataFile> getBlob();

	/**
	 * @param fileName -
	 */
	public void deleteBlob(String fileName);

	/**
	 * @return -
	 */
	public List<String> getAvailableBlobs();
	
}
