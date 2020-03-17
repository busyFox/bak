package com.funbridge.server.ws.notification;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.common.FunbridgeMgr;
import com.funbridge.server.message.MessageNotifMgr;
import com.funbridge.server.message.data.MessageNotif;
import com.funbridge.server.message.data.MessageNotifAll;
import com.funbridge.server.message.data.MessageNotifGroup;
import com.funbridge.server.operation.OperationMgr;
import com.funbridge.server.operation.connection.OperationConnection;
import com.funbridge.server.presence.FBSession;
import com.funbridge.server.presence.PresenceMgr;
import com.funbridge.server.ws.FBExceptionType;
import com.funbridge.server.ws.FBWSException;
import com.funbridge.server.ws.FBWSExceptionRest;
import com.funbridge.server.ws.FBWSResponse;
import com.funbridge.server.ws.notification.param.AddNotificationConnectionForPlayersParam;
import com.funbridge.server.ws.notification.param.AddNotificationConnectionParam;
import com.funbridge.server.ws.notification.param.SendNotificationNowParam;
import com.funbridge.server.ws.notification.response.NotificationDefaultResponse;
import com.gotogames.common.session.Session;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.List;

/**
 * Created by pserent on 04/11/2015.
 */
@Path("/notification")
@Service(value="notificationService")
@Scope(value="singleton")
public class NotificationService extends FunbridgeMgr{
    public static String EXCEPTION_COMMON_SERVER_ERROR = "ERROR_SERVER";
    public static String EXCEPTION_COMMON_PARAM_NOT_VALID = "PARAMETER_NOT_VALID";
    @Resource(name="messageNotifMgr")
    private MessageNotifMgr notifMgr = null;
    @Resource(name="operationMgr")
    private OperationMgr operationMgr = null;
    @Resource(name="presenceMgr")
    private PresenceMgr presenceMgr = null;

