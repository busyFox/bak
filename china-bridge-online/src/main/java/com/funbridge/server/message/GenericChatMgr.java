package com.funbridge.server.message;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.funbridge.server.common.*;
import com.funbridge.server.message.data.*;
import com.funbridge.server.player.PlayerMgr;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.player.cache.PlayerCacheMgr;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.player.data.PlayerLink;
import com.funbridge.server.presence.FBSession;
import com.funbridge.server.presence.PresenceMgr;
import com.funbridge.server.ws.FBExceptionType;
import com.funbridge.server.ws.FBWSException;
import com.funbridge.server.ws.event.Event;
import com.funbridge.server.ws.message.WSChatMessage;
import com.funbridge.server.ws.message.WSChatroom;
import com.funbridge.server.ws.player.WSPlayerLight;
import com.funbridge.server.ws.player.WSPlayerLinked;
import com.gotogames.common.tools.JSONTools;
import com.gotogames.common.tools.StringTools;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Created by bplays on 04/11/16.
 */
public abstract class GenericChatMgr extends FunbridgeMgr {

    @Resource(name="playerCacheMgr")
    protected PlayerCacheMgr playerCacheMgr;
    @Resource(name="mailMgr")
    protected MailMgr mailMgr;
    @Resource(name="playerMgr")
    protected PlayerMgr playerMgr;
    @Resource(name="presenceMgr")
    protected PresenceMgr presenceMgr = null;
    public static Pattern pattern;
    public boolean notifyParticipantsOnNewMessage = true;

    protected JSONTools jsontools = new JSONTools();

    @PostConstruct
    @Override
    public void init() {
        try {
            pattern = Pattern.compile("\\{(.+?)\\}");
        } catch (PatternSyntaxException e) {
            log.error("Pattern not valid !",e);
        }
    }

    @Override
    public void startUp() {

    }

    @PreDestroy
    @Override
    public void destroy() {

    }
    
    public abstract MongoTemplate getMongoTemplate();
    
    public abstract Class<? extends GenericChatroom> getChatroomEntity();

    public abstract String getChatroomCollectionName();
    
    public abstract Class<? extends GenericMessage> getMessageEntity();

    public abstract String getMessageCollectionName();

    /**
     * Find a chatroom by ID
     * @param chatroomID
     * @return
     */
    public GenericChatroom findChatroomByID(String chatroomID){
        return getMongoTemplate().findOne(Query.query(Criteria.where("ID").is(new ObjectId(chatroomID))), getChatroomEntity());
    }

    /***
     * Find the 'SINGLE' chatroom for two players (there can be only one)
     * @param player1ID
     * @param player2ID
     * @return
     */
    public GenericChatroom findSingleChatroomForPlayers(long player1ID, long player2ID){
        Query query = new Query();
        Criteria c1 = Criteria.where("participants.playerID").is(player1ID);
        Criteria c2 = Criteria.where("participants.playerID").is(player2ID);
        query.addCriteria(Criteria.where("type").is(Constantes.CHATROOM_TYPE_SINGLE).andOperator(c1, c2));
        return getMongoTemplate().findOne(query, getChatroomEntity());
    }

    /**
     * Find a message by ID
     * @param messageID
     * @return
     */
    public GenericMessage findMessageByID(String messageID){
        return getMongoTemplate().findOne(Query.query(Criteria.where("ID").is(new ObjectId(messageID))), getMessageEntity());
    }

    /**
     * Find all chatrooms for a player
     * @param playerID playerID
     * @return
     */
    public List<GenericChatroom> getChatroomsForPlayer(long playerID, String type){
        Query q = Query.query(Criteria.where("participants.playerID").is(playerID));
        if(type != null){
            q.addCriteria(Criteria.where("type").is(type));
        }
        q.with(new Sort(Sort.Direction.DESC, "updateDate"));
        return (List<GenericChatroom>) getMongoTemplate().find(q, getChatroomEntity());
    }

    /**
     * Return chatrooms where the player is involved ????????
     * @param playerID playerID
     * @param search search pattern
     * @param number nb max of chatrooms to return
     * @param offset offset
     * @return
     */
    public List<GenericChatroom> getChatroomsForPlayer(long playerID, String search, int number, int offset) {
        // Get all chatrooms for player
        List<GenericChatroom> chatrooms = getChatroomsForPlayer(playerID, null);

        // Filter
        Iterator<GenericChatroom> it = chatrooms.iterator();
        while(it.hasNext()){
            GenericChatroom chatroom = it.next();
            boolean removed = false;

            // Search
            if(search != null && !search.isEmpty()) {
                // If group chatroom search by chatroom name ?????????????
                if (chatroom.getType().equalsIgnoreCase(Constantes.CHATROOM_TYPE_GROUP) || chatroom.getType().equalsIgnoreCase(Constantes.CHATROOM_TYPE_TEAM)) {
                    if (!chatroom.getName().toUpperCase().contains(search.toUpperCase())) {
                        it.remove();
                        continue;
                    }
                // If 1to1 chatroom search by participant's nickname ???????????1to1???
                } else if (chatroom.getType().equalsIgnoreCase(Constantes.CHATROOM_TYPE_SINGLE)) {
                    for (ChatroomParticipant participant : chatroom.getParticipants()) {
                        if (participant.getPlayerID() != playerID) {
                            PlayerCache playerCache = playerCacheMgr.getOrLoadPlayerCache(participant.getPlayerID());
                            if (playerCache != null) {
                                if (!playerCache.pseudo.toUpperCase().contains(search.toUpperCase())) {
                                    it.remove();
                                    removed = true;
                                }
                            } else {
                                it.remove();
                                removed = true;
                            }
                        }
                    }
                }
            }

            // Remove 1to1 chatrooms reset by the player ???????1to1???
            if(!removed && chatroom.getType().equalsIgnoreCase(Constantes.CHATROOM_TYPE_SINGLE)){
                ChatroomParticipant participant = chatroom.getParticipant(playerID);
                if(participant != null){
                    if(participant.getResetDate() >= chatroom.getUpdateDate()){
                        it.remove();
                    }
                }
            }
        }

        // Nbmax and offset
        if(number > 0){
            int maxIndex = Math.min(chatrooms.size(), offset+number);
            chatrooms = chatrooms.subList(offset, maxIndex);
        }

        return chatrooms;
    }

    /**
     * Count the total number of chatrooms in which the player is involved ????????????
     * @param playerID playerID
     * @param search search pattern
     * @return total number of chatrooms
     */
    public int countChatroomsForPlayer(long playerID, String search){
        return getChatroomsForPlayer(playerID, search, 0, 0).size();
    }

    /**
     * Return WSChatrooms where the player is involved ?????WS???
     * @param playerID playerID
     * @param search search pattern
     * @param number nb max of chatrooms to return
     * @param offset offset
     * @return WSChatrooms
     */
    public List<WSChatroom> getWSChatroomsForPlayer(long playerID, String search, int number, int offset){
        List<GenericChatroom> chatrooms = getChatroomsForPlayer(playerID, search, number, offset);
        // Convert to WSChatroom
        List<WSChatroom> wsChatrooms = new ArrayList<>();
        for(GenericChatroom chatroom : chatrooms){
            ChatroomParticipant participant = chatroom.getParticipant(playerID);
            if(participant != null) {
                boolean includeParticipants = chatroom.getType().equalsIgnoreCase(Constantes.CHATROOM_TYPE_SINGLE);
                wsChatrooms.add(toWSChatroom(chatroom, participant, includeParticipants, true));
            }
        }
        return wsChatrooms;
    }

