package test.gotogames.common.crypt;


import com.gotogames.common.crypt.Encryption;
import com.gotogames.common.tools.RandomTools;

import junit.framework.TestCase;

public class EncryptionTest extends TestCase {
	public void testSimlpleCrypt() {
		String key = "testtest";
		String text = RandomTools.generateRandomString(30);
		
		String textCrypt = Encryption.simpleCrypt(key, text);
		assertNotNull(textCrypt);
		
		String textDecrypt = null;
		try {
			textDecrypt = Encryption.simpleDecrypt(key, textCrypt);
		} catch (Exception e) {
			e.printStackTrace();
		}
		assertNotNull(textDecrypt);
		
		assertEquals(text, textDecrypt);
		
	}
}
