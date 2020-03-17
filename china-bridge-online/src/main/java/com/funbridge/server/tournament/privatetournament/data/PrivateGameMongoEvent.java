package com.funbridge.server.tournament.privatetournament.data;

import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;

/**
 * Created by pserent on 23/01/2017.
 */
public class PrivateGameMongoEvent extends AbstractMongoEventListener<PrivateGame> {
    @Override
    public void onAfterConvert(AfterConvertEvent<PrivateGame> event) {
        super.onAfterConvert(event);
        event.getSource().initData();
    }
}
