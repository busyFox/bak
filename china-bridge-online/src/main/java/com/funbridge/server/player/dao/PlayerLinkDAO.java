package com.funbridge.server.player.dao;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.player.data.PlayerLink;
import com.funbridge.server.tournament.serie.TourSerieMgr;
import com.funbridge.server.ws.player.WSPlayerLinked;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Repository(value="playerLinkDAO")
public class PlayerLinkDAO {
	@PersistenceContext
	private EntityManager em;
	
	protected Logger log = LogManager.getLogger(this.getClass());
	
	/**
	 * create a link between players
	 * @param player1
	 * @param player2
	 * @param typeMask1
	 * @param typeMask2
	 * @return
	 */
	public PlayerLink addLinkPlayer(Player player1, Player player2, int typeMask1, int typeMask2, String message) {
		if (player1 != null && player2 != null) {
			PlayerLink pl = new PlayerLink();
			pl.setPlayer1(player1);
			pl.setPlayer2(player2);
			pl.setTypeMask1(typeMask1);
			pl.setTypeMask2(typeMask2);
			pl.setDateLink(System.currentTimeMillis());
			pl.setMessage(message);
			try {
				em.persist(pl);
				return pl;
			} catch (Exception e) {
				log.error("Error to persist playerLink="+pl, e);
			}
		}
		return null;
	}
	
	/**
	 * Remove player link with ID
	 * @param plID
	 * @return
	 */
	public boolean remove(long plID) {
		try {
			Query q = em.createNamedQuery("playerLink.deleteLink");
			q.setParameter("plID", plID);
			return (q.executeUpdate() == 1);
		} catch (Exception e) {
			log.error("Exception to remove link player with ID="+plID, e);
		}
		return false;
	}
	
	/**
	 * Update player link
	 * @param pl
	 * @return
	 */
	public PlayerLink update(PlayerLink pl) {
		if (pl != null) {
			try {
				return em.merge(pl);
			} catch (Exception e) {
				log.error("Error trying to merge player link="+pl.toString(), e);
			}
		}
		return null;
	}
	