    public List<? extends GenericMessage> listMessagesForChatroom(String chatroomID, int offset, int nbMax) {
        Query q = Query.query(Criteria.where("chatroomID").is(chatroomID));
        if (offset > 0) {
            q.skip(offset);
        }
        if (nbMax > 0) {
            q.limit(nbMax);
        }
        q.with(new Sort(Sort.Direction.ASC, "date"));
        return getMongoTemplate().find(q, getMessageEntity());
    }

    public long countMessagesForChatroom(String chatroomID) {
        return getMongoTemplate().count(Query.query(Criteria.where("chatroomID").is(chatroomID)), getMessageEntity());
    }

    /**
     * Get participant of a chatroom by its playerID
     * @param chatroom
     * @param playerID
     * @return
     */
    protected ChatroomParticipant getParticipant(GenericChatroom chatroom, long playerID) {
        return chatroom.getParticipant(playerID);
    }

    /**
     * Return the messages from a chatroom
     * @param chatroomID
     * @param playerID the player asking for the messages
     * @return
     */
    public List<WSChatMessage> getMessagesForChatroom(String chatroomID, long playerID, boolean useJoinDateAndResetDate, int offset, int nbMax, Date minDate, Date maxDate) throws FBWSException{
        List<WSChatMessage> wsChatMessages = new ArrayList<>();
        GenericChatroom chatroom = findChatroomByID(chatroomID);
        if(chatroom != null){
            ChatroomParticipant participant = getParticipant(chatroom, playerID);
            if(participant == null){
                throw new FBWSException(FBExceptionType.MESSAGE_PLAYER_NOT_IN_CHATROOM);
            }

            List<AggregationOperation> operations = new ArrayList<>();
            // Match
            Criteria criteria = Criteria.where("chatroomID").is(chatroomID);
            if(useJoinDateAndResetDate) {
                criteria = criteria.and("date").gt(Math.max(participant.getJoinDate(), participant.getResetDate()));
            } else if(minDate != null && maxDate != null) {
                criteria = criteria.andOperator(Criteria.where("date").gte(minDate.getTime()), Criteria.where("date").lt(maxDate.getTime()));
            }
            operations.add(Aggregation.match(criteria));

            // Sort DESC
            operations.add(Aggregation.sort(Sort.Direction.DESC, "date"));

            // Skip
            if (offset > 0) {
                operations.add(Aggregation.skip((long)offset));
            }

            // Limit
            if (nbMax > 0) {
                operations.add(Aggregation.limit(nbMax));
            }

            // Sort ASC
            operations.add(Aggregation.sort(Sort.Direction.ASC, "date"));

            TypedAggregation<? extends GenericMessage> aggregation = Aggregation.newAggregation(getMessageEntity(), operations);
            AggregationResults<GenericMessage> results = getMongoTemplate().aggregate(aggregation, GenericMessage.class);
            if (results != null) {
                for (GenericMessage message : results.getMappedResults()) {
                    wsChatMessages.add(toWSChatMessage(message, participant));
                }
            }
        }
        return wsChatMessages;
    }

    /**
     * Return the messages from a chatroom
     * @param chatroomID
     * @param playerID the player asking for the messages
     * @return
     */
    public int getNbMessagesForChatroomAndPlayer(String chatroomID, long playerID, boolean useJoinDateAndResetDate, Date minDate, Date maxDate) throws FBWSException{
        GenericChatroom chatroom = findChatroomByID(chatroomID);
        if(chatroom != null){
            ChatroomParticipant participant = getParticipant(chatroom, playerID);
            if(participant == null){
                throw new FBWSException(FBExceptionType.MESSAGE_PLAYER_NOT_IN_CHATROOM);
            }
           Query q;
            if(useJoinDateAndResetDate) {
                q = Query.query(Criteria.where("chatroomID").is(chatroomID).andOperator(Criteria.where("date").gt(Math.max(participant.getJoinDate(), participant.getResetDate()))));
            } else if(minDate != null && maxDate != null) {
                q = Query.query(Criteria.where("chatroomID").is(chatroomID).andOperator(Criteria.where("date").gte(minDate.getTime()).lt(maxDate.getTime())));
            }  else {
                q = Query.query(Criteria.where("chatroomID").is(chatroomID));
            }
            return (int)getMongoTemplate().count(q, getMessageEntity());
        }
        return 0;
    }

    /**
     * Get number of messages in a chatroom
     * @param chatroomID
     * @return
     */
    public int getNbMessagesForChatroom(String chatroomID) {
        Query query = new Query();
        query.addCriteria(Criteria.where("chatroomID").is(chatroomID));
        return (int) getMongoTemplate().count(query, getMessageEntity());
    }

    /**
     * Get number of unread messages in a chatroom for a player
     * @param playerID
     * @param chatroomID
     * @return
     */
    public int getNbUnreadMessagesForPlayerAndChatroom(long playerID, String chatroomID, boolean useJoinDateAndResetDate) {
        int nbUnreadMessages = 0;
        GenericChatroom chatroom = findChatroomByID(chatroomID);
        if(chatroom != null){
            ChatroomParticipant participant = getParticipant(chatroom, playerID);
            if(participant != null){
                Query query = new Query();
                query.addCriteria(Criteria.where("chatroomID").is(chatroomID));
                if(useJoinDateAndResetDate) {
                    query.addCriteria(Criteria.where("date").gt(Math.max(Math.max(participant.getJoinDate(), participant.getResetDate()), participant.getLastRead())));
                } else {
                    query.addCriteria(Criteria.where("date").gt(participant.getLastRead()));
                }
                query.addCriteria(Criteria.where("authorID").ne(participant.getPlayerID()));
                nbUnreadMessages = (int) getMongoTemplate().count(query, getMessageEntity());
            }
        }
        return nbUnreadMessages;
    }

    /**
     * Get nb of unread chat messages for a player
     * @param playerID
     * @param useJoinDateAndResetDate
     * @return
     */
    public int getNbUnreadMessagesForPlayer(long playerID, boolean useJoinDateAndResetDate) {
        int nbUnreadMessages = 0;
        List<GenericChatroom> chatrooms = getChatroomsForPlayer(playerID, null);
        for(GenericChatroom chatroom : chatrooms){
            if(!chatroom.getType().equalsIgnoreCase(Constantes.CHATROOM_TYPE_TOURNAMENT)) {
                nbUnreadMessages += getNbUnreadMessagesForPlayerAndChatroom(playerID, chatroom.getIDStr(), useJoinDateAndResetDate);
            }
        }
        return nbUnreadMessages;
    }

