package com.gotogames.common.crypt;

import java.security.Security;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.gotogames.common.tools.StringTools;

/**
 * Class to crypt byte data using AES algorithm
 * @author pascal
 *
 */
public class AESCrypto {
	private static final String algorithm = "AES";
	private static final String iv = "0123456789123456";
	private static final IvParameterSpec ivspec = new IvParameterSpec(iv.getBytes());
	private static Cipher cipher = null;
	
	/**
	 * Initialize static cryptographic cypher
	 */
	static {
		// provider to support PKCS7 padding
		// using BouncyCastle
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		try {
			cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Crypt the byte data using key
	 * @param data to crypt
	 * @param key
	 * @return byte data crypt or null if fail
	 */
	public static byte[] crypt(byte[] data, String key) {
		if (cipher != null && data != null && key != null) {
			synchronized (cipher) {
				try {
//					String newkey = "";
//					if (key.length() > 16) {
//						newkey = key.substring(0, 16);
//					} else  {
//						newkey = key;
//						while (newkey.length() < 16) {newkey += "0";}
//					}
					byte[] byKey = Arrays.copyOf(key.getBytes(), 16);
					// create secret key
					SecretKeySpec skeySpec = new SecretKeySpec(byKey, algorithm);
					// init mode to crypt
					cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivspec);
					
					byte[] cipherText = new byte[cipher.getOutputSize(data.length)];
				    int ctLength = cipher.update(data, 0, data.length, cipherText, 0);
				    ctLength += cipher.doFinal(cipherText, ctLength);
				    return cipherText;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}
	
	/**
	 * Decrypt the byte data using key
	 * @param data to decrypt
	 * @param key
	 * @return byte data decrypt or null if fail
	 */
	public static byte[] decrypt(byte[] data, String key) {
		if (cipher != null && data != null && key != null) {
			synchronized (cipher) {
				try {
					String newkey = "";
					if (key.length() > 16) {
						newkey = key.substring(0, 16);
					} else  {
						newkey = key;
						while (newkey.length() < 16) {newkey += "0";}
					}
					byte[] byKey = Arrays.copyOf(key.getBytes(), 16);
					// create secret key
					SecretKeySpec skeySpec = new SecretKeySpec(byKey, algorithm);
					// init mode to decrypt
					cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivspec);
					
					return cipher.doFinal(data);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}
	
	/**
	 * Return the string decrypt using the key
	 * @param dataCrypt string to decrypt (
	 * @param key
	 * @return
	 */
	public static String decrypt(String dataCrypt, String key) {
		try {
			byte[] byDataCrypt = StringTools.string2byte(dataCrypt);
			byte[] byDataDecrypt = decrypt(byDataCrypt, key);
			String dataDecrypt = new String(byDataDecrypt);
			return dataDecrypt;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Return the crypt data using the key
	 * @param data
	 * @param key
	 * @return
	 */
	public static String crypt(String data, String key) {
		try {
			return StringTools.byte2String(crypt(data.getBytes(), key));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
