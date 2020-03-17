package com.funbridge.server.tournament.privatetournament;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.message.GenericChatMgr;
import com.funbridge.server.message.data.GenericChatroom;
import com.funbridge.server.message.data.GenericMessage;
import com.funbridge.server.tournament.privatetournament.data.PrivateDeal;
import com.funbridge.server.tournament.privatetournament.data.PrivateTournament;
import com.funbridge.server.tournament.privatetournament.data.PrivateTournamentChatroom;
import com.funbridge.server.tournament.privatetournament.data.PrivateTournamentMessage;
import com.funbridge.server.ws.FBExceptionType;
import com.funbridge.server.ws.FBWSException;
import com.funbridge.server.ws.message.WSChatMessage;
import com.funbridge.server.ws.message.WSChatroom;
import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bplays on 21/02/17.
 */
@Component(value="privateTournamentChatMgr")
@Scope(value="singleton")
public class PrivateTournamentChatMgr extends GenericChatMgr{

    @Resource(name="mongoPrivateTournamentTemplate")
    private MongoTemplate mongoTemplate;
    @Resource(name="privateTournamentMgr")
    private PrivateTournamentMgr privateTournamentMgr;

    @Override
    public void init() {
        super.init();
        notifyParticipantsOnNewMessage = false;
    }

    @Override
    public MongoTemplate getMongoTemplate() {
        return mongoTemplate;
    }

    @Override
    public Class<? extends GenericChatroom> getChatroomEntity() {
        return PrivateTournamentChatroom.class;
    }

    @Override
    public Class<? extends GenericMessage> getMessageEntity() {
        return PrivateTournamentMessage.class;
    }

    @Override
    public String getChatroomCollectionName() {
        return "private_chatroom";
    }

    @Override
    public String getMessageCollectionName() {
        return "private_message";
    }

    public PrivateTournamentMgr getPrivateTournamentMgr(){ return privateTournamentMgr; }

    /**
     * Return chatrooms from the tournament (general chatroom + one chatroom for each deal)
     * @param tournamentID
     * @return
     */
    public List<WSChatroom> getChatroomsForTournament(String tournamentID, long askerID) {
        List<WSChatroom> listChatrooms = new ArrayList<>();
        PrivateTournament tournament = (PrivateTournament) privateTournamentMgr.getTournament(tournamentID);
        if(tournament != null){
            // Add deals chatrooms
            for(PrivateDeal deal : tournament.getDeals()){
                if(deal.getChatroomID() != null){
                    GenericChatroom chatroom = findChatroomByID(deal.getChatroomID());
                    if(chatroom != null){
                        listChatrooms.add(toWSChatroom(chatroom, chatroom.getParticipant(askerID), false, false));
                    }
                }
            }
            // Add general chatroom
            if(tournament.getChatroomID() != null){
                GenericChatroom chatroom = findChatroomByID(tournament.getChatroomID());
                if(chatroom != null){
                    listChatrooms.add(0, toWSChatroom(chatroom, chatroom.getParticipant(askerID), false, false));
                }
            }
        }
        return listChatrooms;
    }

    /**
     * Moderate a comment (only the tournament adlinistrator can do it
     * @param messageID
     * @param tournamentID
     * @param moderated
     * @param playerID
     * @return
     * @throws FBWSException
     */
    public WSChatMessage moderateMessage(String messageID, String tournamentID, boolean moderated, long playerID) throws FBWSException {
        PrivateTournament tournament = (PrivateTournament) privateTournamentMgr.getTournament(tournamentID);
        // Find tournament
        if(tournament != null){
            // Check if player is administrator
            if(tournament.getOwnerID() == playerID){
                // Get message
                GenericMessage message = findMessageByID(messageID);
                if(message != null){
                    // Moderation
                    if(!moderated){
                        message.setType(Constantes.CHAT_MESSAGE_TYPE_PLAYER);
                        message.setTemplateName(null);
                    } else {
                        message.setType(Constantes.CHAT_MESSAGE_TYPE_MODERATED);
                        message.setTemplateName("chat.moderated");
                    }
                    getMongoTemplate().save(message);
                    // Send response
                    GenericChatroom chatroom = findChatroomByID(message.getChatroomID());
                    if(chatroom != null){
                        return toWSChatMessage(message, chatroom.getParticipant(playerID));
                    } else {
                        log.error("chatroom not found ! message.chatroomID="+message.getChatroomID());
                        throw new FBWSException(FBExceptionType.MESSAGE_UNKNOWN_CHATROOM);
                    }
                } else {
                    log.error("message not found! messageID="+messageID);
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
            } else {
                log.error("player is not administrator of this tournament. tournamentID="+tournamentID+" - playerID="+playerID+" - ownerID="+tournament.getOwnerID());
                throw new FBWSException(FBExceptionType.TOURNAMENT_PRIVATE_OWNER_NOT_PLAYER);
            }
        } else {
            log.error("tournament not found! ID="+tournamentID);
            throw new FBWSException(FBExceptionType.TOURNAMENT_PRIVATE_PROPERTIES_NOT_FOUND);
        }
    }

    /**
     * Get nb of unread chat messages for a player
     * @param playerID
     * @param useJoinDateAndResetDate
     * @return
     */
    @Override
    public int getNbUnreadMessagesForPlayer(long playerID, boolean useJoinDateAndResetDate) {
        return 0;
    }

}
