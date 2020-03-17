package com.funbridge.server.pay;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeAppPayModel;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.common.FunbridgeMgr;
import com.funbridge.server.message.MessageNotifMgr;
import com.funbridge.server.message.data.MessageNotif;
import com.funbridge.server.pay.dao.PayOrderDAO;
import com.funbridge.server.pay.data.PayOrder;
import com.funbridge.server.player.PlayerMgr;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.presence.FBSession;
import com.funbridge.server.store.dao.ProductDAO;
import com.funbridge.server.store.data.Product;
import com.funbridge.server.ws.FBWSException;
import com.funbridge.server.ws.ServiceTools;
import com.funbridge.server.ws.pay.alipay.AlipayConfig;
import com.funbridge.server.ws.pay.wxpay.MyConfig;
import com.funbridge.server.ws.pay.wxpay.PayConfig;
import com.github.wxpay.sdk.WXPay;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component(value="payOrderMgr")
@Scope(value="singleton")
public class PayOrderMgr extends FunbridgeMgr {

    @Resource(name="payOrderDAO")
    private PayOrderDAO payOrderDAO = null;

    @Resource(name="productDAO")
    private ProductDAO productDAO = null;

    @Resource(name="playerMgr")
    private PlayerMgr playerMgr = null;


    @Resource(name="messageNotifMgr")
    private MessageNotifMgr notifMgr = null;


    private PayOrder payOrder ;

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

