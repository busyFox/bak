package com.gotogames.common.tools;

public class NumericalTools {
	public static double round(double input, int nbDigit) {
		return (double)((int)(input * Math.pow(10, nbDigit) + 0.5))/ Math.pow(10, nbDigit);
	}
}
