package com.funbridge.server.tournament.game;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by pserent on 22/06/2017.
 */
public class GameThreadArgine implements Runnable {
    private Logger log = LogManager.getLogger(this.getClass());
    private String threadName;
    private boolean interrupt = false;
    private GameMgr gameMgr = null;
    private boolean synchroMethod = true;
    private long dateLastThreadPlayRunning = 0;
    private Game game;

    public synchronized void interruptRun() {
        interrupt = true;
    }

    public synchronized boolean isInterrupt() {
        return interrupt;
    }

    public GameThreadArgine(Game game, GameMgr mgr) {
        this.game = game;
        this.gameMgr = mgr;
        this.threadName = "GameThreadArgine for gameID="+game.getIDStr()+" - mgr="+mgr;
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
        if (game != null) {
            if (log.isDebugEnabled()) {
                log.debug("run - BEGIN - " + threadName);
            }
            // synchro => loop while it is robot to play
            if (synchroMethod) {
                boolean bContinue = true;
                try {
                    while (bContinue) {
                        bContinue = gameMgr.playArgineGameThread(game, this);
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
                    gameMgr.playArgineGameStep1(game);
                } catch (Exception e) {
                    log.error("Exception on playGameStep1 - exception=" + e.getMessage(), e);
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("run - END - " + threadName);
            }
        } else {
            // no game in session
            log.error("Game is null - threadName="+threadName);
        }
    }
}
