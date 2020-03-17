package com.funbridge.server.Utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import com.funbridge.server.common.FBConfiguration;

public class RedisUtils {
    protected static Logger log = LogManager.getLogger();

    private static String HOST = FBConfiguration.getInstance().getStringValue("redis.host","47.99.237.101");
    private static int PORT = FBConfiguration.getInstance().getIntValue("redis.port",60379) ;
    private static String PWD =  FBConfiguration.getInstance().getStringValue("redis.password","Smzyredis2019") ;
    private static int MAX_ACTIVE = FBConfiguration.getInstance().getIntValue("redis.maxActive",1024);
    private static int MAX_IDLE = FBConfiguration.getInstance().getIntValue("redis.poolMaxIdle",5);
    private static int MAX_WAIT = FBConfiguration.getInstance().getIntValue("redis.maxWaitTime",10000);
    private static int TIMEOUT = FBConfiguration.getInstance().getIntValue("redis.outTime",10000);
    private static boolean TEST_ON_BORROW = true;

    private static JedisPool jedisPool = null;
    public static final int DEFAULT_DATABASE = FBConfiguration.getInstance().getIntValue("redis.database",0);


    static {

        try {

            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxTotal(MAX_ACTIVE);
            config.setMaxIdle(MAX_IDLE);
            config.setMaxWaitMillis(MAX_WAIT);
            config.setTestOnBorrow(TEST_ON_BORROW);
            jedisPool = new JedisPool(config, HOST, PORT, TIMEOUT, PWD, DEFAULT_DATABASE);

        } catch (Exception e) {

            e.printStackTrace();
        }

    }

    public synchronized static Jedis getJedis() {

        try {

            if (jedisPool != null) {
                Jedis resource = jedisPool.getResource();
                log.info("redis--server is running : "+resource.ping());
                return resource;
            } else {
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    /***
     *
     * 释放资源
     */

    public static void returnResource(final Jedis jedis) {
        if(jedis != null) {
            jedis.close();
        }

    }
}
