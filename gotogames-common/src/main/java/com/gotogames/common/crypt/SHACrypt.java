package com.gotogames.common.crypt;

import org.apache.commons.codec.digest.DigestUtils;

public class SHACrypt {
	
	/**
	 * Calculates the SHA-256 digest and returns the value as a hex string
	 * @param data
	 * @return
	 */
	public static String sha256Hex(String data) {
		return DigestUtils.sha256Hex(data);
	}
}
