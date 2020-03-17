package com.gotogames.bridge.engineserver.cache;

import com.gotogames.bridge.engineserver.common.Constantes;
import com.gotogames.bridge.engineserver.common.EngineConfiguration;
import com.gotogames.bridge.engineserver.common.LogStatMgr;
import com.gotogames.common.bridge.BridgeTransformData;
import com.gotogames.common.bridge.GameBridgeRule;
import com.gotogames.common.lock.LockWeakString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.List;

@Component(value="treeMgr")
@Scope(value="singleton")
public class TreeMgr {
    @Resource(name="logStatMgr")
    private LogStatMgr logStatMgr;
	private LockWeakString lockTree = new LockWeakString();
	private Logger log = LogManager.getLogger(this.getClass());
	private ICacheTree cacheTree;
    @Resource(name="redisCache")
	private RedisCache redisCache;

	/**
	 * Call to init tree manager
	 */
	@PostConstruct
	public void init() {
	    // TODO create cacheTreeMgr using HashMap or EhCache
        cacheTree = new CacheTreeHashMap();
	    cacheTree.init();
	}
	
	/**
	 * Call to destroy tree manager
	 */
	@PreDestroy
	public void destroy() {
	    cacheTree.destroy();
	}
	
	/**
	 * Clear all data from map tree bid card and map tree bid info
	 */
	public void clearAll() {
		log.info("Clear map tree bid card");
		cacheTree.clearAllTreeBidCard();
		log.info("Clear map tree bid info");
		cacheTree.clearAllTreeBidInfo();
	}
	
	/**
	 * Return list of tree
	 * @return
	 */
	public List<TreeBidInfo> getListTreeBidInfo() {
		return cacheTree.getListTreeBidInfo();
	}
	
	/**
	 * Return the tree associated to this key (deal + options)
	 * @param treeKey
	 * @return
	 */
	public TreeBidCard getTreeBidCard(String treeKey) {
		return cacheTree.getTreeBidCard(treeKey);
	}
	
	/**
	 * Return the tree associated to this key (deal + options)
	 * @param treeKey
	 * @return
	 */
	public TreeBidInfo getTreeBidInfo(String treeKey) {
		return cacheTree.getTreeBidInfo(treeKey);
	}
	
	/**
	 * Return the number of tree
	 * @return
	 */
	public int getNbTreeBidCard() {
		return cacheTree.getNbTreeBidCard();
	}
	
	/**
	 * Return the number of tree
	 * @return
	 */
	public int getNbTreeBidInfo() {
		return cacheTree.getNbTreeBidInfo();
	}
	
	/**
	 * Return the total number of node in all tree. Sum of number node for each tree
	 * @return
	 */
	public long getCacheNbNodeBidCard() {
	    return cacheTree.getNbNodeBidCard();
	}
	
	/**
	 * Return the total number of node in all tree. Sum of number node for each tree
	 * @return
	 */
	public long getCacheNbNodeBidInfo() {
	    return cacheTree.getNbNodeBidInfo();
	}

	public String getDataRedis(TreeData treeData) {
	    String key = treeData.getCompleteKey();
	    String value = redisCache.getValue(key);
        return value;
    }

