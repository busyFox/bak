package com.funbridge.server.presence;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.player.data.Device;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.tournament.game.Table;
import com.funbridge.server.tournament.serie.TourSerieMgr;
import com.funbridge.server.ws.event.Event;
import com.funbridge.server.ws.event.EventField;
import com.funbridge.server.ws.servlet.ClientWebSocketEndpoint;
import com.gotogames.common.session.Session;
import com.gotogames.common.tools.StringTools;

import java.util.*;

public class FBSession extends Session{
	private Table currentGameTable = null;
	private Player player = null;
	private String clientVersion = null;
	private int nbDealPlayed = 0;
	private Map<Integer, Integer> mapCategoryPlay = new HashMap<>();
    private Map<Integer, Integer> mapCategoryReplay = new HashMap<>();
	private List<Event> events = new ArrayList<Event>();
	private Set<Long> listDuelRequest = new HashSet<Long>();
	private Set<Long> listDuelReset = new HashSet<Long>();
	private Set<Long> listDuelInProgress = new HashSet<Long>();
	private long currentTableID = -1;
	private Device device = null;
	private long currentTrainingPartnerTableID = 0;
	private ClientWebSocketEndpoint webSocketEndpoint = null;

    private long dateLastConnection = 0;
    private String lastDeviceTypeUsed = "";
    private PlayerCache playerCache = null;
    private String disconnectValue = null;
    private int nbNewMessage = 0;
    public int storePromo = 0;
    public String protocol;
    private long tsSubscriptionExpiration = 0;
    private int creditDealsBoughtRemaining = 0; // nb deals bought remaining
    private String playerActions = "";
    private int nbFriends = 0;
    private int nbFollowing = 0;
    private int nbFollowers = 0;
    private boolean playerInTeam = false;
    private int nbCallStoreGetProducts = 0;
	private int nbMessagesSent = 0;
	private Map<String, Object> applicationStats = null;
	private boolean rpcEnabled = false;
	private int nbLearningDeals = 0;
	private String nursing = "";
	private boolean admin;

	public FBSession(Session s, Player p, String clientVersion, Device device) {
		this.setID(s.getID());
		this.setLogin(s.getLogin());
		this.setLoginID(s.getLoginID());
		this.setDateCreation(s.getDateCreation());
		this.setDateLastActivity(s.getDateLastActivity());
		this.setTimeout(s.getTimeout());
		this.setPlayer(p);
		this.clientVersion = clientVersion;
		this.device = device;
	}

	
	public long getDeviceID() {
		if (device != null) {
			return device.getID();
		} 
		return -1;
	}

	public Device getDevice() {
		return device;
	}
	
	public Player getPlayer() {
		return player;
	}

	public void setPlayer(Player player) {
		this.player = player;
	}

	public String getClientVersion() {
		return clientVersion;
	}

	public void setClientVersion(String clientVersion) {
		this.clientVersion = clientVersion;
	}

	public String getDeviceType() {
		if (device != null) {
			return device.getType();
		}
		return "";
	}

	public void incrementNbLearningDeals() {
	    this.nbLearningDeals++;
    }

    public int getNbLearningDeals() {
        return nbLearningDeals;
    }

	public void incrementNbDealPlayed(int category, int nbDeal) {
		this.nbDealPlayed+=nbDeal;
		int temp = 0;
		if (mapCategoryPlay.containsKey(category)) {
		    temp = mapCategoryPlay.get(category);
        }
        mapCategoryPlay.put(category, temp+nbDeal);
	}

	public void incrementNbDealReplay(int category, int nbDeal) {
        int temp = 0;
        if (mapCategoryReplay.containsKey(category)) {
            temp = mapCategoryReplay.get(category);
        }
        mapCategoryReplay.put(category, temp+nbDeal);
    }
	
	public int getNbDealPlayed() {
		return nbDealPlayed;
	}

	public int getNbDealReplay() {
	    int nbReplay = 0;
	    for (Integer e : mapCategoryReplay.values()) {
	        nbReplay += e;
        }
	    return nbReplay;
    }
	
	public String toString() {
		if (player != null) {
			return "playerID="+player.getID()+" - nickname="+player.getNickname()+" - dateLastActivity="+Constantes.timestamp2StringDateHour(getDateLastActivity())+" - dateLastConnetion="+Constantes.timestamp2StringDateHour(dateLastConnection)+" - clientVersion"+clientVersion+" - deviceType="+getDeviceType()+" - currentGameTable=["+currentGameTable+"]";
		}
		return "no player !!";
	}
	
