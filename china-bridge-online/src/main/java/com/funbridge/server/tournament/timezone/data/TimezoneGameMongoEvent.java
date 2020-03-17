package com.funbridge.server.tournament.timezone.data;

import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;

/**
 * Created by pserent on 06/07/2015.
 */
public class TimezoneGameMongoEvent extends AbstractMongoEventListener<TimezoneGame> {
    @Override
    public void onAfterConvert(AfterConvertEvent<TimezoneGame> event) {
        super.onAfterConvert(event);
        event.getSource().initData();
    }
}