	/**
	 * Return list of player linked with type follower
	 * @param playerID
	 * @param offset
	 * @param nbMax
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public List<WSPlayerLinked> listWSPlayerLinkedFollower(long playerID, int offset, int nbMax) {
		try {
			List<WSPlayerLinked> listLink = new ArrayList<WSPlayerLinked>();
			// Request : link follower can be done by pla1 or pla2 
			String strQuery = "select p.id, p.nickname, ";
			boolean avatarUseNewField = false;
			if (FBConfiguration.getInstance().getIntValue("player.avatar.useNewField", 1) == 1) {
				avatarUseNewField = true;
				strQuery += "p.avatar_date_validation, ";
			} else {
				strQuery += "p.avatar_present, ";
			}
			strQuery += "p.country_code from (";
			strQuery +=	"(SELECT player2 as plaid FROM `player_link` where player1=:plaID and (type_mask1 & :typeFollow) =:typeFollow and (type_mask1 & :typeFriend) != :typeFriend) union " +
					"(SELECT player1 as plaid FROM `player_link` where player2=:plaID and (type_mask2 & :typeFollow) =:typeFollow and (type_mask1 & :typeFriend) != :typeFriend)) t, " +
					"player p where t.plaid=p.id order by p.nickname";
			Query q = em.createNativeQuery(strQuery);
			q.setParameter("plaID", playerID);
			q.setParameter("typeFollow", Constantes.PLAYER_LINK_TYPE_FOLLOWER);
			q.setParameter("typeFriend", Constantes.PLAYER_LINK_TYPE_FRIEND);
			if (offset > 0) {
				q.setFirstResult(offset);
			}
			if (nbMax > 0) {
				q.setMaxResults(nbMax);
			}
			List l = q.getResultList();
			if (l != null && l.size() > 0) {
				for (int i = 0; i < l.size(); i++) {
					Object[] temp = (Object[])(l.get(i));
					if (temp.length == 5) {
						WSPlayerLinked wspl = new WSPlayerLinked();
						wspl.playerID = ((BigInteger)temp[0]).longValue();
						wspl.pseudo = (String)temp[1];
						PlayerCache pc = ContextManager.getPlayerCacheMgr().getPlayerCache(wspl.playerID);
						if (pc != null) {
							wspl.serie = pc.serie;
						} else {
							wspl.serie = TourSerieMgr.SERIE_NC;
						}
                		if (avatarUseNewField) {
							wspl.avatar = ((BigInteger)temp[3]).longValue()>0;
						} else {
							wspl.avatar = ((Boolean)temp[3]).booleanValue();
						}
						wspl.country = (String)temp[4];
						wspl.relationMask = Constantes.PLAYER_LINK_TYPE_FOLLOWER;
						listLink.add(wspl);
					}
				}
			}
			return listLink;
		} catch (Exception e) {
			log.error("Exception to list link follower for playerID="+playerID+" - offset="+offset+" - nbMax="+nbMax, e);
		}
		return null;
	}
	
	/**
	 * Count link follower for playerID
	 * @param playerID
	 * @return
	 */
	public int countLinkFollower(long playerID) {
		try {
			Query q = em.createNativeQuery("select count(*) from (" +
					"(SELECT player2 as plaid FROM `player_link` where player1=:plaID and (type_mask1 & :type) =:type and (type_mask1 & :typeFriend) != :typeFriend) union " +
					"(SELECT player1 as plaid FROM `player_link` where player2=:plaID and (type_mask2 & :type) =:type and (type_mask2 & :typeFriend) != :typeFriend)) t");
			q.setParameter("plaID", playerID);
			q.setParameter("type", Constantes.PLAYER_LINK_TYPE_FOLLOWER);
			q.setParameter("typeFriend", Constantes.PLAYER_LINK_TYPE_FRIEND);
			return ((BigInteger)q.getSingleResult()).intValue();
		} catch (Exception e) {
			log.error("Exception to count link follower for playerID="+playerID, e);
		}
		return 0;
	}
	
