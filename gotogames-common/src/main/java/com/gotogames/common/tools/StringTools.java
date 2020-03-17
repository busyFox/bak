package com.gotogames.common.tools;

import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

public class StringTools {
	
	/**
	 * Transform input string to hexa format
	 * @param input
	 * @return
	 */
	public static String strToHexa(String input) {
		char[] inputChars = input.toCharArray();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < inputChars.length; i++) {
			String tmp = Integer.toHexString(input.charAt(i));
			if (tmp.length() == 1) {
				sb.append("0");
			}
			sb.append(tmp);
			//sb.append(Integer.toString((int)inputChars[i], 16));
		}
		return sb.toString();
	}
	
	/**
	 * Transform input string from hexa format to normal format
	 * @param input
	 * @return
	 * @throws Exception
	 */
	public static String hexaToStr(String input) throws Exception {
		if (input.length() % 2 != 0) {
			throw new Exception("Taille chaine hexa non valide");
		}
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < input.length(); i+=2) {
			String sub = input.substring(i, i+2);
			sb.append((char)Integer.parseInt(sub, 16));
		}
		
		return sb.toString();
	}
	
	private static char[] goodCharAlphaNum= {
		'a', 'b', 'c', 'd', 'e', 'f', 'g','h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
		'A', 'B', 'C', 'D', 'E', 'F', 'G','H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };
	
	
	/**
	 * Generate a random string with lenght = nbChar
	 * @param nbChar lenght of random string generated
	 * @return
	 */
	public static String generateRandomString(int nbChar) {
		Random random = new Random();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < nbChar; i++) {
			sb.append(goodCharAlphaNum[random.nextInt(goodCharAlphaNum.length)]);
		}
		return sb.toString();
	}
	
	/**
	 * Return true if the string parameter is alphanumeric
	 * @param val
	 * @return
	 */
	public static boolean isStringAlphaNumeric(String val) {
		return val.matches("(\\w)+");
	}
	
	/**
	 * Return true if the string parameter is a valid email string format
	 * @param val
	 * @return
	 */
	public static boolean isStringEmailValid(String val) {
		return val.matches("^[_a-z0-9-]+(\\.[_a-z0-9-]+)*@[a-z0-9-]+(\\.[a-z0-9-]+)+$");
	}
	
	/**
	 * Return true if the string parameter is a valid pseudo. 
	 * Length >= 4 & <= 32 
	 * Alphanumeric + "-_.# "
	 * @param val
	 * @return
	 */
	public static boolean isPseudoValid(String val) {
		if (val.length() >=4 && val.length() <= 32) {
			return val.matches("^[a-zA-Z0-9-_\\.#]([a-zA-Z0-9-_\\.# ])*[a-zA-Z0-9-_\\.#]");
		}
		return false;
	}
	
	/**
	 * Return true if the string parameter is a valid password. 
	 * Alphanumeric with Length >= 4 & <= 32
	 * @param val
	 * @return
	 */
	public static boolean isPasswordValid(String val) {
		if (val.length() >=4 && val.length() <= 32) {
			return val.matches("[a-zA-Z0-9]*");
		}
		return false;
	}
	
	/**
	 * Build a map with all parameters from string params. Format : "param1=toto;param2=tutu;param3=titi" ';' is the delimiter
	 * @param params
	 * @param delimiter
	 * @return a map with param name as key and value associated
	 */
	public static Map<String, String> getParams(String params, String delimiter) {
		Map<String, String> map = new HashMap<String, String>();
		String[] temp = params.split(delimiter);
		for (int i = 0; i < temp.length; i++) {
			int idx = temp[i].indexOf('='); 
			if (idx >= 0) {
				map.put(temp[i].substring(0, idx), temp[i].substring(idx+1));
			}
		}
		return map;
	}
	
	/**
	 * Convert a byte array to string using the format %02X for each byte
	 * @param data
	 * @return
	 */
	public static String byte2String(byte[] data) {
		if (data != null) {
			try {
				StringBuffer sb = new StringBuffer();
			    Formatter format = new Formatter(sb);
			    for (byte b : data) {
			    	format.format("%02X", b);
			    }
			    return sb.toString();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	/**
	 * Convert a string with hexadecimal caractère to byte array. The data must have %2 length and contain only hexadecimal char. 
	 * 29CB27DCB5E3 => 6 bytes (0x29, 0xCB ...)
	 * @param data
	 * @return null if data length %2 != 0
	 */
	public static byte[] string2byte(String data) {
		if (data != null && data.length() > 0 && data.length() % 2 == 0) {
			try {
				byte[] byVal = new byte[data.length()/2];
				for (int i = 0; i < byVal.length; i++) {
					byVal[i] = (byte)Integer.parseInt(data.substring(2*i, (2*i)+2), 16);
//					byVal[i] = Byte.parseByte(data.substring(2*i, (2*i)+2), 16);
				}
				return byVal;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	/**
	 * Return string representation of list (to be used in log)
	 * @param list
	 * @return
	 */
	public static String listToString(List list) {
		if (list != null) {
			String str = "{list size="+list.size()+" - data=";
			String strData = "";
			for (Object o :list) {
				if (strData.length() > 0) {strData += ";";}
				strData += ""+o;
			}
			return str + strData + "}";
		}
		return null;
	}

    /**
     * Return string representation of list (to be used in log)
     * @param list
     * @param separator
     * @return
     */
    public static String listToString(List list, String separator) {
        if (list != null) {
            String strData = "";
            for (Object o :list) {
                if (strData.length() > 0) {strData += separator;}
                strData += ""+o;
            }
            return strData;
        }
        return null;
    }
	
	/**
	 * Return string representation of set (to be used in log)
	 * @param set
	 * @return
	 */
	public static String setToString(Set set) {
		if (set != null) {
			String str = "{set size="+set.size()+" - data=";
			String strData = "";
			for (Object o :set) {
				if (strData.length() > 0) {strData += ";";}
				strData += ""+o;
			}
			return str + strData + "}";
		}
		return null;
	}
	
	/**
	 * Replace non BMP character from string with replace value. 
	 * For characters Outside the unicode Basic Multilingual Plane ([^\u0000-\uFFFF])
	 * @param str
	 * @param replace
	 * @return
	 */
	public static String removeNonBMPCharacters(String str, String replace) {
		if (str != null) {
//			String replaceRegex = str.replaceAll("[^\u0000-\uFFFF]", replace);
//			return replaceRegex;
			
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < str.length();) {
				int codePoint = str.codePointAt(i);
				if (codePoint > 0xFFFF) {
					sb.append(replace);
					i += Character.charCount(codePoint);
				} else {
					sb.appendCodePoint(codePoint);
					i++;
				}
			}
			return sb.toString();
		}
		return null;
	}
	
	/**
	 * Truncate string parameter to maxLength. If indicator not null, add it to the string result.
	 * @param str
	 * @param maxLength
	 * @param indicator
	 * @return
	 */
	public static String truncate(String str, int maxLength, String indicator) {
		if (str != null && maxLength > 0) {
			if (str.length() > maxLength) {
				if (indicator != null && indicator.length() < maxLength) {
					return str.substring(0, (maxLength - indicator.length()))+indicator;
				} else {
					return str.substring(0, maxLength);
				}
			}
		}
		return str;
	}
	
	/**
	 * Check if mail format is valid. Format user@host. 
	 * Contain @ 
	 * user not empty 
	 * host not empty
	 * user & host without space char 
	 * host not ended with .
	 * host not started with .
	 * host without ..
	 * @param mail
	 * @return
	 */
	public static boolean checkFormatMail(String mail) {
		// structure of mail : user@host
		if (mail != null && mail.length() > 0) {
			int idxArobase = mail.indexOf("@");
			
			if (idxArobase > 0) {
				// check part user
				String user = mail.substring(0, idxArobase);
				if (user == null || user.length() == 0) {
					return false;
				}
				
				// check part host
				String host = mail.substring(idxArobase+1);
				if (host == null || host.length() == 0) {
					return false;
				}
				
				// check no white space
				if (user.indexOf(' ') >= 0) {
					return false;
				}
				if (host.indexOf(' ') >= 0) {
					return false;
				}
				
				// host not started or ended with .
				if (host.indexOf('.') == 0) {
					return false;
				}
				if (host.lastIndexOf('.') == host.length()) {
					return false;
				}
				
				// get index of last point
				int idxLastPoint = host.lastIndexOf(".");
				if (idxLastPoint >= 0) {
					// search if host ends with 2 points : ..com
					String temp = host.substring(0, idxLastPoint);
					if (temp.endsWith(".")) {
						return false;
					}
				} else {
					// no point char in the host
					return false;
				}
			} else {
				// no arobase
				return false;
			}
		} else {
			return false;
		}
		return true;
	}
}
