package com.funbridge.server.player.dao;

import com.funbridge.server.player.data.Device;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.player.data.PlayerDevice;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;

@Repository(value="playerDeviceDAO")
public class PlayerDeviceDAO {
	@PersistenceContext
	private EntityManager em;
	
	protected Logger log = LogManager.getLogger(this.getClass());
	
	/**
	 * Create the player device for this player and device data
	 * @param p
	 * @return
	 */
	public PlayerDevice linkPlayerDevice(Player p, Device d) {
		PlayerDevice pd = new PlayerDevice();
		pd.setDevice(d);
		pd.setPlayer(p);
        pd.setDateCreation(System.currentTimeMillis());
        pd.setDateLastConnection(System.currentTimeMillis());
		try {
			em.persist(pd);
			return pd;
		} catch (Exception e) {
			log.error("Exception to persist player device for player - plaID="+p.getID()+" - deviceID="+d.getID(), e);
		}
		return null;
	}

    /**
     * Update the device data to DB
     * @return
     */
    public PlayerDevice updatePlayerDevice(PlayerDevice pd) {
        if (pd != null) {
            try {
                return em.merge(pd);
            } catch (Exception e) {
                log.error("Error trying to merge player device ="+pd, e);
            }
        } else {
            log.error("Parameter player device is null");
        }
        return null;
    }
	
	/**
	 * Return the list of device for a player
	 * @param plaID
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Device> getListDeviceForPlayer(long plaID) {
		Query q = em.createNamedQuery("playerDevice.selectDeviceForPlayer");
		q.setParameter("plaID", plaID);
		return q.getResultList();
	}

    /**
     * Return the list of playerDevice for a player order by date lastUsed desc
     * @param plaID
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<PlayerDevice> getListPlayerDeviceForPlayer(long plaID) {
        Query q = em.createNamedQuery("playerDevice.selectForPlayerOrderLastUsed");
        q.setParameter("plaID", plaID);
        return q.getResultList();
    }

    /**
     * Return the last playerDevice used for a player
     * @param plaID
     * @return
     */
    @SuppressWarnings("unchecked")
    public PlayerDevice getLastPlayerDeviceForPlayer(long plaID) {
        Query q = em.createNamedQuery("playerDevice.selectForPlayerOrderLastUsed");
        q.setParameter("plaID", plaID);
        q.setMaxResults(1);
        List<PlayerDevice> list = q.getResultList();
        if (list != null && list.size() > 0) {
            return list.get(0);
        }
        return null;
    }
	
	/**
	 * Return the list of player using this device
	 * @param deviceID
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Player> getListPlayerForDeviceID(long deviceID) {
		Query q = em.createNamedQuery("playerDevice.selectPlayerForDevice");
		q.setParameter("deviceID", deviceID);
		return q.getResultList();
	}
	
	/**
	 * Return the player device associated to this player and device
	 * @param plaID
	 * @param deviceID
	 * @return null if no player device found for this couple player - device
	 */
	@SuppressWarnings("unchecked")
	public PlayerDevice getPlayerDevice(long plaID, long deviceID) {
		Query q = em.createNamedQuery("playerDevice.selectForPlayerAndDevice");
		q.setParameter("deviceID", deviceID);
		q.setParameter("plaID", plaID);
		try {
			List<PlayerDevice> list = q.getResultList();
			if (list != null) {
				if (list.size() > 0) {
					if (list.size() > 1) {
						log.error("many playerdevice for same device="+deviceID+" and player="+plaID);
					}
					return list.get(0);
				}
			}
		} catch (Exception e) {
			log.error("Exception to get player device with plaID="+plaID+" - deviceID="+deviceID, e);
		}
		return null;
	}

    public int countDeviceForPlayer(long playerID) {
		try {
			Query query = em.createNamedQuery("playerDevice.countDeviceForPlayer");
			query.setParameter("plaID", playerID);
			long result = (Long)query.getSingleResult();
			return (int)result;
		}
		catch (Exception e) {
			log.error("Exception to count device for playerID="+playerID, e);
		}
		return 0;
	}
	
	public int countPlayerForDevice(long deviceID) {
		try {
			Query query = em.createNamedQuery("playerDevice.countPlayerForDevice");
			query.setParameter("deviceID", deviceID);
			long result = (Long)query.getSingleResult();
			return (int)result;
		}
		catch (Exception e) {
			log.error("Exception to count player for deviceID="+deviceID, e);
		}
		return 0;
	}
	
	public boolean deleteForPlayerAndDevice(long playerID, long deviceID) {
		try {
			Query q = em.createNamedQuery("playerDevice.deleteForPlayerAndDevice");
			q.setParameter("plaID", playerID);
			q.setParameter("deviceID", deviceID);
			q.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error("Error to delete for playerID="+playerID+" and deviceID="+deviceID, e);
		}
		return false;
	}
	
	public boolean deleteForDevice(long deviceID) {
		try {
			Query q = em.createNamedQuery("playerDevice.deleteForDevice");
			q.setParameter("deviceID", deviceID);
			q.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error("Error to delete for deviceID="+deviceID, e);
		}
		return false;
	}
}
