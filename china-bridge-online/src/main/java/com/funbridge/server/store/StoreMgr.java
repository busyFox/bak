package com.funbridge.server.store;

import com.funbridge.server.common.*;
import com.funbridge.server.message.MessageNotifMgr;
import com.funbridge.server.message.data.MessageNotif;
import com.funbridge.server.player.PlayerMgr;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.presence.FBSession;
import com.funbridge.server.store.dao.ProductDAO;
import com.funbridge.server.store.dao.TransactionDAO;
import com.funbridge.server.store.data.*;
import com.funbridge.server.texts.TextUIMgr;
import com.funbridge.server.tournament.federation.FederationMgr;
import com.funbridge.server.tournament.federation.data.PlayerFederation;
import com.funbridge.server.ws.FBExceptionType;
import com.funbridge.server.ws.FBWSException;
import com.funbridge.server.ws.store.WSProductGroup;
import com.funbridge.server.ws.store.WSProductStore;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Component(value="storeMgr")
@Scope(value="singleton")
public class StoreMgr extends FunbridgeMgr{
	@Resource(name="productDAO")
	private ProductDAO productDAO = null;
	@Resource(name="transactionDAO")
	private TransactionDAO transactionDAO = null;
	@Resource(name="playerMgr")
	private PlayerMgr playerMgr = null;
	@Resource(name="messageNotifMgr")
	private MessageNotifMgr notifMgr = null;
    @Resource(name = "textUIMgr")
    private TextUIMgr textUIMgr = null;

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

