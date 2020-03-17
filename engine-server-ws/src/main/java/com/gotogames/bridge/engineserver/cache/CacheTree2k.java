package com.gotogames.bridge.engineserver.cache;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.cache2k.CacheEntry;
import org.cache2k.core.InternalCache;
import org.cache2k.event.CacheEntryCreatedListener;
import org.cache2k.event.CacheEntryExpiredListener;
import org.cache2k.event.CacheEntryOperationListener;
import org.cache2k.event.CacheEntryUpdatedListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CacheTree2k implements ICacheTree {
    private Logger log = LogManager.getLogger(this.getClass());
    private Cache<String, TreeBidCard> bidCardCache;
    private Cache<String, TreeBidInfo> bidInfoCache;

    public class CacheTree2kBidCardListener implements CacheEntryExpiredListener<String, TreeBidCard>, CacheEntryCreatedListener<String, TreeBidCard>, CacheEntryUpdatedListener<String, TreeBidCard> {
        @Override
        public void onEntryExpired(Cache<String, TreeBidCard> cache, CacheEntry<String, TreeBidCard> entry) {
            log.info("Expired BidCard on cache="+cache.getName()+" - entry="+entry.getKey());
        }

        @Override
        public void onEntryCreated(Cache<String, TreeBidCard> cache, CacheEntry<String, TreeBidCard> entry) {
            log.info("Created BidCard on cache="+cache.getName()+" - entry="+entry.getKey());
        }

        @Override
        public void onEntryUpdated(Cache<String, TreeBidCard> cache, CacheEntry<String, TreeBidCard> currentEntry, CacheEntry<String, TreeBidCard> entryWithNewData) {
            log.info("Updated BidCard on cache="+cache.getName()+" - currentEntry="+currentEntry.getKey()+" - entryWithNewData="+entryWithNewData.getKey());
        }
    }

    public class CacheTree2kBidInfoListener implements CacheEntryExpiredListener<String, TreeBidInfo> {
        @Override
        public void onEntryExpired(Cache<String, TreeBidInfo> cache, CacheEntry<String, TreeBidInfo> entry) {
            log.info("Expired BidInfo on cache="+cache.getName()+" - entry="+entry.getKey());
        }
    }

    @Override
    public void init() {
        bidCardCache = new Cache2kBuilder<String, TreeBidCard>(){}
                .name("cacheBidCard")
                .eternal(false)
                .entryCapacity(Long.MAX_VALUE)
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .addListener(new CacheTree2kBidCardListener())
                .build();
        bidInfoCache = new Cache2kBuilder<String, TreeBidInfo>(){}
                .name("cacheBidInfo")
                .eternal(false)
                .entryCapacity(Long.MAX_VALUE)
                .expireAfterWrite(2, TimeUnit.MINUTES)
                //.addListener(new CacheTree2kBidInfoListener())
                .build();
    }

    @Override
    public void destroy() {
        bidCardCache.clearAndClose();
        bidInfoCache.clearAndClose();
    }

    @Override
    public String getCacheInfoBidInfo() {
        return ((InternalCache) bidInfoCache).getInfo().toString();
    }

    @Override
    public String getCacheInfoBidCard() {
        return ((InternalCache) bidCardCache).getInfo().toString();
    }

    @Override
    public void clearAllTreeBidCard() {
        bidCardCache.clear();
    }

    @Override
    public void clearAllTreeBidInfo() {
        bidInfoCache.clear();
    }

    @Override
    public TreeBidCard getTreeBidCard(String treeKey) {
        return bidCardCache.get(treeKey);
    }

    @Override
    public TreeBidCard addTreeBidCard(String treeKey, TreeBidCard tree) {
        bidCardCache.put(treeKey, tree);
        return tree;
    }

    @Override
    public TreeBidCard removeTreeBidCard(String treeKey) {
        return bidCardCache.peekAndRemove(treeKey);
    }

    @Override
    public int getNbTreeBidCard() {
        return ((InternalCache) bidCardCache).getTotalEntryCount();
    }

    @Override
    public long getNbNodeBidCard() {
        long nbNodes = 0;
        for (TreeBidCard e : bidCardCache.asMap().values()) {
            nbNodes += e.nbNode;
        }
        return nbNodes;
    }

    @Override
    public TreeBidInfo getTreeBidInfo(String treeKey) {
        return bidInfoCache.get(treeKey);
    }

    @Override
    public TreeBidInfo addTreeBidInfo(String treeKey, TreeBidInfo tree) {
        return bidInfoCache.peekAndPut(treeKey, tree);
    }

    @Override
    public TreeBidInfo removeTreeBidInfo(String treeKey) {
        return bidInfoCache.peekAndRemove(treeKey);
    }

    @Override
    public List<TreeBidInfo> getListTreeBidInfo() {
        return new ArrayList<>(bidInfoCache.asMap().values());
    }

    @Override
    public int getNbTreeBidInfo() {
        return ((InternalCache) bidInfoCache).getTotalEntryCount();
    }

    @Override
    public long getNbNodeBidInfo() {
        long nbNodes = 0;
        for (TreeBidInfo e : bidInfoCache.asMap().values()) {
            nbNodes += e.nbNode;
        }
        return nbNodes;
    }
}