	/**
	 * Return list of player linked with type friend pending
	 * @param playerID
	 * @param offset
	 * @param nbMax
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public List<WSPlayerLinked> listWSPlayerLinkedPending(long playerID, int offset, int nbMax) {
		try {
			List<WSPlayerLinked> listLink = new ArrayList<WSPlayerLinked>();
			// Request : pending from playerID + pending from others to playerID
			String strQuery = "select p.id, p.nickname, ";
			boolean avatarUseNewField = false;
			if (FBConfiguration.getInstance().getIntValue("player.avatar.useNewField", 1) == 1) {
				avatarUseNewField = true;
				strQuery += "p.avatar_date_validation, ";
			} else {
				strQuery += "p.avatar_present, ";
			}
			strQuery += "p.country_code, t.way, t.message from (" +
					"(SELECT player2 as plaid, 1 as way, message FROM `player_link` where player1=:plaID and (type_mask1 & :type) =:type) union " +
					"(SELECT player2 as plaid, 0 as way, message FROM `player_link` where player1=:plaID and (type_mask2 & :type) =:type) union " +
					"(SELECT player1 as plaid, 1 as way, message FROM `player_link` where player2=:plaID and (type_mask2 & :type) =:type) union " +
					"(SELECT player1 as plaid, 0 as way, message FROM `player_link` where player2=:plaID and (type_mask1 & :type) =:type)) t, " +
					"player p where t.plaid=p.id group by t.plaid order by p.nickname";
			Query q = em.createNativeQuery(strQuery);
			
			q.setParameter("plaID", playerID);
			q.setParameter("type", Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING);
			if (offset > 0) {
				q.setFirstResult(offset);
			}
			if (nbMax > 0) {
				q.setMaxResults(nbMax);
			}
			List l = q.getResultList();
			if (l != null && l.size() > 0) {
				for (int i = 0; i < l.size(); i++) {
					Object[] temp = (Object[])(l.get(i));
					if (temp.length == 7) {
						WSPlayerLinked wspl = new WSPlayerLinked();
						wspl.playerID = ((BigInteger)temp[0]).longValue();
						wspl.pseudo = (String)temp[1];
                        PlayerCache pc = ContextManager.getPlayerCacheMgr().getPlayerCache(wspl.playerID);
						if (pc != null) {
							wspl.serie = pc.serie;
						} else {
							wspl.serie = TourSerieMgr.SERIE_NC;
						}
                        if (avatarUseNewField) {
							wspl.avatar = ((BigInteger)temp[3]).longValue() > 0;
						} else {
							wspl.avatar = ((Boolean)temp[3]).booleanValue();
						}
						wspl.country = (String)temp[4];
						wspl.relationMask = Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING;
						if (((BigInteger)temp[5]).intValue() == 1) {
							wspl.relationMask = wspl.relationMask | Constantes.PLAYER_LINK_TYPE_WAY;
						}
						wspl.message = (String)temp[6];
						listLink.add(wspl);
					}
				}
			}
			return listLink;
		} catch (Exception e) {
			log.error("Exception to list link pending for playerID="+playerID+" - offset="+offset+" - nbMax="+nbMax, e);
		}
		return null;
	}
	
	/**
	 * Count link friend pending for player
	 * @param playerID
	 * @return
	 */
	public int countLinkFriendPending(long playerID) {
		try {
			Query q = em.createNativeQuery("select count(distinct(plaid)) from (" +
					"(SELECT player2 as plaid FROM `player_link` where player1=:plaID and (type_mask1 & :type) =:type) union " +
					"(SELECT player1 as plaid FROM `player_link` where player2=:plaID and (type_mask2 & :type) =:type) union " +
					"(SELECT player1 as plaid FROM `player_link` where player2=:plaID and (type_mask1 & :type) =:type) union " +
					"(SELECT player2 as plaid FROM `player_link` where player1=:plaID and (type_mask2 & :type) =:type)) t");
			q.setParameter("plaID", playerID);
			q.setParameter("type", Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING);
			return ((BigInteger)q.getSingleResult()).intValue();
		} catch (Exception e) {
			log.error("Exception to count link friend pending follower for playerID="+playerID, e);
		}
		return 0;
	}
	
	/**
	 * Count link friend pending where player is receiver of the request
	 * @param playerID
	 * @return
	 */
	public int countLinkFriendPendingForReceiver(long playerID) {
		try {
			Query q = em.createNativeQuery("select count(distinct(plaid)) from (" +
					"(SELECT player2 as plaid FROM `player_link` where player1=:plaID and (type_mask2 & :type) =:type) union " +
					"(SELECT player1 as plaid FROM `player_link` where player2=:plaID and (type_mask1 & :type) =:type)) t");
			q.setParameter("plaID", playerID);
			q.setParameter("type", Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING);
			return ((BigInteger)q.getSingleResult()).intValue();
		} catch (Exception e) {
			log.error("Exception to count link friend pending follower for receiver playerID="+playerID, e);
		}
		return 0;
	}
	
	/**
	 * Count nb friend + nb player who follow this player
	 * @param playerID
	 * @return
	 */
	public int countFriendAndFollower(long playerID) {
		try {
			Query q = em.createNativeQuery("select count(distinct plaid) from (" +
					"(SELECT player2 as plaid FROM `player_link` where player1=:plaID and (type_mask1 & :typeFriend) =:typeFriend) union " +
					"(SELECT player1 as plaid FROM `player_link` where player2=:plaID and (type_mask2 & :typeFriend) =:typeFriend) union " +
					"(SELECT player1 as plaid FROM `player_link` where player2=:plaID and (type_mask1 & :typeFollower) =:typeFollower) union" +
					"(SELECT player2 as plaid FROM `player_link` where player1=:plaID and (type_mask2 & :typeFollower) =:typeFollower)) t");
			q.setParameter("plaID", playerID);
			q.setParameter("typeFriend", Constantes.PLAYER_LINK_TYPE_FRIEND);
			q.setParameter("typeFollower", Constantes.PLAYER_LINK_TYPE_FOLLOWER);
			return ((BigInteger)q.getSingleResult()).intValue();
		} catch (Exception e) {
			log.error("Exception to count friend and follower follower for playerID="+playerID, e);
		}
		return 0;
	}
	
