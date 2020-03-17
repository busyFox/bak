package com.funbridge.server.tournament.serie.data;

import org.bson.Document;

/**
 * Created by pserent on 06/06/2017.
 */
public class TourSerieGameLoadData {
    public String gameID;
    public long playerID;
    public boolean finished;
    public int score;
    public String contract;
    public int contractType;
    public long lastDate;
    public int dealIndex;
    public int tricks;
    public String declarer;
    public String cards;

    public String toString() {
        return "gameID="+gameID+" - playerID="+playerID+" - finished="+finished+" - dealIndex="+dealIndex;
    }

    public String getBegins() {
        if (cards != null && cards.length() >= 3) {
            return cards.substring(0, 3);
        }
        return null;
    }

    public static TourSerieGameLoadData loadFromDBObject(Document e) {
        if (e != null) {
            if (e.getObjectId("_id") != null) {
                TourSerieGameLoadData data = new TourSerieGameLoadData();
                data.gameID = e.getObjectId("_id").toString();
                if (e.containsKey("playerID")) {
                    data.playerID = e.getLong("playerID");
                }
                if (e.containsKey("finished")) {
                    data.finished = e.getBoolean("finished");
                }
                if (e.containsKey("score")) {
                    data.score = e.getInteger("score");
                }
                if (e.containsKey("contract")) {
                    data.contract = e.getString("contract");
                }
                if (e.containsKey("contractType")) {
                    data.contractType = e.getInteger("contractType");
                }
                if (e.containsKey("lastDate")) {
                    data.lastDate = e.getLong("lastDate");
                }
                if (e.containsKey("dealIndex")) {
                    data.dealIndex = e.getInteger("dealIndex");
                }
                if (e.containsKey("tricks")) {
                    data.tricks = e.getInteger("tricks");
                }
                if (e.containsKey("declarer")) {
                    data.declarer = e.getString("declarer");
                }
                if (e.containsKey("cards")) {
                    data.cards = e.getString("cards");
                }
                return data;
            }
        }
        return null;
    }
}
