package com.funbridge.server.tournament.trainingpartner;

import com.funbridge.server.common.FunbridgeMgr;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Created by pserent on 29/03/2016.
 */
@Component(value = "trainingPartnerMgr")
public class TrainingPartnerMgr extends FunbridgeMgr {
    @Override
    @PreDestroy
    public void destroy() {

    }

    @Override
    @PostConstruct
    public void init() {

    }

    @Override
    public void startUp() {
        log.info("startUp");
    }
}