    @Override
    public void init() {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void startUp() {
    }

    /**
     * Send a notification now to players
     * @param param
     * @return
     */
    @POST
    @Path("/sendNotificationNow")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse sendNotificationNow(SendNotificationNowParam param) {
        FBWSResponse response = new FBWSResponse();
        if (log.isDebugEnabled()) {
            log.debug("param="+param);
        }
        if (param != null && param.isValid()) {
            param.notifData.transformText();
            try {
                NotificationDefaultResponse resp = new NotificationDefaultResponse();
                if (param.allPlayers) {
                    MessageNotifAll notifAll = notifMgr.createNotifAll(param.notifData.category,
                            param.notifData.displayMode,
                            param.notifData.getExpirationDateTS(),
                            param.notifData.textTemplate,
                            param.notifData.mapTextTemplateParameters,
                            param.notifData.textEN, param.notifData.textFR, null,
                            param.notifData.getParamFieldName(), param.notifData.getParamFieldValue(),
                            param.notifData.titleFR, param.notifData.titleEN, param.notifData.titleIcon, param.notifData.titleBackgroundColor, param.notifData.titleColor,
                            param.notifData.richBodyFR, param.notifData.richBodyEN, param.notifData.actionButtonTextFR, param.notifData.actionButtonTextEN,
                            param.notifData.actionTextFR, param.notifData.actionTextEN);
                    if (notifAll != null) {
                        List<Session> listSession = presenceMgr.getAllCurrentSession();
                        for (Session s : listSession) {
                            if (s instanceof FBSession) {
                                FBSession fbs = (FBSession) s;
                                fbs.pushEvent(notifMgr.buildEvent(notifAll, fbs.getPlayer()));
                            }
                        }
                        resp.result = true;
                        resp.log = "Set notification now to all players - param="+param+" - notifAll="+notifAll;
                        log.warn(resp.log);
                    } else {
                        log.error("Failed to create notif for all - param=" + param);
                        throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                    }
                } else {
                    if (param.selectionPlayers.size() > FBConfiguration.getInstance().getIntValue("notify.sendNotificationNow.groupLimit", 50)) {
                        MessageNotifGroup notif = notifMgr.createNotifGroup(param.selectionPlayers,
                                param.notifData.category,
                                param.notifData.displayMode,
                                0,
                                param.notifData.getExpirationDateTS(),
                                param.notifData.textTemplate,
                                param.notifData.mapTextTemplateParameters,
                                param.notifData.textEN, param.notifData.textFR, null,
                                param.notifData.getParamFieldName(), param.notifData.getParamFieldValue(),
                                param.notifData.titleFR, param.notifData.titleEN, param.notifData.titleIcon, param.notifData.titleBackgroundColor, param.notifData.titleColor,
                                param.notifData.richBodyFR, param.notifData.richBodyEN, param.notifData.actionButtonTextFR, param.notifData.actionButtonTextEN,
                                param.notifData.actionTextFR, param.notifData.actionTextEN);
                        if (notif != null) {
                            for (Long plaID : param.selectionPlayers) {
                                FBSession sessionPlayer = presenceMgr.getSessionForPlayerID(plaID);
                                if (sessionPlayer != null) {
                                    sessionPlayer.pushEvent(notifMgr.buildEvent(notif, sessionPlayer.getPlayer()));
                                }
                            }
                            resp.result = true;
                            resp.log = "Set notification now to players selection using notifGroup - param="+param+" - notif="+notif;
                            log.warn(resp.log);
                        } else {
                            log.error("Failed to create notif for group - param=" + param);
                            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                        }
                    } else {
                        int nbOK = 0, nbFailed = 0;
                        for (Long l : param.selectionPlayers) {
                            MessageNotif notif = notifMgr.createNotif(
                                    System.currentTimeMillis(),
                                    l,
                                    param.notifData.category,
                                    param.notifData.displayMode,
                                    param.notifData.getExpirationDateTS(),
                                    param.notifData.textTemplate,
                                    param.notifData.mapTextTemplateParameters,
                                    param.notifData.textEN, param.notifData.textFR, null,
                                    param.notifData.getParamFieldName(), param.notifData.getParamFieldValue(),
                                    param.notifData.titleFR, param.notifData.titleEN, param.notifData.titleIcon, param.notifData.titleBackgroundColor, param.notifData.titleColor,
                                    param.notifData.richBodyFR, param.notifData.richBodyEN, param.notifData.actionButtonTextFR, param.notifData.actionButtonTextEN,
                                    param.notifData.actionTextFR, param.notifData.actionTextEN);
                            if (notif != null) {
                                FBSession sessionPlayer = presenceMgr.getSessionForPlayerID(l);
                                if (sessionPlayer != null) {
                                    sessionPlayer.pushEvent(notifMgr.buildEvent(notif, sessionPlayer.getPlayer()));
                                }
                                nbOK++;
                            } else {
                                nbFailed++;
                            }
                        }
                        resp.result = nbOK > 0;
                        resp.log = "Notif send for param="+param+" - nbOK="+nbOK+" - nbFailed="+nbFailed;
                        log.warn(resp.log);
                    }
                }
                response.setData(resp);
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
                log.error("Exception param="+param, e);
            }
        } else {
            log.error("Param not valid - param="+param.toString());
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        if (log.isDebugEnabled()) {
            log.debug("response="+response);
        }
        return response;
    }

    /**
     * Add a notification on connection for player with criteria : lang, deviceType, country or ALL
     * @param param
     * @return
     */
    @POST
    @Path("/addNotificationConnection")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse addNotificationConnection(AddNotificationConnectionParam param) {
        FBWSResponse response = new FBWSResponse();
        if (log.isDebugEnabled()) {
            log.debug("param="+param);
        }
        if (param != null && param.isValid()) {
            try {
                NotificationDefaultResponse resp = new NotificationDefaultResponse();
                param.notifData.transformText();
                // create notification group
                MessageNotifGroup notif = notifMgr.createNotifGroup(null,
                        param.notifData.category,
                        param.notifData.displayMode,
                        param.notifData.getNotifDateTS(),
                        param.notifData.getExpirationDateTS(),
                        param.notifData.textTemplate,
                        param.notifData.mapTextTemplateParameters,
                        param.notifData.textEN, param.notifData.textFR, null,
                        param.notifData.getParamFieldName(), param.notifData.getParamFieldValue(),
                        param.notifData.titleFR, param.notifData.titleEN, param.notifData.titleIcon, param.notifData.titleBackgroundColor, param.notifData.titleColor,
                        param.notifData.richBodyFR, param.notifData.richBodyEN, param.notifData.actionButtonTextFR, param.notifData.actionButtonTextEN,
                        param.notifData.actionTextFR, param.notifData.actionTextEN);
                if (notif != null && notif.getIDStr() != null) {
                    long dateStart = System.currentTimeMillis();
                    if (param.notifData.getNotifDateTS() > System.currentTimeMillis()) {
                        dateStart = param.notifData.getNotifDateTS();
                    }
                    OperationConnection op = operationMgr.createOperationConnectionNotif(param.operationName,
                            true,
                            param.notifData.getExpirationDateTS(),
                            dateStart, dateStart + Constantes.TIMESTAMP_HOUR,
                            notif.getIDStr(),
                            param.playerLang, param.deviceType, param.playerCountry, param.playerSegmentation);
                    if (op != null) {
                        // reload operations (wait 1s before reload list operation)
                        Thread.sleep(1000);
                        boolean reloadResult = operationMgr.loadListMemoryOperationConnection();
                        if (reloadResult) {
                            resp.result = true;
                            resp.log = "Create with sucess operationConnection and reload list opeartion OK - param=[" + param + "] - op=[" + op + "]";
                        } else {
                            resp.log = "Create with sucess operationConnection but reload list operation FAILED - param=[" + param + "] - op=" + op + "]";
                        }
                        log.warn(resp.log);
                    } else {
                        notifMgr.removeNotif(notif);
                    }
                } else {
                    log.error("Failed to create notif group - param="+param);
                }
                response.setData(resp);
            } catch (Exception e) {
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
                log.error("Exception param="+param, e);
            }
        } else {
            log.error("Param not valid - param="+param.toString());
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        if (log.isDebugEnabled()) {
            log.debug("response="+response);
        }
        return response;
    }

    /**
     * Add a notification on connection for players (list playerID)
     * @param param
     * @return
     */
    @POST
    @Path("/addNotificationConnectionForPlayers")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse addNotificationConnectionForPlayers(AddNotificationConnectionForPlayersParam param) {
        FBWSResponse response = new FBWSResponse();
        if (log.isDebugEnabled()) {
            log.debug("param="+param);
        }
        if (param != null && param.isValid()) {
            try {
                NotificationDefaultResponse resp = new NotificationDefaultResponse();
                param.notifData.transformText();
                // create notification group
                MessageNotifGroup notif = notifMgr.createNotifGroup(null,
                        param.notifData.category,
                        param.notifData.displayMode,
                        param.notifData.getNotifDateTS(),
                        param.notifData.getExpirationDateTS(),
                        param.notifData.textTemplate,
                        param.notifData.mapTextTemplateParameters,
                        param.notifData.textEN, param.notifData.textFR, null,
                        param.notifData.getParamFieldName(), param.notifData.getParamFieldValue(),
                        param.notifData.titleFR, param.notifData.titleEN, param.notifData.titleIcon, param.notifData.titleBackgroundColor, param.notifData.titleColor,
                        param.notifData.richBodyFR, param.notifData.richBodyEN, param.notifData.actionButtonTextFR, param.notifData.actionButtonTextEN,
                        param.notifData.actionTextFR, param.notifData.actionTextEN);
                if (notif != null && notif.getIDStr() != null) {
                    long dateStart = System.currentTimeMillis();
                    if (param.notifData.getNotifDateTS() > System.currentTimeMillis()) {
                        dateStart = param.notifData.getNotifDateTS();
                    }
                    OperationConnection op = operationMgr.createOperationConnectionNotifPlayerList(param.operationName,
                            true, param.selectionPlayers,
                            param.notifData.getExpirationDateTS(),
                            dateStart, System.currentTimeMillis() + Constantes.TIMESTAMP_HOUR,
                            notif.getIDStr());
                    if (op != null) {
                        // reload operations (wait 1s before reload list operation)
                        Thread.sleep(1000);
                        boolean reloadResult = operationMgr.loadListMemoryOperationConnection();
                        if (reloadResult) {
                            resp.result = true;
                            resp.log = "Create with sucess operationConnection for playerList and reload list opeartion OK - param=[" + param + "] - op=[" + op + "]";
                        } else {
                            resp.log = "Create with sucess operationConnection for playerList but reload list operation FAILED - param=[" + param + "] - op=" + op + "]";
                        }
                        log.warn(resp.log);
                    } else {
                        notifMgr.removeNotif(notif);
                    }
                } else {
                    log.error("Failed to create notif group - param="+param);
                }
                response.setData(resp);
            } catch (Exception e) {
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
                log.error("Exception param="+param, e);
            }
        } else {
            log.error("Param not valid - param="+param.toString());
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        if (log.isDebugEnabled()) {
            log.debug("response="+response);
        }
        return response;
    }
}
