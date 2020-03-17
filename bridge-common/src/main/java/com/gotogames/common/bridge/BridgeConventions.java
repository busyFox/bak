package com.gotogames.common.bridge;


public class BridgeConventions {
	public static final byte[][] GAME_CONV_PROFIL = new byte[6][25];
	static {
		/*								    0    1    2    3    4    5    6    7    8        9   10   11   12   13   14   15   16   17   18   19   20   21   22   23   24   25*/
		GAME_CONV_PROFIL[0] = new byte[] {0x00,0x01,0x00,0x03,0x00,0x01,0x00,0x00,/*0x00,*/0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
		GAME_CONV_PROFIL[1] = new byte[] {0x00,0x01,0x00,0x01,0x00,0x01,0x01,0x01,/*0x01,*/0x00,0x00,0x01,0x00,0x02,0x00,0x01,0x00,0x01,0x00,0x00,0x01,0x01,0x01,0x00,0x00,0x01};
		GAME_CONV_PROFIL[2] = new byte[] {0x00,0x01,0x01,0x01,0x01,0x01,0x01,0x01,/*0x01,*/0x00,0x00,0x01,0x00,0x02,0x01,0x01,0x01,0x01,0x00,0x01,0x01,0x01,0x01,0x00,0x00,0x01};
		GAME_CONV_PROFIL[3] = new byte[] {0x00,0x01,0x01,0x02,0x00,0x01,0x01,0x01,/*0x01,*/0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x00,0x00,0x02};
		GAME_CONV_PROFIL[4] = new byte[] {0x01,0x00,0x01,0x03,0x00,0x00,0x00,0x01,/*0x01,*/0x01,0x00,0x01,0x00,0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x00,0x00,0x03};
		GAME_CONV_PROFIL[5] = new byte[] {0x03,0x01,0x01,0x02,0x01,0x01,0x01,0x01,/*0x01,*/0x01,0x01,0x01,0x00,0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x00,0x00,0x04};
	}
	public static final int GAME_CONV_PROFIL_INDEX_NOVICE = 0;
	public static final int GAME_CONV_PROFIL_INDEX_CLUB = 1;
	public static final int GAME_CONV_PROFIL_INDEX_COMP = 2;
	public static final int GAME_CONV_PROFIL_INDEX_USA = 3;
	public static final int GAME_CONV_PROFIL_INDEX_ACOL = 4;
	public static final int GAME_CONV_PROFIL_INDEX_POL = 5;
	public static final int GAME_CONV_PROFIL_INDEX_DEFAULT = 1;
	
	public static final String GAME_DEFAULT_CONV = new String(GAME_CONV_PROFIL[GAME_CONV_PROFIL_INDEX_DEFAULT]);
	public static final String GAME_CONV_PROFIL_NOVICE = new String(GAME_CONV_PROFIL[GAME_CONV_PROFIL_INDEX_NOVICE]);
	public static final String GAME_CONV_PROFIL_CLUB = new String(GAME_CONV_PROFIL[GAME_CONV_PROFIL_INDEX_CLUB]);
	public static final String GAME_CONV_PROFIL_COMP = new String(GAME_CONV_PROFIL[GAME_CONV_PROFIL_INDEX_COMP]);
	public static final String GAME_CONV_PROFIL_USA = new String(GAME_CONV_PROFIL[GAME_CONV_PROFIL_INDEX_USA]);
	public static final String GAME_CONV_PROFIL_ACOL = new String(GAME_CONV_PROFIL[GAME_CONV_PROFIL_INDEX_ACOL]);
	public static final String GAME_CONV_PROFIL_POL = new String(GAME_CONV_PROFIL[GAME_CONV_PROFIL_INDEX_POL]);
	
	public static final String[] GAME_CONV_PROFIL_STRING = new String[6];
	static {
		for (int i = 0 ; i < GAME_CONV_PROFIL.length; i++) {
			StringBuffer sb = new StringBuffer();
			for (int j = 0; j < GAME_CONV_PROFIL[i].length; j++) {
				sb.append(Byte.toString(GAME_CONV_PROFIL[i][j]));
			}
			GAME_CONV_PROFIL_STRING[i] = sb.toString();
		}
	}
	
	/**
	 * Return the convention string for profil 
	 * @param profil
	 * @return
	 */
	public static String getConventionStrForProfil(int profil) {
		if (profil >= 0 && profil< GAME_CONV_PROFIL_STRING.length) {
			return GAME_CONV_PROFIL_STRING[profil];
		}
		return null;
	}
	
	/**
	 * Return the convention byte for profil
	 * @param profil
	 * @return
	 */
	public static byte[] getConventionByteForProfil(int profil) {
		if (profil >= 0 && profil< GAME_CONV_PROFIL.length) {
			return GAME_CONV_PROFIL[profil];
		}
		return null;
	}
	
	/**
	 * Return byte array for this convention string. The string must be contains only numerical char
	 * @param conv
	 * @return
	 */
	public static byte[] getConventionByteForString(String conv) {
		if (conv != null && conv.length() > 0) {
			try {
				byte[] byConv = new byte[conv.length()];
				for (int i = 0; i < conv.length(); i++) {
					byConv[i] = Byte.parseByte(conv.substring(i, i+1));
				}
				return byConv;
			} catch (NumberFormatException e) {}
		}
		return null;
	}
	
	/**
	 * retrieve the profil index corresponding to this convention string
	 * @param conv
	 * @return
	 */
	public static int getProfilForConvention(String conv) {
		if (conv != null && conv.length() > 0) {
			for (int i = 0; i < GAME_CONV_PROFIL_STRING.length; i++) {
				if (GAME_CONV_PROFIL_STRING[i].equals(conv)) {
					return i;
				}
			}
		}
		return -1;
	}
}
