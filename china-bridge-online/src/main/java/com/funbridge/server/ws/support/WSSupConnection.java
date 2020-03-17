package com.funbridge.server.ws.support;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.player.data.PlayerConnectionHistory2;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.HashMap;
import java.util.Map;

@XmlRootElement(name="connection")
public class WSSupConnection {
	public String dateLogin = "";
	public String dateLogout = "";
	public String clientVersion = "";
	public String deviceInfo = "";
    public String deviceType = "";
	public long deviceID = -1;
	public int nbDealPlayed = 0;
	public int nbDealReplay = 0;
    public Map<String, Integer> mapCategoryPlay = new HashMap<>();
    public Map<String, Integer> mapCategoryReplay = new HashMap<>();
    public String playerActions;
    public int nbMessagesSent = 0;
    public int creditDeal = 0;
    private int creditSubscriptionDay = 0;
    private int totalNbDealPlayed = 0;
    private boolean freemium = false;

	public WSSupConnection() {}

    public WSSupConnection(PlayerConnectionHistory2 pch) {
        if (pch.getDateLogin() > 0) {
            dateLogin = Constantes.timestamp2StringDateHour(pch.getDateLogin());
        }
        if (pch.getDateLogout() > 0){
            dateLogout = Constantes.timestamp2StringDateHour(pch.getDateLogout());
        }
        clientVersion = pch.getClientVersion();
        deviceInfo = pch.getDeviceInfo();
        deviceType = pch.getDeviceType();
        deviceID = pch.getDeviceID();
        nbDealPlayed = pch.getNbDealPlayed();
        nbDealReplay = pch.getNbDealReplayed();
        for (Map.Entry<Integer, Integer> e : pch.getMapCategoryPlay().entrySet()) {
            mapCategoryPlay.put(Constantes.tourCategory2Name(e.getKey()), e.getValue());
        }
        for (Map.Entry<Integer, Integer> e : pch.getMapCategoryReplay().entrySet()) {
            mapCategoryReplay.put(Constantes.tourCategory2Name(e.getKey()), e.getValue());
        }
        playerActions = pch.getPlayerActions();
        nbMessagesSent = pch.getNbMessagesSent();
        creditDeal = pch.getCreditDeal();
        creditSubscriptionDay = pch.getCreditSubscriptionDay();
        totalNbDealPlayed = pch.getTotalNbDealPlayed();
        freemium = pch.isFreemium();
    }
}
