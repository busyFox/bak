package com.gotogames.bridge.engineserver.cache;

import java.util.List;

public interface ICacheTree {
    void init();
    void destroy();
    String getCacheInfoBidCard();
    String getCacheInfoBidInfo();
    void clearAllTreeBidCard();
    void clearAllTreeBidInfo();
    TreeBidCard getTreeBidCard(String treeKey);
    TreeBidCard addTreeBidCard(String treeKey, TreeBidCard tree);
    TreeBidCard removeTreeBidCard(String treeKey);
    int getNbTreeBidCard();
    long getNbNodeBidCard();
    TreeBidInfo getTreeBidInfo(String treeKey);
    TreeBidInfo addTreeBidInfo(String treeKey, TreeBidInfo tree);
    TreeBidInfo removeTreeBidInfo(String treeKey);
    List<TreeBidInfo> getListTreeBidInfo();
    int getNbTreeBidInfo();
    long getNbNodeBidInfo();
}
