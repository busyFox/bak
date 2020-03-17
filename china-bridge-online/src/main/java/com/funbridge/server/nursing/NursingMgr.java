package com.funbridge.server.nursing;

import com.funbridge.server.common.ContextManager;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.common.FunbridgeMgr;
import com.funbridge.server.message.MessageNotifMgr;
import com.funbridge.server.message.data.MessageNotif;
import com.funbridge.server.nursing.data.PlayerNursing;
import com.funbridge.server.player.PlayerMgr;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.player.data.PlayerDeals;
import com.mongodb.MongoClient;
import com.mongodb.client.result.DeleteResult;
import org.apache.commons.lang.StringUtils;
import org.bson.Document;
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.DocumentCodec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Created by ldelbarre on 28/08/2018.
 */
@Component(value="nursingMgr")
@Scope(value="singleton")
public class NursingMgr extends FunbridgeMgr {

    @PersistenceContext
    private EntityManager em;

    @Resource(name = "messageNotifMgr")
    private MessageNotifMgr messageNotifMgr;

    @Resource(name = "playerMgr")
    private PlayerMgr playerMgr;

    @Resource(name="mongoTemplate")
    private MongoTemplate mongoTemplate;

    private static CodecRegistry codecRegistry = CodecRegistries.fromRegistries(MongoClient.getDefaultCodecRegistry());
    private static final DocumentCodec codec = new DocumentCodec(codecRegistry, new BsonTypeClassMap());
    private static Pattern pattern;

    private static final int BDD_TYPE_MYSQL=1;
    private static final int BDD_TYPE_MONGODB=2;

    @PostConstruct
    @Override
    public void init() {
        try {
            pattern = Pattern.compile("@(.+?)@");
        } catch (PatternSyntaxException e) {
            log.error("Pattern not valid !",e);
        }
    }

    @PreDestroy
    @Override
    public void destroy() {

    }

    @Override
    public void startUp() {

    }

