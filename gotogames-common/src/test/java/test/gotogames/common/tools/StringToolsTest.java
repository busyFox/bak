package test.gotogames.common.tools;

import com.gotogames.common.tools.StringTools;

import junit.framework.TestCase;

public class StringToolsTest extends TestCase {
	public void testHexa() throws Exception {
		String strTest = "01CECI EST UN TEST!é&è$ù@";
		String strTestHexa = StringTools.strToHexa(strTest);
		assertEquals(strTestHexa.length()%2, 0);
		assertEquals(strTest, StringTools.hexaToStr(strTestHexa));
		
	}
	
	public void testAlphaNumeric() {
		assertTrue(StringTools.isStringAlphaNumeric("0az123ADF456"));
		assertFalse(StringTools.isStringAlphaNumeric("0az123ADF456>./"));
	}
	
	public void testEmail() {
		assertTrue(StringTools.isStringEmailValid("pascal@goto.fr"));
		assertTrue(StringTools.isStringEmailValid("pascal01.3@goto.fr"));
		assertTrue(StringTools.isStringEmailValid("pascal@goto.fr123"));
		assertFalse(StringTools.isStringEmailValid(".2.pascal@goto.fr"));
		assertFalse(StringTools.isStringEmailValid(".2.pascalgoto.fr"));
		assertFalse(StringTools.isStringEmailValid(".2.pascalgoto"));
		assertFalse(StringTools.isStringEmailValid(".2.pascal@.fr"));
		assertFalse(StringTools.isStringEmailValid(".2.pascal@goto"));
	}
	
	public void testByteString() {
		String val = "29CB27DCB5E3";
		byte[] by = StringTools.string2byte(val);
		String byVal = StringTools.byte2String(by);
		assertEquals(val, byVal);
	}
	
	public void testPseudo() {
		assertTrue(StringTools.isPseudoValid("toto"));
		assertTrue(StringTools.isPseudoValid("to-to"));
		assertTrue(StringTools.isPseudoValid("to to"));
		assertTrue(StringTools.isPseudoValid("to..to"));
		assertTrue(StringTools.isPseudoValid("#toto1.T"));
		
		assertFalse(StringTools.isPseudoValid("tot"));
		assertFalse(StringTools.isPseudoValid("to?to"));
		assertFalse(StringTools.isPseudoValid(" toto"));
		assertFalse(StringTools.isPseudoValid(" toto "));
		assertFalse(StringTools.isPseudoValid("toto "));
		assertFalse(StringTools.isPseudoValid("totô"));
		assertFalse(StringTools.isPseudoValid("éèyt"));
	}
	
	public void testPassword() {
		assertTrue(StringTools.isPasswordValid("Toto"));
		assertTrue(StringTools.isPasswordValid("Tototutu"));
		assertTrue(StringTools.isPasswordValid("Tototutu12"));
		assertTrue(StringTools.isPasswordValid("123456"));
		
		assertFalse(StringTools.isPasswordValid("Toto "));
		assertFalse(StringTools.isPasswordValid("tot"));
		assertFalse(StringTools.isPasswordValid("éèôto"));
		assertFalse(StringTools.isPasswordValid(" toto"));
	}
	
	public void testTruncate() {
		assertEquals("toto", StringTools.truncate("toto", 10, null));
		assertEquals("toto...", StringTools.truncate("totototo", 7, "..."));
		assertEquals("tototot", StringTools.truncate("totototo", 7, null));
		assertEquals("toto", StringTools.truncate("toto", -1, "..."));
	}
	
	public void testCheckMail() {
		assertTrue(StringTools.checkFormatMail("toto@free.fr"));
		assertTrue(StringTools.checkFormatMail("pascal.serent@goto-games.com"));
		assertTrue(StringTools.checkFormatMail("toto@tutu.titi.fr"));
		assertFalse(StringTools.checkFormatMail("toto@free..fr"));
		assertFalse(StringTools.checkFormatMail("toto@free"));
		assertFalse(StringTools.checkFormatMail("@free.fr"));
		assertFalse(StringTools.checkFormatMail("toto@"));
		assertFalse(StringTools.checkFormatMail("toto@free "));
		assertFalse(StringTools.checkFormatMail("toto @free"));
		assertFalse(StringTools.checkFormatMail(" toto@free"));
		assertFalse(StringTools.checkFormatMail("toto@free .fr"));
		assertFalse(StringTools.checkFormatMail("toto.tutu@free"));
	}

    public static void main(String[] args) {
        String strEndWithNumber = "engine12sys123";
        while (strEndWithNumber.matches(".*\\d")) {
            strEndWithNumber = strEndWithNumber.substring(0, strEndWithNumber.length()-1);
        }
        System.out.println(strEndWithNumber);
    }
}
