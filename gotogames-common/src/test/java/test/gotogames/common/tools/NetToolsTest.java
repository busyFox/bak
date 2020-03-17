package test.gotogames.common.tools;

import com.gotogames.common.tools.NetTools;

import junit.framework.TestCase;

public class NetToolsTest extends TestCase {
	
	public void testGetURLContent() {
		String url = "http://www.google.fr";
		assertNotNull(NetTools.getURLContent(url, 0));
		url = "htp://ww.google.f";
		assertNull(NetTools.getURLContent(url, 0));
	}
}
