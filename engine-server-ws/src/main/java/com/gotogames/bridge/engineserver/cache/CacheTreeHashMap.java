package com.gotogames.bridge.engineserver.cache;

import com.gotogames.bridge.engineserver.common.EngineConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CacheTreeHashMap implements ICacheTree {
    private Logger log = LogManager.getLogger(this.getClass());

    // map a tree for bid and card request. A tree for each deal is created and stored in a HashMap. The key to retrieve this tree is the string composed of deal + options
    private ConcurrentHashMap<String, TreeBidCard> mapTreeBidCard = new ConcurrentHashMap<String, TreeBidCard>();
    // map a tree for bid info request. A tree for each deal is created and stored in a HashMap. The key to retrieve this tree is the string composed of deal + options
    private ConcurrentHashMap<String, TreeBidInfo> mapTreeBidInfo = new ConcurrentHashMap<String, TreeBidInfo>();

    private final ScheduledExecutorService schedulerPurgeTree = Executors.newScheduledThreadPool(1);

    /**
     * Task object to purge tree
     * @author pascal
     *
     */
    private class PurgeTreeTask implements Runnable {

        @Override
        public void run() {
            int purgeEnable = EngineConfiguration.getInstance().getIntValue("tree.purge", 1);
            int purgeTimeout = EngineConfiguration.getInstance().getIntValue("tree.purge.timeout", 86400)*1000;
            if (purgeEnable == 1 && purgeTimeout > 0) {
                long curTS = System.currentTimeMillis();

                int nbTreeBidCard = 0;
                int nbTreeBidInfo = 0;
                // purge tree bidCard
                long ts = System.currentTimeMillis();
                List<TreeBidCard> listTreeBidCard = getListTreeBidCard();
                for (TreeBidCard t : listTreeBidCard) {
                    if (((curTS - t.getTsLastConsult()) > purgeTimeout) &&
                            ((curTS - t.getTsLastAdd()) > purgeTimeout)) {
                        if (deleteTreeBidCardForKey(t.getTreeDealKey())) {
                            nbTreeBidCard++;
                            if (log.isDebugEnabled()) {
                                log.debug("Success to delete tree bid card with key=" + t.getTreeDealKey());
                            }
                        } else {
                            log.error("Fail to delete tree bid card with key="+t.getTreeDealKey());
                        }
                    }
                }
                if (nbTreeBidCard > 0) {
                    log.warn("Nb treeBidCard remove:" + nbTreeBidCard + " - ts=" + (System.currentTimeMillis() - ts));
                }
                // purge tree bidInfo
                ts = System.currentTimeMillis();
                List<TreeBidInfo> listTreeBidInfo = getListTreeBidInfo();
                for (TreeBidInfo t : listTreeBidInfo) {
                    if (((curTS - t.getTsLastConsult()) > purgeTimeout) &&
                            ((curTS - t.getTsLastAdd()) > purgeTimeout)) {
                        if (deleteTreeBidInfoForKey(t.getTreeDealKey())) {
                            nbTreeBidInfo++;
                            if (log.isDebugEnabled()) {
                                log.debug("Success to delete tree bid info with key=" + t.getTreeDealKey());
                            }
                        } else {
                            log.error("Fail to delete tree bid info with key="+t.getTreeDealKey());
                        }
                    }
                }
                if (nbTreeBidInfo > 0) {
                    log.warn("Nb treeBidInfo remove:" + nbTreeBidInfo + " - ts=" + (System.currentTimeMillis() - ts));
                }
            } else {
                log.info("Purge disable !");
            }
        }
    }

    @Override
    public void init() {
        int periodPurge = EngineConfiguration.getInstance().getIntValue("tree.purge.period", 30);
        if (periodPurge > 0) {
            schedulerPurgeTree.scheduleAtFixedRate(new PurgeTreeTask(), 0, periodPurge, TimeUnit.SECONDS);
            log.warn("Start scheduler purge with period="+periodPurge+" (seconds)");
        } else {
            log.warn("periodPurge is not valid : "+periodPurge);
        }
    }

    @Override
    public void destroy() {
        log.info("Shutdown purge scheduler");
        schedulerPurgeTree.shutdown();
        try {
            if (schedulerPurgeTree.awaitTermination(30, TimeUnit.SECONDS)) {
                schedulerPurgeTree.shutdownNow();
            }
        } catch (InterruptedException e) {
            schedulerPurgeTree.shutdownNow();
        }
        // remove all tree
        clearAll();
    }

    @Override
    public String getCacheInfoBidCard() {
        return null;
    }

    @Override
    public String getCacheInfoBidInfo() {
        return null;
    }

    @Override
    public TreeBidCard getTreeBidCard(String treeKey) {
        return mapTreeBidCard.get(treeKey);
    }

    @Override
    public TreeBidInfo getTreeBidInfo(String treeKey) {
        return mapTreeBidInfo.get(treeKey);
    }

    @Override
    public void clearAllTreeBidCard() {
        mapTreeBidCard.clear();
    }

    @Override
    public void clearAllTreeBidInfo() {
        mapTreeBidInfo.clear();
    }

    @Override
    public TreeBidCard addTreeBidCard(String treeKey, TreeBidCard tree) {
        return mapTreeBidCard.putIfAbsent(treeKey, tree);
    }

    @Override
    public TreeBidInfo addTreeBidInfo(String treeKey, TreeBidInfo tree) {
        return mapTreeBidInfo.putIfAbsent(treeKey, tree);
    }

    @Override
    public TreeBidCard removeTreeBidCard(String treeKey) {
        return mapTreeBidCard.remove(treeKey);
    }

    @Override
    public TreeBidInfo removeTreeBidInfo(String treeKey) {
        return mapTreeBidInfo.remove(treeKey);
    }

    @Override
    public int getNbTreeBidCard() {
        return mapTreeBidCard.size();
    }

    @Override
    public int getNbTreeBidInfo() {
        return mapTreeBidInfo.size();
    }

    @Override
    public long getNbNodeBidCard() {
        long nb = 0;
        for (TreeBidCard t : mapTreeBidCard.values()) {
            nb += t.getNbNode();
        }
        return nb;
    }

    @Override
    public long getNbNodeBidInfo() {
        long nb = 0;
        for (TreeBidInfo t : mapTreeBidInfo.values()) {
            nb += t.getNbNode();
        }
        return nb;
    }

    /**
     * Clear all data from map tree bid card and map tree bid info
     */
    public void clearAll() {
        log.info("Clear map tree bid card");
        mapTreeBidCard.clear();
        log.info("Clear map tree bid info");
        mapTreeBidInfo.clear();
    }

    /**
     * Return list of tree
     * @return
     */
    public List<TreeBidCard> getListTreeBidCard() {
        return new ArrayList<TreeBidCard>(mapTreeBidCard.values());
    }

    /**
     * Return list of tree
     * @return
     */
    @Override
    public List<TreeBidInfo> getListTreeBidInfo() {
        return new ArrayList<TreeBidInfo>(mapTreeBidInfo.values());
    }

    /**
     * Remove all tree associated to this deal for bid or card. If backup option is enable, save it to file before delete it
     * @param deal
     * @return
     */
    public boolean deleteTreeBidCardForKey(String treeKey) {
        TreeBidCard t = mapTreeBidCard.remove(treeKey);
        if (t != null) {
            t = null;
            return true;
        }
        return false;
    }

    /**
     * Remove all tree associated to this deal for bid info.
     * @param deal
     * @return
     */
    public boolean deleteTreeBidInfoForKey(String treeKey) {
        TreeBidInfo t2 = mapTreeBidInfo.remove(treeKey);
        if (t2 != null) {
            t2 = null;
            return true;
        }
        return false;
    }
}
