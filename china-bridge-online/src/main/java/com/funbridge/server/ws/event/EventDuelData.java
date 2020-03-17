package com.funbridge.server.ws.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.funbridge.server.ws.tournament.WSDuelHistory;
import com.funbridge.server.ws.tournament.WSTournamentDuelResult;

public class EventDuelData {
    public String message = "";
    public int type = 0;
    public WSDuelHistory duelHistory;
    public WSTournamentDuelResult duelResult;

    @JsonIgnore
    public String toString() {
        return "message=" + message + " - type=" + type + " - duelHistory=" + duelHistory + " - duelResult=" + duelResult;
    }
}
