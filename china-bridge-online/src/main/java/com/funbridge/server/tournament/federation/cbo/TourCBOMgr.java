package com.funbridge.server.tournament.federation.cbo;


import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.tournament.federation.*;
import com.funbridge.server.tournament.federation.data.*;
import com.funbridge.server.tournament.federation.cbo.data.*;
import com.funbridge.server.tournament.federation.cbo.memory.*;
import com.funbridge.server.ws.FBExceptionType;
import com.funbridge.server.ws.FBWSException;
import com.funbridge.server.ws.result.ResultServiceRest;
import com.funbridge.server.ws.result.WSMainRankingPlayer;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.Document;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by ldelbarre on 21/12/2017.
 */
@Component(value="tourCBOMgr")
@Scope(value="singleton")
public class TourCBOMgr extends TourFederationMgr{

    @Resource(name = "mongoTourCBOTemplate")
    private MongoTemplate mongoTemplate;

    public TourCBOMgr() { super(Constantes.TOURNAMENT_CATEGORY_TOUR_CBO); }

    @Override
    @PostConstruct
    public void init() {
        log.info("Init");
        super.init(TourCBOMemTournament.class, TourCBOMemTournamentPlayer.class, TourCBOMemDeal.class, TourCBOMemDealPlayer.class);
        memoryMgr = new TourCBOMemoryMgr(this, TourCBOMemTournament.class, TourCBOMemTournamentPlayer.class, TourCBOMemDeal.class, TourCBOMemDealPlayer.class);
    }

