package com.funbridge.server.ws.tournament;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.funbridge.server.ws.game.WSGameDeal;

/**
 * Created by pserent on 05/01/2016.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class WSDealCommented {
    public WSGameDeal deal;
    public String commentsBids;
    public String commentsCards;
    public int nbTricks;
}
