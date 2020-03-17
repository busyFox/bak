package com.gotogames.common.tools;

import java.util.regex.Pattern;

public class StringVersion implements Comparable<StringVersion> {
	private static final String SEPARATOR = ".";
	private static final int NUMBER_MAX_WIDTH = 3;
	private static final Pattern pattern = Pattern.compile(SEPARATOR, Pattern.LITERAL);
	private String version;
	
	public StringVersion(String version) {
		this.version = version;
	}
	
	public String getVersion() {
		return version;
	}
	
	@Override
	public int compareTo(StringVersion o) {
		return normalisedVersion(this.version).compareTo(normalisedVersion(o.getVersion()));
	}
	
	private String normalisedVersion(String version) {
		if (version == null || version.length() == 0) {
			return "";
		}
		String[] split = pattern.split(version);
		StringBuilder sb = new StringBuilder();
		for (String s : split) {
            sb.append(String.format("%" + NUMBER_MAX_WIDTH + 's', s));
        }
        return sb.toString();
	}
	
	/**
	 * Return 0 if version1 = version2, -1 if version1 < version2 and +1 if version1 > version2
	 * @param version1
	 * @param version2
	 * @return
	 */
	public static int compareVersion(String version1, String version2) {
		StringVersion v1 = new StringVersion(version1);
		StringVersion v2 = new StringVersion(version2);
		return v1.compareTo(v2);
	}
}
