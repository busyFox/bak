package com.funbridge.server.ws.servlet;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.presence.FBSession;
import com.funbridge.server.ws.FBExceptionType;
import com.funbridge.server.ws.FBWSException;
import com.funbridge.server.ws.FBWSExceptionRest;
import com.funbridge.server.ws.FBWSResponse;
import com.funbridge.server.ws.event.Event;
import com.funbridge.server.ws.event.EventField;
import com.funbridge.server.ws.event.EventServiceRest;
import com.funbridge.server.ws.event.EventServiceRestImpl;
import com.funbridge.server.ws.game.GameServiceRest;
import com.funbridge.server.ws.game.GameServiceRestImpl;
import com.funbridge.server.ws.message.*;
import com.funbridge.server.ws.player.PlayerServiceRest;
import com.funbridge.server.ws.player.PlayerServiceRestImpl;
import com.funbridge.server.ws.result.ResultServiceRest;
import com.funbridge.server.ws.result.ResultServiceRestImpl;
import com.funbridge.server.ws.tournament.TournamentServiceRest;
import com.funbridge.server.ws.tournament.TournamentServiceRestImpl;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by luke on 22/08/2017.
 */
@Component(value="clientWebSocketEndpoint")
@ServerEndpoint(value = "/wsevent")
public class ClientWebSocketEndpoint {

    private Session session;
    private FBSession fbSession = null;
    private Logger logger = ContextManager.getPresenceMgr().getLogger();
    private Object objSynchroWebSocket = new Object();
    private String description, sessionID;
    private long tsLastActivity = 0;
    private GameServiceRestImpl gameServiceRest = ContextManager.getGameService();
    private TournamentServiceRestImpl tournamentServiceRest = ContextManager.getTournamentService();
    private ResultServiceRestImpl resultServiceRest = ContextManager.getResultService();
    private EventServiceRestImpl eventServiceRest = ContextManager.getEventService();
    private MessageService messageService = ContextManager.getMessageService();
    private PlayerServiceRestImpl playerService = ContextManager.getPlayerService();

    public String getDescription() {
        return description;
    }

    public String getSessionID() {
        return sessionID;
    }

    public long getTsLastActivity() {
        return tsLastActivity;
    }

    public void resetSession() {
        fbSession = null;
        try {
            synchronized (objSynchroWebSocket) {
                if (session != null) {
                    session.close();
                }
            }
        } catch (IOException e) {
            logger.error("Failed to close session", e);
        }
    }

