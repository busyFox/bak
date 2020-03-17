package com.funbridge.server.ws.support;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.player.data.PlayerLocation;

public class WSSupPlayerLocation {
    public String dateLastUpdate;
    public double longitude;
    public double latitude;

    public WSSupPlayerLocation() {}
    public WSSupPlayerLocation(PlayerLocation pl) {
        if (pl != null) {
            dateLastUpdate = Constantes.timestamp2StringDateHour(pl.dateLastUpdate);
            if (pl.location != null) {
                longitude = pl.location.longitude;
                latitude = pl.location.latitude;
            }
        }
    }
}
