package test.gotogames.common.crypt;

import com.gotogames.common.crypt.AESCrypto;
import com.gotogames.common.tools.StringTools;

import junit.framework.TestCase;

public class AESCryptoTest extends TestCase {
	public void testCrypt() {
		String key = "FunBr1dg3ceStFun";
		String data = "coucou";
		byte[] crypt1 = AESCrypto.crypt(data.getBytes(), key);
		assertNotNull(crypt1);
		System.out.println("Data = "+data+" - crypt = "+StringTools.byte2String(crypt1));
		String decrypt1 = new String(AESCrypto.decrypt(crypt1, key));
		System.out.println("decrypt = "+decrypt1);
		assertEquals(data, decrypt1);
	}
	
	public void testCrypt6() {
		String key = "bashorebridge1945";
		String data= "coucoucoucoucoucoucoucoucoucoucoucou";
		byte[] crypt1 = AESCrypto.crypt(data.getBytes(), key);
		assertNotNull(crypt1);
		System.out.println("Data = "+data+" - crypt = "+StringTools.byte2String(crypt1));
		String decrypt1 = new String(AESCrypto.decrypt(crypt1, key));
		System.out.println("decrypt = "+decrypt1);
		assertEquals(data, decrypt1);
	}
	
	public void testCrypt2() {
		String key = "FunBr1dg3ceStFun";
		String data = "WASNEEESESWSSNSEWENESENSWSWNWEWWNNNNNESNWWWWSWNSEWESNE";
		byte[] crypt1 = AESCrypto.crypt(data.getBytes(), key);
		assertNotNull(crypt1);
		String temp = StringTools.byte2String(crypt1);
		System.out.println("Data = "+data+" - crypt = "+temp);
		String decrypt1 = new String(AESCrypto.decrypt(crypt1, key));
		System.out.println("decrypt = "+decrypt1);
		assertEquals(data, decrypt1);
		
		if (temp.length() % 2 == 0) {
			byte[] byVal = new byte[temp.length()/2];
			for (int i = 0; i < byVal.length; i++) {
				int val = Integer.parseInt(temp.substring(2*i, (2*i)+2), 16);
				byVal[i] = (byte)val;
			}
		}
	}
	
	public void testCrypt3() {
		String key = "FunBr1dg3ceStFun";
		String data = "WASNEEESESWSSNSEWENESENSWSWNWEWWNNNNNESNWWWWSWNSEWESNE";
		String dataCrypt = AESCrypto.crypt(data, key);
		assertNotNull(dataCrypt);
		System.out.println("data="+data+" - data crypt="+dataCrypt);
		String decrypt = AESCrypto.decrypt(dataCrypt, key);
		assertNotNull(decrypt);
		assertEquals(data, decrypt);
	}
	
	public void testCrypt4() {
		String key = "coucou";
		String data = "WASNEEESESWSSNSEWENESENSWSWNWEWWNNNNNESNWWWWSWNSEWESNE";
		String dataCrypt = AESCrypto.crypt(data, key);
		assertNotNull(dataCrypt);
		System.out.println("data="+data+" - data crypt="+dataCrypt);
		String decrypt = AESCrypto.decrypt(dataCrypt, key);
		assertNotNull(decrypt);
		assertEquals(data, decrypt);
	}
	
	public void testCrypt5() {
		String key = "coucou01234567890123456789012345678901234567890123456789";
		String data = "WASNEEESESWSSNSEWENESENSWSWNWEWWNNNNNESNWWWWSWNSEWESNE";
		String dataCrypt = AESCrypto.crypt(data, key);
		assertNotNull(dataCrypt);
		System.out.println("data="+data+" - data crypt="+dataCrypt);
		String decrypt = AESCrypto.decrypt(dataCrypt, key);
		assertNotNull(decrypt);
		assertEquals(data, decrypt);
	}
	
	public void testCrypt7() {
		String key = "FunBr1dg3ceStFun";
		String data = "goto";
		String dataCrypt = AESCrypto.crypt(data, key);
		System.out.println("data="+data+" - data crypt="+dataCrypt);
		String decrypt = AESCrypto.decrypt(dataCrypt, key);
		assertNotNull(decrypt);
		assertEquals(data, decrypt);
	}
}
