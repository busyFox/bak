package com.funbridge.server.tournament.learning.data;

import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;

/**
 * Created by pserent on 03/02/2016.
 */
public class LearningGameMongoEvent extends AbstractMongoEventListener<LearningGame> {
    @Override
    public void onAfterConvert(AfterConvertEvent<LearningGame> event) {
        super.onAfterConvert(event);
        event.getSource().initData();
    }
}
