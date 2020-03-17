package com.funbridge.server.player.dao;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.tournament.serie.TourSerieMgr;
import com.funbridge.server.ws.player.WSCommunityCountryPlayer;
import com.funbridge.server.ws.player.WSPlayerLinked;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

@Repository(value="playerDAO")
public class PlayerDAOJpa {
	@PersistenceContext
	private EntityManager em;
	
	protected Logger log = LogManager.getLogger(this.getClass());
	
	public Player getPlayer(long id) {
		return em.find(Player.class, id);
	}

	/**
	 * Return player with this mail
	 * @param mail
	 * @return
	 */
	public Player getPlayerByMail(String mail) {
		try {
			Query query = em.createNamedQuery("player.getForMail");
			query.setParameter("mail", mail);
			Player p = (Player) query.getSingleResult();
			return p;
		}
		catch (NoResultException e) {
			return null;
		}
		catch (Exception e) {
			log.error("Exception on getSingleResult for mail="+mail, e);
			return null;
		}
	}

	/**
	 * Return player with this cert
	 * @param cert
	 * @return
	 */
	public Player getPlayerByCert(String cert) {
		try {
			Query query = em.createNamedQuery("player.getForCert");
			query.setParameter("cert", cert);
			Player p = (Player) query.getSingleResult();
			return p;
		}
		catch (NoResultException e) {
			return null;
		}
		catch (Exception e) {
			log.error("Exception on getSingleResult for cert="+cert, e);
			return null;
		}
	}
	
	/**
	 * Add new player to DB
	 * @param p
	 * @return
	 */
	public boolean add(Player p) {
		if (p != null) {
			try {
				em.persist(p);
				return true;
			} catch (Exception e) {
				log.error("Error trying to persist player : id="+p.toString(), e);
			}
		} else {
			log.error("Parameter player is null");
		}
		return false;
	}

	public boolean insertPlayerWithNativeMethod(long playerID, String cert , int type ,String mail, String password, String nickname, String lang, String displayLang, String countryCode, long creationDate) {
        try {
            String strQuery = "insert into player (id, nickname, password, cert, type , mail, lang, display_lang, country_code, display_country_code, creation_date, credentials_init_mask, last_connection_date) values (?,?,?,?,?,?,?,?,?,?,?,?,?)";
            Query query = em.createNativeQuery(strQuery);
            query.setParameter(1, playerID);
            query.setParameter(2, nickname);
            query.setParameter(3, password);
			query.setParameter(4, cert);
			query.setParameter(5, type);
            query.setParameter(6, mail);
            query.setParameter(7, lang);
            query.setParameter(8, displayLang);
            query.setParameter(9, countryCode);
            query.setParameter(10, countryCode);
            query.setParameter(11, creationDate);
            int credentialsInitMask = 0;
			query.setParameter(12, credentialsInitMask);
			int lastConnectionDate = 0;
			query.setParameter(13, lastConnectionDate);
            query.executeUpdate();
            return true;
        } catch (Exception e) {
            log.error("Exception to insert player playerID="+playerID+" - nickname="+nickname+" - cert="+cert, e);
        }
        return false;
    }
	
	/**
	 * Update the player data to DB
	 * @param p
	 * @return
	 */
	public Player updatePlayer(Player p) {
		if (p != null) {
			try {
				return em.merge(p);
			} catch (Exception e) {
				log.error("Error trying to merge player : id="+p.toString(), e);
			}
		} else {
			log.error("Parameter player is null");
		}
		return null;
	}

