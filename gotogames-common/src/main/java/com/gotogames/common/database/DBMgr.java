package com.gotogames.common.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DBMgr {
	private DBConnection dbcon = new DBConnection();
	private HashMap<String, DBRequest> mapRequest = new HashMap<String, DBRequest>();
	private Logger log = LogManager.getLogger(this.getClass());
	private String server, user, password;
	
	public void init(String dbServer, String dbUser, String dbPassword, List<DBRequest> listRequest) {
		destroy();
		server = dbServer;
		user = dbUser;
		password = dbPassword;
		dbcon.setServerBase(dbServer);
		dbcon.setUser(dbUser);
		dbcon.setPassword(dbPassword);
        if (listRequest != null) {
            for (DBRequest req : listRequest) {
                if (req.name != null && req.name.length() > 0) {
                    if (!mapRequest.containsKey(req.name)) {
                        mapRequest.put(req.name, req);
                    }
                }
            }
        }
		log.info("server="+dbServer+" - nbRequest="+mapRequest.size());
	}
	
	public String toString() {
		return "server="+server+" user="+user+" - nbRequest="+mapRequest.size();
	}
	
	public void destroy() {
		dbcon.closeConnection();
		mapRequest.clear();
	}
	
	private boolean isDBOK() {
		if (!dbcon.isConnectionValid()) {
			log.warn("Connection not valid ... try to connect !");
			boolean bOpenConn = dbcon.openConnection(mapRequest.values());
			log.info("Open connection="+bOpenConn);
			return bOpenConn;
		}
		return true;
	}

    public DBConnection getDBConnection() {
        if (isDBOK()) {
            return dbcon;
        }
        return null;
    }

    public ResultSet executeQuery(String request) {
        if (isDBOK()) {
            try {
                return dbcon.getConnDB().createStatement().executeQuery(request);
            } catch (Exception e) {
                log.error("Failed to execute request="+request, e);
            }
        } else {
            log.error("DB is not OK!");
        }
        return null;
    }

    public PreparedStatement createPreparedStatement(String psReq) {
        if (isDBOK()) {
            try {
                return dbcon.getConnDB().prepareStatement(psReq);
            } catch (Exception e) {
                log.error("Failed to create prepare statement request="+psReq, e);
            }
        } else {
            log.error("DB is not OK!");
        }
        return null;
    }

	public int getRequestValueInt(String rqtName, int defaultValue, List<Object> listParamValue) {
		if (isDBOK()) {
			DBRequest req = mapRequest.get(rqtName);
			if (req != null) {
				int nbParam = 0;
				if (listParamValue != null) {
					nbParam = listParamValue.size();
				}
				if (req.nbParam != nbParam) {
					log.error("Request rqtName="+rqtName+" need "+req.nbParam+" parameters and listParamValue has "+nbParam+" parameters");
				} else {
					try {
						if (listParamValue != null && listParamValue.size() > 0) {
							for (int i = 0; i < listParamValue.size(); i++) {
								setReqParam(req.ps, listParamValue.get(i), i+1);
							}
						}
	//					long ts1=System.currentTimeMillis();
						ResultSet rs = req.ps.executeQuery();
	//					log.info("Time execution rqt="+rqtName+" - ts="+(System.currentTimeMillis()-ts1));
						if (rs.next()) {
							return (int)rs.getLong(1);
						}
					} catch (SQLException e) {
						log.error("Execption to execute request rqtName="+rqtName, e);
					}
				}
			} else {
				log.error("Request not found : rqtName="+rqtName);
			}
		} else {
			log.error("DB not valid");
		}
		return defaultValue;
	}
	
	public long getRequestValueLong(String rqtName, long defaultValue, List<Object> listParamValue) {
		if (isDBOK()) {
			DBRequest req = mapRequest.get(rqtName);
			if (req != null) {
				int nbParam = 0;
				if (listParamValue != null) {
					nbParam = listParamValue.size();
				}
				if (req.nbParam != nbParam) {
					log.error("Request rqtName="+rqtName+" need "+req.nbParam+" parameters and listParamValue has "+nbParam+" parameters");
				} else {
					try {
						if (listParamValue != null && listParamValue.size() > 0) {
							for (int i = 0; i < listParamValue.size(); i++) {
								setReqParam(req.ps, listParamValue.get(i), i+1);
							}
						}
						ResultSet rs = req.ps.executeQuery();
						if (rs.next()) {
							return rs.getLong(1);
						}
					} catch (SQLException e) {
						log.error("Execption to execute request rqtName="+rqtName, e);
					}
				}
			} else {
				log.error("Request not found : rqtName="+rqtName);
			}
		} else {
			log.error("DB not valid");
		}
		return defaultValue;
	}
	
	@SuppressWarnings("rawtypes")
	public List<Object> getRequestResultList(String rqtName, List<Object> listParamValue, String resultValueClassName) {
		try {
			Class c = Class.forName(resultValueClassName);
			if (isDBOK()) {
				DBRequest req = mapRequest.get(rqtName);
				if (req != null) {
					int nbParam = 0;
					if (listParamValue != null) {
						nbParam = listParamValue.size();
					}
					if (req.nbParam != nbParam) {
						log.error("Request rqtName="+rqtName+" need "+req.nbParam+" parameters and listParamValue has "+nbParam+" parameters");
					} else {
						if (listParamValue != null && listParamValue.size() > 0) {
							for (int i = 0; i < listParamValue.size(); i++) {
								setReqParam(req.ps, listParamValue.get(i), i+1);
							}
						}
						ResultSet rs = req.ps.executeQuery();
						List<Object> listResult = new ArrayList<Object>();
						while (rs.next()) {
							DBResult val = (DBResult)c.newInstance();
							val.setResultSet(rs);
							listResult.add(val);
						}
						return listResult;
					}
				} else {
					log.error("Request not found : rqtName="+rqtName);
				}
			} else {
				log.error("DB not valid");
			}
		} catch (Exception e) {
			log.error("Exception to getResultList : rqtName="+rqtName, e);
		}
		return null;
	}

	public List<Object> getSqlResultList(String sqlQuery, String resultValueClassName) {
		try {
			Class c = Class.forName(resultValueClassName);
			if (isDBOK()) {
				if (sqlQuery != null) {
						Connection conn = dbcon.getConnDB();
						Statement st = conn.createStatement();
						ResultSet rs = st.executeQuery(sqlQuery);
						List<Object> listResult = new ArrayList<Object>();
						while (rs.next()) {
							DBResult val = (DBResult)c.newInstance();
							val.setResultSet(rs);
							listResult.add(val);
						}
						return listResult;
				} else {
					log.error("Query null");
				}
			} else {
				log.error("DB not valid");
			}

		} catch (Exception e) {
			log.error("Exception to getSqlResultList : sqlQuery="+sqlQuery, e);
		}
		return null;
	}
	
	private void setReqParam(PreparedStatement ps, Object val, int index) throws SQLException {
		if (val instanceof Long) {
			ps.setLong(index, (Long)val);
		} else if (val instanceof Integer) {
			ps.setInt(index, (Integer)val);
		} else if (val instanceof String) {
			ps.setString(index, (String)val);
		} else {
			log.error("Object type not managed ! val="+val+" - index="+index);
		}
	}
	
	public List<String> getRequestNameList() {
		return new ArrayList<String>(mapRequest.keySet());
	}
}
