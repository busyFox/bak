package com.funbridge.server.ws.engine;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.FunbridgeMgr;
import com.funbridge.server.ws.FBWSResponse;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by pserent on 13/04/2016.
 */
@Service(value = "engineService")
@Scope(value = "singleton")
@Path("/engine")
public class EngineService extends FunbridgeMgr {
    // map asynID => StepData
    private ConcurrentHashMap<String, PlayGameStepData> mapStepData = new ConcurrentHashMap<>();

    @PostConstruct
    @Override
    public void init() {

    }

    @PreDestroy
    @Override
    public void destroy() {

    }

    @Override
    public void startUp() {

    }

    public void putStepData(PlayGameStepData stepData) {
        if (stepData != null) {
            mapStepData.put(stepData.asyncID, stepData);
        }
    }

    public void removeStepData(String asyncID) {
        mapStepData.remove(asyncID);
    }

    /**
     * Clear data with timestamp < tsBefore
     *
     * @param tsBefore
     * @return
     */
    public int clearOldStepData(long tsBefore) {
        int nbRemove = 0;
        Collection<PlayGameStepData> datas = mapStepData.values();
        for (Iterator<PlayGameStepData> it = datas.iterator(); it.hasNext(); ) {
            PlayGameStepData e = it.next();
            if (e.timestamp < tsBefore) {
                it.remove();
                nbRemove++;
            }
        }
        return nbRemove;
    }

    public Map<String, PlayGameStepData> getMapStepData() {
        return mapStepData;
    }

    @POST
    @Path("/setResult")
    @Consumes("application/json")
    @Produces("application/json")
    public FBWSResponse setResult(SetResultParam param) {
        FBWSResponse response = new FBWSResponse();
        if (log.isDebugEnabled()) {
            log.debug("param=" + param);
        }
        for (String asyncID : param.listAsyncID) {
            PlayGameStepData stepData = mapStepData.get(asyncID);
            if (stepData != null) {
                mapStepData.remove(asyncID);
                switch (stepData.requestType) {
                    case Constantes.ENGINE_REQUEST_TYPE_BID:
                    case Constantes.ENGINE_REQUEST_TYPE_CARD: {
                        if (stepData.gameMgr != null) {
                            stepData.gameMgr.playGameStep2(stepData, param.result);
                        } else {
                            log.error("No gameMgr valid for stepData=" + stepData);
                        }
                        break;
                    }
                    case Constantes.ENGINE_REQUEST_TYPE_PAR: {
                        if (stepData.gameMgr != null) {
                            stepData.gameMgr.answerPar(stepData, param.result);
                        } else {
                            log.error("No gameMgr valid for stepData=" + stepData);
                        }
                        break;
                    }
                    case Constantes.ENGINE_REQUEST_TYPE_ADVICE: {
                        if (stepData.gameMgr != null) {
                            stepData.gameMgr.answerAdvice(stepData, param.result);
                        } else {
                            log.error("No gameMgr valid for stepData=" + stepData);
                        }
                        break;
                    }
                    default: {
                        log.error("Request type not supported - stepData=" + stepData);
                        break;
                    }
                }

            } else {
                if (log.isDebugEnabled()) {
                    log.debug("No step data found for asyncID=" + asyncID);
                }
            }
        }
        return response;
    }
}