	/**
	 * Nb players who have a link follower (and not friend link) to this player
	 * @param playerID
	 * @return
	 */
	public int countFollowerForPlayer(long playerID) {
		try {
			Query q = em.createNativeQuery("select count(distinct plaid) from (" +
					"(SELECT player1 as plaid FROM `player_link` where player2=:plaID and (type_mask1 & :typeFollower) = :typeFollower and (type_mask1 & :typeFriend) != :typeFriend) union" +
					"(SELECT player2 as plaid FROM `player_link` where player1=:plaID and (type_mask2 & :typeFollower) = :typeFollower and (type_mask1 & :typeFriend) != :typeFriend)) t");
			q.setParameter("plaID", playerID);
			q.setParameter("typeFriend", Constantes.PLAYER_LINK_TYPE_FRIEND);
			q.setParameter("typeFollower", Constantes.PLAYER_LINK_TYPE_FOLLOWER);
			return ((BigInteger)q.getSingleResult()).intValue();
		} catch (Exception e) {
			log.error("Exception to count friend and follower follower for playerID="+playerID, e);
		}
		return 0;
	}

	/**
	 * Nb players for this player having a link follower (and not friend link)
	 * @param playerID
	 * @return
	 */
	public int countFollowingForPlayer(long playerID) {
		try {
			Query q = em.createNativeQuery("select count(distinct plaid) from (" +
					"(SELECT player1 as plaid FROM `player_link` where player2=:plaID and (type_mask2 & :typeFollower) = :typeFollower and (type_mask1 & :typeFriend) != :typeFriend) union" +
					"(SELECT player2 as plaid FROM `player_link` where player1=:plaID and (type_mask1 & :typeFollower) = :typeFollower and (type_mask1 & :typeFriend) != :typeFriend)) t");
			q.setParameter("plaID", playerID);
			q.setParameter("typeFriend", Constantes.PLAYER_LINK_TYPE_FRIEND);
			q.setParameter("typeFollower", Constantes.PLAYER_LINK_TYPE_FOLLOWER);
			return ((BigInteger)q.getSingleResult()).intValue();
		} catch (Exception e) {
			log.error("Exception to count friend and follower follower for playerID="+playerID, e);
		}
		return 0;
	}
	
	/**
	 * List of playerID with friend pending link to playerID. Request sent by this playerID.
	 * @param playerID
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public List<Long> listPlayerIDLinkedFriendPendingRequest(long playerID) {
		try {
			List<Long> listResult = new ArrayList<Long>();
			Query q = em.createNativeQuery("select distinct(plaid) from (" +
					"(SELECT player1 as plaid FROM player_link where player2=:plaID and (type_mask2 & :typeFriendPending) =:typeFriendPending) union " +
					"(SELECT player2 as plaid FROM player_link where player1=:plaID and (type_mask1 & :typeFriendPending) =:typeFriendPending)) t");
			q.setParameter("plaID", playerID);
			q.setParameter("typeFriendPending", Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING);
			List l = q.getResultList();
			if (l != null && l.size() > 0) {
				for (int i = 0; i < l.size(); i++) {
					listResult.add(((BigInteger)l.get(i)).longValue());
				}
			}
			return listResult;
		} catch (Exception e) {
			log.error("Exception to list playerID link friend pending for playerID="+playerID, e);
		}
		return null;
	}

    /**
     * List of playerID with block link to playerID. Players has blocked this playerID + players blocked by this playerID.
     * @param playerID
     * @return
     */
    @SuppressWarnings("rawtypes")
    public List<Long> listPlayerIDLinkedBlocked(long playerID) {
        try {
            List<Long> listResult = new ArrayList<Long>();
            Query q = em.createNativeQuery("select distinct(plaid) from (" +
                    "(SELECT player1 as plaid FROM player_link where player2=:plaID and (type_mask2 & :typeBlock) =:typeBlock) union " +
                    "(SELECT player2 as plaid FROM player_link where player1=:plaID and (type_mask1 & :typeBlock) =:typeBlock)) t");
            q.setParameter("plaID", playerID);
            q.setParameter("typeBlock", Constantes.PLAYER_LINK_TYPE_BLOCKED);
            List l = q.getResultList();
            if (l != null && l.size() > 0) {
                for (int i = 0; i < l.size(); i++) {
                    listResult.add(((BigInteger)l.get(i)).longValue());
                }
            }
            return listResult;
        } catch (Exception e) {
            log.error("Exception to list playerID link blocked for playerID="+playerID, e);
        }
        return null;
    }