    @OnOpen
    public void onOpen(Session session) throws IOException {
        this.session = session;

        String paramSessionID = session.getRequestParameterMap().get("sessionID").get(0);
        if (paramSessionID == null || paramSessionID.length() == 0) {
            logger.warn("Param sessionID is null !");
        } else {
            tsLastActivity = System.currentTimeMillis();
            FBSession fbs = (FBSession) ContextManager.getPresenceMgr().getSession(paramSessionID);
            if (fbs != null) {
                fbSession = fbs;
                this.sessionID = fbSession.getID();
                fbSession.setWebSocket(this);
                description = "nickname=" + fbSession.getPlayer().getNickname() + " - deviceType=" + fbSession.getDeviceType() + " - device=" + fbSession.getDevice();
                if (logger.isInfoEnabled()) {
                    logger.info("WebSocket - init for session=" + session);
                }

                ContextManager.getClientWebSocketMgr().addWebSocketForSession(this, paramSessionID);

                // send empty event
                Event evt = new Event();
                if (fbSession.getListEvent() != null && !fbSession.getListEvent().isEmpty()) {
                    evt.timestamp = fbSession.getListEvent().get(0).timestamp - 1;
                }
                pushEvent(evt);

                // send all current events in session
                pushEvents(fbSession.getListEvent());
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("No session found with ID=" + paramSessionID);
                    logger.error("WebSocket - init session is null !");
                }
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "No session found with ID=" + paramSessionID));
            }
        }
    }

    @OnMessage
    public void onMessage(String message) throws IOException {
        if (fbSession == null) {
            if (FBConfiguration.getInstance().getIntValue("general.WSLogOnMessageSessionNull", 0) == 1) {
                logger.error("Session is null - description="+description+" - message="+message);
            }
            return;
        }
        tsLastActivity = System.currentTimeMillis();

        fbSession.setDateLastActivity(System.currentTimeMillis());
        if (message != null && message.length() > 0) {
            if (message.equals("GOTO")) {
                synchronized (objSynchroWebSocket) {
                    if (session != null && session.isOpen()) {
                        session.getBasicRemote().sendText(message);
                    }
                }
            }
            else {
                if (message.contains("\"lastTS\":")) {
                    try {
                        ClientWebSocketMsg eventWsMsg = ContextManager.getJSONTools().mapData(message, ClientWebSocketMsg.class);
                        if (eventWsMsg.lastTS > 0) {
                            if (fbSession != null) {
                                fbSession.removeEvents(eventWsMsg.lastTS);
                            } else {
                                logger.error("WebSocket onMessage - Session is null in the websocket !!");
                            }
                        }
                    } catch (JsonGenerationException e) {
                        logger.error("WebSocket onMessage - JsonGenerationException - message=" + message + " - e=" + e.getMessage() + " - session=" + fbSession, e);
                    } catch (JsonMappingException e) {
                        logger.error("WebSocket onMessage - JsonMappingException - message=" + message + " - e=" + e.getMessage() + " - session=" + fbSession, e);
                    } catch (IOException e) {
                        logger.error("WebSocket onMessage - IOException - message=" + message + " - e=" + e.getMessage() + " - session=" + fbSession, e);
                    }
                }
                else {
                    FBWSResponse response = new FBWSResponse();
                    String commandID = "";
                    // parse command
                    try {
                        ClientWebSocketCommand commandWsMsg = ContextManager.getJSONTools().mapData(message, ClientWebSocketCommand.class);
                        commandID = commandWsMsg.id;
                        /** SERVICE GAME */
                        if (commandWsMsg.service.equals("game")) {
                            if (commandWsMsg.command.equals("playBid")) {
                                response.setData(gameServiceRest.processPlayBid(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, GameServiceRest.PlayBidParam.class)));
                            } else if (commandWsMsg.command.equals("playCard")) {
                                response.setData(gameServiceRest.processPlayCard(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, GameServiceRest.PlayCardParam.class)));
                            } else if (commandWsMsg.command.equals("leaveGame")) {
                                response.setData(gameServiceRest.processLeaveGame(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, GameServiceRest.LeaveGameParam.class)));
                            } else if (commandWsMsg.command.equals("resetGame")) {
                                response.setData(gameServiceRest.processResetGame(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, GameServiceRest.ResetGameParam.class)));
                            } else if (commandWsMsg.command.equals("viewGame")) {
                                response.setData(gameServiceRest.processViewGame(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, GameServiceRest.ViewGameParam.class)));
                            } else if (commandWsMsg.command.equals("viewGameForDealScoreAndContract")) {
                                response.setData(gameServiceRest.processViewGameForDealScoreAndContract(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, GameServiceRest.ViewGameForDealScoreAndContractParam.class)));
                            } else if (commandWsMsg.command.equals("claim")) {
                                response.setData(gameServiceRest.processClaim(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, GameServiceRest.ClaimParam.class)));
                            } else if (commandWsMsg.command.equals("startGame")) {
                                response.setData(gameServiceRest.processStartGame(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, GameServiceRest.StartGameParam.class)));
                            } else if (commandWsMsg.command.equals("replay")) {
                                response.setData(gameServiceRest.processReplay(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, GameServiceRest.ReplayParam.class)));
                            } else if (commandWsMsg.command.equals("getArgineAdvice")) {
                                response.setData(gameServiceRest.processGetArgineAdvice(fbSession));
                            } else if (commandWsMsg.command.equals("setClaimSpreadResponse")) {
                                response.setData(gameServiceRest.processSetClaimSpreadResponse(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, GameServiceRest.SetClaimSpreadResponseParam.class)));
                            } else if (commandWsMsg.command.equals("getPlayBidInformation")) {
                                response.setData(gameServiceRest.processGetPlayBidInformation(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, GameServiceRest.GetPlayBidInformationParam.class)));
                            } else if (commandWsMsg.command.equals("getBidInformation")) {
                                response.setData(gameServiceRest.processGetBidInformation(ContextManager.getJSONTools().mapData(commandWsMsg.parameters, GameServiceRest.GetBidInformationParam.class)));
                            } else {
                                logger.error("RPC Command not implemented on service game : "+commandWsMsg.command);
                                throw new FBWSException(FBExceptionType.COMMON_NOT_IMPLEMENTED);
                            }
                        }
                        /** SERVICE EVENT */
                        else if (commandWsMsg.service.equals("event")) {
                            if (commandWsMsg.command.equals("getEvents")) {
                                response.setData(eventServiceRest.processGetEvents(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, EventServiceRest.GetEventsParam.class)));
                            } else if (commandWsMsg.command.equals("getMessages")) {
                                response.setData(eventServiceRest.processGetMessages(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, EventServiceRest.GetMessagesParam.class)));
                            } else if (commandWsMsg.command.equals("getMessagesCount")) {
                                response.setData(eventServiceRest.processGetMessagesCount(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, EventServiceRest.GetMessagesCountParam.class)));
                            } else if (commandWsMsg.command.equals("setMessageRead")) {
                                response.setData(eventServiceRest.processSetMessageRead(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, EventServiceRest.SetMessageReadParam.class)));
                            } else {
                                logger.error("RPC Command not implemented on service event : "+commandWsMsg.command);
                                throw new FBWSException(FBExceptionType.COMMON_NOT_IMPLEMENTED);
                            }
                        }
                        /** SERVICE MESSAGE */
                        else if (commandWsMsg.service.equals("message")) {
                            if (commandWsMsg.command.equals("getChatroomsForPlayer")) {
                                response.setData(messageService.processGetChatroomsForPlayer(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, GetChatroomsForPlayerParam.class)));
                            } else if (commandWsMsg.command.equals("getChatroom")) {
                                response.setData(messageService.processGetChatroom(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, GetChatroomParam.class)));
                            } else if (commandWsMsg.command.equals("getChatroomWithPlayer")) {
                                response.setData(messageService.processGetChatroomWithPlayer(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, GetChatroomWithPlayerParam.class)));
                            } else if (commandWsMsg.command.equals("sendMessageToPlayer")) {
                                response.setData(messageService.processSendMessageToPlayer(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, SendMessageToPlayerParam.class)));
                            } else if (commandWsMsg.command.equals("createGroupChatroom")) {
                                response.setData(messageService.processCreateGroupChatroom(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, CreateGroupChatroomParam.class)));
                            } else if (commandWsMsg.command.equals("addParticipantsToChatroom")) {
                                response.setData(messageService.processAddParticipantsToChatroom(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, AddParticipantsToChatroomParam.class)));
                            } else if (commandWsMsg.command.equals("removeParticipantFromChatroom")) {
                                response.setData(messageService.processRemoveParticipantFromChatroom(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, RemoveParticipantFromChatroomParam.class)));
                            } else if (commandWsMsg.command.equals("addAdministrators")) {
                                response.setData(messageService.processAddAdministrators(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, AddParticipantsToChatroomParam.class)));
                            } else if (commandWsMsg.command.equals("removeAdministrator")) {
                                response.setData(messageService.processRemoveAdministrator(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, RemoveParticipantFromChatroomParam.class)));
                            } else if (commandWsMsg.command.equals("leaveChatroom")) {
                                response.setData(messageService.processLeaveChatroom(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, LeaveChatroomParam.class)));
                            } else if (commandWsMsg.command.equals("deleteMessage")) {
                                response.setData(messageService.processDeleteMessage(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, DeleteMessageParam.class)));
                            } else if (commandWsMsg.command.equals("getMessagesForChatroom")) {
                                response.setData(messageService.processGetMessagesForChatroom(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, GetMessagesParam.class)));
                            } else if (commandWsMsg.command.equals("sendMessageToChatroom")) {
                                response.setData(messageService.processSendMessageToChatroom(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, SendMessageToChatroomParam.class)));
                            } else if (commandWsMsg.command.equals("setMessageRead")) {
                                response.setData(messageService.processSetMessageRead(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, SetMessageReadParam.class)));
                            } else if (commandWsMsg.command.equals("setChatroomName")) {
                                response.setData(messageService.processSetChatroomName(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, SetChatroomNameParam.class)));
                            } else if (commandWsMsg.command.equals("setChatroomImage")) {
                                response.setData(messageService.processSetChatroomImage(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, SetChatroomImageParam.class)));
                            } else if (commandWsMsg.command.equals("resetChatroomHistory")) {
                                response.setData(messageService.processResetChatroomHistory(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, ChatroomIDParam.class)));
                            } else if (commandWsMsg.command.equals("getChatroomsForTournament")) {
                                response.setData(messageService.processGetChatroomsForTournament(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, TournamentIDParam.class)));
                            } else if (commandWsMsg.command.equals("moderateMessage")) {
                                response.setData(messageService.processModerateMessage(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, ModerateMessageParam.class)));
                            } else if (commandWsMsg.command.equals("getSuggestedParticipants")) {
                                response.setData(messageService.processGetSuggestedParticipants(fbSession));
                            } else if (commandWsMsg.command.equals("setAlertsForChatroom")) {
                                response.setData(messageService.processSetAlertsForChatroom(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, SetAlertsForChatroomParam.class)));
                            }
                            else {
                                logger.error("RPC Command not implemented on service message : "+commandWsMsg.command);
                                throw new FBWSException(FBExceptionType.COMMON_NOT_IMPLEMENTED);
                            }
                        }
                        /** SERVICE RESULT */
                        else if (commandWsMsg.service.equals("result")) {
                            if (commandWsMsg.command.equals("getSerieSummary2")) {
                                response.setData(resultServiceRest.processGetSerieSummary(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, ResultServiceRest.GetSerieSummaryParam.class)));
                            } else if (commandWsMsg.command.equals("getResultSummaryForReplayDeal")) {
                                response.setData(resultServiceRest.processGetResultSummaryForReplayDeal(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, ResultServiceRest.GetResultSummaryForReplayDealParam.class)));
                            } else if (commandWsMsg.command.equals("getResultForDeal")) {
                                response.setData(resultServiceRest.processGetResultForDeal(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, ResultServiceRest.GetResultForDealParam.class)));
                            } else if (commandWsMsg.command.equals("resetLastResultForPlayer")) {
                                response.setData(resultServiceRest.processResetLastResultForPlayer(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, ResultServiceRest.ResetLastResultForPlayerParam.class)));
                            } else if (commandWsMsg.command.equals("getTournamentArchives")) {
                                response.setData(resultServiceRest.processGetTournamentArchives(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, ResultServiceRest.GetTournamentArchivesParam.class)));
                            } else if (commandWsMsg.command.equals("getResultTournamentArchiveForCategory")) {
                                response.setData(resultServiceRest.processGetResultTournamentArchiveForCategory(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, ResultServiceRest.GetResultTournamentArchiveForCategoryParam.class)));
                            } else if (commandWsMsg.command.equals("getResultDealForTournament")) {
                                response.setData(resultServiceRest.processGetResultDealForTournament(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, ResultServiceRest.GetResultDealForTournamentParam.class)));
                            } else if (commandWsMsg.command.equals("getResultForTournament")) {
                                response.setData(resultServiceRest.processGetResultForTournament(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, ResultServiceRest.GetResultForTournamentParam.class)));
                            } else if (commandWsMsg.command.equals("getRankingSerie2")) {
                                response.setData(resultServiceRest.processGetRankingSerie(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, ResultServiceRest.GetRankingSerie2Param.class)));
                            } else if (commandWsMsg.command.equals("getPreviousRankingSerie")) {
                                response.setData(resultServiceRest.processGetPreviousRankingSerie(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, ResultServiceRest.GetPreviousRankingSerieParam.class)));
                            } else if (commandWsMsg.command.equals("getTrainingSummary2")) {
                                response.setData(resultServiceRest.processGetTrainingSummary2(fbSession));
                            } else if (commandWsMsg.command.equals("getTrainingSummary")) {
                                response.setData(resultServiceRest.processGetTrainingSummary(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, ResultServiceRest.GetTrainingSummaryParam.class)));
                            } else if (commandWsMsg.command.equals("getDealResultSummary")) {
                                response.setData(resultServiceRest.processGetDealResultSummary(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, ResultServiceRest.GetDealResultSummaryParam.class)));
                            } else if (commandWsMsg.command.equals("getSerieTopChallengeSummary")) {
                                response.setData(resultServiceRest.processGetSerieTopChallengeSummary(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, ResultServiceRest.GetSerieTopChallengeSummaryParam.class)));
                            } else if (commandWsMsg.command.equals("getSerieEasyChallengeSummary")) {
                                response.setData(resultServiceRest.processGetSerieEasyChallengeSummary(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, ResultServiceRest.GetSerieEasyChallengeSummaryParam.class)));
                            } else if (commandWsMsg.command.equals("getMainRanking")) {
                                response.setData(resultServiceRest.processGetMainRanking(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, ResultServiceRest.GetMainRankingParam.class)));
                            } else if (commandWsMsg.command.equals("getDuelBestScoreEver")) {
                                response.setData(resultServiceRest.processGetDuelBestScoreEver(ContextManager.getJSONTools().mapData(commandWsMsg.parameters, ResultServiceRest.GetDuelBestScoreParam.class)));
                            } else if (commandWsMsg.command.equals("getDuelBestScoreMonthly")) {
                                response.setData(resultServiceRest.processGetDuelBestScoreMonthly(ContextManager.getJSONTools().mapData(commandWsMsg.parameters, ResultServiceRest.GetDuelBestScoreMonthlyParam.class)));
                            } else if (commandWsMsg.command.equals("getDuelBestScoreWeekly")) {
                                response.setData(resultServiceRest.processGetDuelBestScoreWeekly(ContextManager.getJSONTools().mapData(commandWsMsg.parameters, ResultServiceRest.GetDuelBestScoreWeeklyParam.class)));
                            }
                            else {
                                logger.error("RPC Command not implemented on service result : "+commandWsMsg.command);
                                throw new FBWSException(FBExceptionType.COMMON_NOT_IMPLEMENTED);
                            }
                        }
                        /** SERVICE PLAYER */
                        else if (commandWsMsg.service.equals("player")) {
                            if (commandWsMsg.command.equals("getPlayerInfo")) {
                                response.setData(playerService.processGetPlayerInfo(fbSession));
                            } else if (commandWsMsg.command.equals("setPlayerInfoSettings")) {
                                response.setData(playerService.processSetPlayerInfoSettings(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, PlayerServiceRest.SetPlayerInfoSettingsParam.class)));
                            } else if (commandWsMsg.command.equals("getPlayerProfile")) {
                                response.setData(playerService.processGetPlayerProfile(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, PlayerServiceRest.GetPlayerProfileParam.class)));
                            } else if (commandWsMsg.command.equals("setPlayerProfile")) {
                                response.setData(playerService.processSetPlayerProfile(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, PlayerServiceRest.SetPlayerProfileParam.class)));
                            } else if (commandWsMsg.command.equals("changePlayerMail")) {
                                response.setData(playerService.processChangePlayerMail(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, PlayerServiceRest.ChangePlayerMailParam.class)));
                            } else if (commandWsMsg.command.equals("changePlayerPassword2")) {
                                response.setData(playerService.processChangePlayerPassword2(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, PlayerServiceRest.ChangePlayerPasswordParam.class)));
                            } else if (commandWsMsg.command.equals("setPlayerAvatar")) {
                                response.setData(playerService.processSetPlayerAvatar(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, PlayerServiceRest.SetPlayerAvatarParam.class)));
                            } else if (commandWsMsg.command.equals("getPlayerAvatar")) {
                                response.setData(playerService.processGetPlayerAvatar(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, PlayerServiceRest.GetPlayerAvatarParam.class)));
                            } else if (commandWsMsg.command.equals("getContextInfo")) {
                                response.setData(playerService.processGetContextInfo(fbSession));
                            } else if (commandWsMsg.command.equals("getPlayerLinked")) {
                                response.setData(playerService.processGetPlayerLinked(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, PlayerServiceRest.GetPlayerLinkedParam.class)));
                            } else if (commandWsMsg.command.equals("searchPlayer")) {
                                response.setData(playerService.processSearchPlayer(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, PlayerServiceRest.SearchPlayerParam.class)));
                            } else if (commandWsMsg.command.equals("setLink")) {
                                response.setData(playerService.processSetLink(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, PlayerServiceRest.SetLinkParam.class)));
                            } else if (commandWsMsg.command.equals("getPlayer")) {
                                response.setData(playerService.processGetPlayer(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, PlayerServiceRest.GetPlayerParam.class)));
                            } else if (commandWsMsg.command.equals("getProfile")) {
                                response.setData(playerService.processGetProfile(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, PlayerServiceRest.GetProfileParam.class)));
                            } else if (commandWsMsg.command.equals("getLinkedPlayers")) {
                                response.setData(playerService.processGetLinkedPlayers(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, PlayerServiceRest.GetLinkedPlayersParam.class)));
                            } else if (commandWsMsg.command.equals("getLinkedPlayersLight")) {
                                response.setData(playerService.processGetLinkedPlayersLight(fbSession));
                            } else if (commandWsMsg.command.equals("setLocation")) {
                                playerService.processSetLocation(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, PlayerServiceRest.SetLocationParam.class));
                            } else if (commandWsMsg.command.equals("checkEmails")) {
                                response.setData(playerService.processCheckEmails(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, PlayerServiceRest.CheckEmailsParam.class)));
                            } else if (commandWsMsg.command.equals("getSuggestedPlayers")) {
                                response.setData(playerService.processGetSuggestedPlayers(fbSession));
                            } else if (commandWsMsg.command.equals("changePlayerPseudo")) {
                                response.setData(playerService.processChangePlayerPseudo(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, PlayerServiceRest.ChangePlayerPseudoParam.class)));
                            } else if (commandWsMsg.command.equals("getListCountryPlayer")) {
                                response.setData(playerService.processGetListCountryPlayer(fbSession));
                            } else if (commandWsMsg.command.equals("getFriendsSerieStatus")) {
                                response.setData(playerService.processGetFriendsSerieStatus(fbSession));
                            } else if (commandWsMsg.command.equals("getConnectedPlayers")) {
                                response.setData(playerService.processGetConnectedPlayers(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, PlayerServiceRest.GetConnectedPlayersParam.class)));
                            } else if (commandWsMsg.command.equals("getPlayerDuels")) {
                                response.setData(playerService.processGetPlayerDuels(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, PlayerServiceRest.GetPlayerDuelsParam.class)));
                            } else if (commandWsMsg.command.equals("setApplicationStats")) {
                                response.setData(playerService.processSetApplicationStats(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, PlayerServiceRest.SetApplicationStatsParam.class)));
                            }
                            else {
                                logger.error("RPC Command not implemented on service player : "+commandWsMsg.command);
                                throw new FBWSException(FBExceptionType.COMMON_NOT_IMPLEMENTED);
                            }
                        }
                        /** SERVICE TOURNAMENT */
                        else if (commandWsMsg.service.equals("tournament")) {
                            if (commandWsMsg.command.equals("leaveTournament")) {
                                response.setData(tournamentServiceRest.processLeaveTournament(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, TournamentServiceRest.LeaveTournamentParam.class)));
                            } else if (commandWsMsg.command.equals("createChallenge")) {
                                response.setData(tournamentServiceRest.processCreateChallenge(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, TournamentServiceRest.CreateChallengeParam.class)));
                            } else if (commandWsMsg.command.equals("setChallengeResponse")) {
                                response.setData(tournamentServiceRest.processSetChallengeResponse(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, TournamentServiceRest.SetChallengeResponseParam.class)));
                            } else if (commandWsMsg.command.equals("playTournamentChallenge")) {
                                response.setData(tournamentServiceRest.processPlayTournamentChallenge(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, TournamentServiceRest.PlayTournamentChallengeParam.class)));
                            } else if (commandWsMsg.command.equals("getTrainingPartners")) {
                                response.setData(tournamentServiceRest.processGetTrainingPartners(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, TournamentServiceRest.GetTrainingPartnersParam.class)));
                            } else if (commandWsMsg.command.equals("requestDuel")) {
                                response.setData(tournamentServiceRest.processRequestDuel(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, TournamentServiceRest.RequestDuelParam.class)));
                            } else if (commandWsMsg.command.equals("answerDuelRequest")) {
                                response.setData(tournamentServiceRest.processAnswerDuelRequest(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, TournamentServiceRest.AnswerDuelRequestParam.class)));
                            } else if (commandWsMsg.command.equals("getDuelHistory")) {
                                response.setData(tournamentServiceRest.processGetDuelHistory(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, TournamentServiceRest.GetDuelHistoryParam.class)));
                            } else if (commandWsMsg.command.equals("getDuel")) {
                                response.setData(tournamentServiceRest.processGetDuel(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, TournamentServiceRest.GetDuelParam.class)));
                            } else if (commandWsMsg.command.equals("getDuels")) {
                                response.setData(tournamentServiceRest.processGetDuels(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, TournamentServiceRest.GetDuelsParam.class)));
                            } else if (commandWsMsg.command.equals("playTournamentTraining")) {
                                response.setData(tournamentServiceRest.processPlayTournamentTraining(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, TournamentServiceRest.PlayTournamentTrainingParam.class)));
                            } else if (commandWsMsg.command.equals("playTournamentSerie2")) {
                                response.setData(tournamentServiceRest.processPlayTournamentSerie2(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, TournamentServiceRest.PlayTournamentSerieParam.class)));
                            } else if (commandWsMsg.command.equals("playTournamentTrainingPartner")) {
                                response.setData(tournamentServiceRest.processPlayTournamentTrainingPartner(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, TournamentServiceRest.PlayTournamentTrainingPartnerParam.class)));
                            } else if (commandWsMsg.command.equals("getTimezoneTournaments")) {
                                response.setData(tournamentServiceRest.processGetTimezoneTournaments(fbSession));
                            } else if (commandWsMsg.command.equals("playTournamentTimezone")) {
                                response.setData(tournamentServiceRest.processPlayTournamentTimezone(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, TournamentServiceRest.PlayTournamentTimezoneParam.class)));
                            } else if (commandWsMsg.command.equals("setMatchMakingEnabled")) {
                                response.setData(tournamentServiceRest.processSetMatchMakingEnabled(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, TournamentServiceRest.SetMatchMakingEnabledParam.class)));
                            } else if (commandWsMsg.command.equals("removePlayerFromDuelList")) {
                                response.setData(tournamentServiceRest.processRemovePlayerFromDuelList(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, TournamentServiceRest.RemovePlayerFromDuelListParam.class)));
                            } else if (commandWsMsg.command.equals("playTournamentDuel")) {
                                response.setData(tournamentServiceRest.processPlayTournamentDuel(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, TournamentServiceRest.PlayTournamentDuelParam.class)));
                            } else if (commandWsMsg.command.equals("getTournamentBadges")) {
                                response.setData(tournamentServiceRest.processGetTournamentBadges(fbSession));
                            } else if (commandWsMsg.command.equals("getTournamentInProgress")) {
                                response.setData(tournamentServiceRest.processGetTournamentInProgress(fbSession));
                            } else if (commandWsMsg.command.equals("playSerieTopChallengeTournament")) {
                                response.setData(tournamentServiceRest.processPlaySerieTopChallengeTournament(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, TournamentServiceRest.PlaySerieTopChallengeTournamentParam.class)));
                            } else if (commandWsMsg.command.equals("playSerieEasyChallengeTournament")) {
                                response.setData(tournamentServiceRest.processPlaySerieEasyChallengeTournament(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, TournamentServiceRest.PlaySerieEasyChallengeTournamentParam.class)));
                            } else if (commandWsMsg.command.equals("getFederationSummary")) {
                                response.setData(tournamentServiceRest.processGetFederationSummary(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, TournamentServiceRest.GetFederationSummaryParam.class)));
                            } else if (commandWsMsg.command.equals("registerTournamentFederation")) {
                                response.setData(tournamentServiceRest.processRegisterTournamentFederation(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, TournamentServiceRest.RegisterTournamentFederationParam.class)));
                            } else if (commandWsMsg.command.equals("playTournamentFederation")) {
                                response.setData(tournamentServiceRest.processPlayTournamentFederation(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, TournamentServiceRest.PlayTournamentFederationParam.class)));
                            } else if (commandWsMsg.command.equals("getPrivateTournamentSummary")) {
                                response.setData(tournamentServiceRest.processGetPrivateTournamentSummary(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, TournamentServiceRest.GetPrivateTournamentSummaryParam.class)));
                            } else if (commandWsMsg.command.equals("getPrivateTournament")) {
                                response.setData(tournamentServiceRest.processGetPrivateTournament(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, TournamentServiceRest.GetPrivateTournamentParam.class)));
                            } else if (commandWsMsg.command.equals("createPrivateTournamentProperties")) {
                                response.setData(tournamentServiceRest.processCreatePrivateTournamentProperties(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, TournamentServiceRest.CreatePrivateTournamentPropertiesParam.class)));
                            } else if (commandWsMsg.command.equals("removePrivateTournamentProperties")) {
                                response.setData(tournamentServiceRest.processRemovePrivateTournamentProperties(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, TournamentServiceRest.RemovePrivateTournamentPropertiesParam.class)));
                            } else if (commandWsMsg.command.equals("changePasswordPrivateTournament")) {
                                response.setData(tournamentServiceRest.processChangePasswordPrivateTournament(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, TournamentServiceRest.ChangePasswordPrivateTournamentParam.class)));
                            } else if (commandWsMsg.command.equals("setPrivateTournamentFavorite")) {
                                response.setData(tournamentServiceRest.processSetPrivateTournamentFavorite(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, TournamentServiceRest.SetPrivateTournamentFavoriteParam.class)));
                            } else if (commandWsMsg.command.equals("listPrivateTournaments")) {
                                response.setData(tournamentServiceRest.processListPrivateTournaments(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, TournamentServiceRest.ListPrivateTournamentsParam.class)));
                            } else if (commandWsMsg.command.equals("getPlayerPrivateTournamentProperties")) {
                                response.setData(tournamentServiceRest.processGetPlayerPrivateTournamentProperties(fbSession));
                            } else if (commandWsMsg.command.equals("playPrivateTournament")) {
                                response.setData(tournamentServiceRest.processPlayPrivateTournament(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, TournamentServiceRest.PlayPrivateTournamentParam.class)));
                            } else if (commandWsMsg.command.equals("getLearningDealCommented")) {
                                response.setData(tournamentServiceRest.processGetLearningDealCommented(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, TournamentServiceRest.GetLearningDealCommentedParam.class)));
                            } else if (commandWsMsg.command.equals("playLearningTournament")) {
                                response.setData(tournamentServiceRest.processPlayLearningTournament(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, TournamentServiceRest.PlayLearningTournamentParam.class)));
                            } else if (commandWsMsg.command.equals("setLearningProgression")) {
                                response.setData(tournamentServiceRest.processSetLearningProgression(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, TournamentServiceRest.SetLearningProgressionParam.class)));
                            } else if (commandWsMsg.command.equals("getLearningProgression")) {
                                response.setData(tournamentServiceRest.processGetLearningProgression(fbSession));
                            } else if (commandWsMsg.command.equals("getGameByCategoryAndID")) {
                                response.setData(tournamentServiceRest.processGetGameByCategoryAndID(fbSession, ContextManager.getJSONTools().mapData(commandWsMsg.parameters, TournamentServiceRest.GetGameByCategoryAndIDParam.class)));
                            } else {
                                logger.error("RPC Command not implemented on service tournament : "+commandWsMsg.command);
                                throw new FBWSException(FBExceptionType.COMMON_NOT_IMPLEMENTED);
                            }
                        }
                        else {
                            logger.error("RPC Service not implemented : "+commandWsMsg.service);
                            throw new FBWSException(FBExceptionType.COMMON_NOT_IMPLEMENTED);
                        }
                    } catch (FBWSException e1) {
                        logger.error("FBWSException : "+e1.getMessage()+" - session="+fbSession+" - message="+message, e1);
                        response.setException(new FBWSExceptionRest(e1.getType(), e1.localizedMessage));
                    } catch (Exception e2) {
                        logger.error("Exception : "+e2.getMessage(), e2);
                        response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
                    }
                    // send response
                    String data = ContextManager.getJSONTools().transform2String(response, false);
                    Event evtRpc = new Event();
                    evtRpc.timestamp = fbSession.getTimeStamp();
                    evtRpc.senderID = Constantes.EVENT_PLAYER_ID_SERVER;
                    evtRpc.receiverID = fbSession.getPlayer().getID();
                    evtRpc.addFieldCategory(Constantes.EVENT_CATEGORY_RPC);
                    evtRpc.addField(new EventField("id", commandID, data));
                    pushEvent(evtRpc);
                }
            }
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason status) throws IOException {
        tsLastActivity = System.currentTimeMillis();
        if (fbSession != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("WebSocket - onClose for session="+fbSession+" - status="+status);
            }
            fbSession.removeWebSocket(this);
        }
        if (sessionID != null) {
            ContextManager.getClientWebSocketMgr().removeWebSocketForSession(sessionID, false);
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        if (FBConfiguration.getInstance().getIntValue("general.websocketLogOnError", 0) == 1) {
            logger.error("WebSocket - Error - e=" + throwable.getMessage() + " - session=" + fbSession, throwable);
        }
    }

    /**
     * Push an event to the websocket
     * @param evt
     * @return true if write to websocket success
     */
    public boolean pushEvent(Event evt) {
        if (evt != null) {
            List<Event> list = new ArrayList<Event>();
            list.add(evt);
            return pushEvents(list) == 1;
        }
        return false;
    }

    /**
     * Push events list to the websocket
     * @param listEvent
     * @return the number of event writen to websocket success. -1 if error
     */
    public int pushEvents(List<Event> listEvent) {
        if (listEvent != null && listEvent.size() > 0) {
            synchronized (objSynchroWebSocket) {
                // session is null if not connected
                if (session != null && session.isOpen()) {
                    try {
                        int nbEvent = listEvent.size();
                        String data = ContextManager.getJSONTools().transform2String(listEvent, false);
                        if (data != null && data.length() > 0 && this.session != null) {
                            this.session.getBasicRemote().sendText(data);
                            if (logger.isDebugEnabled()) {
                                logger.debug("WebSocket - NbEvent sended = "+listEvent.size()+" - session="+fbSession+" - data="+data);
                            }
                            return nbEvent;
                        }
                    } catch (JsonGenerationException e) {
                        logger.error("WebSocket pushEvents - JsonGenerationException - e="+e.getMessage()+" - session="+fbSession, e);
                    } catch (JsonMappingException e) {
                        logger.error("WebSocket pushEvents - JsonMappingException - e="+e.getMessage()+" - session="+fbSession, e);
                    } catch (IOException e) {
                        logger.error("WebSocket pushEvents - IOException - e="+e.getMessage()+" - session="+fbSession, e);
                    } catch (Exception e) {
                        logger.error("WebSocket pushEvents - Exception - e="+e.getMessage()+" - session="+fbSession, e);
                    }
                }
            }
            return -1;
        }
        return 0;
    }

    public boolean sendResponse(FBWSResponse response) {
        if (response != null) {
            synchronized (objSynchroWebSocket) {
                // session is null if not connected
                if (session != null && session.isOpen()) {
                    try {
                        String data = ContextManager.getJSONTools().transform2String(response, false);
                        if (data != null && data.length() > 0 && this.session != null) {
                            this.session.getBasicRemote().sendText(data);
                            if (logger.isDebugEnabled()) {
                                logger.debug("WebSocket - sendResponse = " + data + " - session=" + fbSession);
                            }
                            return true;
                        }
                    } catch (JsonGenerationException e) {
                        logger.error("WebSocket pushEvents - JsonGenerationException - e="+e.getMessage()+" - session="+fbSession, e);
                    } catch (JsonMappingException e) {
                        logger.error("WebSocket pushEvents - JsonMappingException - e="+e.getMessage()+" - session="+fbSession, e);
                    } catch (IOException e) {
                        logger.error("WebSocket pushEvents - IOException - e="+e.getMessage()+" - session="+fbSession, e);
                    } catch (Exception e) {
                        logger.error("WebSocket pushEvents - Exception - e="+e.getMessage()+" - session="+fbSession, e);
                    }
                }
            }
        }
        return false;
    }
}
