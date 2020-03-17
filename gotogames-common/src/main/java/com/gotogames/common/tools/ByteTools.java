package com.gotogames.common.tools;

public class ByteTools {
	
	/**
	 * Return the index of the first occurence of byte b in byte array
	 * @param array array of byte into search
	 * @param b byte to search
	 * @return -1 if byte b not found
	 */
	public static int getFirstIdxOf(byte[] array, byte b) {
		if (array != null) {
			for (int i = 0; i < array.length; i++) {
				if (array[i] == b) {
					return i;
				}
			}
		}
		return -1;
	}
}
