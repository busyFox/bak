package com.funbridge.server.tournament.duel.data;

import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;

/**
 * Created by pserent on 12/08/2015.
 */
public class DuelGameMongoEvent extends AbstractMongoEventListener<DuelGame> {
    @Override
    public void onAfterConvert(AfterConvertEvent<DuelGame> event) {
        super.onAfterConvert(event);
        event.getSource().initData();
    }
}
