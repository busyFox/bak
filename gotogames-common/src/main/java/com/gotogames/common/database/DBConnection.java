package com.gotogames.common.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collection;

public class DBConnection {
	private String serverBase = null, user = null, password = null;
	private Connection connDB = null;
	private Logger log = LogManager.getLogger(this.getClass());
	
	public void setServerBase(String serverBase) {
		this.serverBase = serverBase;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Connection getConnDB(){
		return connDB;
	}

	private boolean checkParameters() {
		if (serverBase == null || serverBase.length() == 0 || user == null || user.length() == 0 || password == null || password.length() == 0) {
			return false;
		}
		return true;
	}
	
	public String parametersToString() {
		return "server="+serverBase+" - user="+user+" - password="+password;
	}

	/**
	 * Open the connection and create prepareStatement for each request
	 * @param listRqt list of request name to retrieve from configuration
	 * @return
	 */
	public boolean openConnection(Collection<DBRequest> listRqt) {
		if (checkParameters()) {
			try {
				closeConnection();
				Class.forName("com.mysql.jdbc.Driver");
				connDB = DriverManager.getConnection("jdbc:mysql://"+serverBase, user, password);
				log.debug("Open connection on DB="+parametersToString());
				for (DBRequest rqt : listRqt) {
					rqt.ps = connDB.prepareStatement(rqt.sql);
					log.debug("Prepare statment OK for request="+rqt.name);
				}
				log.info("Open connection with success");
				return true;
			} catch (Exception e) {
				log.error("Problem to openConnection "+parametersToString(), e);
			}
		} else {
			log.error("Parameters not valid ... param="+parametersToString());
		}
		return false;
	}
	
	public boolean isConnectionValid() {
		if (connDB != null) {
			try {
				if (connDB.isClosed()) {
					log.info("Connection is closed !");
					return false;
				}
				return connDB.isValid(0);
			} catch (SQLException e) {
				log.error("Problem to check connection "+parametersToString(), e);
			}
		}
		return false;
	}
	
	public void closeConnection() {
		if (connDB != null) {
			try {
				connDB.close();
			} catch (SQLException e) {
				log.error("Problem to close connection "+parametersToString(), e);
			}
		}
	}
}