    /**
     * Create a new transaction
     * @param productID
     * @param session
     * @param playerID
     * @param storeTransactionID
     * @param information
     * @return
     * @throws FBWSException
     */
    @Transactional
    public Transaction newTransaction(String productID, FBSession session, long playerID, String storeTransactionID, String information) throws FBWSException {
        Transaction transaction = null;
        Player player = null;
        String clientVersion = null;
        try {
            if (session != null) {
                player = session.getPlayer();
                clientVersion = session.getClientVersion();
            } else {
                player = playerMgr.getPlayer(playerID);
            }
            if (player == null) {
                log.error("Player not found for session="+session+" - playerID="+playerID);
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID, true, "Player not found for session="+session+" - playerID="+playerID);
            }
            // Get product
            Product product = productDAO.getProductForProductID(productID);
            if (product == null || !product.isEnable()) {
                log.error("Product not valid product="+product);
                throw new FBWSException(FBExceptionType.STORE_PRODUCT_NOT_VALID, true, "Product not valid product="+product);
            }
            // insert transaction in DB
            transaction = ContextManager.getStoreMgr().createTransactionDB(product, player.getID(), clientVersion, storeTransactionID, information);
            if (transaction == null) {
                log.error("Transaction is null ! playerID="+player.getID()+" - product="+product.toString());
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR, true, "Transaction is null ! playerID="+player.getID()+" - product="+product.toString());
            }
            // credit player
            if(product.getCategory() == Constantes.PRODUCT_CATEGORY_DEAL){
                // deal products
                player.setCreditAmount(player.getCreditAmount() + product.getCreditAmount());
                player.setDateLastCreditUpdate(transaction.getTransactionDate());
            } else if(product.getCategory() == Constantes.PRODUCT_CATEGORY_SUBSCRIPTION){
                // subscription products
                Calendar cal = Calendar.getInstance();
                long expirationDate = player.getSubscriptionExpirationDate();
                if(expirationDate < System.currentTimeMillis()){
                    expirationDate = System.currentTimeMillis();
                }
                cal.setTimeInMillis(expirationDate);
                switch (product.getCreditType()){
                    case Constantes.PRODUCT_CREDIT_TYPE_DAY:
                        cal.add(Calendar.DATE, product.getCreditAmount());
                        break;
                    case Constantes.PRODUCT_CREDIT_TYPE_MONTH:
                        cal.add(Calendar.MONTH, product.getCreditAmount());
                        break;
                    default:
                        throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR, true, "Invalid credit type for a subscription product. Product="+product);
                }
                player.setSubscriptionExpirationDate(cal.getTimeInMillis());
            } else if(product.getCategory() == Constantes.PRODUCT_CATEGORY_TOUR_CBO){
                // federation tournament products
                // TODO
            }
            playerMgr.updatePlayerToDB(player, PlayerMgr.PlayerUpdateType.CREDIT);

            // update player on the client side
            if (session != null) {
                session.pushEvent(playerMgr.buildEventPlayerUpdate(player.getID(), playerMgr.playerToWSPlayerInfo(session)));
            }
            // send notif
            if (FBConfiguration.getInstance().getIntValue("message.sendAfterBuy", 0) == 1) {
                if (product.getCategory() == Constantes.PRODUCT_CATEGORY_DEAL) {
                    MessageNotif notif = notifMgr.createNotifCreditBuyDeal(player, product.getCreditAmount());
                    if (notif != null && session != null) {
                        session.pushEvent(notifMgr.buildEvent(notif, player));
                    }
                } else if (product.getCategory() == Constantes.PRODUCT_CATEGORY_SUBSCRIPTION) {
                    MessageNotif notif = notifMgr.createNotifCreditBuySubscription(player);
                    if (notif != null && session != null) {
                        session.pushEvent(notifMgr.buildEvent(notif, player));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Exception after creating transaction - player="+player, e);
        }
        if (log.isDebugEnabled()) {
            log.debug("Transaction success for player="+player+" - st="+transaction);
        }
        return transaction;
    }
	
	/**
	 * Return the product for this productID
	 * @param productID
	 * @return
	 */
	public Product getProduct(String productID) {
		return productDAO.getProductForProductID(productID);
	}

    /**
     * Create transaction for this product and player
     * @param product
     * @param playerID
     * @param clientVersion
     * @param transactionID
     * @param information
     * @return
     */
    @Transactional
	public Transaction createTransactionDB(Product product, long playerID, String clientVersion, String transactionID, String information) {
        if (product != null) {
            try {
                Transaction transaction = new Transaction();
                transaction.setPlayerID(playerID);
                transaction.setStoreTransactionID(transactionID);
                transaction.setTransactionDate(System.currentTimeMillis());
                transaction.setVersion(clientVersion);
                transaction.setProduct(product);
                transaction.setInformation(information);
                transaction.setTransactionType(product.getType());
                transaction = transactionDAO.createTransaction(transaction);
                return transaction;
            } catch (Exception e) {
                log.error("Error creating store transaction playerID="+playerID+" - product="+product.toString());
            }
        }
        return null;
    }
	
	/**
	 * Number of transactions for player and transaction bonus flag
	 * @param playerID
	 * @param bonus
	 * @return
	 */
	public int getNbTransactionsForPlayer(long playerID, boolean bonus) {
		try {
			return transactionDAO.countForPlayerAndBonus(playerID, bonus);
		} catch (Exception e) {
			log.error("Exception to count transaction for player="+playerID, e);
		}
		return -1;
	}

    public String convertStoreType2ProductType(String storeType) {
        if (storeType != null) {
            if (storeType.equals(Constantes.DEVICE_TYPE_ANDROID)) {
                return Constantes.PRODUCT_TYPE_ANDROID;
            }
            else if (storeType.equals(Constantes.DEVICE_TYPE_IOS)) {
                return Constantes.PRODUCT_TYPE_IOS;
            }
            else {
                log.error("StoreType is not supported !! - storeType="+storeType);
            }
        } else {
            log.error("StoreType parameter is null !!");
        }
        return null;
    }

    /**
     * List products for a player
     * @param playerID
     * @param lang
     * @param storeType
     * @return
     */
    public List<WSProductGroup> listProductsForPlayer(long playerID, String lang, String storeType) throws FBWSException {
        List<WSProductGroup> productGroups = new ArrayList<>();
        String producType = convertStoreType2ProductType(storeType);
        if (producType == null) {
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }

        // Subscriptions
        WSProductGroup subscriptionProductsGroup = new WSProductGroup();
        subscriptionProductsGroup.title = textUIMgr.getTextUIForLang("storeCategoryTitle.subscriptions", lang);
        List<Product> subscriptionProducts = productDAO.listEnableProductsForTypeAndCategory(producType, Constantes.PRODUCT_CATEGORY_SUBSCRIPTION);
        for(Product product : subscriptionProducts){
            WSProductStore wsProduct = new WSProductStore(product);
            subscriptionProductsGroup.products.add(wsProduct);
        }
        if(!subscriptionProductsGroup.products.isEmpty()){
            productGroups.add(subscriptionProductsGroup);
        }
        
        // Deals
        WSProductGroup dealsProductsGroup = new WSProductGroup();
        dealsProductsGroup.title = textUIMgr.getTextUIForLang("storeCategoryTitle.deals", lang);
        List<Product> dealsProducts = productDAO.listEnableProductsForTypeAndCategory(producType, Constantes.PRODUCT_CATEGORY_DEAL);
        for(Product product : dealsProducts){
            WSProductStore wsProduct = new WSProductStore(product);
            dealsProductsGroup.products.add(wsProduct);
        }
        if(!dealsProductsGroup.products.isEmpty()){
            productGroups.add(dealsProductsGroup);
        }

        return productGroups;
    }

    public int getNbProductPromoForPlayer(FBSession session) {
        int nbPromo = 0;
        if (session != null) {
            try {
                List<WSProductGroup> listProductGroup = listProductsForPlayer(session.getPlayer().getID(), session.getPlayer().getDisplayLang(), session.getDeviceType());
                for (WSProductGroup e : listProductGroup) {
                    for (WSProductStore p : e.products) {
                        if (p.promo) {
                            nbPromo++;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to list products for player - session="+session, e);
            }
        }
        return nbPromo;
    }

}
