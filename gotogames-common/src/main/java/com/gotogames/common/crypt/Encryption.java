package com.gotogames.common.crypt;

public class Encryption {
	public static String simpleCrypt(String password, String textToCrypt) {

		if (password.length() == 0) {
			return textToCrypt;
		}
		
		StringBuffer sb = new StringBuffer();
		int lenStr = textToCrypt.length();
		int lenKey = password.length();

		for (int i = 0, j = 0; i < lenStr; i++, j++) {
			if (j >= lenKey) {
				j = 0; 
			}
			sb.append((char) (textToCrypt.charAt(i) ^ password.charAt(j)));
		}
		return sb.toString();
	}

	public static String simpleDecrypt(String password, String cryptText) {
		return simpleCrypt(password, cryptText);
	}
}
