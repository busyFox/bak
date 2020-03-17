package com.gotogames.bridge.engineserver.request.data;

import com.gotogames.bridge.engineserver.common.Constantes;
import com.gotogames.bridge.engineserver.user.UserVirtualFBServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Object contained in the queue. It represented a request waiting to be computed by an engine
 * @author pascal
 *
 */
public class QueueData implements Comparable<QueueData> {
	public long ID = - 1;
	private String request = "";
	private List<Long> listEngine = new ArrayList<Long>();
	public long timestamp = 0;
	public String resultValue;
	private String resultOriginal; // result sent by engine before treatment
    public boolean saveInCache = true;
    private int engineVersion = -1;
    private int requestType = -1;
    private String deal = null;
    private String game = null;
    private String options = null;
    private String conventions = null;
    private int nbTricksForClaim = 0;
    private String claimPlayer = null;
    private Set<String> setAsyncID = null;
    private String urlSetResult = null;
    private UserVirtualFBServer user = null;
    private boolean logStat = false;
    private boolean isForCompare = false;

    public String toString() {
        return "ID="+ID+" - request="+request+" - resultValue="+resultValue+" - saveInCache="+saveInCache;
    }

	public int getNbEngine() {
		return listEngine.size();
	}
	
	public boolean isEngineComputing(long userID) {
		return listEngine.contains(userID);
	}
	
	public String engineComputingToString() {
		String temp = "";
		for (int i = 0; i < listEngine.size(); i++) {
			if (temp.length()>0) {
				temp+=" - ";
			}
			temp += listEngine.get(i);
		}
		return temp;
	}
	
	public void addEngineComputing(long userID) {
		listEngine.add(userID);
	}

	public void resetEngineComputing() {
		listEngine.clear();
	}
	