    public long getDateNextJobNotifRanking() {
        try {
            Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey("triggerNotifRankingTask", getTriggerGroup()));
            if (trigger != null) {
                return trigger.getNextFireTime().getTime();
            }
        } catch (Exception e) {
            log.error("Failed to get trigger", e);
        }
        return 0;
    }

    @Override
    public void checkTournamentMode(boolean isReplay, long playerId) throws FBWSException {
        if(playerId > 0 && !playerMgr.getPlayer(playerId).isDateSubscriptionValid() && playerMgr.getPlayer(playerId).getTotalCreditAmount() < 1){
                throw new FBWSException(FBExceptionType.GAME_PLAYER_CREDIT_EMPTY);
        }
    }

    @Override
    public MongoTemplate getMongoTemplate() {
        return mongoTemplate;
    }

    @Override
    public Class<? extends TourFederationTournament> getTournamentEntity() {
        return TourCBOTournament.class;
    }

    @Override
    public Class<? extends TourFederationTournamentPlayer> getTournamentPlayerEntity() {
        return TourCBOTournamentPlayer.class;
    }

    @Override
    public String getTournamentPlayerCollectionName() {
        return "cbo_tournament_player";
    }

    @Override
    public Class<? extends TourFederationGame> getGameEntity() {
        return TourCBOGame.class;
    }

    @Override
    public String getGameCollectionName() {
        return "cbo_game";
    }

    @Override
    public String getPlayerFederationCollectionName() {
        return "cbo_player";
    }

    @Override
    public String getTourFederationStatCollectionName() {
        return "cbo_stat";
    }

    @Override
    public String getFederationName() {
        return "CBO";
    }

    @Override
    public Class<? extends TourFederationGeneratorTask> getTourFederationGeneratorTaskEntity() {
        return TourCBOGeneratorTask.class;
    }

    @Override
    public Class<? extends TourFederationEnableTask> getTourFederationEnableTaskEntity() {
        return TourCBOEnableTask.class;
    }

    @Override
    public Class<? extends TourFederationFinishTask> getTourFederationFinishTaskEntity() {
        return TourCBOFinishTask.class;
    }

    @Override
    public Class<? extends TourFederationMonthlyReportTask> getTourFederationMonthlyReportTask() {
        return TourCBOMonthlyReportTask.class;
    }

    @Override
    public Class<? extends TourFederationGenerateSettings> getTourFederationGenerateSettingsEntity() {
        return TourCBOGenerateSettings.class;
    }

    @Override
    public Class<? extends TourFederationStat> getTourFederationStatEntity() {
        return TourCBOStat.class;
    }

    @Override
    public String getTriggerGroup() {
        return "TourCBO";
    }


    @Override
    public TourFederationGenerateSettingsElement buildGenerateSettingsElement() {
        return new TourCBOGenerateSettingsElement();
    }

    @Override
    protected TourFederationTournament buildTournament() {
        return new TourCBOTournament();
    }

    @Override
    protected TourFederationGame buildGame(long playerID, TourFederationTournament tour, int dealIndex, int playerIndex) {
        return new TourCBOGame(playerID, tour, dealIndex, playerIndex);
    }

    @Override
    protected TourFederationGame buildGame(long playerID, TourFederationTournament tour, int dealIndex) {
        return new TourCBOGame(playerID, tour, dealIndex);
    }

    @Override
    protected TourFederationTournamentPlayer buildTournamentPlayer() {
        return new TourCBOTournamentPlayer();
    }

    @Override
    protected TourFederationStat buildStat() {
        return new TourCBOStat();
    }

    /**
     * Compute the Masterpoints for each player result
     * @param playerResults
     * @return
     */
    public boolean computePoints(List<TourFederationTournamentPlayer> playerResults) {
        if (playerResults != null && playerResults.size() > 0) {
            // Compute points earned for each player
            for(TourFederationTournamentPlayer playerResult : playerResults){
                playerResult.setPoints((int) Math.ceil(2 * ((playerResults.size()+1) - playerResult.getRank()) / Math.log10(playerResult.getRank()+2)));
            }
            return true;
        }
        return false;
    }

    /**
     * Generate the report file of a tournament to be sent to Federation CBO
     */
    public void generateReportForTournament(TourFederationTournament tournament, List<TourFederationTournamentPlayer> playerResults, boolean sendMail) throws FBWSException{
        if(tournament == null){
            log.error("tournament is null !");
        } else if(playerResults == null || playerResults.size() == 0){
            log.warn("results are null or empty ! tourID="+tournament.getIDStr());
        } else {
            // Start date
            Date startDate = new Date(tournament.getStartDate());
            // Generate file
            String fileContent = "#FUNBRIDGE TOURNAMENT "+new SimpleDateFormat("dd/MM/yyyy").format(startDate)+"\n";
            fileContent += "#NUMBER OF PARTICIPANTS : "+playerResults.size()+"\n";
            fileContent += "RANK;NICKNAME;RESULT;MASTERPOINTS;LICENCE\n";
            for(TourFederationTournamentPlayer result : playerResults){
                String licence = "";
                String pseudo = "";
                try{
                    PlayerFederation playerFederation = getPlayerFederationwithTouchID(result.getPlayerID());
                    if(playerFederation != null){
                        if(playerFederation.licence != null && testLicenceValidity(playerFederation.licence)) {
                            licence = playerFederation.licence;
                        }
                        Player p = ContextManager.getPlayerMgr().getPlayer(result.getPlayerID());
                        if(p != null) pseudo = p.getNickname().toUpperCase();
                    }
                } catch (FBWSException e){
                    log.error("PlayerFederation "+getFederationName()+" not found for touchID=" + result.getPlayerID());
                }
                if (tournament.getResultType() == Constantes.TOURNAMENT_RESULT_PAIRE) {
                    fileContent += result.getRank()+";"+pseudo+";"+(double) Math.round(result.getResult()*10000)/100+";"+result.getStringPoints()+";"+licence+";\n";
                } else {
                    fileContent += result.getRank()+";"+pseudo+";"+result.getResult()+";"+result.getStringPoints()+";"+licence+";\n";
                }
            }
            try {
                // Write file
                Calendar cal = Calendar.getInstance();
                cal.setTime(startDate);
                String path = getStringResolvEnvVariableValue("reportFile.path", null);
                path += "/" + cal.get(Calendar.YEAR) + "/" + (cal.get(Calendar.MONTH)+1);
                new File(path).mkdirs();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                String dateString = dateFormat.format(startDate);
                String pathFile = FilenameUtils.concat(path, "Funbridge-CBO-tournament-"+dateString+".TXT");
                FileUtils.writeStringToFile(new File(pathFile), fileContent, "utf-8");
                // Send file by mail to the CBO
                if(sendMail && getConfigIntValue("reportFile.sendMail", 0) == 1) {
                    // Get recipient mail
                    String recipients = getConfigStringValue("reportFile.recipients", null);
                    if (recipients != null && !recipients.isEmpty()) {
                        String[] recipientsList = recipients.split(";");
                        // Send mail
                        boolean result = ContextManager.getMailMgr().sendMail("smtp.goto-games.net", "touch@funbridge.com", recipientsList, "CBO Tournament on Funbridge " + new SimpleDateFormat("dd/MM/yyyy").format(startDate), "Please find in attachment the report for last Funbridge CBO Tournament " + new SimpleDateFormat("dd/MM/yyyy").format(startDate), pathFile);
                        if (!result) {
                            log.error("Error while trying to send mail to the CBO containing the report file " + pathFile);
                        }
                    }
                }
            } catch (IOException e) {
                log.error("Error while writing file", e);
                e.printStackTrace();
            }
        }
    }

    public boolean generateMonthlyReport(int month, int year, boolean sendMail) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        cal.add(Calendar.MONTH, 1);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.MILLISECOND, -1);
        long end = cal.getTimeInMillis();
        List<TourFederationTournament> tournaments = listTournamentsBetweenTwoDates(start, end);
        Date endDate = cal.getTime();
        cal.setTimeInMillis(start);
        Date startDate = cal.getTime();
        log.debug("Number of tournaments between "+startDate+" and "+endDate+" : "+tournaments.size());
        Map<Long, Integer> mapTouchIDMasterpoints = new HashMap<>();
        int participations = 0;
        for(TourFederationTournament tournament : tournaments){
            List<TourCBOTournamentPlayer> results = listTournamentPlayerForTournament(tournament.getIDStr(), 0, -1);
            log.debug("Number of participants for tournament "+tournament+" : "+results.size());
            for(TourCBOTournamentPlayer result : results){
                participations++;
                if (mapTouchIDMasterpoints.containsKey(result.getPlayerID())) {
                    mapTouchIDMasterpoints.put(result.getPlayerID(), mapTouchIDMasterpoints.get(result.getPlayerID()) + (int) result.getPoints());
                } else {
                    mapTouchIDMasterpoints.put(result.getPlayerID(), (int) result.getPoints());
                }
            }
        }
        // Construct report file content
        String fileContent = "#FUNBRIDGE TOURNAMENT - MONTHLY REPORT - "+new SimpleDateFormat("MM/yyyy").format(startDate)+"\n";
        fileContent += "#NUMBER OF TOURNAMENTS : "+tournaments.size()+"\n";
        fileContent += "#NUMBER OF PARTICIPATIONS : "+participations+"\n";
        fileContent += "#NUMBER OF UNIQUE PARTICIPANTS : "+mapTouchIDMasterpoints.size()+"\n";
        fileContent += "NICKNAME;LICENCE;MASTERPOINTS;\n";
        for(Map.Entry<Long, Integer> playerPoints : mapTouchIDMasterpoints.entrySet()){
            String licence = "00000000";
            String pseudo = "";
            try{
                PlayerFederation playerFederation = getPlayerFederationwithTouchID(playerPoints.getKey());
                if(playerFederation != null){
                    if(playerFederation.licence != null && testLicenceValidity(playerFederation.licence)) {
                        licence = playerFederation.licence;
                    }
                    Player p = ContextManager.getPlayerMgr().getPlayer(playerPoints.getKey());
                    if(p != null) pseudo = p.getNickname().toUpperCase();
                }
            } catch (FBWSException e){
                log.error("PlayeCBOF not found for touchID=" + playerPoints.getKey());
            }
            fileContent += pseudo+";"+licence+";"+playerPoints.getValue()+";\n";
        }
        // Write report file
        try {
            // Write file
            String path = getStringResolvEnvVariableValue("reportFile.path", null);
            path += "/" + cal.get(Calendar.YEAR) + "/" + (cal.get(Calendar.MONTH)+1);
            new File(path).mkdirs();
            String pathFile = FilenameUtils.concat(path, "Funbridge-CBO-tournaments-"+new SimpleDateFormat("yyyy-MM").format(startDate)+".TXT");
            FileUtils.writeStringToFile(new File(pathFile), fileContent, "utf-8");
            // Send file by mail to the CBO
            if(sendMail && getConfigIntValue("reportFile.sendMail", 0) == 1) {
                // Get recipient mail
                String recipients = getConfigStringValue("reportFile.recipients", null);
                if (recipients != null && !recipients.isEmpty()) {
                    String[] recipientsList = recipients.split(";");
                    // Send mail
                    boolean result = ContextManager.getMailMgr().sendMail("smtp.goto-games.net", "touch@funbridge.com", recipientsList, "CBO Tournaments on Funbridge - Monthly report " + new SimpleDateFormat("MM/yyyy").format(startDate), "Please find in attachment the monthly report for Funbridge CBO Tournaments " + new SimpleDateFormat("MM/yyyy").format(startDate), pathFile);
                    if (!result) {
                        log.error("Error while trying to send mail to the CBO containing the report file " + pathFile);
                    }
                    return result;
                }
            }
            return true;
        } catch (IOException e) {
            log.error("Error while writing file", e);
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Test if a licence has the correct CBO format
     * @param licence
     * @return
     */
    public boolean testLicenceValidity(String licence){
        return true;
    }

    /**
     * Get settings for date. Read config file to get settings for this weekday
     * @param tsDate
     * @return
     */
    @Override
    public TourFederationGenerateSettings getTourFederationGenerateSettingsForDate(long tsDate) {
        TourFederationGenerateSettings generateSettings = super.getTourFederationGenerateSettingsForDate(tsDate);
        if (generateSettings != null) {
            return generateSettings;
        }

        String settings = getConfigStringValue("generateTour", null);
        if (settings != null) {
            try {
                generateSettings = jsonTools.mapData(settings, getTourFederationGenerateSettingsEntity());
                List<TourFederationGenerateSettingsElement> elements = new ArrayList<>();
                for (TourCBOGenerateSettingsElement element : (List<TourCBOGenerateSettingsElement>)generateSettings.settings) {
                    elements.addAll(element.getElements(tsDate));
                }
                generateSettings.settings = elements;
                return generateSettings;
            } catch (Exception e) {
                log.error("Failed to mapData for settings=" + settings);
            }
        } else {
            log.info("No settings");
        }
        return null;
    }

    /***********
     * Ranking
     */

    /**
     * Get funbridge points current period ranking for player
     * @param playerID
     * @return
     */
    public WSMainRankingPlayer getCurrentPeriodRankingFunbridgePointsForPlayer(long playerID) {
        TourFunbridgePointsStat tourFunbridgePointsStat = funbridgeMongoTemplate.findById(playerID, TourFunbridgePointsStat.class);
        if (tourFunbridgePointsStat != null) {
            TourFederationStatResult statResult = tourFunbridgePointsStat.getStatResult(null, tourFederationStatPeriodMgr.getStatCurrentPeriodID());
            if (statResult != null) {
                WSMainRankingPlayer rankingPlayerFunbridgePoints = new WSMainRankingPlayer(playerID);
                rankingPlayerFunbridgePoints.value = statResult.funbridgePoints;
                rankingPlayerFunbridgePoints.rank = countNbMoreFunbridgePoints(null, statResult, null, null) + 1;
                return rankingPlayerFunbridgePoints;
            }
        }
        return null;
    }

    /**
     * Count nb players with funbridge points for current period
     * @return
     */
    public int countCurrentPeriodRankingFunbridgePoints() {
        return this.countRankingFunbridgePoints(null, null, null);
    }

    /**
     * Get funbridge points ranking for players with filter players, country, offset & limit
     * @param playerAsk
     * @param periodID
     * @param selectionPlayerID
     * @param countryCode
     * @param offset
     * @param nbMax
     * @return
     */
    public ResultServiceRest.GetMainRankingResponse getRankingFunbridgePoints(PlayerCache playerAsk, String periodID, List<Long> selectionPlayerID, String countryCode, int offset, int nbMax) {
        if (nbMax == 0) {
            nbMax = 50;
        }
        ResultServiceRest.GetMainRankingResponse response = new ResultServiceRest.GetMainRankingResponse();
        if (getConfigIntValue("disableMainRanking", 0) == 1) {
            return response;
        }
        response.totalSize = countRankingFunbridgePoints(periodID, selectionPlayerID, countryCode);
        response.nbRankedPlayers = countRankingFunbridgePoints(periodID, null, countryCode);
        response.ranking = new ArrayList<>();
        // rankingPlayer
        if (playerAsk != null) {
            WSMainRankingPlayer rankingPlayerFunbridgePoints = new WSMainRankingPlayer(playerAsk, true, playerAsk.ID);
            TourFunbridgePointsStat tourFunbridgePointsStat = funbridgeMongoTemplate.findById(playerAsk.ID, TourFunbridgePointsStat.class);
            if (tourFunbridgePointsStat != null) {
                TourFederationStatResult statResult = tourFunbridgePointsStat.getStatResult(periodID, tourFederationStatPeriodMgr.getStatCurrentPeriodID());
                if (statResult != null) {
                    rankingPlayerFunbridgePoints.value = statResult.funbridgePoints;
                    rankingPlayerFunbridgePoints.rank = countNbMoreFunbridgePoints(periodID, statResult, countryCode, null) + 1;
                    if (offset == -1) {
                        int playerOffset = countNbMoreFunbridgePoints(periodID, statResult, countryCode, selectionPlayerID) + 1;
                        offset = playerOffset - (nbMax / 2);
                    }
                }
            }
            rankingPlayerFunbridgePoints.rank = (rankingPlayerFunbridgePoints.rank == 0)?-1:rankingPlayerFunbridgePoints.rank;
            response.rankingPlayer = rankingPlayerFunbridgePoints;
        }
        if (offset < 0) {
            offset = 0;
        }

        // list stat
        List<TourFunbridgePointsStat> listStat = this.listTourFunbridgePointsStat(periodID, offset, nbMax, selectionPlayerID, countryCode);

        int currentRank = -1, nbWithSameFunbridgePoints = 0;
        double currentNbFunbridgePoints = -1;
        for (TourFunbridgePointsStat stat : listStat) {
            WSMainRankingPlayer data = new WSMainRankingPlayer(playerCacheMgr.getPlayerCache(stat.playerID), presenceMgr.isSessionForPlayerID(stat.playerID), playerAsk.ID);
            TourFederationStatResult statResult = stat.getStatResult(periodID, tourFederationStatPeriodMgr.getStatCurrentPeriodID());
            if (statResult != null) {
                // init current counter
                if (currentRank == -1) {
                    currentRank = countNbMoreFunbridgePoints(periodID, statResult, countryCode, null) + 1;
                    nbWithSameFunbridgePoints = (offset + 1) - currentRank + 1;
                } else {
                    if (statResult.funbridgePoints == currentNbFunbridgePoints) {
                        nbWithSameFunbridgePoints++;
                    } else {
                        if (selectionPlayerID != null && selectionPlayerID.size() > 0) {
                            currentRank = countNbMoreFunbridgePoints(periodID, statResult, countryCode, null) + 1;
                        } else {
                            currentRank = currentRank + nbWithSameFunbridgePoints;
                        }
                        nbWithSameFunbridgePoints = 1;
                    }
                }
                currentNbFunbridgePoints = statResult.funbridgePoints;

                data.value = statResult.funbridgePoints;
                data.rank = currentRank;
            }
            response.ranking.add(data);
        }

        response.offset = offset;
        return response;
    }

    /**
     * Count nb players with funbridge points for the period
     * @param periodID
     * @return
     */
    public int countRankingFunbridgePoints(String periodID, List<Long> listFollower, String countryCode) {
        Criteria criteria = new Criteria();
        if (listFollower != null && !listFollower.isEmpty()) {
            criteria = Criteria.where("_id").in(listFollower);
        }
        if (StringUtils.isNotBlank(periodID)) {
            criteria = criteria.and("resultPeriod." + periodID).exists(true);
        } else {
            criteria = criteria.and("totalPeriod." + tourFederationStatPeriodMgr.getStatCurrentPeriodID()).exists(true);
        }
        if (countryCode != null) {
            criteria = criteria.and("countryCode").is(countryCode);
        }
        Query query = new Query(criteria);
        return (int)funbridgeMongoTemplate.count(query, TourFunbridgePointsStat.class);
    }

    /**
     * List tour funbridge points stat for a period with offset and nbMax.
     * If period null, return stat total results.
     * @param resultPeriod
     * @param offset
     * @param nbMax
     * @return
     */
    private List<TourFunbridgePointsStat> listTourFunbridgePointsStat(String resultPeriod, int offset, int nbMax, List<Long> listFollower, String countryCode) {
        Criteria criteria = new Criteria();
        if (listFollower != null && !listFollower.isEmpty()) {
            criteria = Criteria.where("_id").in(listFollower);
        }

        Sort sort;
        if (StringUtils.isNotBlank(resultPeriod)) {
            criteria = criteria.andOperator(Criteria.where("resultPeriod."+resultPeriod).exists(true));
            sort = new Sort(Sort.Direction.DESC, "resultPeriod." + resultPeriod + ".funbridgePoints");
        } else {
            criteria = criteria.andOperator(Criteria.where("totalPeriod."+ tourFederationStatPeriodMgr.getStatCurrentPeriodID()).exists(true));
            sort = new Sort(Sort.Direction.DESC, "totalPeriod."+ tourFederationStatPeriodMgr.getStatCurrentPeriodID() +".funbridgePoints");
        }
        if (StringUtils.isNotBlank(countryCode)) {
            criteria = criteria.and("countryCode").is(countryCode);
        }

        Query query = new Query(criteria).with(sort).skip(offset).limit(nbMax);

        if (StringUtils.isNotBlank(resultPeriod)) {
            query.fields().include("resultPeriod." + resultPeriod + ".funbridgePoints");
        } else {
            query.fields().include("totalPeriod."+ tourFederationStatPeriodMgr.getStatCurrentPeriodID() +".funbridgePoints");
        }
        return funbridgeMongoTemplate.find(query, TourFunbridgePointsStat.class);
    }

    /**
     * Count nb players with more funbridge points than player
     * @param periodID
     * @param statResult
     * @param countryCode
     * @return
     */
    private int countNbMoreFunbridgePoints(String periodID, TourFederationStatResult statResult, String countryCode, List<Long> selectionPlayerID) {
        Criteria criteria = new Criteria();
        if (StringUtils.isNotBlank(periodID)) {
            criteria = criteria.and("resultPeriod." + periodID).exists(true)
                    .and("resultPeriod." + periodID + ".funbridgePoints").gt(statResult.funbridgePoints);
        } else {
            criteria = criteria.and("totalPeriod." + tourFederationStatPeriodMgr.getStatCurrentPeriodID()).exists(true)
                    .and("totalPeriod." + tourFederationStatPeriodMgr.getStatCurrentPeriodID() + ".funbridgePoints").gt(statResult.funbridgePoints);
        }
        if (countryCode != null) {
            criteria = criteria.and("countryCode").is(countryCode);
        }
        if (selectionPlayerID != null && !selectionPlayerID.isEmpty()) {
            criteria = criteria.and("playerId").in(selectionPlayerID);
        }

        Query query = new Query(criteria);
        return (int)funbridgeMongoTemplate.count(query, TourFunbridgePointsStat.class);
    }

    @Override
    public boolean initStatPeriodSpecific(String statCurrentPeriodID, String statPreviousPeriodID) {
        boolean processOK = true;

        MongoCollection collectionFunbridgePointsStat = funbridgeMongoTemplate.getCollection("funbridge_points_stat");
        if (collectionFunbridgePointsStat != null) {
            ListIndexesIterable<Document> indexes = collectionFunbridgePointsStat.listIndexes();
            List<String> indexesToRemove = new ArrayList<>();
            boolean existIdxCurrentPeriod = false, existIdxPreviousPeriod = false, existIdxCurrentTotalPeriod = false;
            for (Document idx : indexes) {
                String idxName = (String)idx.get("name");
                if (idxName != null && idxName.startsWith("idx_stat_")) {
                    if (idxName.equals("idx_stat_" + statCurrentPeriodID)) {
                        existIdxCurrentPeriod = true;
                    } else if (idxName.equals("idx_stat_" + statPreviousPeriodID)) {
                        existIdxPreviousPeriod = true;
                    } else if (idxName.equals("idx_stat_total_" + statCurrentPeriodID)) {
                        existIdxCurrentTotalPeriod = true;
                    } else {
                        indexesToRemove.add(idxName);
                    }
                }
            }
            if (!existIdxCurrentPeriod) {
                collectionFunbridgePointsStat.createIndex(Indexes.descending("resultPeriod." + tourFederationStatPeriodMgr.getStatCurrentPeriodID() + ".funbridgePoints"), new IndexOptions().name("idx_stat_"+ tourFederationStatPeriodMgr.getStatCurrentPeriodID()).background(true));
            }
            if (!existIdxPreviousPeriod) {
                collectionFunbridgePointsStat.createIndex(Indexes.descending("resultPeriod."+ tourFederationStatPeriodMgr.getStatPreviousPeriodID() +".funbridgePoints"), new IndexOptions().name("idx_stat_"+ tourFederationStatPeriodMgr.getStatPreviousPeriodID()).background(true));
            }
            if (!existIdxCurrentTotalPeriod) {
                collectionFunbridgePointsStat.createIndex(Indexes.descending("totalPeriod." + tourFederationStatPeriodMgr.getStatCurrentPeriodID() + ".funbridgePoints"), new IndexOptions().name("idx_stat_total_" + tourFederationStatPeriodMgr.getStatCurrentPeriodID()).background(true));
            }
            if (indexesToRemove.size() > 0) {
                for (String e : indexesToRemove) {
                    collectionFunbridgePointsStat.dropIndex(e);
                }
            }
        }

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -1);
        String statPeriodIDToRemove = tourFederationStatPeriodMgr.sdfStatPeriod.format(calendar.getTime());

        // Funbridge points reduction
        List<TourFunbridgePointsStat> statList = funbridgeMongoTemplate.findAll(TourFunbridgePointsStat.class);
        for (TourFunbridgePointsStat stat : statList) {
            synchronized (lockTourFunbridgePointsStat.getLock("" + stat.playerID)) {
                try {
                    TourFederationStatResult currentTotalPeriod = stat.totalPeriod.get(statCurrentPeriodID);
                    if (currentTotalPeriod == null) {
                        currentTotalPeriod = new TourFederationStatResult();
                        TourFederationStatResult previousTotalPeriod = stat.totalPeriod.get(statPreviousPeriodID);
                        if (previousTotalPeriod != null) {
                            currentTotalPeriod.funbridgePoints = Math.ceil(previousTotalPeriod.funbridgePoints * 0.8);
                            currentTotalPeriod.nbTournaments = previousTotalPeriod.nbTournaments;
                        }
                        stat.totalPeriod.put(statCurrentPeriodID, currentTotalPeriod);

                        stat.totalPeriod.remove(statPeriodIDToRemove);

                        funbridgeMongoTemplate.save(stat);
                    }
                } catch (Exception e) {
                    processOK = false;
                    log.error("Failed to create current totalPeriod - playerID=" + stat.playerID, e);
                }
            }
        }

        return processOK;
    }

    /**
     * Update country in tour federation stat for playerID.
     * @param player
     * @return
     */
    public void updatePlayerFunbridgePointsStatCountryCode(Player player) {
        if (player != null) {
            try {
                funbridgeMongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(player.getID())),
                        Update.update("countryCode", player.getDisplayCountryCode()),
                        TourFunbridgePointsStat.class);
            } catch (Exception e) {
                log.error("Failed to update country code for player="+player, e);
            }
        }
    }

    public PlayerFederation getPlayerFederationEntity() {
        return new PlayerCBO();
    }

}