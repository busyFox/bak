package com.gotogames.common.database;

import java.sql.PreparedStatement;

public class DBRequest {
	public String sql;
	public PreparedStatement ps;
	public String name;
	public int nbParam = 0;
	
	public String toString() {
		return "{name="+name+" - nbParam="+nbParam+"}";
	}
	
	public boolean isValid() {
		if (sql == null || sql.length() == 0 || name == null || name.length() == 0) {
			return false;
		}
		return true;
	}
}
