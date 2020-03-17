package com.gotogames.bridge.engineserver.cache;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.CacheRuntimeConfiguration;
import org.ehcache.config.ResourceType;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;

import java.util.ArrayList;
import java.util.List;

public class CacheTreeEhcache implements ICacheTree {
    private CacheManager cacheManager;
    private Cache<String, TreeBidCard> bidCardCache;
    private Cache<String, TreeBidInfo> bidInfoCache;

    @Override
    public void init() {
        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build();
        cacheManager.init();
        bidCardCache = cacheManager.createCache("bidCardCache", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, TreeBidCard.class, ResourcePoolsBuilder.heap(10)));
        bidInfoCache = cacheManager.createCache("bidInfoCache", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, TreeBidInfo.class, ResourcePoolsBuilder.heap(10)));
    }

    @Override
    public void destroy() {
        bidCardCache.clear();
        bidInfoCache.clear();
        cacheManager.close();
    }

    @Override
    public String getCacheInfoBidCard() {
        bidCardCache.getRuntimeConfiguration().toString();
        return null;
    }

    @Override
    public String getCacheInfoBidInfo() {
        return null;
    }

    @Override
    public void clearAllTreeBidCard() {

    }

    @Override
    public void clearAllTreeBidInfo() {

    }

    @Override
    public TreeBidCard getTreeBidCard(String treeKey) {
        return bidCardCache.get(treeKey);
    }

    @Override
    public TreeBidCard addTreeBidCard(String treeKey, TreeBidCard tree) {
        return bidCardCache.putIfAbsent(treeKey, tree);
    }

    @Override
    public TreeBidCard removeTreeBidCard(String treeKey) {
        bidCardCache.remove(treeKey);
        return null;
    }

    @Override
    public int getNbTreeBidCard() {
        return (int)bidCardCache.getRuntimeConfiguration().getResourcePools().getPoolForResource(ResourceType.Core.HEAP).getSize();
    }

    @Override
    public long getNbNodeBidCard() {
        return 0;
    }

    @Override
    public TreeBidInfo getTreeBidInfo(String treeKey) {
        return bidInfoCache.get(treeKey);
    }

    @Override
    public TreeBidInfo addTreeBidInfo(String treeKey, TreeBidInfo tree) {
        return bidInfoCache.putIfAbsent(treeKey, tree);
    }

    @Override
    public TreeBidInfo removeTreeBidInfo(String treeKey) {
        bidInfoCache.remove(treeKey);
        return null;
    }

    @Override
    public List<TreeBidInfo> getListTreeBidInfo() {
        return new ArrayList<>();
    }

    @Override
    public int getNbTreeBidInfo() {
        return (int)bidInfoCache.getRuntimeConfiguration().getResourcePools().getPoolForResource(ResourceType.Core.HEAP).getSize();
    }

    @Override
    public long getNbNodeBidInfo() {
        return 0;
    }
}