	public void removeEngineComputing(long userID) {
		listEngine.remove(new Long(userID));
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof QueueData)) {
			return false;
		}
		QueueData data = (QueueData)o;
		return ((data.ID == this.ID) && (data.request.equals(this.request))); 
	}
	
	@Override
	public int compareTo(QueueData o) {
		int thisNbEngine = this.getNbEngine();
		int otherNbEngine = o.getNbEngine();
		
		if (thisNbEngine < otherNbEngine) {
			return -1;
		} if (thisNbEngine > otherNbEngine) {
			return 1;
		}
		if (this.ID < o.ID) {
			return -1;
		}
		if (this.ID > o.ID) {
			return 1;
		}
		
		return 0;
	}

    /**
     * Convert int value to string hexa
     * @param value must be > 0
     * @return
     */
    private static String intToHexaString(int value) {
        StringBuilder sb = new StringBuilder();
        sb.append(Integer.toHexString(value));
        if (sb.length() %2 == 1) {
            sb.insert(0, "0");
        }
        return sb.toString().toUpperCase();
    }

    /**
     * change the engine version on request. setRequest method is called to extract all data.
     * @param newEngineVersion
     */
	public void changeRequestEngineVersion(int newEngineVersion) {
        String newOptions = options.substring(0, 6);
        newOptions += intToHexaString(newEngineVersion & 0xFF); // lowbyte
        newOptions += intToHexaString((newEngineVersion & 0xFF00) >> 8); // highbyte
        if (options.length() > 10) {
            newOptions += options.substring(10);
        }
        String newRequest = deal+Constantes.REQUEST_FIELD_SEPARATOR+
                newOptions+Constantes.REQUEST_FIELD_SEPARATOR+
                conventions+Constantes.REQUEST_FIELD_SEPARATOR+
                game+Constantes.REQUEST_FIELD_SEPARATOR+
                requestType+Constantes.REQUEST_FIELD_SEPARATOR+
                nbTricksForClaim+Constantes.REQUEST_FIELD_SEPARATOR+
                claimPlayer;
        setRequest(newRequest);
    }

    /**
     * Copy all data from origin object
     * @param origin
     */
    public void copyFrom(QueueData origin) {
        this.ID = origin.ID;
        this.request = origin.request;
        this.listEngine = new ArrayList<Long>(origin.listEngine);
        this.timestamp = origin.timestamp;
        this.saveInCache = origin.saveInCache;
        this.engineVersion = origin.engineVersion;
        this.requestType = origin.requestType;
        this.deal = origin.deal;
        this.game = origin.game;
        this.options = origin.options;
        this.conventions = origin.conventions;
        this.nbTricksForClaim = origin.nbTricksForClaim;
        this.claimPlayer = origin.claimPlayer;
        this.setAsyncID = origin.setAsyncID;
        this.urlSetResult = origin.urlSetResult;
        this.user = origin.user;
        this.logStat = origin.logStat;
        this.resultValue = origin.resultValue;
        this.resultOriginal = origin.resultOriginal;
    }

    public void setRequest(String strRequest) {
        if (strRequest != null && strRequest.length() > 0) {
            this.request = strRequest;
            // parse value to find values ...
            String[] temp = request.split(Constantes.REQUEST_FIELD_SEPARATOR);
            if (temp.length == Constantes.REQUEST_NB_FIELD) {
                // DEAL
                deal = temp[Constantes.REQUEST_INDEX_FIELD_DEAL];
                // ENGINE VERSION (field options)
                options = temp[Constantes.REQUEST_INDEX_FIELD_OPTIONS];
                try {
                    if (options.length() >= Constantes.REQUEST_FIELD_OPTIONS_LENGTH) {
                        engineVersion = Integer.decode("0x"+options.substring(8, 10)+options.substring(6, 8));
                    }
                }
                catch (Exception e) {
                    Logger log = LogManager.getLogger(this.getClass());
                    log.error("Unable to find engine version on options="+options, e);
                }
                // CONVENTIONS
                conventions = temp[Constantes.REQUEST_INDEX_FIELD_CONV];
                // GAME
                game = temp[Constantes.REQUEST_INDEX_FIELD_GAME];
                // REQUEST TYPE
                String strRequestType = temp[Constantes.REQUEST_INDEX_FIELD_TYPE];
                try {
                    requestType = Integer.parseInt(strRequestType);
                } catch (Exception e) {
                    Logger log = LogManager.getLogger(this.getClass());
                    log.error("Unable to parse requestType on strRequestType="+strRequestType, e);
                }
                // NB TRICKS FOR CLAIM
                String strNbTricksForClaim = temp[Constantes.REQUEST_INDEX_FIELD_NB_TRICKS_FOR_CLAIM];
                try {
                    nbTricksForClaim = Integer.parseInt(strNbTricksForClaim);
                } catch (Exception e) {
                    Logger log = LogManager.getLogger(this.getClass());
                    log.error("Unable to parse nbTricksForClaim - strNbTricksForClaim="+strNbTricksForClaim, e);
                }
                // CLAIM PLAYER
                claimPlayer = temp[Constantes.REQUEST_INDEX_FIELD_CLAIM_PLAYER];
            }
        }
    }

    public String getRequest() {
        return request;
    }

	/**
	 * Return the engine version contained in the options field
	 * @return
	 */
	public int getEngineVersion() {
        return engineVersion;
	}

    public int getRequestType() {
        return requestType;
    }

    public int getNbTricksForClaim() {return nbTricksForClaim;}

    public String getClaimPlayer() {
        return claimPlayer;
    }

    public String getDeal() {
        return deal;
    }

    public String getGame() {
        return game;
    }

    public String getOptions() {
        return options;
    }

    public String getConventions() {
        return conventions;
    }

    public void addAsyncID(String val) {
        if (val != null) {
            if (setAsyncID == null) {
                setAsyncID = new HashSet<>();
            }
            setAsyncID.add(val);
        }
    }

    public Set<String> getSetAsyncID() {
        return setAsyncID;
    }

    public String getUrlSetResult() {
        return urlSetResult;
    }

    public void setUrlSetResult(String urlSetResult) {
        this.urlSetResult = urlSetResult;
    }

    public UserVirtualFBServer getUser() {
        return user;
    }

    public void setUser(UserVirtualFBServer user) {
        this.user = user;
    }

    public boolean isLogStat() {
        return logStat;
    }

    public void setLogStat(boolean logStat) {
        this.logStat = logStat;
    }

    public boolean isForCompare() {
        return isForCompare;
    }

    public void setForCompare(boolean forCompare) {
        isForCompare = forCompare;
    }

    public String getResultOriginal() {
        return resultOriginal;
    }

    public void setResultOriginal(String resultOriginal) {
        this.resultOriginal = resultOriginal;
    }
}