	/**
	 * Add event to the existing list 
	 * @param event
	 */
	public void pushEvent(Event event) {
		if (event != null) {
            synchronized (events) {
				events.add(event);
				if (ContextManager.getPresenceMgr().getLogger().isDebugEnabled()) {
					ContextManager.getPresenceMgr().getLogger().debug(toString()+" - add event="+event+" - events="+StringTools.listToString(events));
				}
                if (webSocketEndpoint != null) {
                    webSocketEndpoint.pushEvent(event);
                }
			}
		}
	}
	
	/**
	 * Add list event to the existing list
	 * @param listEvent
	 */
	public void pushListEvent(List<Event> listEvent) {
		if (listEvent != null && listEvent.size() > 0) {
			synchronized (events) {
				for (Event e : listEvent) {
					e.receiverID = getLoginID();
					events.add(e);
				}
				if (ContextManager.getPresenceMgr().getLogger().isDebugEnabled()) {
					ContextManager.getPresenceMgr().getLogger().debug(toString()+" - add nbEvent="+listEvent.size()+" - listEvent="+listEvent);
				}
                if (webSocketEndpoint != null) {
                    webSocketEndpoint.pushEvents(listEvent);
                }
			}
			listEvent.clear();
		}
	}
	
	/**
	 * Remove all event matching filter
	 * @param filter
	 * @return
	 */
	public int purgeEvent(FilterEvent filter) {
		int nbEventPurge = 0;
		synchronized (events) {
			Iterator<Event> it = events.iterator();
			while (it.hasNext()) {
				Event e = it.next();	
				if (filter.matchesEvent(e)) {
					// remove event from list
					it.remove();
					nbEventPurge++;
				}
			}
		}
		if (ContextManager.getPresenceMgr().getLogger().isDebugEnabled()) {
			ContextManager.getPresenceMgr().getLogger().debug(toString() + " - filter=" + filter + " - nbEventPurge=" + nbEventPurge);
		}
		return nbEventPurge;
	}
	
	/**
	 * Return list of events according to filter and with timestamp > value. All event matching filter avec with timestamp < value are removed.
	 * @param tsSinceTime
	 * @param filter
	 * @return
	 */
	public List<Event> popEvents(long tsSinceTime, FilterEvent filter) {
        long ts = System.currentTimeMillis();
		List<Event> result = new ArrayList<Event>();
		synchronized (events) {
			Iterator<Event> it = events.iterator();
			while (it.hasNext()) {
				Event e = it.next();
				if (filter.matchesEvent(e)) {
					if (e.timestamp > tsSinceTime) {
						result.add(e);
					} else {
						// remove old event from list (client has already have event)
						it.remove();
					}
				}
			}
		}
		if (!result.isEmpty()) {
			if (ContextManager.getPresenceMgr().getLogger().isDebugEnabled()) {
				ContextManager.getPresenceMgr().getLogger().debug(toString()+" - tsSinceTime="+tsSinceTime+" - nbEventsRemaining="+events.size()+" - popEvents="+StringTools.listToString(result));
			}
		}
		return result;
	}
	
	/**
	 * Remove events with timestamps <= tsLimit
	 * @param tsLimit
	 * @return
	 */
	public int removeEvents(long tsLimit) {
		int nbEventsRemoved = 0;
		synchronized (events) {
			Iterator<Event> it = events.iterator();
			while (it.hasNext()) {
				Event e = it.next();
				if (e.timestamp <= tsLimit) {
					it.remove();
					nbEventsRemoved++;
				}
			}
		}
		if (ContextManager.getPresenceMgr().getLogger().isDebugEnabled()) {
			ContextManager.getPresenceMgr().getLogger().debug(toString()+" - tsLimit="+tsLimit+" - nbEventsRemoved="+nbEventsRemoved);
		}
		return nbEventsRemoved;
	}
	
	/**
	 * Return the entire events list
	 * @return
	 */
	public List<Event> getListEvent() {
		return events;
	}
	
	public synchronized long getCurrentTableID() {
		return currentTableID;
	}

	public synchronized void setCurrentTableID(long currentTableID) {
		this.currentTableID = currentTableID;
	}
	
