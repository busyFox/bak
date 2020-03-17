package com.funbridge.server.ws.event;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.FunbridgeMgr;
import com.funbridge.server.message.MessageMgr;
import com.funbridge.server.message.MessageNotifMgr;
import com.funbridge.server.message.data.MessageNotif;
import com.funbridge.server.message.data.MessageNotifAll;
import com.funbridge.server.message.data.MessageNotifGroup;
import com.funbridge.server.message.data.MessagePlayer2;
import com.funbridge.server.player.PlayerMgr;
import com.funbridge.server.presence.FBSession;
import com.funbridge.server.presence.FilterEvent;
import com.funbridge.server.tournament.category.TournamentTrainingPartnerMgr;
import com.funbridge.server.ws.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service(value = "eventService")
@Scope(value = "singleton")
public class EventServiceRestImpl extends FunbridgeMgr implements EventServiceRest {
    @Resource(name = "playerMgr")
    private PlayerMgr playerMgr;
    @Resource(name = "messageMgr")
    private MessageMgr messageMgr;
    @Resource(name = "messageNotifMgr")
    private MessageNotifMgr notifMgr;
    @Resource(name = "tournamentTrainingPartnerMgr")
    private TournamentTrainingPartnerMgr tournamentTrainingPartnerMgr = null;

    /**
     * Extract the messageID from the wsMessageID
     *
     * @param msgType
     * @param wsMessageID
     * @return
     */
    public static String extractMessageID(String msgType, String wsMessageID) {
        if (wsMessageID != null && wsMessageID.startsWith(msgType + "-")) {
            return wsMessageID.substring((msgType + "-").length());
        }
        return null;
    }

    /**
     * Retrieve the message type from WSMessageID
     *
     * @param wsMessageID
     * @return
     */
    public static String getMessageTypeFromWSMessageID(String wsMessageID) {
        if (wsMessageID != null) {
            if (wsMessageID.startsWith(MessageNotif.MSG_TYPE + "-")) {
                return MessageNotif.MSG_TYPE;
            }
            if (wsMessageID.startsWith(MessageNotifAll.MSG_TYPE + "-")) {
                return MessageNotifAll.MSG_TYPE;
            }
            if (wsMessageID.startsWith(MessageNotifGroup.MSG_TYPE + "-")) {
                return MessageNotifGroup.MSG_TYPE;
            }
            if (wsMessageID.startsWith(MessageNotifGroup.MSG_TYPE + "-")) {
                return MessageNotifGroup.MSG_TYPE;
            }
            if (wsMessageID.startsWith(MessagePlayer2.MSG_TYPE + "-")) {
                return MessagePlayer2.MSG_TYPE;
            }
        }
        return null;
    }

    /**
     * Call by spring on initialisation of bean
     */
    @PostConstruct
    @Override
    public void init() {
    }

    @PreDestroy
    @Override
    public void destroy() {
    }

    @Override
    public void startUp() {

    }

    @Override
    public FBWSResponse getEvents(String sessionID, GetEventsParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetEvents(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType(), e.localizedMessage));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid - param=" + param + " - sessionID=" + sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetEventsResponse processGetEvents(FBSession session, GetEventsParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        GetEventsResponse resp = new GetEventsResponse();
        // change tableID
        long previousTableID = session.getCurrentTableID();
        // value change ?
        if (previousTableID != param.tableID) {
            // prevent player on previous table
            if (previousTableID != -1) {
                tournamentTrainingPartnerMgr.onEventPlayerStatusChange(previousTableID, session.getPlayer().getID(), Constantes.TABLE_PLAYER_STATUS_NOT_PRESENT);
            }

            // prevent player on new table
            if (param.tableID != -1) {
                tournamentTrainingPartnerMgr.onEventPlayerStatusChange(param.tableID, session.getPlayer().getID(), Constantes.TABLE_PLAYER_STATUS_PRESENT);
            }
        }

        session.setCurrentTableID(param.tableID);

