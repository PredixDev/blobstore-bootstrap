package com.ge.predix.solsvc.blobstore.bootstrap.api;

import java.util.List;
import java.util.Map;

import com.ge.predix.entity.datafile.DataFile;

/**
 * 
 * @author predix -
 */
public interface BlobstoreClient {

	/**
	 * @param data -
	 * @param userObjectMetaData -
	 * @return -
	 * @throws Exception -
	 */
	public String saveBlob(DataFile data, Map<String,String> userObjectMetaData);

	/**
	 * @param fileName -
	 * @param blobstoreConfig -
	 * @return -
	 */
	public DataFile getBlob(String fileName);

	/**
	 * @param blobstoreConfig -
	 * @return -
	 */
	public List<DataFile> getBlob();

	/**
	 * @param fileName -
	 * @param blobstoreConfig -
	 */
	public void deleteBlob(String fileName);
	
}
