package com.gotogames.common.database;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface DBResult {
	public void setResultSet(ResultSet rs) throws SQLException;
}
