package com.funbridge.server.ws.store;

import com.alibaba.fastjson.JSONObject;
import com.funbridge.server.Utils.IosVerifyUtil;
import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.FunbridgeMgr;
import com.funbridge.server.pay.PayOrderMgr;
import com.funbridge.server.player.PlayerMgr;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.presence.FBSession;
import com.funbridge.server.store.StoreMgr;
import com.funbridge.server.store.data.Product;
import com.funbridge.server.store.data.Transaction;
import com.funbridge.server.ws.*;
import com.funbridge.server.ws.player.WSPlayerInfo;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;

@Service(value = "storeService")
@Scope(value = "singleton")
public class StoreServiceRestImpl extends FunbridgeMgr implements StoreServiceRest {
    @Resource(name = "storeMgr")
    private StoreMgr storeMgr = null;

    @Resource(name = "playerMgr")
    private PlayerMgr playerMgr = null;

    @Resource(name = "payOrderMgr")
    private PayOrderMgr payOrderMgr = null;

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

    @Override
    public FBWSResponse getProducts(String sessionID, GetProductsParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            FBSession session = null;
            try {
                session = ServiceTools.getAndCheckSession(sessionID);
                if (param == null || !param.isValid()) {
                    log.error("Parameter not valid ! - param=[" + param + "] - session=" + session);
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
                if (log.isDebugEnabled()) {
                    log.debug("playerID=" + session.getPlayer().getID() + " - param=" + param);
                }
                GetProductsResponse resp = new GetProductsResponse();
                resp.groupList = storeMgr.listProductsForPlayer(session.getPlayer().getID(), session.getPlayer().getDisplayLang(), param.storeType);
                int nbPromo = 0;
                for (WSProductGroup e : resp.groupList) {
                    for (WSProductStore p : e.products) {
                        if (p.promo) {
                            nbPromo++;
                        }
                    }
                }
                session.storePromo = nbPromo;
                response.setData(resp);
                session.incrementNbCallStoreGetProducts();
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameters not valid ... sessionID=" + sessionID + " - param=" + param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    @Override
    public FBWSResponse newTransaction(String sessionID, NewTransactionParam param) {
        // TODO China team : implement this method for app stores
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            FBSession session = null;
            try {
                long ts = System.currentTimeMillis();
                session = ServiceTools.getAndCheckSession(sessionID);
                if (param == null || !param.isValid()) {
                    log.error("Parameter not valid ! - param=["+param+"] - session="+session);
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
                if (log.isDebugEnabled()) {
                    log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
                }
                NewTransactionResponse resp = new NewTransactionResponse();
                // TODO : change arguments
                Transaction transaction = storeMgr.newTransaction(param.productID, session, session.getPlayer().getID(), "TestTransaction"+ new Random().nextInt(1000), "TestInformation");
                if (transaction != null) {
                    resp.checkTransactionReceipt = new WSNewTransactionReceipt();
                    resp.checkTransactionReceipt.creditAccount = session.getPlayer().getCreditAmount();
                    resp.checkTransactionReceipt.accountExpirationDate = session.getPlayer().getSubscriptionExpirationDate();
                    resp.checkTransactionReceipt.transactionValid = true;
                }
                response.setData(resp);
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : "+e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameters not valid ... sessionID="+sessionID+" - param="+param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    @Override
    public FBWSResponse checkTransactionReceipt(String sessionID, CheckTransactionReceiptParam param) {
        FBWSResponse response = new FBWSResponse();
        Map<String, Object> map = new HashMap<String, Object>();
        Map<String, Object> mapChange = new HashMap<String, Object>();
        CheckTransactionReceiptResponse resp = new CheckTransactionReceiptResponse() ;
        resp.checkTransactionReceipt = new WSNewTransactionReceipt();
        if (sessionID != null) {
            FBSession session = null;
            try {
                session = ServiceTools.getAndCheckSession(sessionID);

                if (param == null || !param.isValid()) {
                    log.error("Parameter not valid ! - param=["+param+"]");
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
                log.info("????????1?"+param.transactionID+"????????2?"+param.receiptData);
                String verifyResult =  IosVerifyUtil.buyAppVerify(param.receiptData,1); 			//1.?????    ??????
                if (verifyResult == null) {   											// ?????????????
                    System.out.println("?????!");
                } else {
                    log.info("?????????JSON:"+verifyResult);
                    JSONObject job = JSONObject.parseObject(verifyResult);
                    String states = job.getString("status");
                    if("21007".equals(states)){											//??????????????????
                        verifyResult =  IosVerifyUtil.buyAppVerify(param.receiptData,0);			//2.?????  ??????
                        System.out.println("???????????JSON:"+verifyResult);
                        job = JSONObject.parseObject(verifyResult);
                        states = job.getString("status");
                    }
                    log.info("????????job"+job);
                    if (states.equals("0")){ // ????????????    ????
                        String r_receipt = job.getString("receipt");
                        JSONObject returnJson = JSONObject.parseObject(r_receipt);
                        String inApp = returnJson.getString("in_app");
                        JSONObject inAppJson = JSONObject.parseObject(inApp.substring(1, inApp.length()-1));

                        String purchaseDateMs = inAppJson.getString("purchase_date_ms");
                        Date dealTime = new Date(new Long(purchaseDateMs));
                        String productId = inAppJson.getString("product_id");
                        String transactionId = inAppJson.getString("transaction_id");   // ???
/************************************************+???????**********************************************************/
                        Product product = storeMgr.getProduct(productId) ;

                        Player player = session.getPlayer();
                        //??????
                        if (product.getCategory() == Constantes.PRODUCT_CATEGORY_SUBSCRIPTION){
                            //????????
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
                                case Constantes.PRODUCT_CREDIT_TYPE_YEAR:
                                    cal.add(Calendar.YEAR, product.getCreditAmount());
                                    break;
                                default:
                                    log.error("Invalid credit type for a subscription product. Product={}",product);
                            }
                            player.setSubscriptionExpirationDate(cal.getTimeInMillis());

                        }else if (product.getCategory() == Constantes.PRODUCT_CATEGORY_DEAL){
                            //???
                            int amout = player.getCreditAmount() + product.getCreditAmount() ;
                            player.setCreditAmount(amout);
                            player.setDateLastCreditUpdate(System.currentTimeMillis());
                        }

                        playerMgr.updatePlayerToDB(player, PlayerMgr.PlayerUpdateType.CREDIT_DEAL);
                        log.info("CreditAmount=" + session.getPlayer().getCreditAmount() + ", Deals=" + session.getPlayer().getNbPlayedDeals());
                        String localNo = payOrderMgr.insertIOSPayOrder(session.getPlayer().getID(),(int)product.getID(),transactionId,dealTime,product.getPrice());

/************************************************+???????end**********************************************************/
                        if(localNo != null && !localNo.isEmpty()){//??????
                            resp.checkTransactionReceipt.transactionValid = true ;
                            resp.checkTransactionReceipt.creditAccount = session.getPlayer().getCreditAmount();
                            resp.checkTransactionReceipt.accountExpirationDate = session.getPlayer().getSubscriptionExpirationDate() ;
                        }else{
                            log.info("The PayOrder is save false!  transaction_id:{}",transactionId);
                            throw new FBWSException(FBExceptionType.IOS_SAVE_PAYORDER_FALSE);
                        }
                    } else {
                        log.info("receiptDate is false . transaction_id:{}",param.transactionID);
                        throw new FBWSException(FBExceptionType.IOS_RECEIPTDATA_ERROR);
                    }
                }

                resp.checkTransactionReceipt.subscriptionTimeLeft = playerMgr.playerToWSPlayerInfo(session).subscriptionTimeLeft ;

                response.setData(resp);
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            }
        } else {
            log.warn("Parameters not valid ... sessionID="+sessionID+" - param="+param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }

        return response;
    }
}