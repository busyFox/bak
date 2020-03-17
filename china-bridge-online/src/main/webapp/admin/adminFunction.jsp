<%@ page import="com.funbridge.server.common.Constantes" %>
<%@ page import="com.funbridge.server.common.ContextManager" %>
<%@ page import="com.funbridge.server.message.MessageMgr" %>
<%@ page import="com.funbridge.server.player.PlayerMgr" %>
<%@ page import="com.funbridge.server.player.data.Player" %>
<%@ page import="com.funbridge.server.player.data.PlayerLink" %>
<%@ page import="com.funbridge.server.tournament.serie.TourSerieMgr" %>
<%@ page import="com.funbridge.server.tournament.serie.data.TourSeriePlayer" %>
<%@ page import="com.funbridge.server.ws.player.WSPlayer" %>
<%@ page import="java.util.ArrayList" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%!
  PlayerMgr playerMgr = ContextManager.getPlayerMgr();
  MessageMgr messageMgr = ContextManager.getMessageMgr();

  public List<String> playerToWSPlayer(Player playerToTransform, Player playerAsk) {
    List<String> listTS = new ArrayList<String>();
    if (playerToTransform != null && playerAsk != null) {
      WSPlayer wsp = new WSPlayer();
      wsp.playerID = playerToTransform.getID();
      wsp.pseudo = playerToTransform.getNickname();
      if (playerToTransform.getID() == playerAsk.getID()) {
        wsp.avatar = playerToTransform.isAvatarPresent();
      }
      wsp.nbDealPlayed = playerToTransform.getNbPlayedDeals();
      long ts = System.currentTimeMillis();
      wsp.nbFriendsAndFollowers = playerMgr.countFriendAndFollowerForPlayer(playerToTransform.getID());
      wsp.nbFriends = playerMgr.countLinkForPlayerAndType(playerToTransform.getID(), Constantes.PLAYER_LINK_TYPE_FRIEND);
      wsp.nbFollowers = playerMgr.countFollowerForPlayer(playerToTransform.getID());
      listTS.add("CountFriend;"+(System.currentTimeMillis()-ts));
      ts = System.currentTimeMillis();
      wsp.connected = ContextManager.getPresenceMgr().isSessionForPlayer(playerToTransform);
      listTS.add("connected;"+(System.currentTimeMillis()-ts));
      ts = System.currentTimeMillis();
      wsp.profile = playerMgr.playerToWSProfile(playerToTransform, playerAsk.getID());
      listTS.add("playerToWSProfile;"+(System.currentTimeMillis()-ts));
      ts = System.currentTimeMillis();
      wsp.setDuelStat(playerMgr.getDuelStat(playerToTransform.getID()));
      listTS.add("getDuelStat;" +(System.currentTimeMillis()-ts));

      // add serie status
      ts = System.currentTimeMillis();
      wsp.serieStatus = ContextManager.getTourSerieMgr().buildSerieStatusForPlayer(ContextManager.getPlayerCacheMgr().getOrLoadPlayerCache(playerToTransform.getID()));
      listTS.add("serieStatus;" +(System.currentTimeMillis()-ts));
      ts = System.currentTimeMillis();
      TourSeriePlayer tsp = ContextManager.getTourSerieMgr().getTourSeriePlayer(playerToTransform.getID());
      if (tsp != null && tsp.getBestSerie() != null && tsp.getSerie().length() > 0) {
        wsp.serieBest = tsp.getBestSerie();
        wsp.serieBestRank = tsp.getBestRank();
        wsp.serieBestPeriodStart = TourSerieMgr.transformPeriodID2TS(tsp.getBestPeriod(), true);
        wsp.serieBestPeriodEnd = TourSerieMgr.transformPeriodID2TS(tsp.getBestPeriod(), false);
      }
      listTS.add("serieBest;" +(System.currentTimeMillis()-ts));
      // player to transform != player session => view CV player
      if (playerToTransform.getID() != playerAsk.getID()) {
        ts = System.currentTimeMillis();
        wsp.relationMask = playerMgr.getLinkMaskBetweenPlayer(playerAsk.getID(), playerToTransform.getID());
        listTS.add("getLinkMaskBetweenPlayer;" +(System.currentTimeMillis()-ts));
        if ((wsp.relationMask & Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING) == Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING) {
          PlayerLink pl = playerMgr.getLinkBetweenPlayer(playerAsk.getID(), playerToTransform.getID());
          if (pl != null) {
            wsp.requestMessage = pl.getMessage();
          }
        }
        // check relation type FRIEND
        if ((wsp.relationMask & Constantes.PLAYER_LINK_TYPE_FRIEND) == Constantes.PLAYER_LINK_TYPE_FRIEND) {
          // message only between friend !
          PlayerLink pl = playerMgr.getLinkBetweenPlayer(playerAsk.getID(), playerToTransform.getID());
          if (pl != null) {
            ts = System.currentTimeMillis();
            wsp.nbMessageNotRead = messageMgr.getNbMessageNotReadForPlayerAndSender(playerAsk.getID(), playerToTransform.getID(), pl.getDateMessageReset(playerAsk.getID()));
            wsp.dateLastMessage = pl.getDateLastMessage();
            listTS.add("Messages;" +(System.currentTimeMillis()-ts));
          }
          // duelHistory only between friend !
          ts = System.currentTimeMillis();
          wsp.duelHistory = playerMgr.getDuelHistoryBetweenPlayers(playerAsk, playerToTransform, true);
          listTS.add("duelHistory;" +(System.currentTimeMillis()-ts));
          // trainingPartner only between friend !
          ts = System.currentTimeMillis();
          wsp.trainingPartnerStatus = playerMgr.getTrainingPartnerStatusBetweenPlayers(playerAsk.getID(), playerToTransform.getID());
          listTS.add("trainingPartnerStatus;" +(System.currentTimeMillis()-ts));

        }
      }
    }
    return listTS;
  }
%>
