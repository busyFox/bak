package com.gotogames.bridge.engineserver.cache;

import com.gotogames.bridge.engineserver.common.EngineConfiguration;
import com.gotogames.common.tools.NumericalTools;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.inject.Singleton;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component(value="redisCache")
@Singleton
public class RedisCache {
    private RedisTemplate<String, String> redisTemplate;

    @Resource(name = "jedisConnectionFactory")
    JedisConnectionFactory jedisConnectionFactory;

    private ConcurrentLinkedQueue<Long> meterTimeCacheGet = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<Long> meterTimeCacheSet = new ConcurrentLinkedQueue<>();
    private Logger log = LogManager.getLogger(this.getClass());

    public RedisCache() {
    }

    @PostConstruct
    public void init() {
        redisTemplate = new RedisTemplate<String, String>();
        redisTemplate.setConnectionFactory(jedisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.afterPropertiesSet();
    }

    @PreDestroy
    public void destroy() {
    }

    public String getValue(String key) {
        long ts = System.currentTimeMillis();
        String value = null;
        try {
            value = redisTemplate.boundValueOps(key).get();
        } catch (Exception e) {
            log.error("Failed to getValue for key="+key, e);
        }
        addMeterTimeCacheGet(System.currentTimeMillis() - ts);
        return value;
    }

    public void setValue(String key, String value) {
        long ts = System.currentTimeMillis();
        try {
            redisTemplate.boundValueOps(key).set(value);
        } catch (Exception e) {
            log.error("Failed to setValue for key="+key+" - value="+value, e);
        }
        addMeterTimeCacheSet(System.currentTimeMillis() - ts);
    }

    public boolean isMeterTimeEnable() {
        return EngineConfiguration.getInstance().getIntValue("general.cacheRedis.meterTimeEnable", 1) ==1;
    }

    public int getMeterTimeMaxSize() {
        return EngineConfiguration.getInstance().getIntValue("general.cacheRedis.meterTimeMaxSize", 1000);
    }

    private void addMeterTimeCacheGet(long ts) {
        if (isMeterTimeEnable()) {
            meterTimeCacheGet.add(ts);
            if (meterTimeCacheGet.size() > 2 * getMeterTimeMaxSize()) {
                synchronized (meterTimeCacheGet) {
                    while (meterTimeCacheGet.size() > getMeterTimeMaxSize()) {
                        meterTimeCacheGet.remove();
                    }
                }
            }
        }
    }

    private void addMeterTimeCacheSet(long ts) {
        if (isMeterTimeEnable()) {
            meterTimeCacheSet.add(ts);
            if (meterTimeCacheSet.size() > 2 * getMeterTimeMaxSize()) {
                synchronized (meterTimeCacheSet) {
                    while (meterTimeCacheSet.size() > getMeterTimeMaxSize()) {
                        meterTimeCacheSet.remove();
                    }
                }
            }
        }
    }

    public double getMeterAverageTimeCacheGet() {
        try {
            Long[] tabTime = meterTimeCacheGet.toArray(new Long[meterTimeCacheGet.size()]);
            long total = 0;
            int nbProcess = 0;
            if (tabTime.length > 0) {
                while (true) {
                    total += tabTime[tabTime.length - 1 - nbProcess];
                    nbProcess++;
                    if (nbProcess >= tabTime.length) {
                        break;
                    }
                    if (nbProcess > getMeterTimeMaxSize()) {
                        break;
                    }
                }
            }
            if (nbProcess > 0) {
                return NumericalTools.round(total / nbProcess, 3);
            }
        } catch (Exception e) {
            log.error("Failed to compute average time cache !", e);
        }
        return 0;
    }

    public double getMeterAverageTimeCacheSet() {
        try {
            Long[] tabTime = meterTimeCacheSet.toArray(new Long[meterTimeCacheSet.size()]);
            long total = 0;
            int nbProcess = 0;
            if (tabTime.length > 0) {
                while (true) {
                    total += tabTime[tabTime.length - 1 - nbProcess];
                    nbProcess++;
                    if (nbProcess >= tabTime.length) {
                        break;
                    }
                    if (nbProcess > getMeterTimeMaxSize()) {
                        break;
                    }
                }
            }
            if (nbProcess > 0) {
                return NumericalTools.round(total / nbProcess, 3);
            }
        } catch (Exception e) {
            log.error("Failed to compute average time cache !", e);
        }
        return 0;
    }

    public Queue<Long> getMeterTimeCacheGet() {
        return meterTimeCacheGet;
    }

    public Queue<Long> getMeterTimeCacheSet() {
        return meterTimeCacheSet;
    }
}
