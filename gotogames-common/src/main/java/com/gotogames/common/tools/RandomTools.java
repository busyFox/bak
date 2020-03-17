package com.gotogames.common.tools;

import java.util.Random;

public class RandomTools {
	
	private static char[] goodChar= {
		'a', 'b', 'c', 'd', 'e', 'f', 'g','h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
		'A', 'B', 'C', 'D', 'E', 'F', 'G','H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'/*, '+', '-', '@'*/ };
	
	/**
	 * Return a random string with nbChar length. Use only with char (a-z,A-Z,0-9)
	 * @param nbChar
	 * @return
	 */
	public static String generateRandomString(int nbChar) {
		Random random = new Random();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < nbChar; i++) {
			sb.append(goodChar[random.nextInt(goodChar.length)]);
		}
		return sb.toString();
	}
	
	public static int getRandomInt(int min, int max) {
		if (min < max) {
			Random random = new Random(System.nanoTime());
			return random.nextInt(max - min + 1) + min;
		}
		return min;
	}
}
