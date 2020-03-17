package com.funbridge.server.tournament.serie.data.event;

import com.funbridge.server.tournament.serie.data.TourSerieGame;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;

/**
 * Created by pserent on 12/06/2014.
 * Intercept Mongo events on TourSerieGame bean
 */
public class TourSerieGameMongoEvent extends AbstractMongoEventListener<TourSerieGame> {

    @Override
    public void onAfterConvert(AfterConvertEvent<TourSerieGame> event) {
        super.onAfterConvert(event);
        event.getSource().initData();
    }

}
