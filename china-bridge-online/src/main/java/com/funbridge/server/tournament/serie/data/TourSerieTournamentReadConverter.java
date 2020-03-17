package com.funbridge.server.tournament.serie.data;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by pserent on 27/04/2016.
 */
@ReadingConverter
public class TourSerieTournamentReadConverter implements Converter<Document, TourSerieTournament>{
    @Override
    public TourSerieTournament convert(Document source) {
        TourSerieTournament tour = new TourSerieTournament();
        tour.setID((ObjectId) source.get("_id"));
        tour.setSerie((String) source.get("serie"));
        tour.setPeriod((String) source.get("period"));

        if(source.containsKey("startDate")){
            tour.setTsDateStart((Long) source.get("startDate"));
        } else if (source.containsKey("tsDateStart")) {
            tour.setTsDateStart((Long) source.get("tsDateStart"));
        }

        if(source.containsKey("endDate")){
            tour.setTsDateEnd((Long) source.get("endDate"));
        }else if (source.containsKey("tsDateEnd")) {
            tour.setTsDateEnd((Long) source.get("tsDateEnd"));
        }
        tour.setCreationDateISO((Date) source.get("creationDateISO"));

        // Process during field migration
        tour.setNbPlayers(source.containsKey("nbPlayers") ? (Integer)source.get("nbPlayers") : (Integer)source.get("nbPlayer"));

        tour.setFinished((Boolean)source.get("isFinished"));
        tour.setNbCreditsPlayDeal((Integer)source.get("nbCreditPlayDeal"));
        ArrayList<Document> dbList = (ArrayList)source.get("listDeal");
        List<TourSerieDeal> deals = new ArrayList<>();
        for (Document e : dbList) {
            TourSerieDeal deal = new TourSerieDeal();
            deal.index = e.getInteger("index");
            deal.dealer = e.getString("dealer").charAt(0);
            deal.vulnerability = e.getString("vulnerability").charAt(0);
            deal.cards = e.getString("cards");
            deal.paramGenerator = e.getString("paramGenerator");
            deal.engineParInfo = e.getString("engineParInfo");
            deals.add(deal);
        }
        tour.setListDeal(deals);
        return tour;
    }
}
