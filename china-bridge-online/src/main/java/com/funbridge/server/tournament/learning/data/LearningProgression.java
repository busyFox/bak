package com.funbridge.server.tournament.learning.data;

import com.funbridge.server.tournament.learning.TourLearningMgr;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Document(collection="learning_progression")
public class LearningProgression {
    @Id
    public long playerID;

    public Map<String, LearningProgressionElement> elementMap = new HashMap<>();
    public int nbDealFinish = 0;
    public String lastKeyUpdate = null;
    public Date dateLastUpdate;

    public static String buildKey(String sb, int chapter, int deal) {
        return sb+"-"+chapter+"-"+deal;
    }

    /**
     * Update progression. Return true if object changed
     * @param sb
     * @param chapter
     * @param deal
     * @param status
     * @return
     */
    public boolean addElement(String sb, int chapter, int deal, int status, int step) {
        boolean hasChanged = false;
        String key = buildKey(sb, chapter, deal);
        LearningProgressionElement element = elementMap.get(key);
        if (element == null) {
            element = new LearningProgressionElement();
            element.status = status;
            element.step = step;
            element.date = new Date();
            elementMap.put(key, element);
            dateLastUpdate = new Date();
            lastKeyUpdate = key;
            if (status == TourLearningMgr.DEAL_STATUS_FINISH) {
                nbDealFinish++;
            }
            hasChanged = true;
        } else {
            if (element.status <= status) {
                element.date = new Date();
                element.status = status;
                element.step = step;
                dateLastUpdate = new Date();
                lastKeyUpdate = key;
                if (status == TourLearningMgr.DEAL_STATUS_FINISH) {
                    nbDealFinish++;
                }
                hasChanged = true;
            }
        }
        return hasChanged;
    }
}
