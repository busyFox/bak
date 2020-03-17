package com.funbridge.server.player.cache;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by pserent on 18/08/2014.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.ANY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class PlayerCacheMap implements Serializable {
    public Map<Long, PlayerCache> map = new ConcurrentHashMap<>();

    public int size() {
        return map.size();
    }
}
