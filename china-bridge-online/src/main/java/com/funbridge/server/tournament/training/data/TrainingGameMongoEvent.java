package com.funbridge.server.tournament.training.data;

import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;

/**
 * Created by pserent on 06/07/2015.
 */
public class TrainingGameMongoEvent extends AbstractMongoEventListener<TrainingGame> {
    @Override
    public void onAfterConvert(AfterConvertEvent<TrainingGame> event) {
        super.onAfterConvert(event);
        event.getSource().initData();
    }
}