	public int countLinkedBlockedForPlayer(long playerID) {
		try {
			List<Long> listResult = new ArrayList<Long>();
			Query q = em.createNativeQuery("select count(distinct(plaid)) from (" +
					"(SELECT player1 as plaid FROM player_link where player2=:plaID and (type_mask1 & :typeBlock) =:typeBlock) union " +
					"(SELECT player2 as plaid FROM player_link where player1=:plaID and (type_mask2 & :typeBlock) =:typeBlock)) t");
			q.setParameter("plaID", playerID);
			q.setParameter("typeBlock", Constantes.PLAYER_LINK_TYPE_BLOCKED);
			return ((BigInteger)q.getSingleResult()).intValue();
		} catch (Exception e) {
			log.error("Exception to count link blocked with playerID="+playerID, e);
		}
		return 0;
	}

	/**
	 * List of playerID with friend pending link to playerID. Request sent to this playerID and list of player waiting for response.
	 * @param playerID
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public List<Long> listPlayerIDLinkedFriendPendingWaiting(long playerID) {
		try {
			List<Long> listResult = new ArrayList<Long>();
			Query q = em.createNativeQuery("select distinct(plaid) from (" +
					"(SELECT player1 as plaid FROM player_link where player2=:plaID and (type_mask1 & :typeFriendPending) =:typeFriendPending) union " +
					"(SELECT player2 as plaid FROM player_link where player1=:plaID and (type_mask2 & :typeFriendPending) =:typeFriendPending)) t");
			q.setParameter("plaID", playerID);
			q.setParameter("typeFriendPending", Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING);
			List l = q.getResultList();
			if (l != null && l.size() > 0) {
				for (int i = 0; i < l.size(); i++) {
					listResult.add(((BigInteger)l.get(i)).longValue());
				}
			}
			return listResult;
		} catch (Exception e) {
			log.error("Exception to list playerID link friend pending for playerID="+playerID, e);
		}
		return null;
	}
	
	/**
	 * Return list of player id followed by this playerID
	 * @param playerID
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public List<Long> listPlayerIDLinkedFollower(long playerID) {
		try {
			List<Long> listResult = new ArrayList<Long>();
            Query q = em.createNativeQuery("select distinct(plaid) from (" +
                    "(SELECT player2 as plaid FROM player_link where player1=:plaID and type_mask1 in (:typeFollow, :typeFriend, :typeFollowFriend, :typeFollowFriendPending)) union " +
                    "(SELECT player1 as plaid FROM player_link where player2=:plaID and type_mask2 in (:typeFollow, :typeFriend, :typeFollowFriend, :typeFollowFriendPending))) t");
			q.setParameter("plaID", playerID);
			q.setParameter("typeFollow", Constantes.PLAYER_LINK_TYPE_FOLLOWER);
            q.setParameter("typeFriend", Constantes.PLAYER_LINK_TYPE_FRIEND);
            q.setParameter("typeFollowFriend", Constantes.PLAYER_LINK_TYPE_FRIEND | Constantes.PLAYER_LINK_TYPE_FOLLOWER);
            q.setParameter("typeFollowFriendPending", Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING | Constantes.PLAYER_LINK_TYPE_FOLLOWER);
			List l = q.getResultList();
			if (l != null && l.size() > 0) {
				for (int i = 0; i < l.size(); i++) {
					listResult.add(((BigInteger)l.get(i)).longValue());
				}
			}
			return listResult;
		} catch (Exception e) {
			log.error("Exception to list playerID link follower for playerID="+playerID, e);
		}
		return null;
	}
	
	/**
	 * Return list of player id with friend link to this playerID
	 * @param playerID
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public List<Long> listPlayerIDLinkedFriend(long playerID) {
		try {
			List<Long> listResult = new ArrayList<Long>();
			Query q = em.createNativeQuery("select if (player1=:plaID,player2,player1) as pla_id from player_link pl where (type_mask1 & :typeFriend)=:typeFriend and (player1=:plaID or player2=:plaID)");
			q.setParameter("plaID", playerID);
			q.setParameter("typeFriend", Constantes.PLAYER_LINK_TYPE_FRIEND);
			List l = q.getResultList();
			if (l != null && l.size() > 0) {
				for (int i = 0; i < l.size(); i++) {
					listResult.add(((BigInteger)l.get(i)).longValue());
				}
			}
			return listResult;
		} catch (Exception e) {
			log.error("Exception to list playerID link friend for playerID="+playerID, e);
		}
		return null;
	}
	
	/**
	 * List player friend for player
	 * @param playerID
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Player> listFriendForPlayer(long playerID) {
		String strQuery = "select p.* from player p join " +
				"(select if (player1=:plaID,player2,player1) as pla_id from player_link pl where (type_mask1 & :typeFriend)=:typeFriend and (player1=:plaID or player2=:plaID)) t " +
				"on p.id=t.pla_id";
		try {
			Query q = em.createNativeQuery(strQuery, Player.class);
			q.setParameter("plaID", playerID);
			q.setParameter("typeFriend", Constantes.PLAYER_LINK_TYPE_FRIEND);
			return q.getResultList();
		} catch (Exception e) {
			log.error("Exception to list friend for playerID="+playerID+" - query="+strQuery, e);
		}
		return null;
	}
	
	/**
	 * Return list of ID for player friend with playerID
	 * @param playerID
	 * @param random
	 * @return
	 */
	public List<Long> listFriendIDForPlayer(long playerID, boolean random, int nbMax) {
		String strQuery = "select if (player1=:plaID,player2,player1) as pla_id from player_link pl where (type_mask1 & :typeFriend)=:typeFriend and (player1=:plaID or player2=:plaID)";
		if (random) {
			strQuery += " order by rand()";
		}
		try {
			Query q = em.createNativeQuery(strQuery);
			q.setParameter("plaID", playerID);
			q.setParameter("typeFriend", Constantes.PLAYER_LINK_TYPE_FRIEND);
			if (nbMax > 0) {
				q.setMaxResults(nbMax);
			}
			List<Long> listResult = new ArrayList<Long>();
			List l = q.getResultList();
			if (l != null && l.size() > 0) {
				for (int i = 0; i < l.size(); i++) {
					listResult.add(((BigInteger)l.get(i)).longValue());
				}
			}
			return listResult;
		} catch (Exception e) {
			log.error("Exception to list friend for playerID="+playerID+" - query="+strQuery, e);
		}
		return null;
	}
	