    /**
	 * Get the next result for this data
	 * @param data
	 * @return null or empty string if not found else values to play
	 */
	public String getCacheData(String data) {
		TreeData treeData = createTreeData(data);
		String valResult = null;
        if (treeData != null && EngineConfiguration.getInstance().getIntValue("general.useCache", 0) == 1) {
            if (EngineConfiguration.getInstance().getIntValue("general.cacheRedis", 0) == 1) {
                valResult = getDataRedis(treeData);
            } else {
                if (EngineConfiguration.getInstance().getIntValue("general.cacheRedis.testGet", 0) == 1) {
                    getDataRedis(treeData);
                }
                // request BID or CARD
                if (treeData.isRequestBidCard()) {
                    // find tree for this deal
                    TreeBidCard tree = cacheTree.getTreeBidCard(treeData.getTreeDealKey());
                    if (tree != null) {
                        String valCompressed = null;
                        synchronized (tree) {
                            tree.setTsLastConsult(System.currentTimeMillis());
                            // find next result in this tree
                            String keyCompressed = treeData.getConventions() + BridgeTransformData.transformGame(treeData.getGame());
                            valCompressed = tree.getValueRobot(keyCompressed);
                        }
                        // value found in tree
                        if (valCompressed != null && valCompressed.length() > 0) {
                            int idx = 0;
                            String g = treeData.getGame();
                            String val = "";
                            // need to transform compressed value to bridge value
                            boolean bTransformOK = true;
                            while (bTransformOK && idx < valCompressed.length()) {
                                String temp = null;
                                boolean isEndBids = GameBridgeRule.isEndBids(g);
                                if (isEndBids) {
                                    temp = BridgeTransformData.convertBridgeCard2String(valCompressed.charAt(idx));
                                } else {
                                    temp = BridgeTransformData.convertBridgeBid2String(valCompressed.charAt(idx));
                                }
                                if (temp == null) {
                                    log.error("Error in convertion char to string : char=" + valCompressed.charAt(idx) + " int value=" + (int) valCompressed.charAt(idx) + " - endBid=" + GameBridgeRule.isEndBids(g) + " - data=" + data + " - treeData=" + treeData);
                                    bTransformOK = false;
                                }
                                idx++;
                                if (valCompressed.charAt(idx) == '1') {
                                    if (isEndBids) {
                                        // flag to indicate spread enable
                                        temp += "s";
                                    } else {
                                        // flag to indicate alert on bid
                                        temp += "a";
                                    }
                                }
                                idx++;
                                val += temp;
                                g += temp;
                            }
                            // if tranformation OK => return value else null
                            if (bTransformOK) {
                                valResult = val;
                            }
                        }
                    }
                }
                // request BID INFO
                else if (treeData.isRequestBidInfo()) {
                    // find tree for this deal
                    TreeBidInfo tree = cacheTree.getTreeBidInfo(treeData.getTreeDealKey());
                    if (tree != null) {
                        synchronized (tree) {
                            tree.setTsLastConsult(System.currentTimeMillis());
                            String keyCompressed = treeData.getConventions() + BridgeTransformData.transformGame(treeData.getGame());
                            valResult = tree.getBidInfo(keyCompressed);
                        }
                    }
                }
            }
		}
		return valResult;
	}

    public void addDataRedis(TreeData treeData, String value, boolean bidAlert, boolean cardSpread) {
	    String key = treeData.getCompleteKey();
	    if (treeData.isRequestBidCard()) {
	        if (bidAlert) {
	            value += "a";
            }
            if (cardSpread) {
	            value += "s";
            }
        }
        redisCache.setValue(key, value);
    }

