package com.funbridge.server.tournament.game;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.presence.FBSession;
import com.funbridge.server.tournament.data.SpreadGameData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by pserent on 20/02/2015.
 */
public class GameThread implements Runnable {
    private Logger log = LogManager.getLogger(this.getClass());
    private String threadName;
    private boolean interrupt = false;
    private FBSession session = null;
    private boolean replay = false;
    private GameMgr gameMgr = null;
    private boolean startGame;
    private boolean synchroMethod = true;
    private long dateLastThreadPlayRunning = 0;

    public synchronized void interruptRun() {
        interrupt = true;
    }

    public synchronized boolean isInterrupt() {
        return interrupt;
    }

    public boolean isReplay() {
        return replay;
    }

    public GameThread(FBSession session, boolean replay, GameMgr mgr, boolean startGame) {
        this.session = session;
        this.replay = replay;
        this.gameMgr = mgr;
        this.startGame = startGame;
        this.threadName = "GameThread for session="+session+" - mgr="+mgr+" - replay="+replay;
    }

    public boolean isSynchroMethod() {
        return synchroMethod;
    }

    public void setSynchroMethod(boolean synchroMethod) {
        this.synchroMethod = synchroMethod;
    }

    public String getThreadName() {
        return threadName;
    }

    public long getDateLastThreadPlayRunning() {
        return dateLastThreadPlayRunning;
    }

    public void setDateLastThreadPlayRunning(long dateLastThreadPlayRunning) {
        this.dateLastThreadPlayRunning = dateLastThreadPlayRunning;
    }

    @Override
    public void run() {
        if (session != null) {
            if (session.getCurrentGameTable() != null) {
                Table table = session.getCurrentGameTable();
                Game game = table.getGame();
                if (game != null) {
                    // add to list of thread game running
                    gameMgr.addThreadPlayRunning(game.getIDStr(), this);

                    if (log.isDebugEnabled()) {
                        log.debug("run - BEGIN - " + threadName);
                    }
                    // if start game => event begin game & current player
                    if (startGame) {
                        int delay = gameMgr.getTournamentMgr().getConfigIntValue("delayBeforeEventAfterStart", 500);
                        if (delay > 0) {
                            try {
                                Thread.sleep(delay);
                            } catch (Exception e){}
                        }
                        // send event BEGIN_GAME
                        session.pushEventGame(table, Constantes.EVENT_TYPE_GAME_BEGIN_GAME, null, null);
                        SpreadGameData spreadData = gameMgr.getSpreadPlay(game.getIDStr());
                        if (spreadData != null) {
                            // waiting for spread ?
                            if (!spreadData.isWaitingForCheckSpread()) {
                                // a spread claim is possible. Send event to ask player agreement
                                session.pushEventGame(table, Constantes.EVENT_TYPE_GAME_SPREAD, Character.toString(spreadData.getRequester()), null);
                            }
                        } else {
                            // send event CURRENT_PLAYER
                            session.pushEventGame(table, Constantes.EVENT_TYPE_GAME_CURRENT_PLAYER, Character.toString(game.getCurrentPlayer()), null);
                        }
                    }
                    // synchro => loop while it is robot to play
                    if (synchroMethod) {
                        boolean bContinue = true;
                        try {
                            while (bContinue) {
                                bContinue = gameMgr.playGameThread(session, this);
                                if (isInterrupt()) {
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            log.error("Exception on loop run - exception=" + e.getMessage(), e);
                        }
                    }
                    // asynchro => call step1. Next execution are done by setResult on engine service
                    else {
                        try {
                            gameMgr.playGameStep1(session);
                        } catch (Exception e) {
                            log.error("Exception on playGameStep1 - exception=" + e.getMessage(), e);
                        }
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("run - END - " + threadName);
                    }
                    // remove to list of thread game running
                    gameMgr.removeThreadPlayRunning(game.getIDStr(), this);
                } else {
                    log.error("Game is null - threadName=" + threadName);
                }
            } else {
                log.error("No game found in table="+session.getCurrentGameTable());
            }
        } else {
            // no game in session
            log.error("Session is null - threadName="+threadName);
        }
    }
}