	public String getInfo() {
		String info = "";
		info = "playerID="+player.getID()+" - deviceType="+getDeviceType()+" - deviceID="+getDeviceID()+" - clientVersion="+clientVersion;
		if (currentGameTable != null) {
			info += " - table="+currentGameTable;
		}
		return info;
	}

	public void addDuelRequest(long playerDuelID) {
		synchronized (listDuelRequest) {
			listDuelRequest.add(playerDuelID);
		}
	}
	
	public void removeDuelRequest(long playerDuelID) {
		synchronized (listDuelRequest) {
			listDuelRequest.remove(playerDuelID);
		}
	}
	
	public int countDuelRequest() {
		synchronized (listDuelRequest) {
			return listDuelRequest.size();
		}
	}
	
	public void clearDuelRequest() {
		synchronized (listDuelRequest) {
			listDuelRequest.clear();
		}
	}
	
	public void addDuelInProgress(long playerDuelID) {
		synchronized (listDuelInProgress) {
			listDuelInProgress.add(playerDuelID);
		}
	}
	
	public void removeDuelInProgress(long playerDuelID) {
		synchronized (listDuelInProgress) {
			listDuelInProgress.remove(playerDuelID);
		}
	}
	
	public int countDuelInProgress() {
		synchronized (listDuelInProgress) {
			return listDuelInProgress.size();
		}
	}
	
	public void clearDuelInProgress() {
		synchronized (listDuelInProgress) {
			listDuelInProgress.clear();
		}
	}
	
	public void addDuelReset(long playerDuelID) {
		synchronized (listDuelReset) {
			listDuelReset.add(playerDuelID);
		}
	}
	
	public void removeDuelReset(long playerDuelID) {
		synchronized (listDuelReset) {
			listDuelReset.remove(playerDuelID);
		}
	}
	
	public int countDuelReset() {
		synchronized (listDuelReset) {
			return listDuelReset.size();
		}
	}
	
	public void clearDuelReset() {
		synchronized (listDuelReset) {
			listDuelReset.clear();
		}
	}
	
	public Table getCurrentGameTable() {
		return currentGameTable;
	}
	public void setCurrentGameTable(Table table) {
		this.currentGameTable = table;
	}

	public void removeGame() {
		if (currentGameTable != null) {
			currentGameTable.setGame(null);
		}
	}


	public void setWebSocket(ClientWebSocketEndpoint ws) {
		webSocketEndpoint = ws;
	}
	
	public void removeWebSocket(ClientWebSocketEndpoint ws) {
		if (webSocketEndpoint == ws) {
            if (ContextManager.getPresenceMgr().getLogger().isDebugEnabled()) {
                ContextManager.getPresenceMgr().getLogger().debug("Remove websocket for session=" + this);
            }
			webSocketEndpoint = null;
		} else {
            if (ContextManager.getPresenceMgr().getLogger().isDebugEnabled()) {
                ContextManager.getPresenceMgr().getLogger().debug("current websocket and param not same => no remove is done for session=" + this);
            }
		}
	}
	
	public ClientWebSocketEndpoint getWebSocket() {
		return webSocketEndpoint;
	}
	
	public long getCurrentTrainingPartnerTableID() {
		return currentTrainingPartnerTableID;
	}
	
	public void setCurrentTrainingPartnerTableID(long value) {
		currentTrainingPartnerTableID = value;
	}
	
	/**
	 * Return the current time stamp. A sleep time of 2 ms is do to be sure to have different TS
	 * @return
	 */
	public synchronized long getTimeStamp() {
		try {
			Thread.sleep(2);
		} catch (InterruptedException e) {
		}
		return System.currentTimeMillis();
	}

	public void pushEventGame(Table table, String type, String typeData, EventField[] events) {
		if (player != null && table != null) {
			Event evt = new Event();
			evt.timestamp = getTimeStamp();
			evt.senderID = Constantes.EVENT_PLAYER_ID_SERVER;
			evt.receiverID = player.getID();
			String gameStep = null;
			if (table.getGame() != null) {
				gameStep = ""+table.getGame().getStep();
			}
			evt.addFieldCategory(Constantes.EVENT_CATEGORY_GAME, gameStep);
			evt.addFieldType(type, typeData);
			evt.addField(new EventField(Constantes.EVENT_FIELD_TABLE_ID, ""+table.getID(), null));
			if (table.getGame() != null) {
				evt.addField(new EventField(Constantes.EVENT_FIELD_GAME_ID, table.getGame().getIDStr(), null));
			}
			if (events != null) {
				for (int i = 0; i < events.length; i++) {
					evt.addField(events[i]);
				}
			}
			pushEvent(evt);
		}
	}

