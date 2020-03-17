package com.funbridge.server.ws.support;

import javax.xml.bind.annotation.XmlRootElement;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.player.data.Device;
import com.funbridge.server.player.data.PlayerDevice;

@XmlRootElement(name="device")
public class WSSupDevice {
	public long deviceID = -1;
	public String info;
	public String pushToken;
	public String type;
    public String deviceDateLastConnection;
    public String playerDateLastConnection;
    public String clientVersion;
    public String lang;
    public long lastPlayerID;
	
	public WSSupDevice() {}

    public WSSupDevice(Device d) {
		deviceID = d.getID();
		info = d.getDeviceInfo();
		pushToken = d.getPushToken();
		type = d.getType();
        deviceDateLastConnection = Constantes.timestamp2StringDateHour(d.getDateLastConnection());
		if (type == null || type.length() == 0) {
			if (info != null && info.contains("iPhone")) {
				type = Constantes.DEVICE_TYPE_IOS;
			}
		}
        clientVersion = d.getClientVersion();
        lang = d.getLang();
        lastPlayerID = d.getLastPlayerID();
	}

    public WSSupDevice(PlayerDevice pd) {
        deviceID = pd.getDevice().getID();
        info = pd.getDevice().getDeviceInfo();
        pushToken = pd.getDevice().getPushToken();
        type = pd.getDevice().getType();
        deviceDateLastConnection = Constantes.timestamp2StringDateHour(pd.getDevice().getDateLastConnection());
        playerDateLastConnection = Constantes.timestamp2StringDateHour(pd.getDateLastConnection());
        if (type == null || type.length() == 0) {
            if (info != null && info.contains("iPhone")) {
                type = Constantes.DEVICE_TYPE_IOS;
            }
        }
        clientVersion = pd.getDevice().getClientVersion();
        lang = pd.getDevice().getLang();
        lastPlayerID = pd.getDevice().getLastPlayerID();
    }
}