    public String processNursing(Player player) {
        boolean nursingEnable = FBConfiguration.getInstance().getConfigBooleanValue("nursing.enable", false);
        long startCreationDate = FBConfiguration.getInstance().getLongValue("nursing.startCreationDate", 1539554400000L);
        if (nursingEnable && player.getCreationDate() > startCreationDate) {
            PlayerDeals playerDeals = playerMgr.getPlayerDeals(player.getID());
            if (playerDeals != null) {
                // A/B Testing
                int maxLastDigit = FBConfiguration.getInstance().getIntValue("nursing.idMaxLastDigit", 1);
                boolean isSampleNoNursing = (player.getID() % 10 <= maxLastDigit);

                if (!isSampleNoNursing) {
                    int timeBetween = FBConfiguration.getInstance().getIntValue("nursing.nbDaysBetween", 1) * 24 * 60 * 60 * 1000;
                    // Process nursing 1 day after player creation date ???????1?????
                    if (System.currentTimeMillis() > player.getCreationDate() + timeBetween) {
                        List<String> nursingList = Arrays.asList(FBConfiguration.getInstance().getStringValue("nursing.list", "").split(";"));
                        if (nursingList != null && !nursingList.isEmpty()) {
                            // Get last player nursing
                            Query query = Query.query(Criteria.where("playerID").is(player.getID()))
                                    .with(Sort.by(Sort.Direction.DESC, "date"));
                            PlayerNursing lastPlayerNursing = mongoTemplate.findOne(query, PlayerNursing.class);

                            // If last nursing step was less than 1 day ago or not read, we skip
                            if (lastPlayerNursing != null &&
                                    (lastPlayerNursing.date > System.currentTimeMillis() - timeBetween || !lastPlayerNursing.read)) {
                                return null;
                            }

                            // Get next nursing for player
                            String nursingName = getNextNursingName(player.getID(), nursingList, lastPlayerNursing);

                            // If nursing is found process it
                            if (nursingName != null) {
                                addNursingForPlayer(player.getID(), nursingName);
                            }

                            return nursingName;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the name of the next nursing that should be done
     * ??????????????
     * @param nursingList
     * @param lastPlayerNursing
     * @return
     */
    public String getNextNursingName(long playerID, List<String> nursingList, PlayerNursing lastPlayerNursing) {
        String nextNursingName = null;
        boolean lastNursingFound = false;
        for (int i = -1; i < nursingList.size()-1; i++) {
            if (lastPlayerNursing == null || ( i >= 0 && nursingList.get(i).equals(lastPlayerNursing.nursingName))) {
                lastNursingFound = true;
            }
            if (lastNursingFound) {
                nextNursingName = nursingList.get(i + 1);
                try {
                    if (isNursingAvailable(nextNursingName, playerID)) {
                        return nextNursingName;
                    }
                } catch (Exception e) {
                    log.error("Exception next nursing " + nextNursingName, e);
                    return null;
                }
            }
        }
        return nextNursingName;
    }

    /**
     * Check if nursing should be done for player
     * @param nursingName
     * @param playerID
     * @return
     */
    public boolean isNursingAvailable(String nursingName, long playerID) throws Exception {
        if (nursingName != null) {
            // Check if nursing has already been done
            Query query = Query.query(Criteria.where("playerID").is(playerID).and("nursingName"));
            PlayerNursing playerNursingAlreadyExists = mongoTemplate.findOne(query, PlayerNursing.class);
            if (playerNursingAlreadyExists != null) {
                return false;
            }

            int nbQuery = FBConfiguration.getInstance().getIntValue("nursing." + nursingName + ".nbQuery", 0);
            // 0 query = No condition. Return true
            if (nbQuery == 0) {
                return true;
            }

            Map<String, String> varValues = new HashMap<>();
            varValues.put("playerID", "" + playerID);

            // Check all conditions
            for (int i = 0; i < nbQuery; i++) {
                int bddType = FBConfiguration.getInstance().getIntValue("nursing." + nursingName + ".query." + i + ".bddType", 0);
                boolean expectedResult = FBConfiguration.getInstance().getConfigBooleanValue("nursing." + nursingName + ".query." + i + ".expectedResult", false);
                if (bddType == BDD_TYPE_MYSQL) {
                    String strQuery = FBConfiguration.getInstance().getStringValue("nursing." + nursingName + ".query." + i + ".sql", null);
                    if (strQuery != null) {
                        strQuery = replaceQueryVariables(strQuery, varValues);
                        javax.persistence.Query mysqlQuery = em.createNativeQuery(strQuery);
                        List<Object[]> result = mysqlQuery.getResultList();
                        if (log.isDebugEnabled() && !result.isEmpty()) {
                            String resultFields = "";
                            for (Object field : result.get(0)) {
                                resultFields += field + " - ";
                            }
                            log.debug(resultFields);
                        }

                        // If condition not respected
                        if ((result.isEmpty() && expectedResult) || (!result.isEmpty() && !expectedResult)) {
                            return false;
                        }
                    }
                } else if (bddType == BDD_TYPE_MONGODB) {
                    String templateName = FBConfiguration.getInstance().getStringValue("nursing." + nursingName + ".query." + i + ".mongoTemplate", null);
                    String collectionName = FBConfiguration.getInstance().getStringValue("nursing." + nursingName + ".query." + i + ".collection", null);
                    String strQuery = FBConfiguration.getInstance().getStringValue("nursing." + nursingName + ".query." + i + ".json", null);
                    if (templateName != null && collectionName != null && strQuery != null) {
                        strQuery = replaceQueryVariables(strQuery, varValues);
                        MongoTemplate mongoTemplate = (MongoTemplate) ContextManager.getContext().getBean(templateName);
                        Query mongoQuery = new BasicQuery(strQuery);
                        Document result = mongoTemplate.findOne(mongoQuery, Document.class, collectionName);
                        // ((Document)mongoTemplate.executeCommand("{ find : '"+collectionName+"', filter : { _id : 5, 'nbPlayedDealsCategory.11' : { $gt : 0 } } }").get("cursor")).get("firstBatch")
                        // mongoTemplate.executeCommand("{ aggregate : '"+collectionName+"', pipeline : [ { $match : { _id : 5, 'nbPlayedDealsCategory.11' : { $gt : 0 } } } ] }").get("result");
                        // mongoTemplate.executeCommand("{ count : '"+collectionName+"', filter : { _id : 5, 'nbPlayedDealsCategory.11' : { $gt : 0 } } }").getInteger("n")
                        if (log.isDebugEnabled() && result != null) {
                            log.debug(result.toJson(codec));
                        }

                        // If condition not respected
                        if ((result == null && expectedResult) || (result != null && !expectedResult)) {
                            return false;
                        }
                    }
                } else {
                    throw new Exception("bdd type " + bddType + " invalid");
                }
            }
            // All conditions are respected
            return true;
        }
        return false;
    }

    /**
     * Add nursing for player (send notif and create PlayerNursing)
     * @param playerID
     * @param nursingName
     */
    public void addNursingForPlayer(long playerID, String nursingName) {
        // create notif
        MessageNotif notif = createNotifNursing(playerID, nursingName);

        // create PlayerNursing
        String notifID = (notif != null) ? notif.getIDStr() : null;
        PlayerNursing playerNursing = new PlayerNursing(playerID, nursingName, System.currentTimeMillis(), notifID);
        mongoTemplate.insert(playerNursing);
    }

    /**
     * Create notif nursing for player
     * @param playerID
     * @param nursingName
     * @return notif
     */
    public MessageNotif createNotifNursing(long playerID, String nursingName){
        if (FBConfiguration.getInstance().getConfigBooleanValue("nursing."+ nursingName +".notification.enable", true)) {
            log.debug("create notif");
            long notifDate = System.currentTimeMillis();
            long recipientID = playerID;
            int category = FBConfiguration.getInstance().getIntValue("nursing." + nursingName + ".notification.category", 1);
            int displayMode = FBConfiguration.getInstance().getIntValue("nursing." + nursingName + ".notification.displayMode", MessageNotifMgr.MESSAGE_DISPLAY_DIALOG_BOX);
            int hoursBeforeExpiration = FBConfiguration.getInstance().getIntValue("nursing." + nursingName + ".notification.hoursBeforeExpiration", 48);
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.HOUR, hoursBeforeExpiration);
            long dateExpiration = cal.getTimeInMillis();
            String templateName = FBConfiguration.getInstance().getStringValue("nursing." + nursingName + ".notification.textTemplate", null);
            Map<String, String> templateParameters = new HashMap<>();
            String[] parameters = FBConfiguration.getInstance().getStringValue("nursing." + nursingName + ".notification.parameters", "").split(";");
            for(String parameter : parameters){
                String[] keyValue = parameter.split(":");
                if (keyValue.length != 2) continue;
                templateParameters.put(keyValue[0], keyValue[1]);
            }
            String fieldName = FBConfiguration.getInstance().getStringValue("nursing." + nursingName + ".notification.fieldName", null);
            String fieldValue = FBConfiguration.getInstance().getStringValue("nursing." + nursingName + ".notification.fieldValue", null);
            String titleIcon = FBConfiguration.getInstance().getStringValue("nursing." + nursingName + ".notification.titleIcon", null);
            String titleBackgroundColor = FBConfiguration.getInstance().getStringValue("nursing." + nursingName + ".notification.titleBackgroundColor", null);
            String titleColor = FBConfiguration.getInstance().getStringValue("nursing." + nursingName + ".notification.titleColor", null);

            return messageNotifMgr.createNotifNursing(notifDate, recipientID, category, displayMode, dateExpiration,
                    templateName, templateParameters, fieldName, fieldValue,
                    titleIcon, titleBackgroundColor, titleColor);
        }
        return null;
    }

    /**
     * Replace variables in queries. Var must be in "@var@" format
     * @param query
     * @param varVal
     * @return
     */
    private String replaceQueryVariables(String query, Map<String, String> varVal) {
        String strReturn = query;
        if (StringUtils.isNotBlank(query) && query.indexOf("@") >= 0) {
            if (varVal != null && !varVal.isEmpty()) {
                // replace all token by the value
                Matcher matcher = pattern.matcher(query);
                StringBuffer sb = new StringBuffer();
                while (matcher.find()) {
                    String val = varVal.get(matcher.group(1));
                    if (val != null) {
                        matcher.appendReplacement(sb, val);
                    }
                }
                matcher.appendTail(sb);
                strReturn = sb.toString();
            }
        }
        return strReturn;
    }

    /**
     * Reset nursing for player
     * @param playerID
     * @return
     */
    public boolean resetNursing(long playerID) {
        DeleteResult result = mongoTemplate.remove(Query.query(Criteria.where("playerID").is(playerID)), PlayerNursing.class);
        return result.wasAcknowledged();
    }

    /**
     * List player nursing
     * @param playerID
     * @return
     */
    public List<PlayerNursing> listPlayerNursing(long playerID) {
        Query query = Query.query(Criteria.where("playerID").is(playerID))
                .with(Sort.by(Sort.Direction.DESC, "date"));
        return mongoTemplate.find(query, PlayerNursing.class);
    }

    /**
     * Delete player nursing
     * @param playerID
     * @param name
     * @return
     */
    public boolean deletePlayerNursing(long playerID, String name) {
        DeleteResult result = mongoTemplate.remove(Query.query(Criteria.where("playerID").is(playerID).and("nursingName").is(name)), PlayerNursing.class);
        return result.wasAcknowledged();
    }
}