	/**
	 * Return the player with this pseudo
	 * @param login
	 * @return
	 */
	public Player getPlayerByNickname(String login) {
		Query query = em.createNamedQuery("player.getForNickname");
		query.setParameter("nickname", login);
		try {
			Player p = (Player) query.getSingleResult();
			return p;
		}
		catch (NoResultException e) {
			return null;
		}
		catch (Exception e) {
			log.error("getSingleResult exception - "+e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Remove player
	 * @param p
	 * @return
	 */
	public boolean removePlayer(Player p) {
		if (p != null) {
			try {
				em.remove(p);
				return true;
			} catch (Exception e) {
				log.error("Remove exception", e);
			}
		}
		return false;
	}

	/**
	 * Return list of player with mail or pseudo containing the expected login
	 * @param login
	 * @param nbMax
     * @param useFullIndexText
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Player> searchPlayerWithMailOrPseudo(String login, int nbMax, boolean useFullIndexText) {
		try {
            Query query = null;
			if (useFullIndexText) {
                query = em.createNativeQuery("select * from player p where p.id > 0 and (p.mail like :login or match(p.nickname) against(:login in boolean mode))", Player.class);
                query.setParameter("login", login+"*");
            } else {
                query = em.createNativeQuery("select * from player p where p.id > 0 and p.mail like :login union select * from player p where p.id > 0 and p.nickname like :login", Player.class);
                query.setParameter("login", login + "%");
            }
            if (nbMax > 0) query.setMaxResults(nbMax);
            return query.getResultList();
		}
		catch (Exception e) {
			log.error("Exception to searchPlayerWithMailOrPseudo", e);
			return null;
		}
	}

    /**
     * Return list of player with pseudo, firstname or lastname starting the expected login
     * @param login
     * @param nbMax
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<Player> searchPlayerStartingPseudoOrName(String login, int nbMax, boolean useIndexFullText) {
        try {
            Query query = null;
            if (useIndexFullText) {
                query = em.createNativeQuery("select * from player p where p.id > 0 and match(p.nickname) against (:login in boolean mode) union select * from player p where p.id > 0 and match(p.lastname) against(:login in boolean mode) union select * from player p where p.id > 0 and match(p.firstname) against(:login in boolean mode)", Player.class);
                query.setParameter("login", login+"*");
            } else {
				query = em.createNativeQuery("select * from player p where p.ID > 0 and p.nickname like :login union select * from player p where p.ID > 0 and p.lastName like :login union select * from player p where p.ID > 0 and p.firstName like :login", Player.class);
                query.setParameter("login", login+"%");
            }
            if (nbMax > 0) query.setMaxResults(nbMax);
            return query.getResultList();
        }
        catch (Exception e) {
            log.error("Exception to searchForPseudoOrName", e);
            return null;
        }
    }
	
	/**
	 * Return list of player with mail like the expected value (ignore case)
	 * @param mail 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Player> searchPlayerWithMail(String mail) {
		try {
			Query query = em.createNamedQuery("player.searchForMail");
			query.setParameter("mail", mail);
			List<Player> players = query.getResultList();
			return players;
		}
		catch (Exception e) {
			log.error("Exception to searchPlayerWithMail", e);
			return null;
		}
	}

	/**
	 * Return list of player with cert like the expected value (ignore case)
	 * @param cert
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Player> searchPlayerWithCert(String cert) {
		try {
			Query query = em.createNamedQuery("player.searchForCert");
			query.setParameter("cert", cert);
			List<Player> players = query.getResultList();
			return players;
		}
		catch (Exception e) {
			log.error("Exception to searchPlayerWithCert", e);
			return null;
		}
	}
	
	/**
	 * Return list of player with pseudo like the expected value (ignore case)
	 * @param pseudo
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Player> searchPlayerWithPseudo(String pseudo) {
		try {
			Query query = em.createNamedQuery("player.searchForPseudo");
			query.setParameter("pseudo", pseudo);
			List<Player> players = query.getResultList();
			return players;
		}
		catch (Exception e) {
			log.error("Exception to searchPlayerWithPseudo", e);
			return null;
		}
	}
	
	/**
	 * Return the total nb player
	 * @return
	 */
	public int getNbPlayer() {
		try {
			Query query = em.createNamedQuery("player.count");
			long result = (Long)query.getSingleResult();
			return (int)result;
		}
		catch (Exception e) {
			log.error("Exception to count nb player", e);
		}
		return 0;
	}
	

	
	/**
	 * return nb authentified player
	 * @return
	 */
	public int getNbPlayerAuth() {
		try {
			Query query = em.createNamedQuery("player.countAuth");
			long result = (Long)query.getSingleResult();
			return (int)result;
		}
		catch (Exception e) {
			log.error("Exception to count nb auth", e);
		}
		return 0;
	}
	
	/**
	 * return nb authentified player not certif
	 * @return
	 */
	public int getNbPlayerAuthNotCertif() {
		try {
			Query query = em.createNamedQuery("player.countAuthNotCertif");
			long result = (Long)query.getSingleResult();
			return (int)result;
		}
		catch (Exception e) {
			log.error("Exception to count nb auth not certif", e);
		}
		return 0;
	}
	
	/**
	 * Return list of player where birthday is day of month
	 * @param month
	 * @param day
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Player> searchPlayerWithBirthdayOnDay(int month, int day) {
		try {
			String searchBirthday = "%-"+String.format("%02d", month)+"-"+String.format("%02d", day);
			String strQuery = "select p from Player p where p.dateBirthday is not null and p.dateBirthday like '"+searchBirthday+"'";
			Query query = em.createQuery(strQuery);
			return query.getResultList();
		} catch (Exception e) {
			log.error("Exception to searchPlayerWithBirthdayOnDay month="+month+" - day="+day, e);
		}
		return null;
	}

    /**
	 * Return list of playerID with pattern (pseudo and lastname)
	 * @param pattern
	 * @param countryCode
	 * @param offset
	 * @param nbMax
     * @param useIndexFullText
	 * @return
	 */
	public List<Long> searchPlayerID(String pattern, String countryCode, int offset, int nbMax, boolean useIndexFullText) {
		List<Long> result = new ArrayList<>();
		if(pattern == null || pattern.isEmpty()){
			return result;
		}
		String rqt = "select p.id, p.last_connection_date from player p where p.id > 0 ";
		if(countryCode != null && !countryCode.isEmpty()){
			rqt += " and (p.display_country_code like :countryCode) ";
		}

		String unionQuery = "SELECT p.id, p.last_connection_date from (";
		if (useIndexFullText) {
			unionQuery += rqt + "and match(p.nickname) against(:pattern in boolean mode) union";
			unionQuery += rqt + "and match(p.lastname) against(:pattern in boolean mode) ";
		} else {
			unionQuery += rqt + "and p.nickname like :pattern union ";
			unionQuery += rqt + "and p.lastname like :pattern ";
		}
		unionQuery += ") as p order by p.last_connection_date desc";

		try {
			Query q = em.createNativeQuery(unionQuery);
			q.setParameter("pattern", pattern+ (useIndexFullText ? "*" : "%"));
			if(countryCode != null && !countryCode.isEmpty()) q.setParameter("countryCode", countryCode);
			if (offset > 0) {
				q.setFirstResult(offset);
			}
			if (nbMax > 0) {
				q.setMaxResults(nbMax);
			}
			List l = q.getResultList();

			if (l != null && l.size() > 0) {
				for (int i = 0; i < l.size(); i++) {
					result.add(((BigInteger) ((Object[]) l.get(i))[0]).longValue());
				}
			}
		} catch (Exception e) {
			log.error("Failed to searchPlayerID with pattern="+pattern+" and countryCode="+countryCode+" - rqt="+rqt, e);
		}
		return result;
	}
	
	/**
	 * Return list of playerLinked where player mail is exactly this pattern or pseudo, firstname or lastname contains pattern
	 * @param pattern
	 * @param playerID
	 * @param type
	 * @param offset
	 * @param nbMax
     * @param useIndexFullText
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public List<WSPlayerLinked> searchPlayerLinked(String pattern, long playerID, int type, int offset, int nbMax, boolean useIndexFullText) {
		try {
			pattern = pattern + (useIndexFullText ? '*' : '%');


			String rqt = "select p.id, p.nickname, p.display_country_code, p.avatar_present, pl1.type11, pl1.type12, pl2.type21, pl2.type22, p.firstname, p.lastname from player p ";
			rqt += "left join (select player2, type_mask1 as type11, type_mask2 as type12 from player_link where player1=:playerID) as pl1 on p.id=pl1.player2 ";
			rqt += "left join (select player1, type_mask1 as type21, type_mask2 as type22 from player_link where player2=:playerID) as pl2 on p.id=pl2.player1 ";

			// exclude players who have blocked this player
			rqt += "WHERE (type21 is null or (type21 & :typeBlock) != :typeBlock) and (type12 is null or (type12 & :typeBlock) != :typeBlock) ";
			rqt += "and p.id != :playerID and p.id > 0 ";


			String finalQuery = null;
			// Search by name
			if (type == 1){
			    if (useIndexFullText) {
					finalQuery = rqt + "and (match(p.lastname) against (:pattern in boolean mode))";
                } else {
					finalQuery = rqt + "and (p.lastname like :pattern)";
                }
			}
			// Search by pseudo
			else if (type == 2){
			    if (useIndexFullText) {
					finalQuery = rqt + "and (match(p.nickname) against(:pattern in boolean mode))";
                } else {
					finalQuery = rqt + "and (p.nickname like :pattern)";
                }
			}
			// Search by mail
			else if (type == 3){
				finalQuery = rqt + "and (p.mail like :pattern)";
			}
			// Default behaviour
			else{
				finalQuery = "select * from (";
				// Create union query, last query with or comparator was slowly
				if (useIndexFullText) {
					finalQuery += rqt + "and match(p.nickname) against(:pattern in boolean mode) union ";
					finalQuery += rqt + "and match(p.lastname) against(:pattern in boolean mode) union ";
					finalQuery += rqt + "and p.mail like :pattern ";
                } else {
					finalQuery += rqt + "and p.nickname like :pattern union ";
					finalQuery += rqt + "and p.lastname like :pattern union ";
					finalQuery += rqt + "and p.mail like :pattern ";
                }
				finalQuery += ") as p ";
			}

			switch (type) {
				case 1:
					finalQuery += " order by p.lastname";
					break;
				case 3:
					finalQuery += " order by p.mail";
					break;
				default:
					finalQuery += " order by p.nickname";
					break;
			}

			Query q = em.createNativeQuery(finalQuery);
			q.setParameter("playerID", playerID);
			q.setParameter("pattern", pattern);
			q.setParameter("typeBlock", Constantes.PLAYER_LINK_TYPE_BLOCKED);

			if (offset > 0) { q.setFirstResult(offset); }
			if (nbMax > 0) { q.setMaxResults(nbMax); }

			List<WSPlayerLinked> listLink = new ArrayList<WSPlayerLinked>();
			List l = q.getResultList();
			if (l != null && l.size() > 0) {
				for (int i = 0; i < l.size(); i++) {
					Object[] temp = (Object[])(l.get(i));
					if (temp.length == 10) {
						WSPlayerLinked wspl = new WSPlayerLinked();
						wspl.playerID = ((BigInteger)temp[0]).longValue();
						wspl.pseudo = (String)temp[1];
						wspl.firstName = (String)temp[8];
						wspl.lastName = (String)temp[9];
						PlayerCache pc = ContextManager.getPlayerCacheMgr().getPlayerCache(wspl.playerID);
						if (pc != null) {
							wspl.serie = pc.serie;
						} else {
							wspl.serie = TourSerieMgr.SERIE_NC;
						}
                        wspl.country = (String)temp[2];
						wspl.avatar = Boolean.parseBoolean(temp[3].toString());
						// link where player=player1
						if (temp[4] != null && temp[5] != null) {
							int val1 = (Integer)temp[4]; // value of mask1 where player=player1
							int val2 = (Integer)temp[5]; // value of mask2 where player=player1
							if ((val1 & Constantes.PLAYER_LINK_TYPE_FRIEND) == Constantes.PLAYER_LINK_TYPE_FRIEND) {
								wspl.relationMask = wspl.relationMask | Constantes.PLAYER_LINK_TYPE_FRIEND;
							}
							else {
								if (((val1 & Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING) == Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING) || 
									((val2 & Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING) == Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING)){
									wspl.relationMask = wspl.relationMask | Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING;
									if ((val1 & Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING) == Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING) {
										wspl.relationMask = wspl.relationMask | Constantes.PLAYER_LINK_TYPE_WAY;
									}
								}
							}
						}
						// link where player=player2
						else if (temp[6] != null && temp[7] != null) {
							int val1 = (Integer)temp[6]; // value of mask1 where player=player2
							int val2 = (Integer)temp[7]; // value of mask2 where player=player2
							if ((val1 & Constantes.PLAYER_LINK_TYPE_FRIEND) == Constantes.PLAYER_LINK_TYPE_FRIEND) {
								wspl.relationMask = wspl.relationMask | Constantes.PLAYER_LINK_TYPE_FRIEND;
							}
							else {
								if (((val1 & Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING) == Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING) || 
									((val2 & Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING) == Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING)){
									wspl.relationMask = wspl.relationMask | Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING;
									if ((val2 & Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING) == Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING) {
										wspl.relationMask = wspl.relationMask | Constantes.PLAYER_LINK_TYPE_WAY;
									}
								}
							}
						}
						listLink.add(wspl);
					}
				}
			}
			return listLink.stream().filter(x -> !x.pseudo.startsWith("DELETED")).collect(Collectors.toList());
		} catch (Exception e) {
			log.error("Exception to searchPlayerLinked for pattern="+pattern+" - playerID="+playerID, e);
		}
		return null;
	}
	
	/**
	 * Count active players : player with last_connection_date > nbMonthBefore
	 * @param nbMonthBefore
	 * @return
	 */
	public int countActivePlayers(int nbMonthBefore) {
		try {
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.MONTH, -nbMonthBefore);
			Query query = em.createNativeQuery("select count(*) from player where last_connection_date>:dateLimit");
			query.setParameter("dateLimit", cal.getTimeInMillis());
			return ((BigInteger)query.getSingleResult()).intValue();
		} catch (Exception e) {
			log.error("Exception to count active players", e);
		}
		return -1;
	}

	/**
	 * Build list of country with nb player order by nb player desc
	 * @param nbMonthBefore
	 * @return
	 */
	public List<WSCommunityCountryPlayer> getListCountryPlayer(int nbMonthBefore) {
		List<WSCommunityCountryPlayer> result = new ArrayList<WSCommunityCountryPlayer>();
		try {
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.MONTH, -nbMonthBefore);
			Query query = em.createNativeQuery("select * from (select display_country_code, count(*) as nbPlayers from player where length(display_country_code)>0 and last_connection_date > :dateLimit group by display_country_code)t order by nbPlayers desc;");
			query.setParameter("dateLimit", cal.getTimeInMillis());
			List l = query.getResultList();
			for (Object e : l) {
				Object [] temp = (Object[])e;
				if (temp.length == 2) {
					WSCommunityCountryPlayer ccp = new WSCommunityCountryPlayer();
					ccp.countryCode = (String)temp[0];
					ccp.nbPlayers = ((BigInteger)temp[1]).intValue();
					result.add(ccp);
				}
			}
		} catch (Exception e) {
			log.error("Exception to list country player", e);
		}
		return result;
	}

	public List<Player> listPlayer(int offset, int nbMax) {
		Query query = em.createNamedQuery("player.list");
		query.setFirstResult(offset);
		query.setMaxResults(nbMax);
		return query.getResultList();
	}

	/**
	 * Return list of player with same countryCode and town
	 * @param countryCode
	 * @param town
	 * @param listPlaToExclude list of player ID to exclude from result
	 * @param nbMax
	 * @return
	 */
	public List<Player> listPlayerWithCountryAndTown(String countryCode, String town, List<Long> listPlaToExclude, int nbMax) {
		try {
			String strQuery = "select * from player where country_code=:countryCode and town=:town";
			if (listPlaToExclude != null && listPlaToExclude.size() > 0) {
				strQuery += " and id not in (";
				for (int i=0; i<listPlaToExclude.size();i++) {
					if (i > 0) {
						strQuery+= ",";
					}
					strQuery += ""+listPlaToExclude.get(i);
				}
				strQuery += ")";
			}
			Query query = em.createNativeQuery(strQuery, Player.class);
			query.setParameter("countryCode", countryCode);
			query.setParameter("town", town);
			if (nbMax > 0) {
				query.setMaxResults(nbMax);
			}
			return query.getResultList();
		} catch (Exception e) {
			log.error("Exception to list player with same countryCode="+countryCode+" and town="+town, e);
		}
		return null;
	}
	
	/**
	 * Return list of player with same lastName
	 * @param lastName
	 * @param listPlaToExclude list of player ID to exclude from result
	 * @param nbMax
	 * @return
	 */
	public List<Player> listPlayerWithLastName(String lastName, List<Long> listPlaToExclude, int nbMax) {
		try {
			String strQuery = "select * from player where lastname=:lastName";
			if (listPlaToExclude != null && listPlaToExclude.size() > 0) {
				strQuery += " and id not in (";
				for (int i=0; i<listPlaToExclude.size();i++) {
					if (i > 0) {
						strQuery+= ",";
					}
					strQuery += ""+listPlaToExclude.get(i);
				}
				strQuery += ")";
			}
			Query query = em.createNativeQuery(strQuery, Player.class);
			query.setParameter("lastName", lastName);
			if (nbMax > 0) {
				query.setMaxResults(nbMax);
			}
			return query.getResultList();
		} catch (Exception e) {
			log.error("Exception to list player with same lastName="+lastName, e);
		}
		return null;
	}

    /**
     * Return list of player random with date last connection > dateAfter
     * @param dateAfter
     * @param listPlaToExclude
     * @param nbMax
     * @return
     */
    public List<Player> listPlayerRandomWithDateConnection(long dateAfter, List<Long> listPlaToExclude, int nbMax) {
        try {
            String strQuery = "select * from player where last_connection_date > "+dateAfter;
            if (listPlaToExclude != null && listPlaToExclude.size() > 0) {
                strQuery += " and id not in (";
                for (int i=0; i<listPlaToExclude.size();i++) {
                    if (i > 0) {
                        strQuery+= ",";
                    }
                    strQuery += ""+listPlaToExclude.get(i);
                }
                strQuery += ")";
            }
            strQuery += " order by rand()";
            Query query = em.createNativeQuery(strQuery, Player.class);
            if (nbMax > 0) {
                query.setMaxResults(nbMax);
            }
            return query.getResultList();
        } catch (Exception e) {
            log.error("Exception to list player with date connection > "+dateAfter, e);
        }
        return null;
    }
}