	/**
	 * Return list of friend of player1 and not friend with player2
	 * @param player1ID
	 * @param player2ID
	 * @param nbMax
	 * @param random
	 * @param listPlaToExclude
	 * @return
	 */
	public List<Player> listFriendsOfFriend(long player1ID, long player2ID, boolean random, int nbMax, List<Long> listPlaToExclude, String countryCode) {
		// list of friend for player1
		// 
		String strQuery = "select p.* from player p join " +
				"(select if (player1=:player1ID,player2,player1) as pla_id from player_link pl where (type_mask1 & :typeFriend)=:typeFriend and (player1=:player1ID or player2=:player1ID)) t " +
				"on p.id=t.pla_id " +
				"where t.pla_id not in (" +
				"select if (player1=:player2ID,player2,player1) as pla_id from player_link pl where (type_mask1 & :typeFriend)=:typeFriend and (player1=:player2ID or player2=:player2ID) " +
				")";
		if (listPlaToExclude != null && listPlaToExclude.size() > 0) {
			strQuery += " and t.pla_id not in (";
			for (int i=0; i<listPlaToExclude.size();i++) {
				if (i > 0) {
					strQuery+= ",";
				}
				strQuery += ""+listPlaToExclude.get(i);
			}
			strQuery += ")";
		}
		if (countryCode != null && !countryCode.isEmpty()){
			strQuery += " and country_code=:countryCode";
		}
		if (random) {
			strQuery += " order by rand()";
		}
		try {
			Query q = em.createNativeQuery(strQuery, Player.class);
			q.setParameter("player1ID", player1ID);
			q.setParameter("player2ID", player2ID);
			q.setParameter("typeFriend", Constantes.PLAYER_LINK_TYPE_FRIEND);
			if (countryCode != null && !countryCode.isEmpty()){
				q.setParameter("countryCode", countryCode);
			}
			if (nbMax > 0) {
				q.setMaxResults(nbMax);
			}
			return q.getResultList();
		} catch (Exception e) {
			log.error("Exception to list friend of friend player1ID="+player1ID+" - player1ID="+player1ID+" - query="+strQuery, e);
		}
		return null;
	}
	