    /**
	 * Add the tree this data and value. In the case of bid or card request, value is append to the tree.
	 * For bid info request, the value if associated to the data in the tree.
	 * @param data
	 * @param value
     * @param bidAlert
     * @param cardSpread
	 * @return
	 */
	public boolean addCacheData(String data, String value, boolean bidAlert, boolean cardSpread) {
	    if (log.isDebugEnabled()) {
	        log.debug("Add value="+value+" - bidAlert="+bidAlert+" to data="+data);
        }
		TreeData treeData = createTreeData(data);
		boolean bResult = false;
		if (treeData != null && EngineConfiguration.getInstance().getIntValue("general.useCache", 0) == 1) {
			long ts1 = System.currentTimeMillis();
            if (EngineConfiguration.getInstance().getIntValue("general.cacheRedis", 0) == 1) {
                addDataRedis(treeData, value, bidAlert, cardSpread);
                bResult = true;
            } else {
                if (EngineConfiguration.getInstance().getIntValue("general.cacheRedis.testSet", 0) == 1) {
                    addDataRedis(treeData, value, bidAlert, cardSpread);
                }
                // request BID or CARD
                if (treeData.isRequestBidCard()) {
                    treeData.appendGameResult(value);
                    String treeDealKey = treeData.getTreeDealKey();
                    TreeBidCard tree = cacheTree.getTreeBidCard(treeDealKey);
                    // no tree for this deal ? insert a new !
                    if (tree == null) {
                        TreeBidCard newtree = new TreeBidCard(treeData.getDeal(), treeData.getOptions());
                        tree = cacheTree.addTreeBidCard(treeDealKey, newtree);
                        if (tree == null) {
                            tree = newtree;
                        }
                    }
                    synchronized (tree) {
                        // insert data in tree
                        tree.setTsLastAdd(System.currentTimeMillis());
                        String valCompressed = treeData.getConventions() + BridgeTransformData.transformGame(treeData.getGame());
                        bResult = tree.insert(valCompressed, bidAlert, cardSpread);
                    }
                }
                // REQUEST BID INFO
                else if (treeData.isRequestBidInfo()) {
                    TreeBidInfo tree = cacheTree.getTreeBidInfo(treeData.getTreeDealKey());
                    // no tree for this deal ? insert a new !
                    if (tree == null) {
                        TreeBidInfo newtree = new TreeBidInfo(treeData.getDeal(), treeData.getOptions());
                        tree = cacheTree.addTreeBidInfo(treeData.getTreeDealKey(), newtree);
                        if (tree == null) {
                            tree = newtree;
                        }
                    }
                    synchronized (tree) {
                        tree.setTsLastAdd(System.currentTimeMillis());
                        String valCompressed = treeData.getConventions() + BridgeTransformData.transformGame(treeData.getGame());
                        bResult = tree.insert(valCompressed, value);
                    }
                } else {
                    log.error("Tree data type not valid : " + treeData);
                }
            }
			long ts2 = System.currentTimeMillis();
            logStatMgr.logInfo(false, ts2-ts1, "addCacheData - data="+data+" - value="+value+" - bidAlert="+bidAlert);
		}
		return bResult;
	}
	
	/**
	 * Remove all tree associated to this deal for bid or card. If backup option is enable, save it to file before delete it
	 * @param deal
	 * @return
	 */
	public boolean deleteTreeBidCardForKey(String treeKey) {
		TreeBidCard t = cacheTree.removeTreeBidCard(treeKey);
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
		TreeBidInfo t2 = cacheTree.removeTreeBidInfo(treeKey);
		if (t2 != null) {
			t2 = null;
			return true;
		}
		return false;
	}
	
	/**
	 * Create tree data associated to this request
	 * The request is the string with char ';' to separate each parameter
	 * @param request
	 * @return
	 */
	private TreeData createTreeData(String request) {
		if (request != null && request.length() > 0) {
			try {
				String[] temp = request.split(Constantes.REQUEST_FIELD_SEPARATOR);
				if (temp.length == Constantes.REQUEST_NB_FIELD) {
					TreeData data = new TreeData(
							temp[Constantes.REQUEST_INDEX_FIELD_DEAL],
							temp[Constantes.REQUEST_INDEX_FIELD_OPTIONS],
							temp[Constantes.REQUEST_INDEX_FIELD_CONV],
							temp[Constantes.REQUEST_INDEX_FIELD_GAME],
							Integer.parseInt(temp[Constantes.REQUEST_INDEX_FIELD_TYPE]));
					return data;
				}
			} catch (Exception e) {
				log.error("Exception to create treedata using request="+request, e);
			}
		}
		return null;
	}

	public String getTreeBidCardCacheInfo() {
	    return cacheTree.getCacheInfoBidCard();
    }

    public String getTreeBidInfoCacheInfo() {
        return cacheTree.getCacheInfoBidCard();
    }
}
