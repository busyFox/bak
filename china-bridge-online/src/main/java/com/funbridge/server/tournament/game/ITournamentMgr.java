package com.funbridge.server.tournament.game;

import com.funbridge.server.presence.FBSession;
import com.funbridge.server.ws.FBWSException;

/**
 * Created by pserent on 23/04/2015.
 */
public interface ITournamentMgr {
    String getTournamentCategoryName();

    Tournament getTournament(String tourID);

    Game getGame(String gameID);

    String _buildDealID(String tournamentId, int index);

    String _extractTourIDFromDealID(String dealID);

    int _extractDealIndexFromDealID(String dealID);

    GameMgr getGameMgr();

    void checkTournamentMode(boolean isReplay, long playerId) throws FBWSException;

    /**
     * Read int value for parameter in name (tournament."+configName+".paramName) in config file
     * @param paramName
     * @param defaultValue
     * @return
     */
    int getConfigIntValue(String paramName, int defaultValue);

    /**
     * Persist tournament data in database
     * @param tour
     * @throws FBWSException
     */
    void updateTournamentDB(Tournament tour) throws FBWSException;

    /**
     * Manager Game in session is finished => updte memory ...
     * @param session
     * @throws FBWSException
     */
    void updateGameFinished(FBSession session) throws FBWSException;

    /**
     * Save game and update result for argine game
     * @param game
     * @throws FBWSException
     */
    void updateGamePlayArgineFinished(Game game) throws FBWSException;

    /**
     * Save if necessary game argine
     * @param game
     */
    void updateGamePlayArgine(Game game);

    /**
     * Check game is valid and tournament is not finished
     * @param tournament
     * @throws FBWSException
     */
    void checkGame(Game tournament) throws FBWSException;

    /**
     * Check if bid analyze is enable
     * @return
     */
    boolean isBidAnalyzeEnable();

    /**
     * Check if dela PAR information is enable
     * @return
     */
    boolean isDealParEnable();
}