    /**
     * Return list of unread messages for player on a chatroom
     * @param playerID
     * @param chatroomID
     * @param useJoinDateAndResetDate
     * @return
     */
    public List<WSChatMessage> getUnreadMessagesForPlayerAndChatroom(long playerID, String chatroomID, boolean useJoinDateAndResetDate) {
        List<WSChatMessage> listResult = new ArrayList<>();
        GenericChatroom chatroom = findChatroomByID(chatroomID);
        if(chatroom != null){
            ChatroomParticipant participant = getParticipant(chatroom, playerID);
            if(participant != null){
                Query query = new Query();
                query.addCriteria(Criteria.where("chatroomID").is(chatroomID));
                if(useJoinDateAndResetDate) {
                    query.addCriteria(Criteria.where("date").gt(Math.max(Math.max(participant.getJoinDate(), participant.getResetDate()), participant.getLastRead())));
                } else {
                    query.addCriteria(Criteria.where("date").gt(participant.getLastRead()));
                }
                query.addCriteria(Criteria.where("authorID").ne(participant.getPlayerID()));
                List<GenericMessage> listMessage = (List<GenericMessage>)getMongoTemplate().find(query, getMessageEntity());
                for (GenericMessage e : listMessage) {
                    listResult.add(toWSChatMessage(e, participant));
                }
            }
        }
        return listResult;
    }

    /**
     * Get the last message sent in a chatroom
     * @param chatroomID
     * @return
     */
    public GenericMessage getLastMessageForChatroom(String chatroomID){
        Query query = new Query();
        query.addCriteria(Criteria.where("chatroomID").is(chatroomID));
        query.with(new Sort(Sort.Direction.DESC, "date"));
        query.limit(1);
        return getMongoTemplate().findOne(query, getMessageEntity());
    }

    /**
     * Build the message to send
     * @param chatroomID
     * @param body
     * @param lang
     * @param player
     * @return
     */
    protected GenericMessage buildMessage(String chatroomID, String body, String mediaID, String mediaSize, String lang, String quotedMessageID, Player player) {
        GenericMessage message = new Message();
        message.setAuthorID(player.getID());
        message.setBody(body);
        message.setQuotedMessageID(quotedMessageID);
        message.setMediaID(mediaID);
        message.setMediaSize(mediaSize);
        message.setChatroomID(chatroomID);
        message.setDate(System.currentTimeMillis());
        message.setCreationDateISO(new Date());
        message.setType(Constantes.CHAT_MESSAGE_TYPE_PLAYER);

        return message;
    }

    /**
     * Notify participant when a new message is written
     * @param chatroom
     * @param message
     * @param player
     * @param playerParticipant
     */
    protected void notifyNewMessage(GenericChatroom chatroom, GenericMessage message, Player player, ChatroomParticipant playerParticipant) {
        if(notifyParticipantsOnNewMessage){
            // Add event for recipients
            for(ChatroomParticipant participant : chatroom.getParticipants()){
                // Don't send event to the message author
                if(participant.getPlayerID() == playerParticipant.getPlayerID()) continue;
                // Convert to wsChatMessage
                WSChatMessage wsChatMessage = toWSChatMessage(message, participant);
                // Check if participant is connected
                FBSession session = presenceMgr.getSessionForPlayerID(participant.getPlayerID());
                if (session != null) {
                    session.setNbNewMessage(getNbUnreadMessagesForPlayer(session.getPlayer().getID(), true));
                    // Create WSChatMessage for participant
                    try {
                        Event evt = new Event();
                        evt.timestamp = getTimeStamp();
                        evt.senderID = wsChatMessage.author.playerID;
                        evt.receiverID = participant.getPlayerID();
                        int maxLength = FBConfiguration.getInstance().getIntValue("message.fixBugEventTooBig", 0);
                        if (maxLength > 0) {
                            wsChatMessage.body = StringTools.truncate(wsChatMessage.body, maxLength, "...");
                        }
                        evt.addFieldCategory(Constantes.EVENT_CATEGORY_CHAT_MESSAGE, jsontools.transform2String(wsChatMessage, false));
                        session.pushEvent(evt);
                    } catch (JsonGenerationException e) {
                        log.error("JsonGenerationException to transform msg="+message,e);
                    } catch (JsonMappingException e) {
                        log.error("JsonMappingException to transform msg="+message,e);
                    } catch (IOException e) {
                        log.error("IOException to transform msg="+message,e);
                    }
                }
            }
        }
    }

    /**
     * Send a message written by a player to a chatroom
     * @param chatroomID the chatroom to whom the message must be sent
     * @param body body of the message
     * @param mediaID mediaID contained in the message
     * @param mediaSize size of the image contained in the message
     * @param quotedMessageID if a message is quoted inside the current message, here is its ID !
     * @param author the author
     * @return
     * @throws FBWSException
     */
    public WSChatMessage sendMessageToChatroom(String chatroomID, String body, String mediaID, String mediaSize, String lang, String quotedMessageID, Player author, String tempID, boolean testRight) throws FBWSException{
        // Get chatroom
        GenericChatroom chatroom = findChatroomByID(chatroomID);
        if(chatroom == null){
            log.error("Chatroom not found! chatroomID="+chatroomID);
            throw new FBWSException(FBExceptionType.MESSAGE_UNKNOWN_CHATROOM);
        }

        // Check if player is a participant of the chatroom
        ChatroomParticipant p = getParticipant(chatroom, author.getID());
        if(p == null){
            log.error("Player "+author.getID()+" can't send a message to chatroom "+chatroomID+" because he's not one of its participants");
            throw new FBWSException(FBExceptionType.MESSAGE_PLAYER_NOT_IN_CHATROOM);
        }

        // Check links between players if SINGLE chatroom
        if (chatroom.getType() != null && chatroom.getType().equals(Constantes.CHATROOM_TYPE_SINGLE)) {
            if (chatroom.getNbParticipants() == 2 && chatroom.getParticipants() != null && chatroom.getParticipants().size() == 2) {
                long recipientID = 0;
                for (ChatroomParticipant participant : chatroom.getParticipants()) {
                    if (participant.getPlayerID() != author.getID()) {
                        recipientID = participant.getPlayerID();
                    }
                }

                if (recipientID == 0) {
                    throw new FBWSException(FBExceptionType.PLAYER_NOT_EXISTING);
                }

                Player recipient = playerMgr.getPlayer(recipientID);
                if(recipient == null){
                    throw new FBWSException(FBExceptionType.PLAYER_NOT_EXISTING);
                }

                // Check player link
                checkPlayerLink(author, recipient);
            } else {
                log.warn(chatroom.getNbParticipants() + " participants in SINGLE chatroom - chatroomID="+chatroomID);
            }
        }

        // If a quoted message is sent, check its existence
        if(quotedMessageID != null && !quotedMessageID.isEmpty()){
            GenericMessage quotedMessage = findMessageByID(quotedMessageID);
            if(quotedMessage == null){
                log.error("quoted message not found! quotedMessageID="+quotedMessageID);
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            } else if(!quotedMessage.getChatroomID().equalsIgnoreCase(chatroomID)){
                log.error("quoted message exists but is not a part of the current chatroom! quotedMessageID="+quotedMessageID+" - chatroomID="+chatroomID+" - quotedMsg.chatroomID="+quotedMessage.getChatroomID());
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            }
        }

        // Create message and insert in DB
        GenericMessage message = buildMessage(chatroomID, body, mediaID, mediaSize, lang, quotedMessageID, author);
        getMongoTemplate().insert(message, getMessageCollectionName());
        // Update lastRead value for the message author
        p.setLastRead(message.getDate());
        // Change chatroom updateDate
        chatroom.setUpdateDate(message.getDate());
        saveChatroom(chatroom);
        // Create WSChatMessage for response
        WSChatMessage wsChatMessage = toWSChatMessage(message, p);
        wsChatMessage.tempID = tempID;
        // Notifify participants on new message
        this.notifyNewMessage(chatroom, message, author, p);

        return wsChatMessage;
    }

