package com.funbridge.server.tournament.serie.data.event;

import com.funbridge.server.tournament.serie.data.SerieEasyChallengeGame;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;

public class SerieEasyChallengeGameMongoEvent extends AbstractMongoEventListener<SerieEasyChallengeGame> {

    @Override
    public void onAfterConvert(AfterConvertEvent<SerieEasyChallengeGame> event) {
        super.onAfterConvert(event);
        event.getSource().initData();
    }

}