	/**
	 * List common player between 2 players
	 * @param playerID1
	 * @param playerID2
	 * @return
	 */
	public List<Player> listCommonFriendForPlayers(long playerID1, long playerID2) {
		String strQuery = "select p.* from player p join (select t1.pla_id from " +
				"(select if (player1=:plaID1,player2,player1) as pla_id from player_link pl where (type_mask1 & :typeFriend)=:typeFriend and (player1=:plaID1 or player2=:plaID1)) t1 " +
				"join " +
				"(select if (player1=:plaID2,player2,player1) as pla_id from player_link pl where (type_mask1& :typeFriend)=:typeFriend and (player1=:plaID2 or player2=:plaID2)) t2 " +
				"on t1.pla_id=t2.pla_id) t " +
				"on t.pla_id=p.id";
		try {
			Query q = em.createNativeQuery(strQuery, Player.class);
			q.setParameter("plaID1", playerID1);
			q.setParameter("plaID2", playerID2);
			q.setParameter("typeFriend", Constantes.PLAYER_LINK_TYPE_FRIEND);
			return q.getResultList();
		} catch (Exception e) {
			log.error("Exception to list friend for playerID1="+playerID1+" - playerID2="+playerID2+" - query="+strQuery, e);
		}
		return null;
	}
	
