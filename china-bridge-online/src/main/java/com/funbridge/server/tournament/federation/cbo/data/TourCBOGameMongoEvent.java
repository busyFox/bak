package com.funbridge.server.tournament.federation.cbo.data;

import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;

/**
 * Created by ldelbarre on 21/12/2017.
 */
public class TourCBOGameMongoEvent extends AbstractMongoEventListener<TourCBOGame> {
    @Override
    public void onAfterConvert(AfterConvertEvent<TourCBOGame> event) {
        super.onAfterConvert(event);
        event.getSource().initData();
    }
}
