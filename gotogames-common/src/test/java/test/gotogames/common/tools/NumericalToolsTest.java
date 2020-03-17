package test.gotogames.common.tools;


import com.gotogames.common.tools.NumericalTools;

import junit.framework.TestCase;

public class NumericalToolsTest extends TestCase {
	public void testRound() {
		assertEquals(1.24, NumericalTools.round(1.235789, 2));
		assertEquals(1.236, NumericalTools.round(1.235789, 3));
		assertEquals(74.82, NumericalTools.round(74.817527212614, 2));
		assertEquals(0.0, NumericalTools.round(0.3, 0));
		assertEquals(1.0, NumericalTools.round(0.7, 0));
		assertEquals(14.0, NumericalTools.round(((double)(20 * 100)) / 140, 0));
		assertEquals(16.0, NumericalTools.round(((double)(22 * 100)) / 140, 0));
	}
}