    /**
     * Send a message written by a player to another player (inside an existing chatroom or by creating a new one between the two players)
     * @param author the author of the message
     * @param recipientID the playerID of the recipient
     * @param body the body of the message
     * @param mediaID the mediaID contained in the message
     * @param mediaSize the image size
     * @return
     * @throws FBWSException
     */
    public WSChatroom sendMessageToPlayer(Player author, long recipientID, String body, String mediaID, String mediaSize, String tempID) throws FBWSException{
        if(author == null || recipientID == 0 || (body == null && mediaID == null)){
            log.error("Parameters not valid : author="+author+" - recipientID="+recipientID+" - body="+body+" - mediaID="+mediaID);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }

        Player recipient = playerMgr.getPlayer(recipientID);
        if(recipient == null){
            throw new FBWSException(FBExceptionType.PLAYER_NOT_EXISTING);
        }

        // Check player link
        PlayerLink playerLink = checkPlayerLink(author, recipient);

        // Get chatroom
        GenericChatroom chatroom = findSingleChatroomForPlayers(author.getID(), recipientID);

        // If there's no chatroom between the two players, let's create a new one
        if(chatroom == null){
            chatroom = createChatroom(null, null, null, Constantes.CHATROOM_TYPE_SINGLE);
            chatroom.addParticipant(author.getID());
            chatroom.addParticipant(recipientID);
            saveChatroom(chatroom);
        }

        // Send the message
        sendMessageToChatroom(chatroom.getIDStr(), body, mediaID, mediaSize, author.getLang(), null, author, tempID, false);
        // Return the chatroom
        GenericChatroom updatedChatroom = findChatroomByID(chatroom.getIDStr());
        WSChatroom wsChatroom = toWSChatroom(updatedChatroom, chatroom.getParticipant(author.getID()), true, true);
        if(wsChatroom != null && wsChatroom.lastMessage != null){
            wsChatroom.lastMessage.tempID = tempID;
        }
        return wsChatroom;
    }

    /**
     * Check player link (friend only & blocked)
     * @param author
     * @param recipient
     * @return
     * @throws FBWSException
     */
    public PlayerLink checkPlayerLink(Player author, Player recipient) throws FBWSException {
        PlayerLink playerLink = playerMgr.getLinkBetweenPlayer(author.getID(), recipient.getID());

        if (recipient.hasFriendFlag(Constantes.FRIEND_MASK_MESSAGE_ONLY_FRIEND)) {
            // check sender & recipient are friend
            if (playerLink == null || !playerLink.isLinkFriend()) {
                if (log.isDebugEnabled()) {
                    log.debug("Players are not friend and recipient only want message from friend - author=" + author + " - recipient=" + recipient);
                }
                throw new FBWSException(FBExceptionType.PLAYER_MESSAGE_ONLY_FRIEND);
            }
        }

        // check no link block between sender & recipient
        if (playerLink != null) {
            if (playerLink.hasBlocked()) {
                if (log.isDebugEnabled()) {
                    log.debug("Recipient has blocked this player playerLink=" + playerLink + " - author=" + author + " - recipient=" + recipient);
                }
                throw new FBWSException(FBExceptionType.PLAYER_LINK_BLOCKED);
            }
        }
        return playerLink;
    }

