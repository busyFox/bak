package com.gotogames.common.tools;

public class GeoLocTools {
	/**
	 * This routine calculates the distance between two points (given the latitude/longitude of those points). 
	 * Source code inspired from http://www.geodatasource.com/developers/java
	 * @param latitudePoint1
	 * @param longitudePoint1
	 * @param latitudePoint2
	 * @param longitudePoint2
	 * @return the distance between the 2 points in KM
	 */
	public static double distance(double latitudePoint1, double longitudePoint1, double latitudePoint2, double longitudePoint2) {
		double theta = longitudePoint1 - longitudePoint2;
		double dist = Math.sin(deg2rad(latitudePoint1)) * Math.sin(deg2rad(latitudePoint2)) + Math.cos(deg2rad(latitudePoint1)) * Math.cos(deg2rad(latitudePoint2)) * Math.cos(deg2rad(theta));
		dist = Math.acos(dist);
		dist = rad2deg(dist);
		dist = dist * 60 * 1.1515;
//		if (unit == "K") {
			dist = dist * 1.609344;
//		} else if (unit == "N") {
//			dist = dist * 0.8684;
//		}
		return (dist);
	}
	
	/**
	 * This function converts decimal degrees to radians
	 * @param deg
	 * @return
	 */
	private static double deg2rad(double deg) {
		return (deg * Math.PI / 180.0);
	}
	
	/**
	 * This function converts radians to decimal degrees
	 * @param rad
	 * @return
	 */
	private static double rad2deg(double rad) {
		return (rad * 180 / Math.PI);
	}
}
