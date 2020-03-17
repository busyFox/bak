package com.funbridge.server.tournament.privatetournament.memory;

import com.funbridge.server.common.ContextManager;
import com.funbridge.server.tournament.game.Game;
import com.funbridge.server.tournament.game.Tournament;
import com.funbridge.server.tournament.generic.memory.GenericMemDealPlayer;
import com.funbridge.server.tournament.generic.memory.GenericMemTournament;
import com.funbridge.server.tournament.generic.memory.GenericMemTournamentPlayer;
import com.funbridge.server.tournament.generic.memory.TournamentGenericMemoryMgr;
import com.funbridge.server.tournament.privatetournament.data.PrivateDeal;
import com.funbridge.server.tournament.privatetournament.data.PrivateTournament;
import com.funbridge.server.tournament.privatetournament.data.PrivateTournamentChatroom;

/**
 * Created by bplays on 20/02/17.
 */
public class PrivateMemTournament extends GenericMemTournament<GenericMemTournamentPlayer, PrivateMemDeal, GenericMemDealPlayer>{

    public String chatroomID;

    public PrivateMemTournament(){}

    @Override
    public void initData(Tournament tour, TournamentGenericMemoryMgr memoryMgr) {
        super.initData(tour, memoryMgr);
        this.chatroomID = ((PrivateTournament)tour).getChatroomID();
        for (int i = 0; i < deals.length; i++){
            deals[i].chatroomID = ((PrivateDeal)tour.getDealAtIndex(i + 1)).getChatroomID();
        }
    }

    @Override
    public GenericMemTournamentPlayer addResult(Game game, boolean computeResultRanking, boolean finishProcess) {
        // Do the generic stuff
        GenericMemTournamentPlayer result = super.addResult(game, computeResultRanking, finishProcess);
        // Add the player to the deal chatroom
        PrivateTournamentChatroom chatroom = (PrivateTournamentChatroom) ContextManager.getPrivateTournamentMgr().getChatMgr().findChatroomByID(((PrivateDeal) game.getDeal()).getChatroomID());
        if(chatroom != null){
            if(chatroom.addParticipant(game.getPlayerID())){
                chatroom.getParticipant(game.getPlayerID()).setGameID(game.getIDStr());
                ContextManager.getPrivateTournamentMgr().getChatMgr().saveChatroom(chatroom);
            }
        }
        return result;
    }

    @Override
    public void addPlayer(long playerID, long dateStart) {
        // Do the generic stuff
        super.addPlayer(playerID, dateStart);
        // Add the player to the tournament chatroom
        PrivateTournamentChatroom chatroom = (PrivateTournamentChatroom) ContextManager.getPrivateTournamentMgr().getChatMgr().findChatroomByID(this.chatroomID);
        if(chatroom != null){
            if(chatroom.addParticipant(playerID)){
                ContextManager.getPrivateTournamentMgr().getChatMgr().saveChatroom(chatroom);
            }
        }
    }

    @Override
    public GenericMemTournamentPlayer getOrCreateTournamentPlayer(long playerID) {
        GenericMemTournamentPlayer plaRank = tourPlayer.get(playerID);
        if (plaRank == null) {
            plaRank = super.getOrCreateTournamentPlayer(playerID);

            // Add the player to the tournament chatroom
            PrivateTournamentChatroom chatroom = (PrivateTournamentChatroom) ContextManager.getPrivateTournamentMgr().getChatMgr().findChatroomByID(this.chatroomID);
            if(chatroom != null){
                if(chatroom.addParticipant(playerID)){
                    ContextManager.getPrivateTournamentMgr().getChatMgr().saveChatroom(chatroom);
                }
            }
        }
        return plaRank;
    }
}
