package test.gotogames.common.tools;

import com.gotogames.common.tools.GeoLocTools;

import junit.framework.TestCase;

public class GeoLocToolsTest extends TestCase{
	public void testDistance() {
		double roubaixLongitude = 3.1808;
		double roubaixLatitude = 50.6891;
		double stqLongitude = 3.2855;
		double stqLatitude = 49.8477;
		
		System.out.println("Distance ROUBAIX - SAINT-QUENTIN = "+GeoLocTools.distance(roubaixLatitude, roubaixLongitude, stqLatitude, stqLongitude));
	}
}
