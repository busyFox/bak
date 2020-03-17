package test.gotogames.common.tools;

import com.gotogames.common.tools.StringVersion;

import junit.framework.TestCase;

public class StringVersionTest extends TestCase {
	public void testStringVersion() {
		assertTrue(new StringVersion("1.0.0").compareTo(new StringVersion("1.0.0")) == 0);
		assertTrue(new StringVersion("1.0.0").compareTo(new StringVersion("1.1.0")) < 0);
		assertTrue(new StringVersion("1.1.0").compareTo(new StringVersion("1.0.0")) > 0);
		assertTrue(new StringVersion("1.9.0").compareTo(new StringVersion("2.0.0")) < 0);
		assertTrue(new StringVersion("2.0.0").compareTo(new StringVersion("2.0.0b")) < 0);
		
		assertTrue(StringVersion.compareVersion("1.0.0", "1.0.0") == 0);
		assertTrue(StringVersion.compareVersion("1.0.0", "1.1.0") < 0);
		assertTrue(StringVersion.compareVersion("2.0.0", "1.0.0") > 0);
		assertTrue(StringVersion.compareVersion("2.0.0", "2.0.0b") < 0);
		assertTrue(StringVersion.compareVersion("2.0.0", "") > 0);
		assertTrue(StringVersion.compareVersion(".1.0", "1") < 0);

        assertTrue(new StringVersion("4.19.0").compareTo(new StringVersion("4.2.0")) > 0);
        assertTrue(new StringVersion("4.19.0").compareTo(new StringVersion("4.20.0")) < 0);
        assertTrue(new StringVersion("4.19.0").compareTo(new StringVersion("4.19.0.1")) < 0);
	}
}
