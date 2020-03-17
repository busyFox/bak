package com.funbridge.server.ws.message;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.FunbridgeMgr;
import com.funbridge.server.message.ChatMgr;
import com.funbridge.server.message.GenericChatMgr;
import com.funbridge.server.message.data.GenericChatroom;
import com.funbridge.server.presence.FBSession;
import com.funbridge.server.tournament.privatetournament.PrivateTournamentChatMgr;
import com.funbridge.server.tournament.privatetournament.data.PrivateTournament;
import com.funbridge.server.ws.*;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ws.rs.*;

/**
 * Created by bplays on 07/11/16.
 */
@Service(value="messageService")
@Path("/message")
public class MessageService extends FunbridgeMgr {

    @Resource(name = "chatMgr")
    private ChatMgr chatMgr;
    @Resource(name = "privateTournamentChatMgr")
    private PrivateTournamentChatMgr privateTournamentChatMgr;

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

    @POST
    @Path("/getChatroomsForPlayer")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse getChatroomsForPlayer(@HeaderParam("sessionID") String sessionID, GetChatroomsForPlayerParam param){
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetChatroomsForPlayer(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : "+e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid");
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetChatroomsResponse processGetChatroomsForPlayer(FBSession session, GetChatroomsForPlayerParam param) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID());
        }

        GetChatroomsResponse resp = new GetChatroomsResponse();
        resp.chatrooms = chatMgr.getWSChatroomsForPlayer(session.getPlayer().getID(), param.search, param.number, param.offset);
        resp.offset = param.offset;
        resp.totalSize = chatMgr.countChatroomsForPlayer(session.getPlayer().getID(), param.search);
        return resp;
    }

    @POST
    @Path("/getChatroom")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse getChatroom(@HeaderParam("sessionID") String sessionID, GetChatroomParam param){
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null && param.isValid()) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetChatroom(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : "+e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid");
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetChatroomResponse processGetChatroom(FBSession session, GetChatroomParam param) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID());
        }
        GetChatroomResponse resp = new GetChatroomResponse();
        GenericChatroom chatroom = chatMgr.findChatroomByID(param.chatroomID);
        if(chatroom == null){
            throw new FBWSException(FBExceptionType.MESSAGE_UNKNOWN_CHATROOM);
        }
        resp.chatroom = chatMgr.toWSChatroom(chatroom, chatroom.getParticipant(session.getPlayer().getID()), true, true);
        return resp;
    }

    @POST
    @Path("/getChatroomWithPlayer")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse getChatroomWithPlayer(@HeaderParam("sessionID") String sessionID, GetChatroomWithPlayerParam param){
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null && param.isValid()) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetChatroomWithPlayer(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : "+e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid");
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetChatroomResponse processGetChatroomWithPlayer(FBSession session, GetChatroomWithPlayerParam param) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID());
        }
        GetChatroomResponse resp = new GetChatroomResponse();
        GenericChatroom chatroom = chatMgr.findSingleChatroomForPlayers(session.getPlayer().getID(), param.playerID);
        if(chatroom != null){
            resp.chatroom = chatMgr.toWSChatroom(chatroom, chatroom.getParticipant(session.getPlayer().getID()), true, true);
        }
        return resp;
    }

    @POST
    @Path("/getMessagesForChatroom")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse getMessagesForChatroom(@HeaderParam("sessionID") String sessionID, GetMessagesParam param){
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null && param.isValid()) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetMessagesForChatroom(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : "+e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid");
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetMessagesResponse processGetMessagesForChatroom(FBSession session, GetMessagesParam param) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        GetMessagesResponse resp = new GetMessagesResponse();
        GenericChatMgr manager = getManager(param.categoryID);
        boolean useJoinDateAndResetDate = false;
        if (manager.equals(chatMgr)) {
            useJoinDateAndResetDate = true;
        }
        resp.messages = manager.getMessagesForChatroom(param.chatroomID, session.getPlayer().getID(), useJoinDateAndResetDate, param.offset, param.nbMax, param.minDate, param.maxDate);
        resp.offset = param.offset;
        resp.totalSize = manager.getNbMessagesForChatroomAndPlayer(param.chatroomID, session.getPlayer().getID(), useJoinDateAndResetDate, param.minDate, param.maxDate);
        return resp;
    }

    @POST
    @Path("/sendMessageToChatroom")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse sendMessageToChatroom(@HeaderParam("sessionID") String sessionID, SendMessageToChatroomParam param){
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null && param.isValid()) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processSendMessageToChatroom(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : "+e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid");
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public SendMessageToChatroomResponse processSendMessageToChatroom(FBSession session, SendMessageToChatroomParam param) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        SendMessageToChatroomResponse resp = new SendMessageToChatroomResponse();
        GenericChatMgr manager = getManager(param.categoryID);
        boolean useJoinDateAndResetDate = false;
        if (manager.equals(chatMgr)) {
            useJoinDateAndResetDate = true;
        }
        // the order is important !!
        // 1 => list unread messages
        resp.unreadMessages = manager.getUnreadMessagesForPlayerAndChatroom(session.getPlayer().getID(), param.chatroomID, useJoinDateAndResetDate);

        // 2 => send message => change the lastRead value
        resp.message = manager.sendMessageToChatroom(param.chatroomID, param.body, param.mediaID, param.mediaSize, param.lang, param.quotedMessageID, session.getPlayer(), param.tempID, true);
        return resp;
    }

    @POST
    @Path("/setMessageRead")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse setMessageRead(@HeaderParam("sessionID") String sessionID, SetMessageReadParam param){
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null && param.isValid()) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processSetMessageRead(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : "+e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid");
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public BooleanResponse processSetMessageRead(FBSession session, SetMessageReadParam param) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        BooleanResponse resp = new BooleanResponse();
        GenericChatMgr manager = getManager(param.categoryID);
        resp.result = manager.setMessageRead(param.messageID, session);
        return resp;
    }

    @POST
    @Path("/resetChatroomHistory")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse resetChatroomHistory(@HeaderParam("sessionID") String sessionID, ChatroomIDParam param){
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null && param.isValid()) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processResetChatroomHistory(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : "+e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid");
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public BooleanResponse processResetChatroomHistory(FBSession session, ChatroomIDParam param) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        BooleanResponse resp = new BooleanResponse();
        resp.result = chatMgr.resetChatroomHistory(param.chatroomID, session.getPlayer().getID());
        return resp;
    }

    @POST
    @Path("/getChatroomsForTournament")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse getChatroomsForTournament(@HeaderParam("sessionID") String sessionID, TournamentIDParam param){
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null && param.isValid()) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetChatroomsForTournament(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : "+e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid");
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetChatroomsForTournamentResponse processGetChatroomsForTournament(FBSession session, TournamentIDParam param) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        GetChatroomsForTournamentResponse resp = new GetChatroomsForTournamentResponse();
        resp.chatrooms = privateTournamentChatMgr.getChatroomsForTournament(param.tournamentID, session.getPlayer().getID());
        PrivateTournament tournament = (PrivateTournament) privateTournamentChatMgr.getPrivateTournamentMgr().getTournament(param.tournamentID);
        resp.canModerate = tournament.getOwnerID() == session.getPlayer().getID();
        return resp;
    }

    @POST
    @Path("/moderateMessage")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse moderateMessage(@HeaderParam("sessionID") String sessionID, ModerateMessageParam param){
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null && param.isValid()) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processModerateMessage(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : "+e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid");
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public ModerateMessageResponse processModerateMessage(FBSession session, ModerateMessageParam param) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        ModerateMessageResponse resp = new ModerateMessageResponse();
        resp.message = privateTournamentChatMgr.moderateMessage(param.messageID, param.tournamentID, param.moderated, session.getPlayer().getID());
        return resp;
    }

    @POST
    @Path("/deleteMessage")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse deleteMessage(@HeaderParam("sessionID") String sessionID, DeleteMessageParam param){
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null && param.isValid()) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processDeleteMessage(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : "+e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid");
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public BooleanResponse processDeleteMessage(FBSession session, DeleteMessageParam param) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        BooleanResponse resp = new BooleanResponse();
        switch(param.categoryID) {
            case Constantes.TOURNAMENT_CATEGORY_PRIVATE:
                resp.result = privateTournamentChatMgr.deleteMessage(session.getPlayer(), param.messageID);
                break;
            default:
                resp.result = chatMgr.deleteMessage(session.getPlayer(), param.messageID);
                break;
        }
        return resp;
    }

    @POST
    @Path("/sendMessageToPlayer")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse sendMessageToPlayer(@HeaderParam("sessionID") String sessionID, SendMessageToPlayerParam param){
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null && param.isValid()) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processSendMessageToPlayer(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : "+e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid");
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public SendMessageToPlayerResponse processSendMessageToPlayer(FBSession session, SendMessageToPlayerParam param) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        SendMessageToPlayerResponse resp = new SendMessageToPlayerResponse();
        resp.chatroom = chatMgr.sendMessageToPlayer(session.getPlayer(), param.playerID, param.body, param.mediaID, param.mediaSize, param.tempID);
        return resp;
    }

    @POST
    @Path("/createGroupChatroom")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse createGroupChatroom(@HeaderParam("sessionID") String sessionID, CreateGroupChatroomParam param) throws FBWSException {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null && param.isValid()) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processCreateGroupChatroom(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : "+e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid");
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetChatroomResponse processCreateGroupChatroom(FBSession session, CreateGroupChatroomParam param) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        GetChatroomResponse resp = new GetChatroomResponse();
        resp.chatroom = chatMgr.createGroupChatroom(session.getPlayer(), param.name, param.imageID, param.players);
        return resp;
    }

    @POST
    @Path("/setChatroomName")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse setChatroomName(@HeaderParam("sessionID") String sessionID, SetChatroomNameParam param) throws FBWSException {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null && param.isValid()) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processSetChatroomName(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : "+e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid");
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public BooleanResponse processSetChatroomName(FBSession session, SetChatroomNameParam param) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        BooleanResponse resp = new BooleanResponse();
        resp.result = chatMgr.setChatroomName(session.getPlayer(), param.chatroomID, param.name);
        return resp;
    }

    @POST
    @Path("/setChatroomImage")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse setChatroomImage(@HeaderParam("sessionID") String sessionID, SetChatroomImageParam param) throws FBWSException {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null && param.isValid()) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processSetChatroomImage(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : "+e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid");
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public BooleanResponse processSetChatroomImage(FBSession session, SetChatroomImageParam param) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        BooleanResponse resp = new BooleanResponse();
        resp.result = chatMgr.setChatroomImage(session.getPlayer(), param.chatroomID, param.imageID);
        return resp;
    }

    @POST
    @Path("/addParticipantsToChatroom")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse addParticipantsToChatroom(@HeaderParam("sessionID") String sessionID, AddParticipantsToChatroomParam param) throws FBWSException {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null && param.isValid()) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processAddParticipantsToChatroom(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : "+e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid");
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetChatroomResponse processAddParticipantsToChatroom(FBSession session, AddParticipantsToChatroomParam param) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        GetChatroomResponse resp = new GetChatroomResponse();
        resp.chatroom = chatMgr.addParticipantsToChatroom(session.getPlayer(), param.chatroomID, param.players, true);
        return resp;
    }

    @POST
    @Path("/removeParticipantFromChatroom")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse removeParticipantFromChatroom(@HeaderParam("sessionID") String sessionID, RemoveParticipantFromChatroomParam param) throws FBWSException {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null && param.isValid()) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processRemoveParticipantFromChatroom(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : "+e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid");
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetChatroomResponse processRemoveParticipantFromChatroom(FBSession session, RemoveParticipantFromChatroomParam param) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        GetChatroomResponse resp = new GetChatroomResponse();
        resp.chatroom = chatMgr.removeParticipantFromChatroom(session.getPlayer(), param.chatroomID, param.playerID, true);
        return resp;
    }

    @POST
    @Path("/leaveChatroom")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse leaveChatroom(@HeaderParam("sessionID") String sessionID, LeaveChatroomParam param) throws FBWSException {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null && param.isValid()) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processLeaveChatroom(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : "+e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid");
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public BooleanResponse processLeaveChatroom(FBSession session, LeaveChatroomParam param) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        BooleanResponse resp = new BooleanResponse();
        resp.result = chatMgr.leaveChatroom(session.getPlayer(), param.chatroomID);
        return resp;
    }

    @POST
    @Path("/addAdministrators")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse addAdministrators(@HeaderParam("sessionID") String sessionID, AddParticipantsToChatroomParam param) throws FBWSException {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null && param.isValid()) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processAddAdministrators(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : "+e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid");
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetChatroomResponse processAddAdministrators(FBSession session, AddParticipantsToChatroomParam param) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        GetChatroomResponse resp = new GetChatroomResponse();
        resp.chatroom = chatMgr.addAdministrators(session.getPlayer(), param.chatroomID, param.players);
        return resp;
    }

    @POST
    @Path("/removeAdministrator")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse removeAdministrator(@HeaderParam("sessionID") String sessionID, RemoveParticipantFromChatroomParam param) throws FBWSException {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null && param.isValid()) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processRemoveAdministrator(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : "+e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid");
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetChatroomResponse processRemoveAdministrator(FBSession session, RemoveParticipantFromChatroomParam param) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        GetChatroomResponse resp = new GetChatroomResponse();
        resp.chatroom = chatMgr.removeAdministrator(session.getPlayer(), param.chatroomID, param.playerID);
        return resp;
    }
    
    

    @POST
    @Path("/getSuggestedParticipants")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse getSuggestedParticipants(@HeaderParam("sessionID") String sessionID){
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processGetSuggestedParticipants(session));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : "+e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid");
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public GetSuggestedParticipantsResponse processGetSuggestedParticipants(FBSession session) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID());
        }
        GetSuggestedParticipantsResponse resp = new GetSuggestedParticipantsResponse();
        resp.players = chatMgr.getSuggestedParticipants(session.getPlayer());
        return resp;
    }

    @POST
    @Path("/setAlertsForChatroom")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse setAlertsForChatroom(@HeaderParam("sessionID") String sessionID, SetAlertsForChatroomParam param) throws FBWSException {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null && param.isValid()) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                response.setData(processSetAlertsForChatroom(session, param));
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : "+e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid");
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    public BooleanResponse processSetAlertsForChatroom(FBSession session, SetAlertsForChatroomParam param) throws FBWSException {
        if (log.isDebugEnabled()) {
            log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
        }
        BooleanResponse resp = new BooleanResponse();
        resp.result = chatMgr.setAlertsForChatroom(session.getPlayer(), param.chatroomID, param.enabled);
        return resp;
    }

    // Get right manager for categoryID
    private GenericChatMgr getManager(int categoryID) {
        switch (categoryID) {
            case Constantes.TOURNAMENT_CATEGORY_PRIVATE :
                return privateTournamentChatMgr;
            default:
                return chatMgr;
        }
    }

}
