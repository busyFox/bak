package com.funbridge.server.player.dao;

import com.funbridge.server.player.data.Device;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;

@Repository(value="deviceDAO")
public class DeviceDAO {
	@PersistenceContext
	private EntityManager em;
	
	protected Logger log = LogManager.getLogger(this.getClass());
	
	/**
	 * Return the device associated to this ID
	 * @param deviceID
	 * @return
	 */
	public Device getDevice(long ID) {
		return em.find(Device.class, ID);
	}
	
	/**
	 * Return the device with this string deviceID
	 * @param deviceID
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Device getDevice(String deviceID) {
		Query q = em.createNamedQuery("device.selectForDeviceID");
		q.setParameter("deviceID", deviceID);
		try {
			List<Device> l = q.getResultList();
			if (l != null && l.size() > 0) {
				if (l.size() > 1) {
					log.error("Many device for deviceID="+deviceID+" - nb="+l.size());
				}
				return l.get(0);
			}
		} catch (Exception e) {
			log.error("Exception to get device with deviceID="+deviceID, e);
		}
		return null;
	}
	
	/**
	 * Create device. No check done for existing device with this deviceID !
	 * @param deviceID
	 * @param deviceType
	 * @param deviceInfo
     * @param lang
     * @param clientVersion
     * @param playerID
     * @param bonusFlag
	 * @return
	 */
	public Device createDevice(String deviceID, String deviceType, String deviceInfo, String lang, String clientVersion, long playerID, int bonusFlag) {
		Device d = new Device();
		d.setDeviceID(deviceID);
		d.setDateCreation(System.currentTimeMillis());
		d.setType(deviceType);
		d.setDeviceInfo(deviceInfo);
		d.setLang(lang);
		d.setClientVersion(clientVersion);
        d.setDateLastConnection(System.currentTimeMillis());
        d.setLastPlayerID(playerID);
		try {
			em.persist(d);
		} catch (Exception e) {
			log.error("Exception to persist device deviceID="+deviceID,e);
			d = null;
		}
		return d;
	}
	
	/**
	 * Return the total nb device
	 * @return
	 */
	public int getNbDevice() {
		try {
			Query query = em.createNamedQuery("device.count");
			long result = (Long)query.getSingleResult();
			return (int)result;
		}
		catch (Exception e) {
			log.error("Exception to count nb device", e);
		}
		return 0;
	}
	
	/**
	 * Update the device data to DB
	 * @param d
	 * @return
	 */
	public Device updateDevice(Device d) {
		if (d != null) {
			try {
				return em.merge(d);
			} catch (Exception e) {
				log.error("Error trying to merge device id="+d.getDeviceID(), e);
			}
		} else {
			log.error("Parameter device is null");
		}
		return null;
	}
	
	/**
	 * Set inactiveApns to true for all device with token in list
	 * @param listToken
	 * @return
	 */
	public int updateInactiveApns(List<String> listToken) {
		if (listToken != null && listToken.size() > 0) {
			try {
				StringBuffer sbListToken = new StringBuffer();
				for (String s : listToken) {
					if (sbListToken.length() > 0) sbListToken.append(',');
					sbListToken.append("'"+s+"'");
				}
				String strQuery = "update device d set d.inactive_apns=true where d.push_token in ("+sbListToken.toString()+")";
				Query query = em.createNativeQuery(strQuery);
				return query.executeUpdate();
			} catch (Exception e) {
				log.error("Exception to update inactiveApns", e);
			}
		}
		return 0;
	}
	
	public boolean deleteForID(long deviceID) {
		try {
			String strQuery = "delete from device where id="+deviceID;
			Query q = em.createNativeQuery(strQuery);
			q.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error("Exception to delete device with ID="+deviceID, e);
		}
		return false;
	}
}