    /**
     * Create a new GROUP chatroom
     * @param author the chatroom creator
     * @param name the chatroom name
     * @param imageID the chatroom imageID
     * @param participants list of playerIDs (creator not included)
     * @return WSChatroom
     * @throws FBWSException
     */
    public WSChatroom createGroupChatroom(Player author, String name, String imageID, List<Long> participants) throws FBWSException {
        // Verifications
        if(author == null || name == null || participants.size() < 1){
            log.error("Parameters not valid : author="+author+" - name="+name+" - participants.size="+participants.size()+" - mediaID="+imageID);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        // Chatroom creation
        GenericChatroom chatroom = createChatroom(name, null, imageID, Constantes.CHATROOM_TYPE_GROUP);
        chatroom.addParticipant(author.getID());
        // Give admin rights
        setAdminRights(chatroom, author.getID(), true);
        // Add participants
        addParticipantsToChatroom(author, chatroom.getIDStr(), participants, false);
        // Send system message to announce the creation
        Map<String, String> params = new HashMap<>();
        params.put("AUTHOR", author.getNickname());
        params.put("NAME", name);
        sendSystemMessageToChatroom("chat.groupChatroomCreated", params, chatroom.getIDStr());
        // Return
        GenericChatroom updatedChatroom = findChatroomByID(chatroom.getIDStr());
        return toWSChatroom(updatedChatroom, chatroom.getParticipant(author.getID()), true, true);
    }

    /**
     * Change the name of a GROUP chatroom
     * @param player the player changing the name
     *
     * @param chatroomID the chatroom ID
     * @param name the new name
     * @return boolean result
     * @throws FBWSException
     */
    public boolean setChatroomName(Player player, String chatroomID, String name) throws FBWSException{
        GenericChatroom chatroom = findChatroomByID(chatroomID);
        if(chatroom != null && name != null && !name.isEmpty() && player != null){
            if(chatroom.getType().equalsIgnoreCase(Constantes.CHATROOM_TYPE_GROUP)){
                ChatroomParticipant currentPlayer = chatroom.getParticipant(player.getID());
                if(currentPlayer != null){
                    if(currentPlayer.isAdministrator()){
                        chatroom.setName(name.trim());
                        Map<String, String> params = new HashMap<>();
                        params.put("AUTHOR", player.getNickname());
                        params.put("NAME", chatroom.getName());
                        sendSystemMessageToChatroom("chat.groupChatroomRenamed", params, chatroomID);
                        saveChatroom(chatroom);
                        return true;
                    } else {
                        log.error("Player "+player.getID()+" is not an admin of chatroom "+chatroomID);
                        throw new FBWSException(FBExceptionType.MESSAGE_PLAYER_NOT_AUTHORIZED);
                    }
                } else {
                    log.error("Player "+player.getID()+" is not a member of chatroom "+chatroomID);
                    throw new FBWSException(FBExceptionType.MESSAGE_PLAYER_NOT_IN_CHATROOM);
                }
            }
        }
        return false;
    }

    /**
     * Change the mediaID of a GROUP chatroom
     * @param player the player changing the mediaID
     * @param chatroomID the chatroom ID
     * @param imageID the new mediaID
     * @return boolean result
     * @throws FBWSException
     */
    public boolean setChatroomImage(Player player, String chatroomID, String imageID) throws FBWSException {
        GenericChatroom chatroom = findChatroomByID(chatroomID);
        if(player != null && chatroom != null){
            if(chatroom.getType().equalsIgnoreCase(Constantes.CHATROOM_TYPE_GROUP)) {
                ChatroomParticipant currentPlayer = chatroom.getParticipant(player.getID());
                if (currentPlayer != null) {
                    if (currentPlayer.isAdministrator()) {
                        chatroom.setImageID(imageID);
                        saveChatroom(chatroom);
                        return true;
                    } else {
                        log.error("Player "+player.getID()+" is not an admin of chatroom "+chatroomID);
                        throw new FBWSException(FBExceptionType.MESSAGE_PLAYER_NOT_AUTHORIZED);
                    }
                } else {
                    log.error("Player "+player.getID()+" is not a member of chatroom "+chatroomID);
                    throw new FBWSException(FBExceptionType.MESSAGE_PLAYER_NOT_IN_CHATROOM);
                }
            }
        }
        return false;
    }

    /**
     * Give or remove admin rights to a participant of the chatroom
     * @param chatroom the chatroom
     * @param participantID the participant ID
     * @param administrator boolean to tell wether or not the participant will be admin
     * @return boolean result
     * @throws FBWSException
     */
    public boolean setAdminRights(GenericChatroom chatroom, long participantID, boolean administrator) throws FBWSException {
        if(chatroom != null){
            ChatroomParticipant participant = chatroom.getParticipant(participantID);
            if(participant != null){
                participant.setAdministrator(administrator);
                saveChatroom(chatroom);
                return true;
            } else {
                throw new FBWSException(FBExceptionType.MESSAGE_PLAYER_NOT_IN_CHATROOM);
            }
        } else {
            throw new FBWSException(FBExceptionType.MESSAGE_UNKNOWN_CHATROOM);
        }
    }

    /**
     * Delete a message (only the author can do it)
     * @param player
     * @param messageID
     * @return boolean result
     * @throws FBWSException
     */
    public boolean deleteMessage(Player player, String messageID) throws FBWSException {
        // Get message
        GenericMessage message = findMessageByID(messageID);
        if(message != null && player != null){
            if(message.getAuthorID() == player.getID()) {
                message.setType(Constantes.CHAT_MESSAGE_TYPE_DELETED);
                message.setTemplateName("chat.deletedMessage");
                message.setQuotedMessageID(null);
                // TODO: delete from Cloudinary
                message.setMediaID(null);
                message.setMediaSize(null);
                getMongoTemplate().save(message);
                return true;
            } else {
                log.error("player can't delete message : he's not the author! player="+player.getID()+" - messageID="+messageID);
                throw new FBWSException(FBExceptionType.MESSAGE_PLAYER_NOT_AUTHORIZED);
            }
        } else {
            log.error("message not found! messageID="+messageID);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
    }

    /**
     * Build the system message to send
     * @param templateName
     * @param templateParameters
     * @param chatroomID
     * @return
     */
    protected GenericMessage buildSystemMessage(String templateName, Map<String, String> templateParameters, String chatroomID) {
        GenericMessage message = new Message();
        message.setChatroomID(chatroomID);
        message.setTemplateName(templateName);
        message.setTemplateParameters(templateParameters);
        message.setDate(System.currentTimeMillis());
        message.setCreationDateISO(new Date());
        message.setType(Constantes.CHAT_MESSAGE_TYPE_SYSTEM);

        return message;
    }

    /**
     * Notify participant when a new system message is written
     * @param chatroom
     * @param message
     */
    protected void notifyNewSystemMessage(GenericChatroom chatroom, GenericMessage message) {
        if(notifyParticipantsOnNewMessage) {
            // Add event for recipients
            for (ChatroomParticipant participant : chatroom.getParticipants()) {
                // Check if participant is connected
                FBSession session = presenceMgr.getSessionForPlayerID(participant.getPlayerID());
                if (session != null) {
                    // Create WSChatMessage for participant
                    WSChatMessage wsChatMsg = toWSChatMessage(message, participant);
                    try {
                        Event evt = new Event();
                        evt.timestamp = getTimeStamp();
                        evt.senderID = message.getAuthorID();
                        evt.receiverID = participant.getPlayerID();
                        int maxLength = FBConfiguration.getInstance().getIntValue("message.fixBugEventTooBig", 0);
                        if (maxLength > 0) {
                            wsChatMsg.body = StringTools.truncate(wsChatMsg.body, maxLength, "...");
                        }
                        evt.addFieldCategory(Constantes.EVENT_CATEGORY_CHAT_MESSAGE, jsontools.transform2String(wsChatMsg, false));
                        session.pushEvent(evt);
                    } catch (JsonGenerationException e) {
                        log.error("JsonGenerationException to transform msg=" + wsChatMsg, e);
                    } catch (JsonMappingException e) {
                        log.error("JsonMappingException to transform msg=" + wsChatMsg, e);
                    } catch (IOException e) {
                        log.error("IOException to transform msg=" + wsChatMsg, e);
                    }
                }
            }
        }
    }

    /**
     * Send a message sent by the system (not written by a player) to a chatroom
     * @param templateName
     * @param templateParameters
     * @param chatroomID
     * @return
     */
    public boolean sendSystemMessageToChatroom(String templateName, Map<String, String> templateParameters, String chatroomID) throws FBWSException{
        // Get chatroom
        GenericChatroom chatroom = findChatroomByID(chatroomID);
        if(chatroom == null){
            log.error("Chatroom not found! chatroomID="+chatroomID);
            throw new FBWSException(FBExceptionType.MESSAGE_UNKNOWN_CHATROOM);
        }
        // Create message and insert in DB
        GenericMessage message = buildSystemMessage(templateName, templateParameters, chatroomID);
        getMongoTemplate().insert(message, getMessageCollectionName());

        // Change chatroom updateDate
        chatroom.setUpdateDate(message.getDate());
        saveChatroom(chatroom);

        // Notify participants about new message
        this.notifyNewSystemMessage(chatroom, message);

        return true;
    }

    /**
     * Specify that a message has been read by a chatroom participant
     * @param messageID
     * @param session
     * @return
     * @throws FBWSException
     */
    public boolean setMessageRead(String messageID, FBSession session) throws FBWSException{
        // Get message
        GenericMessage message = findMessageByID(messageID);
        if(message == null){
            log.error("message not found for ID="+messageID);
            throw new FBWSException(FBExceptionType.MESSAGE_UNKNOWN_MESSAGE);
        }
        // Find corresponding chatroom
        GenericChatroom chatroom = findChatroomByID(message.getChatroomID());
        if(chatroom == null){
            log.error("chatroom not found for ID="+message.getChatroomID()+" although this ID is written in message "+messageID);
            throw new FBWSException(FBExceptionType.MESSAGE_UNKNOWN_CHATROOM);
        }
        // Get participant who just read the message
        long playerID = session.getPlayer().getID();
        ChatroomParticipant participant = getParticipant(chatroom, playerID);
        if(participant == null){
            log.error("the player asking to set a message to 'read' is not a participant of the chatroom!");
            throw new FBWSException(FBExceptionType.MESSAGE_PLAYER_NOT_IN_CHATROOM);
        }
        // Set lastRead field for participant
        if(message.getDate() > participant.getLastRead()){
            participant.setLastRead(message.getDate());
            getMongoTemplate().save(chatroom);
        }
        // Update nbUnreadMessages in session
        if(notifyParticipantsOnNewMessage){
            session.setNbNewMessage(getNbUnreadMessagesForPlayer(playerID, true));
        }

        return true;
    }

    /**
     *
     * @param chatroomID
     * @param playerID
     * @return
     * @throws FBWSException
     */
    public boolean resetChatroomHistory(String chatroomID, long playerID) throws FBWSException{
        // Find chatroom
        GenericChatroom chatroom = findChatroomByID(chatroomID);
        if(chatroom == null){
            log.error("chatroom not found for ID="+chatroomID);
            throw new FBWSException(FBExceptionType.MESSAGE_UNKNOWN_CHATROOM);
        }
        // Get the participant who want to reset message history
        ChatroomParticipant participant = getParticipant(chatroom, playerID);
        if(participant == null){
            log.error("the player asking to reset chatroom history is not a participant of the chatroom!");
            throw new FBWSException(FBExceptionType.MESSAGE_PLAYER_NOT_IN_CHATROOM);
        }
        // Set resetDate field for participant
        participant.setResetDate(System.currentTimeMillis());
        getMongoTemplate().save(chatroom);
        return true;
    }

    /**
     * Create a new chatroom and insert it in DB
     * @param name (optional)
     * @return
     */
    public GenericChatroom createChatroom(String name, String nameTemplate, String imageID, String type){
        GenericChatroom chatroom = new Chatroom();
        chatroom.setName(name);
        chatroom.setNameTemplate(nameTemplate);
        chatroom.setImageID(imageID);
        chatroom.setType(type);
        chatroom.setCreationDate(System.currentTimeMillis());
        chatroom.setUpdateDate(chatroom.getCreationDate());
        chatroom.setCreationDateISO(new Date());
        getMongoTemplate().insert(chatroom, getChatroomCollectionName());
        return chatroom;
    }

    public List<WSPlayerLight> getSuggestedParticipants(Player author){
        List<WSPlayerLight> suggestedParticipants = new ArrayList<>();
        List<WSPlayerLinked> friends = playerMgr.getListLinkedForType(author.getID(), Constantes.PLAYER_LINK_TYPE_FRIEND, 0, 50);
        for(WSPlayerLinked friend : friends){
            WSPlayerLight playerLight = playerMgr.playerToWSPlayerLight(friend.playerID, author.getID());
            if(playerLight != null) suggestedParticipants.add(playerLight);
        }
        return suggestedParticipants;
    }


    /**
     * Update a chatroom to DB
     * @param chatroom
     */
    public void saveChatroom(GenericChatroom chatroom){
        getMongoTemplate().save(chatroom);
    }

    /**
     * Delete a chatroom and all its messages
     * @param chatroomID
     */
    public void deleteChatroom(String chatroomID){
        Query q1 = new Query(Criteria.where("chatroomID").is(chatroomID));
        getMongoTemplate().remove(q1, getMessageEntity());
        Query q2 = new Query(Criteria.where("ID").is(new ObjectId(chatroomID)));
        getMongoTemplate().remove(q2, getChatroomEntity());
    }

    /**
     * Add the player to the chatroom
     * @param playerID the player ID to add to the chatroom participants
     * @param chatroom the chatroom in which to add the participant
     * @return the new ChatroomParticipant object
     */
    public ChatroomParticipant addParticipantToChatroom(long playerID, GenericChatroom chatroom, boolean sendSystemMessage) {
        if(chatroom != null){
            Player player = playerMgr.getPlayer(playerID);
            if(player != null) {
                if (chatroom.addParticipant(playerID)) {
                    saveChatroom(chatroom);
                    if (sendSystemMessage) {
                        Map<String, String> params = new HashMap<>();
                        params.put("PLAYER", player.getNickname());
                        try{
                            sendSystemMessageToChatroom("chat.playerJoin", params, chatroom.getIDStr());
                        } catch (FBWSException e){
                            log.error("Can't send chat.playerJoin system message to chatroomID="+chatroom.getIDStr());
                        }
                    }
                }
                return chatroom.getParticipant(playerID);
            }
        }
        return null;
    }

    /**
     * Adds a list of player to the list of participants
     * @param author the player making the call
     * @param chatroomID the chatroom ID
     * @param participants the list of participants to add (player IDs)
     * @param sendSystemMessage boolean to tell wether or not to send a system message announcing the entrance of participants in the chatroom
     * @return the updated chatroom
     * @throws FBWSException
     */
    public WSChatroom addParticipantsToChatroom(Player author, String chatroomID, List<Long> participants, boolean sendSystemMessage) throws FBWSException {
        if(author != null && chatroomID != null && !chatroomID.isEmpty()){
            // Verifications
            GenericChatroom chatroom = findChatroomByID(chatroomID);
            if(chatroom == null) {
                log.error("chatroom with ID="+chatroomID+" not found");
                throw new FBWSException(FBExceptionType.MESSAGE_UNKNOWN_CHATROOM);
            }
            ChatroomParticipant currentPlayer = chatroom.getParticipant(author.getID());
            if(currentPlayer == null){
                throw new FBWSException(FBExceptionType.MESSAGE_PLAYER_NOT_IN_CHATROOM);
            }
            if(!currentPlayer.isAdministrator()){
                throw new FBWSException(FBExceptionType.MESSAGE_PLAYER_NOT_AUTHORIZED);
            }
            // Adding authorized participants
            List<Long> participantsRemoved = new ArrayList<>();
            for(long participantID : participants){
                Player participant = playerMgr.getPlayer(participantID);
                if(participant == null){
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                } else if(playerMgr.isPlayer1BlockedByPlayer2(author.getID(), participantID) || playerMgr.isPlayer1BlockedByPlayer2(participantID, author.getID())){
                    participantsRemoved.add(participantID);
                } else {
                    addParticipantToChatroom(participantID, chatroom, sendSystemMessage);
                }
            }
            return toWSChatroom(chatroom, currentPlayer,true,true);
        }
        return null;
    }

    public WSChatroom removeParticipantFromChatroom(Player author, String chatroomID, long playerID, boolean sendSystemMessage) throws FBWSException {
        if(author != null && chatroomID != null && !chatroomID.isEmpty()){
            // Verifications
            GenericChatroom chatroom = findChatroomByID(chatroomID);
            if(chatroom == null) {
                log.error("chatroom with ID="+chatroomID+" not found");
                throw new FBWSException(FBExceptionType.MESSAGE_UNKNOWN_CHATROOM);
            }
            ChatroomParticipant currentPlayer = chatroom.getParticipant(author.getID());
            ChatroomParticipant participantToRemove = chatroom.getParticipant(playerID);
            if(currentPlayer == null || participantToRemove == null){
                throw new FBWSException(FBExceptionType.MESSAGE_PLAYER_NOT_IN_CHATROOM);
            }
            if(!currentPlayer.isAdministrator()){
                throw new FBWSException(FBExceptionType.MESSAGE_PLAYER_NOT_AUTHORIZED);
            }
            if(participantToRemove.isAdministrator() && chatroom.getAdministrators().size() < 2){
                throw new FBWSException(FBExceptionType.MESSAGE_FORBIDDEN_LAST_ADMIN);
            }
            // Remove participant
            removeParticipantFromChatroom(playerID, chatroom, sendSystemMessage, true);
            // Return
            return toWSChatroom(chatroom, currentPlayer,true,true);
        }
        return null;
    }

    public boolean leaveChatroom(Player player, String chatroomID) throws FBWSException {
        if(player != null && chatroomID != null && !chatroomID.isEmpty()){
            // Verifications
            GenericChatroom chatroom = findChatroomByID(chatroomID);
            if(chatroom == null) {
                log.error("chatroom with ID="+chatroomID+" not found");
                throw new FBWSException(FBExceptionType.MESSAGE_UNKNOWN_CHATROOM);
            }
            ChatroomParticipant currentPlayer = chatroom.getParticipant(player.getID());
            if(currentPlayer == null){
                throw new FBWSException(FBExceptionType.MESSAGE_PLAYER_NOT_IN_CHATROOM);
            }
            if(chatroom.getType().equalsIgnoreCase(Constantes.CHATROOM_TYPE_TEAM)){
                throw new FBWSException(FBExceptionType.MESSAGE_NOT_ALLOWED_TO_LEAVE);
            }
            if(chatroom.getType().equalsIgnoreCase(Constantes.CHATROOM_TYPE_GROUP) && currentPlayer.isAdministrator() && chatroom.getAdministrators().size() < 2 && chatroom.getParticipants().size() > 1){
                throw new FBWSException(FBExceptionType.MESSAGE_FORBIDDEN_LAST_ADMIN);
            }
            // Remove participant
            boolean sendSystemMessage = chatroom.getType().equalsIgnoreCase(Constantes.CHATROOM_TYPE_GROUP);
            return removeParticipantFromChatroom(player.getID(), chatroom, sendSystemMessage, false);
        }
        return false;
    }

    public boolean setAlertsForChatroom(Player player, String chatroomID, boolean enabled) throws FBWSException {
        if(player != null && chatroomID != null && !chatroomID.isEmpty()){
            // Verifications
            GenericChatroom chatroom = findChatroomByID(chatroomID);
            if(chatroom == null) {
                log.error("chatroom with ID="+chatroomID+" not found");
                throw new FBWSException(FBExceptionType.MESSAGE_UNKNOWN_CHATROOM);
            }
            ChatroomParticipant currentPlayer = chatroom.getParticipant(player.getID());
            if(currentPlayer == null){
                throw new FBWSException(FBExceptionType.MESSAGE_PLAYER_NOT_IN_CHATROOM);
            }
            currentPlayer.setMuted(!enabled);
            saveChatroom(chatroom);
            return true;
        }
        return false;
    }

    /**
     * Adds a list of player to the list of administrators
     * @param author the player making the call
     * @param chatroomID the chatroom ID
     * @param newAdmins the list of new administrators (player IDs)
     * @return the updated chatroom
     * @throws FBWSException
     */
    public WSChatroom addAdministrators(Player author, String chatroomID, List<Long> newAdmins) throws FBWSException {
        if(author != null && chatroomID != null && !chatroomID.isEmpty()){
            // Verifications
            GenericChatroom chatroom = findChatroomByID(chatroomID);
            if(chatroom == null) {
                log.error("chatroom with ID="+chatroomID+" not found");
                throw new FBWSException(FBExceptionType.MESSAGE_UNKNOWN_CHATROOM);
            }
            ChatroomParticipant currentPlayer = chatroom.getParticipant(author.getID());
            if(currentPlayer == null){
                throw new FBWSException(FBExceptionType.MESSAGE_PLAYER_NOT_IN_CHATROOM);
            }
            if(!currentPlayer.isAdministrator()){
                throw new FBWSException(FBExceptionType.MESSAGE_PLAYER_NOT_AUTHORIZED);
            }
            // Adding administrators
            for(long adminID : newAdmins){
                Player participant = playerMgr.getPlayer(adminID);
                if(participant == null){
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                } else {
                    setAdminRights(chatroom, adminID, true);
                }
            }
            return toWSChatroom(chatroom, currentPlayer,true,true);
        }
        return null;
    }

    public WSChatroom removeAdministrator(Player author, String chatroomID, long playerID) throws FBWSException {
        if(author != null && chatroomID != null && !chatroomID.isEmpty()){
            // Verifications
            GenericChatroom chatroom = findChatroomByID(chatroomID);
            if(chatroom == null) {
                log.error("chatroom with ID="+chatroomID+" not found");
                throw new FBWSException(FBExceptionType.MESSAGE_UNKNOWN_CHATROOM);
            }
            ChatroomParticipant currentPlayer = chatroom.getParticipant(author.getID());
            if(currentPlayer == null){
                throw new FBWSException(FBExceptionType.MESSAGE_PLAYER_NOT_IN_CHATROOM);
            }
            if(!currentPlayer.isAdministrator()){
                throw new FBWSException(FBExceptionType.MESSAGE_PLAYER_NOT_AUTHORIZED);
            }
            if(chatroom.getAdministrators().size() < 2){
                throw new FBWSException(FBExceptionType.MESSAGE_FORBIDDEN_LAST_ADMIN);
            }
            // Remove admin rights
            setAdminRights(chatroom, playerID, false);
            // Return
            return toWSChatroom(chatroom, currentPlayer,true,true);
        }
        return null;
    }

    /**
     * Removes the player from the chatroom
     * @param playerID
     * @param chatroom
     * @return
     */
    protected boolean removeParticipantFromChatroom(long playerID, GenericChatroom chatroom, boolean sendSystemMessage, boolean kickedByAdmin) {
        if(chatroom != null){
            Player player = playerMgr.getPlayer(playerID);
            if(player != null) {
                if (chatroom.removeParticipant(playerID)) {
                    saveChatroom(chatroom);
                    if (sendSystemMessage) {
                        Map<String, String> params = new HashMap<>();
                        params.put("PLAYER", player.getNickname());
                        try{
                            // Send system message
                            String messageTemplate = kickedByAdmin ? "chat.playerKickedOut" : "chat.playerLeave";
                            sendSystemMessageToChatroom(messageTemplate, params, chatroom.getIDStr());
                            // Send event to kicked player if connected
                            if(kickedByAdmin) {
                                FBSession session = presenceMgr.getSessionForPlayerID(playerID);
                                if (session != null) {
                                    Event evt = new Event();
                                    evt.timestamp = getTimeStamp();
                                    evt.receiverID = playerID;
                                    evt.addFieldCategory(Constantes.EVENT_CATEGORY_CHAT, null);
                                    evt.addFieldType(Constantes.EVENT_TYPE_CHAT_KICKED_FROM_CHATROOM, chatroom.getIDStr());
                                    session.pushEvent(evt);
                                }
                            }
                        } catch (FBWSException e){
                            log.error("Can't send chat.playerKickedOut system message to chatroomID="+chatroom.getIDStr());
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Convert GenericChatroom object to a WSChatroom object to be sent to the client
     * @param chatroom the chatroom to convert
     * @param participant the participant asking for chatroom details
     * @return
     */
    public WSChatroom toWSChatroom(GenericChatroom chatroom, ChatroomParticipant participant, boolean includeParticipants, boolean useJoinDateAndResetDate){
        WSChatroom wsChatroom = new WSChatroom();
        wsChatroom.ID = chatroom.getIDStr();
        wsChatroom.name = chatroom.getName();
        wsChatroom.nameTemplate = chatroom.getNameTemplate();
        wsChatroom.imageID = chatroom.getImageID();
        wsChatroom.type = chatroom.getType();
        wsChatroom.updateDate = chatroom.getUpdateDate();
        if(participant != null) {
            try {
                wsChatroom.nbMessages = getNbMessagesForChatroomAndPlayer(chatroom.getIDStr(), participant.getPlayerID(), useJoinDateAndResetDate, null, null);
            } catch (Exception e) {
                log.error("Player " + participant.getPlayerID() + " is not a participant of chatroom " + chatroom.getIDStr());
                wsChatroom.nbMessages = getNbMessagesForChatroom(chatroom.getIDStr());
            }
        } else {
            wsChatroom.nbMessages = getNbMessagesForChatroom(chatroom.getIDStr());
        }
        if(participant != null) {
            wsChatroom.nbUnreadMessages = getNbUnreadMessagesForPlayerAndChatroom(participant.getPlayerID(), chatroom.getIDStr(), useJoinDateAndResetDate);
            wsChatroom.muted = participant.hasMuted();
        }
        GenericMessage lastMessage = getLastMessageForChatroom(chatroom.getIDStr());
        if(lastMessage != null){
            wsChatroom.lastMessage = toWSChatMessage(lastMessage, participant);
            if(participant != null){
                wsChatroom.read = lastMessage.getDate() > participant.getLastRead();
            }
        }
        if(includeParticipants) {
            for (ChatroomParticipant p : chatroom.getParticipants()) {
                PlayerCache playerCache = playerCacheMgr.getPlayerCache(p.getPlayerID());
                if (playerCache == null) {
                    playerCache = playerCacheMgr.getOrLoadPlayerCache(p.getPlayerID());
                }
                WSPlayerLight playerLight = new WSPlayerLight();
                playerLight.playerID = playerCache.ID;
                playerLight.pseudo = playerCache.getPseudo();
                playerLight.avatar = playerCache.avatarPresent;
                playerLight.countryCode = playerCache.countryCode;
                playerLight.connected = ContextManager.getPresenceMgr().isSessionForPlayerID(playerCache.ID);
                wsChatroom.participants.add(playerLight);
            }
        }
        for(ChatroomParticipant p : chatroom.getParticipants()){
            if(p.isAdministrator()) wsChatroom.administrators.add(p.getPlayerID());
        }
        return wsChatroom;
    }

    /**
     * Convert Message object to a WSChatMessage object to be sent to the client
     * @param message the message to convert
     * @param participant the participant asking for the message
     * @return
     */
    public WSChatMessage toWSChatMessage(GenericMessage message, ChatroomParticipant participant){
        WSChatMessage wsChatMessage = new WSChatMessage();
        wsChatMessage.ID = message.getIDStr();
        wsChatMessage.body = message.getBody();
        wsChatMessage.mediaID = message.getMediaID();
        wsChatMessage.mediaSize = message.getMediaSize();
        wsChatMessage.date = message.getDate();
        wsChatMessage.type = message.getType();
        if(message.getAuthorID() != 0){
            // Player message, fill the author info
            PlayerCache authorCache = playerCacheMgr.getPlayerCache(message.getAuthorID());
            if (authorCache == null) {
                authorCache = playerCacheMgr.getOrLoadPlayerCache(message.getAuthorID());
            }
            WSPlayerLight author = new WSPlayerLight();
            author.playerID = authorCache.ID;
            author.pseudo = authorCache.getPseudo();
            author.avatar = authorCache.avatarPresent;
            author.countryCode = authorCache.countryCode;
            author.connected = ContextManager.getPresenceMgr().isSessionForPlayerID(authorCache.ID);
            wsChatMessage.author = author;
            // GameID if it's a message from a deal chatroom
            ChatroomParticipant p = findChatroomByID(message.getChatroomID()).getParticipant(message.getAuthorID());
            if(p != null){
                wsChatMessage.gameID = p.getGameID();
            }
        }
        // If it's a template message create the body based on the participant's language
        if(message.getTemplateName() != null && !message.getTemplateName().isEmpty()){
            // Get participant's language
            String lang = Constantes.PLAYER_LANG_EN;
            if(participant != null){
                PlayerCache participantCache = playerCacheMgr.getOrLoadPlayerCache(participant.getPlayerID());
                lang = participantCache.lang;
            }
            wsChatMessage.body = ContextManager.getTextUIMgr().getTextUIForLang(message.getTemplateName(), lang);
            if(message.getTemplateParameters().size() > 0) {
                wsChatMessage.body = replaceTextVariables(wsChatMessage.body, message.getTemplateParameters());
            }
        }
        if(participant != null){
            wsChatMessage.read = (message.getDate() <= participant.getLastRead());
        }
        if(message.getQuotedMessageID() != null && !message.getQuotedMessageID().isEmpty()){
            GenericMessage quotedMessage = findMessageByID(message.getQuotedMessageID());
            if(quotedMessage != null){
                wsChatMessage.quotedMessage = toWSChatMessage(quotedMessage, participant);
            }
        }
        wsChatMessage.chatroomID = message.getChatroomID();
        return wsChatMessage;
    }

    /**
     * Replace the variables in text by the values contain in the map. Variable are like '{VAR_NAME}' and map contain pair value (VAR_NAME, value)
     * @param msgText
     * @param varVal
     * @return
     */
    public static String replaceTextVariables(String msgText, Map<String, String> varVal) {
        String strReturn = msgText;
        if (msgText != null && msgText.length() > 0) {
            if (msgText.contains("{")) {
                if (varVal != null && varVal.size() > 0) {
                    // replace all token by the value
                    Matcher matcher = pattern.matcher(msgText);
                    StringBuffer sb = new StringBuffer();
                    while (matcher.find()) {
                        String val = varVal.get(matcher.group(1));
                        if (val != null) {
                            matcher.appendReplacement(sb, "");
                            sb.append(val);
                        }
                    }
                    matcher.appendTail(sb);
                    strReturn = sb.toString();
                }
            }
        }
        return strReturn;
    }

    /**
     * Read string value for parameter in name (chat.paramName) in config file
     * @param paramName
     * @param defaultValue
     * @return
     */
    public String getConfigStringValue(String paramName, String defaultValue) {
        return FBConfiguration.getInstance().getStringValue("chat." + paramName, defaultValue);
    }

    /**
     * Read string value for parameter in name (chat.paramName) in config file
     * @param paramName
     * @param defaultValue
     * @return
     */
    public String getStringResolvEnvVariableValue(String paramName, String defaultValue) {
        return FBConfiguration.getInstance().getStringResolvEnvVariableValue("chat." + paramName, defaultValue);
    }

    /**
     * Read int value for parameter in name (team.paramName) in config file
     * @param paramName
     * @param defaultValue
     * @return
     */
    public int getConfigIntValue(String paramName, int defaultValue) {
        return FBConfiguration.getInstance().getIntValue("chat." + paramName, defaultValue);
    }

    /**
     * Return the current time stamp. A sleep time of 2 ms is do to be sure to have different TS
     * @return
     */
    protected static synchronized long getTimeStamp() {
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
        }
        return System.currentTimeMillis();
    }
}