	public long getDateLastConnection() {
        return dateLastConnection;
    }

    public void setDateLastConnection(long dateLastConnection) {
        this.dateLastConnection = dateLastConnection;
    }


    public String getSerie() {
        if (playerCache != null) {
            return playerCache.serie;
        }
        return TourSerieMgr.SERIE_NC;
    }

    public PlayerCache getPlayerCache() {
        return playerCache;
    }

    public void setPlayerCache(PlayerCache o) {
        this.playerCache = o;
    }

    public String getDisconnectValue() {
        return disconnectValue;
    }

    public void setDisconnectValue(String disconnectValue) {
        this.disconnectValue = disconnectValue;
    }

    public int getNbNewMessage() {
        return nbNewMessage;
    }

    public void setNbNewMessage(int nbNewMessage) {
        this.nbNewMessage = nbNewMessage;
    }

    public long getTsSubscriptionExpiration() {
        return tsSubscriptionExpiration;
    }

	public void setTsSubscriptionExpiration(long subscriptionExpiration) {
		if (subscriptionExpiration > 0 ){
			this.tsSubscriptionExpiration = subscriptionExpiration ;
		}
	}

    public int getCreditDealsBoughtRemaining() {
        return creditDealsBoughtRemaining;
    }

    public void setCreditDealsBoughtRemaining(int creditDealsBoughtRemaining) {
        this.creditDealsBoughtRemaining = creditDealsBoughtRemaining;
    }

    public boolean isFreemium() {
	    if (tsSubscriptionExpiration <= System.currentTimeMillis()) {
	        // subscription expire => check nb deals bought remaining
            return getCreditDealsBoughtRemaining() <= 0;
        }
        return false;
    }

    public Map<Integer, Integer> getMapCategoryPlay() {
        return mapCategoryPlay;
    }

    public Map<Integer, Integer> getMapCategoryReplay() {
        return mapCategoryReplay;
    }

    public String getPlayerActions() {
        return playerActions;
    }

    public void addPlayerActions(String action) {
	    if (playerActions.length() > 0) {
	        playerActions += Constantes.SEPARATOR_VALUE;
        }
        playerActions += action;
    }

    public int getNbFriends() {
        return nbFriends;
    }

    public void setNbFriends(int nbFriends) {
        this.nbFriends = nbFriends;
    }

	public int getNbFollowing() {
		return nbFollowing;
	}

	public void setNbFollowing(int nbFollowing) {
		this.nbFollowing = nbFollowing;
	}

	public int getNbFollowers() {
		return nbFollowers;
	}

	public void setNbFollowers(int nbFollowers) {
		this.nbFollowers = nbFollowers;
	}

	public boolean isAvatarPlayerPresent() {
	    if (player != null) {
	        return player.isAvatarPresent();
        }
        return false;
    }

    public boolean isPlayerInTeam() {
        return playerInTeam;
    }

    public void setPlayerInTeam(boolean playerInTeam) {
        this.playerInTeam = playerInTeam;
    }

    public int getNbCallStoreGetProducts() {
        return nbCallStoreGetProducts;
    }

    public void incrementNbCallStoreGetProducts() {
	    nbCallStoreGetProducts++;
    }

    public int getNbMessagesSent() {
        return nbMessagesSent;
    }

    public String getLastDeviceTypeUsed() {
        return lastDeviceTypeUsed;
    }

    public void setLastDeviceTypeUsed(String lastDeviceTypeUsed) {
        this.lastDeviceTypeUsed = lastDeviceTypeUsed;
    }

    public Map<String, Object> getApplicationStats() {
        return applicationStats;
    }

    public void setApplicationStats(Map<String, Object> applicationStats) {
        this.applicationStats = applicationStats;
    }

    public boolean isRpcEnabled() {
        return rpcEnabled;
    }

    public void setRpcEnabled(boolean rpcEnabled) {
        this.rpcEnabled = rpcEnabled;
    }

	public String getNursing() {
		return nursing;
	}

	public void setNursing(String nursing) {
		this.nursing = nursing;
	}

	public boolean isAdmin() {
		return admin;
	}

	public void setAdmin(boolean admin) {
		this.admin = admin;
	}
}
