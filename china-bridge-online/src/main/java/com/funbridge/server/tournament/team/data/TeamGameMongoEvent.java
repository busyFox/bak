package com.funbridge.server.tournament.team.data;

import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;

/**
 * Created by pserent on 05/12/2016.
 */
public class TeamGameMongoEvent extends AbstractMongoEventListener<TeamGame> {
    @Override
    public void onAfterConvert(AfterConvertEvent<TeamGame> event) {
        super.onAfterConvert(event);
        event.getSource().initData();
    }
}
