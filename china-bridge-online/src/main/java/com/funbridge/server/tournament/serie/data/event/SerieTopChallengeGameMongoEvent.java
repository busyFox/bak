package com.funbridge.server.tournament.serie.data.event;

import com.funbridge.server.tournament.serie.data.SerieTopChallengeGame;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;

/**
 * Created by pserent on 28/04/2016.
 */
public class SerieTopChallengeGameMongoEvent extends AbstractMongoEventListener<SerieTopChallengeGame> {

    @Override
    public void onAfterConvert(AfterConvertEvent<SerieTopChallengeGame> event) {
        super.onAfterConvert(event);
        event.getSource().initData();
    }

}