	/**
	 * Return list of player linked with type friend
	 * @param playerID
	 * @param offset
	 * @param nbMax
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public List<WSPlayerLinked> listWSPlayerLinkedFriend(long playerID, int offset, int nbMax) {
		try {
			List<WSPlayerLinked> listLink = new ArrayList<WSPlayerLinked>();
			// list of player2 with link friend to player1 + list of player1 with link friend to player2
			String strQuery = "select p.id, p.nickname, ";
			boolean avatarUseNewField = false;
			if (FBConfiguration.getInstance().getIntValue("player.avatar.useNewField", 1) == 1) {
				avatarUseNewField = true;
				strQuery += "p.avatar_date_validation, ";
			} else {
				strQuery += "p.avatar_present, ";
			}
			strQuery += "p.country_code from (" +
					"(SELECT player2 as plaid FROM `player_link` where player1=:plaID and (type_mask1 & :type) =:type) union " +
					"(SELECT player1 as plaid FROM `player_link` where player2=:plaID and (type_mask2 & :type) =:type)) t, " +
					"player p where t.plaid=p.id order by p.nickname";
			
			Query q = em.createNativeQuery(strQuery);
			
			q.setParameter("plaID", playerID);
			q.setParameter("type", Constantes.PLAYER_LINK_TYPE_FRIEND);
			if (offset > 0) {
				q.setFirstResult(offset);
			}
			if (nbMax > 0) {
				q.setMaxResults(nbMax);
			}
			List l = q.getResultList();
			if (l != null && l.size() > 0) {
				for (int i = 0; i < l.size(); i++) {
					Object[] temp = (Object[])(l.get(i));
					if (temp.length == 5) {
						WSPlayerLinked wspl = new WSPlayerLinked();
						wspl.playerID = ((BigInteger)temp[0]).longValue();
						wspl.pseudo = (String)temp[1];
						PlayerCache pc = ContextManager.getPlayerCacheMgr().getPlayerCache(wspl.playerID);
						if (pc != null) {
							wspl.serie = pc.serie;
						} else {
							wspl.serie = TourSerieMgr.SERIE_NC;
						}
                        if (avatarUseNewField) {
							wspl.avatar = ((BigInteger)temp[3]).longValue() > 0;
						} else {
							wspl.avatar = ((Boolean)temp[3]).booleanValue();
						}
						wspl.country = (String)temp[4];
						wspl.relationMask = Constantes.PLAYER_LINK_TYPE_FRIEND;
						listLink.add(wspl);
					}
				}
			}
			return listLink;
		} catch (Exception e) {
			log.error("Exception to list link friend for playerID="+playerID+" offset="+offset+" - nbMax="+nbMax, e);
		}
		return null;
	}
	
	/**
	 * Count link friend for player
	 * @param playerID
	 * @return
	 */
	public int countLinkFriend(long playerID) {
		try {
			Query q = em.createNativeQuery("select count(*) from (" +
					"(SELECT player2 as plaid FROM `player_link` where player1=:plaID and (type_mask1 & :type) =:type) union " +
					"(SELECT player1 as plaid FROM `player_link` where player2=:plaID and (type_mask2 & :type) =:type)) t");
			q.setParameter("plaID", playerID);
			q.setParameter("type", Constantes.PLAYER_LINK_TYPE_FRIEND);
			return ((BigInteger)q.getSingleResult()).intValue();
		} catch (Exception e) {
			log.error("Exception to count link friend for playerID="+playerID, e);
		}
		return 0;
	}
	
	/**
	 * Links between 2 players
	 * @param plaID1
	 * @param plaID2
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public PlayerLink getLinkBetweenPlayer(long plaID1, long plaID2) {
		try {
			Query q = em.createNamedQuery("playerLink.selectBetweenPlayer");
			q.setParameter("plaID1", plaID1);
			q.setParameter("plaID2", plaID2);
			List<PlayerLink> l = q.getResultList();
			if (l != null && l.size() > 0) {
				if (l.size() > 1) {
					log.error("Many link between plaID1="+plaID1+" - plaID2="+plaID2);
				}
				return l.get(0);
			}
		} catch (Exception e) {
			log.error("Exception to list link between players plaID1="+plaID1+" - plaID2="+plaID2, e);
		}
		return null;
	}

	public int getNbFriendInList(long playerId, Set<Long> players){
		String strQuery = "select count(1) from player_link pl where (type_mask1 & 2) = 2 and ( ";
		strQuery +="(pl.player1=:playerId and pl.player2 in (:players)) ";
		strQuery += "or (pl.player1 in (:players) and pl.player2=:playerId)";
		strQuery += ")";
		Query query = em.createNativeQuery(strQuery);
		query.setParameter("playerId", playerId);
		query.setParameter("players", players);
		return ((BigInteger)query.getSingleResult()).intValue();
	}

	/**
	 * Delete link for player in list
	 * @param listPlaID
	 * @return
	 */
	public boolean deleteForPlayerList(List<Long> listPlaID) {
		if (listPlaID != null && listPlaID.size() > 0) {
			try {
				String strPlaID = "";
				for (Long l : listPlaID) {
					if (strPlaID.length() > 0) {strPlaID+=",";}
					strPlaID += ""+l;
				}
				String strQuery = "delete from player_link where player1 in ("+strPlaID+") or player2 in ("+strPlaID+")";
				Query q = em.createNativeQuery(strQuery);
				q.executeUpdate();
				return true;
			} catch (Exception e) {
				log.error("Exception to delete link for player list size="+listPlaID.size(), e);
			}
		}
		return false;
	}
}