        // filter event
        FilterEvent filter = new FilterEvent();
        filter.receiverID = session.getLoginID();
        resp.listEvent = session.popEvents(param.lastTS, filter);
        return resp;
    }

    @Override
    public FBWSResponse getMessages(String sessionID, GetMessagesParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetMessages(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType(), e.localizedMessage));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid - param=" + param + " - sessionID=" + sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetMessagesReponse processGetMessages(FBSession session, GetMessagesParam param) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
        }
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        GetMessagesReponse resp = new GetMessagesReponse();
        if (param.typeMask == 0) {
            param.typeMask = Constantes.MESSAGE_TYPE_ALL;
        }
        resp.listMessages = new ArrayList<WSMessage>();
        resp.totalSize = 0;
        resp.offset = param.offset;

        if (param.sender == Constantes.PLAYER_FUNBRIDGE_ID) {
            long tsCurrent = System.currentTimeMillis();
            resp.listMessages = notifMgr.listWSMessageForPlayer(session.getPlayer(), param.offset, param.nbMax, tsCurrent);
            resp.totalSize = notifMgr.countNotifForPlayer(session.getPlayer().getID(), tsCurrent);
            resp.offset = param.offset;
        }
        return resp;
    }

    @Override
    public FBWSResponse getMessagesCount(String sessionID, GetMessagesCountParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetMessagesCount(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType(), e.localizedMessage));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid - param=" + param + " - sessionID=" + sessionID);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetMessagesCountResponse processGetMessagesCount(FBSession session, GetMessagesCountParam param) throws FBWSException {
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        long ts = System.currentTimeMillis();
        GetMessagesCountResponse resp = new GetMessagesCountResponse();
        resp.currentTS = ts;
        return resp;
    }

    @Override
    public FBWSResponse setMessageRead(String sessionID, SetMessageReadParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processSetMessageRead(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType(), e.localizedMessage));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("SessionID not valid - sessionID=" + sessionID + " - param=" + param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public SetMessageReadResponse processSetMessageRead(FBSession session, SetMessageReadParam param) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
        }
        if (param == null || !param.isValid()) {
            log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        SetMessageReadResponse resp = new SetMessageReadResponse();
        if (param.listID == null || param.listID.size() == 0) {
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        List<String> listNotifID = new ArrayList<String>();
        List<String> listNotifAllID = new ArrayList<String>();
        List<String> listNotifGroupID = new ArrayList<String>();
        List<String> listMsgID = new ArrayList<String>();
        for (String s : param.listID) {
            if (s.length() == 0) {
                continue;
            }
            String msgType = getMessageTypeFromWSMessageID(s);
            if (msgType != null) {
                String msgID = extractMessageID(msgType, s);
                if (msgID != null && msgType.equals(MessageNotif.MSG_TYPE)) {
                    listNotifID.add(msgID);
                } else if (msgID != null && msgType.equals(MessageNotifAll.MSG_TYPE)) {
                    listNotifAllID.add(msgID);
                } else if (msgID != null && msgType.equals(MessageNotifGroup.MSG_TYPE)) {
                    listNotifGroupID.add(msgID);
                } else if (msgID != null && msgType.equals(MessagePlayer2.MSG_TYPE)) {
                    listMsgID.add(msgID);
                } else {
                    log.error("Failed to process for msgID=" + msgID + " - s=" + s);
                }
            }
        }
        if (listNotifID.size() > 0) {
            notifMgr.setNotifSimpleRead(session.getPlayer().getID(), listNotifID);
        }
        if (listNotifAllID.size() > 0) {
            notifMgr.setNotifAllRead(session.getPlayer().getID(), listNotifAllID);
        }
        if (listNotifGroupID.size() > 0) {
            notifMgr.setNotifGroupRead(session.getPlayer().getID(), listNotifGroupID);
        }
        if (listMsgID.size() > 0) {
            messageMgr.setMessagePlayerRead(session.getPlayer().getID(), listMsgID);
        }
        resp.result = true;
        return resp;
    }
}
