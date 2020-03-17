package com.funbridge.server.ws.game;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.funbridge.server.ws.tournament.WSTournament;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class WSGameView {
    public WSTableGame table;
    public WSGameDeal game;
    public WSTournament tournament;
}
