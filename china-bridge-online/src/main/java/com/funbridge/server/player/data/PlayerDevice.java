package com.funbridge.server.player.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name="player_device")
@NamedQueries({
    @NamedQuery(name="playerDevice.selectForPlayerOrderLastUsed", query="select pd from PlayerDevice pd where pd.player.ID=:plaID order by pd.dateLastConnection desc"),
	@NamedQuery(name="playerDevice.selectDeviceForPlayer", query="select pd.device from PlayerDevice pd where pd.player.ID=:plaID"),
	@NamedQuery(name="playerDevice.countDeviceForPlayer", query="select count(pd) from PlayerDevice pd where pd.player.ID=:plaID"),
	@NamedQuery(name="playerDevice.selectPlayerForDevice", query="select pd.player from PlayerDevice pd where pd.device.ID=:deviceID"),
	@NamedQuery(name="playerDevice.countPlayerForDevice", query="select count(pd) from PlayerDevice pd where pd.device.ID=:deviceID"),
	@NamedQuery(name="playerDevice.selectForPlayerAndDevice", query="select pd from PlayerDevice pd where pd.device.ID=:deviceID and pd.player.ID=:plaID"),
	@NamedQuery(name="playerDevice.deleteForPlayer", query="delete from PlayerDevice pd where pd.player.ID=:plaID"),
	@NamedQuery(name="playerDevice.deleteForDevice", query="delete from PlayerDevice pd where pd.device.ID=:deviceID"),
	@NamedQuery(name="playerDevice.deleteForPlayerAndDevice", query="delete from PlayerDevice pd where pd.player.ID=:plaID and pd.device.ID=:deviceID")
})
public class PlayerDevice {
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="id", nullable=false)
	private long ID;
	
	@ManyToOne
	@JoinColumn(name="pla_id", nullable=false)
	private Player player;
	
	@ManyToOne
	@JoinColumn(name="device_id", nullable=false)
	private Device device;

    @Column(name="date_creation")
    private long dateCreation = 0;

    @Column(name="date_last_connection")
    private long dateLastConnection = 0;

	public long getID() {
		return ID;
	}
	public void setID(long iD) {
		ID = iD;
	}
	public Player getPlayer() {
		return player;
	}
	public void setPlayer(Player player) {
		this.player = player;
	}
	public Device getDevice() {
		return device;
	}
	public void setDevice(Device device) {
		this.device = device;
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
}
