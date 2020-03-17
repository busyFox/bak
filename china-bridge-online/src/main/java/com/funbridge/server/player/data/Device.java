package com.funbridge.server.player.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import com.funbridge.server.common.Constantes;

@Entity
@Table(name="device")
@NamedQueries({
	@NamedQuery(name="device.selectForDeviceID", query="select device from Device device where device.deviceID=:deviceID"),
	@NamedQuery(name="device.count", query="select count(d) from Device d"),
    @NamedQuery(name="device.selectWithPushAndLastPlayer", query="select device from Device device where device.lastPlayerID=:playerID and device.pushToken != '' and device.inactiveApns = 0 order by device.dateLastConnection desc")
})
public class Device {
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="id", nullable=false)
	private long ID;
	
	@Column(name="device_id", length=100, nullable=false)
	private String deviceID;
	
	@Column(name="device_info", nullable=false)
	private String deviceInfo = "";
	
	@Column(name="push_token", nullable=false)
	private String pushToken = "";

	@Column(name="push_fcmToken", nullable=false)
	private String pushFcmToken = "";
	
	@Column(name="date_creation")
	private long dateCreation = 0;

	@Column(name="type")
	private String type = Constantes.DEVICE_TYPE_IOS;
	
	@Column(name="date_last_connection")
	private long dateLastConnection = 0;
	
	@Column(name="date_inactive_apns")
	private long dateInactiveApns = 0;
	
	@Column(name="inactive_apns")
	private boolean inactiveApns = false;
	
	@Column(name="lang")
	private String lang = Constantes.PLAYER_LANG_EN;

	@Column(name="client_version")
	private String clientVersion = "0.0.0";

    @Column(name="last_player_id")
    private long lastPlayerID = 0;
	
	public long getID() {
		return ID;
	}
	public void setID(long iD) {
		ID = iD;
	}
	public String getDeviceID() {
		return deviceID;
	}
	public void setDeviceID(String deviceID) {
		this.deviceID = deviceID;
	}
	public String getDeviceInfo() {
		return deviceInfo;
	}
	public void setDeviceInfo(String deviceInfo) {
		this.deviceInfo = deviceInfo;
	}
	public String getPushToken() {
		return pushToken;
	}
	public void setPushToken(String pushToken) {
		this.pushToken = pushToken;
	}
	public String getPushFcmToken() {
		return pushFcmToken;
	}
	public void setPushFcmToken(String pushFcmToken) {
		this.pushFcmToken = pushFcmToken;
	}

	public String toString() {
		return "id="+ID+" - info="+deviceInfo+" - deviceID="+deviceID+" - creation="+dateCreation;
	}

	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public long getDateCreation() {
		return dateCreation;
	}
	public void setDateCreation(long dateCreation) {
		this.dateCreation = dateCreation;
	}
	public long getDateLastConnection() {
		return dateLastConnection;
	}
	public void setDateLastConnection(long dateLastConnection) {
		this.dateLastConnection = dateLastConnection;
	}
	public long getDateInactiveApns() {
		return dateInactiveApns;
	}
	public void setDateInactiveApns(long dateInactiveApns) {
		this.dateInactiveApns = dateInactiveApns;
	}
	public boolean isInactiveApns() {
		return inactiveApns;
	}
	public void setInactiveApns(boolean inactiveApns) {
		this.inactiveApns = inactiveApns;
	}
	public String getLang() {
		return lang;
	}
	public void setLang(String value) {
		if (value != null && !value.equals(Constantes.VALUE_TEST)) {
			if (value.length() > Constantes.PLAYER_LANG_MAX_LENGTH) {
				value = value.substring(0, Constantes.PLAYER_LANG_MAX_LENGTH);
			}
			this.lang = value;
		}
	}
	public String getClientVersion() {
		return clientVersion;
	}
	public void setClientVersion(String clientVersion) {
		this.clientVersion = clientVersion;
	}
    public long getLastPlayerID() {
        return lastPlayerID;
    }

    public void setLastPlayerID(long lastPlayerID) {
        this.lastPlayerID = lastPlayerID;
    }
}
