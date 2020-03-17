package com.funbridge.server.tournament.serie.data;

import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pserent on 27/04/2016.
 */
@WritingConverter
public class TourSerieTournamentWriteConverter implements Converter<TourSerieTournament, Document>{
    @Override
    public Document convert(TourSerieTournament source) {
        Document dbo = new Document();
        dbo.put("_id", source.getID());
        dbo.put("serie", source.getSerie());
        dbo.put("period", source.getPeriod());
        dbo.put("creationDateISO", source.getCreationDateISO());
        dbo.put("isFinished", source.isFinished());
        dbo.put("nbCreditPlayDeal", source.getNbCreditsPlayDeal());

        // migrations fields
        dbo.put("nbPlayer", source.getNbPlayers());
        dbo.put("nbPlayers", source.getNbPlayers());
        dbo.put("tsDateStart", source.getTsDateStart());
        dbo.put("startDate", source.getTsDateStart());
        dbo.put("tsDateEnd", source.getTsDateEnd());
        dbo.put("endDate", source.getTsDateEnd());

        List<Document> listDeal = new ArrayList<>();
        for (TourSerieDeal d : source.getListDeal()) {
            Document e = new Document();
            e.put("index", d.index);
            e.put("dealer", d.dealer);
            e.put("vulnerability", d.vulnerability);
            e.put("cards", d.cards);
            if (d.paramGenerator != null) {
                e.put("paramGenerator", d.paramGenerator);
            }
            if (d.engineParInfo != null) {
                e.put("engineParInfo", d.engineParInfo);
            }
            listDeal.add(e);
        }
        dbo.put("listDeal", listDeal);
        return dbo;
    }
}
