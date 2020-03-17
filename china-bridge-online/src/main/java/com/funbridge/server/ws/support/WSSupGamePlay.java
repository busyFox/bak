package com.funbridge.server.ws.support;

import com.funbridge.server.tournament.game.Game;

public class WSSupGamePlay {
	public WSSupDeal deal;
	public WSSupGame game;
	
	public WSSupGamePlay() {}

    //	public WSSupGamePlay(TourSerieGame g) {
//        if (g!=null) {
//            deal = new WSSupDeal(g.getDeal(), g.getTournament().getIDStr());
//            game = new WSSupGame(g);
//        }
//    }
    public WSSupGamePlay(Game g) {
        if (g!=null) {
            deal = new WSSupDeal(g.getDeal(), g.getTournament().getIDStr(), g.getTournament().getResultType());
            game = new WSSupGame(g);
        }
    }
}
