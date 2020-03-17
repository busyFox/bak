package com.funbridge.server.tournament.game;

/**
 * Created by pserent on 23/04/2015.
 */
public class GameParThread implements Runnable{
    private GameMgr gameMgr = null;
    private Game game = null;

    public GameParThread(GameMgr mgr, Game g) {
        this.gameMgr = mgr;
        this.game = g;
    }

    @Override
    public void run() {
        if (gameMgr != null && game != null) {
            gameMgr.getPar(game);
        }
    }
}