    public String sendPay(String sessionId){
        AlipayClient alipayClient = new DefaultAlipayClient(AlipayConfig.ALIPAY_URL, AlipayConfig.APP_ID,
                AlipayConfig.PRIVATE_KEY, AlipayConfig.FORMAT, AlipayConfig.CHARSET, AlipayConfig.ALIPAY_PUBLIC_KEY,
                AlipayConfig.SIGN_TYPE);

        AlipayTradeAppPayRequest request = new AlipayTradeAppPayRequest();
        AlipayTradeAppPayModel model = new AlipayTradeAppPayModel();
        //订单信息
        model.setBody("商品购买");

        //商品/交易/订单标题
        model.setSubject("ChinaBridge System APP");

        //订单号
        model.setOutTradeNo(payOrder.getLocalOrderNo());

        //该笔订单允许的最晚付款时间，逾期将关闭交易。取值范围：1m～15d。m-分钟，h-小时，d-天，
        // 1c-当天（1c-当天的情况下，无论交易何时创建，都在0点关闭）。 该参数数值不接受小数点，
        // 如 1.5h，可转换为 90m。注：若为空，则默认为15d。
        model.setTimeoutExpress("60m");

        //交易金额
        model.setTotalAmount(String.valueOf((float) payOrder.getAmount()*0.01));

        //销售产品码，商家和支付宝签约的产品码，为固定值QUICK_MSECURITY_PAY
        model.setProductCode("QUICK_MSECURITY_PAY");

        //商品主类型：0—虚拟类商品，1—实物类商品注：虚拟类商品不支持使用花呗渠道
        //model.setGoodsType("0");

        //利用回传参数传递sessionId
        try {
            sessionId = URLEncoder.encode(sessionId,AlipayConfig.CHARSET);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            log.error("SessionId URLEncoder Is ERROR ! LocalOrderNo:{}",payOrder.getLocalOrderNo());
        }
        model.setPassbackParams(sessionId);


        request.setApiVersion("1.0");
        request.setBizModel(model);
        request.setNotifyUrl(AlipayConfig.CALLBACK_URL);
        try {
            AlipayTradeAppPayResponse response = alipayClient.sdkExecute(request);
            log.info("PJH--------{}",response.isSuccess());
            return response.getBody();
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return null ;
    }

    @Transactional
    public synchronized String insertPayOrder(long playerId, int shopId , int orderType){
            String localOrderId = "" ;

            for (int i = 0 ; i< 100 ; i++){
                localOrderId = getLocalOrderNo(playerId);
                if (!payOrderDAO.isLocalOrderNoExist(localOrderId)){
                    break;
                }else{
                    if (i==99){
                        return null;
                    }
                }
            }

            payOrder = new PayOrder() ;
            payOrder.setOrderType(orderType);
            payOrder.setLocalOrderNo(localOrderId);
            Date date = new Date(System.currentTimeMillis());
            payOrder.setLocalOrderTime(date);
            payOrder.setUserId(String.valueOf(playerId));
            payOrder.setShopuId(shopId);
            payOrder.setAmount(productDAO.getProductForProductID(String.valueOf(shopId)).getPrice());
            payOrder.setOrdeStatus(Constantes.NO_PAY);
            if (payOrderDAO.insertPayOrder(payOrder)){
                return localOrderId ;
            }
            return null ;
    }

    /**
     * 交易成功后完善订单信息
     * @return Product
     */
    @Transactional
    public PayOrder updatePayOrderByLocalOrderNo(String localOrderNo, String orderNo, String dealTime, String extraDetail){
        PayOrder payOrder = payOrderDAO.getPayOrderforLocalOrderNo(localOrderNo);
        if (payOrder == null ){
            log.error("The order is not exit ! LocalOrderNo:{}",localOrderNo);
            return null ;
        }else if (payOrder.getOrdeStatus() == 1){
            log.error("The orderStatus is 1 ! LocalOrderNo:{}",localOrderNo);
            return null ;
        }

        //根据订单信息中的shopId查询Product
        Product product = productDAO.getProductForProductID(String.valueOf(payOrder.getShopuId()));
        if (product == null){
            log.error("The Product is null ! localOrderNo:{},productId:{}",localOrderNo,payOrder.getShopuId());
            return null ;
        }

        payOrder.setOrderNo(orderNo);
        SimpleDateFormat formatter = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss");
        try {
            payOrder.setDealTime(formatter.parse(dealTime));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        payOrder.setOrdeStatus(Constantes.YES_PAY);
        payOrder.setExtraDetail(extraDetail);

        //修改订单信息
        PayOrder p  = payOrderDAO.updatePayOrder(payOrder) ;
        if (p == null){
            log.error("PayOderInfo update fail ! LocalOrderNo:{}",localOrderNo);
            return null ;
        }

        return p ;
    }

    private String getLocalOrderNo(long playerID){
        String str = "" ;
        Long timeCurrent = System.currentTimeMillis();
        str += timeCurrent ;
        str += String.valueOf(playerID) ;
        int rand = (int)((Math.random()*9+1)*1000) ;
        str += String.valueOf(rand) ;
        return str ;
    }

    /**
     * 微信支付下单
     */
    public Map<String, String> WXStartOrder(String sessionId) throws Exception {
        MyConfig config = new MyConfig();
        WXPay wxpay = new WXPay(config);

        Map<String, String> data = new HashMap<String, String>();
        data.put("body", "testgoods");
        data.put("out_trade_no", payOrder.getLocalOrderNo());
        data.put("device_info", "");
        data.put("fee_type", "CNY");
        data.put("total_fee", String.valueOf(payOrder.getAmount()));
        data.put("spbill_create_ip", "1.1.1.1");
        data.put("notify_url", PayConfig.NOTIFY_URL);
        data.put("trade_type", "APP");
        data.put("product_id", String.valueOf(payOrder.getShopuId()));
        data.put("attach", sessionId);

        try {
            Map<String, String> resp = wxpay.unifiedOrder(data);
            return resp;
        } catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /**
     *微信/支付宝回调成功需改数据库信息
     */
    @Transactional
    public void updatePayOrderAndPlayer(String out_trade_no, String trade_no, String gmt_payment, String body, String passback_params){
        //完善订单信息
        PayOrder payOrder = updatePayOrderByLocalOrderNo(out_trade_no,trade_no,gmt_payment,body);
        if (payOrder == null) {
            log.error("The PayOrder is not exit ! LocalOrderNo:{}",out_trade_no);
            return;
        }

        //根据订单信息中的shopId查询Product
        Product product = productDAO.getProductForProductID(String.valueOf(payOrder.getShopuId()));
        if (product == null){
            log.error("The Product is not exit ! LocalOrderNo:{}",out_trade_no);
            return;
        }
        //将产品发放到账户。
        Player player = playerMgr.getPlayer(Long.valueOf(payOrder.getUserId())) ;

        //判断产品类型
        if (product.getCategory() == Constantes.PRODUCT_CATEGORY_SUBSCRIPTION){
            //包月会员（订阅）
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
            //加牌局
            int amout = player.getCreditAmount() + product.getCreditAmount() ;
            player.setCreditAmount(amout);
            player.setDateLastCreditUpdate(System.currentTimeMillis());
        }
        playerMgr.updatePlayerToDB(player, PlayerMgr.PlayerUpdateType.CREDIT_DEAL);

        try {
            FBSession session = ServiceTools.getAndCheckSession(passback_params);
            session.setPlayer(player);
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
        } catch (FBWSException e) {
            e.printStackTrace();
        }
    }

    @Transactional
    public synchronized String insertIOSPayOrder(long playerId, int shopId, String orderNo, Date dealTime, int price){
        String localOrderId = "" ;

        for (int i = 0 ; i< 100 ; i++){
            localOrderId = getLocalOrderNo(playerId);
            if (!payOrderDAO.isLocalOrderNoExist(localOrderId)){
                break;
            }else{
                if (i==99){
                    return null;
                }
            }
        }

        payOrder = new PayOrder() ;
        payOrder.setOrderType(Constantes.IOS_PAYORDER_TYPE);
        payOrder.setLocalOrderNo(localOrderId);
        Date date = new Date(System.currentTimeMillis());
        payOrder.setLocalOrderTime(date);
        payOrder.setOrderNo(orderNo);
        payOrder.setDealTime(dealTime);
        payOrder.setUserId(String.valueOf(playerId));
        payOrder.setShopuId(shopId);
        payOrder.setAmount(price);
        payOrder.setOrdeStatus(Constantes.YES_PAY);
        if (payOrderDAO.insertPayOrder(payOrder)){
            return localOrderId ;
        }
        return null ;
    }

}
