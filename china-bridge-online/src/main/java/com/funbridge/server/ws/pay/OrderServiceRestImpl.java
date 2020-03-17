package com.funbridge.server.ws.pay;


import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.FunbridgeMgr;
import com.funbridge.server.pay.PayOrderMgr;
import com.funbridge.server.pay.dao.PayOrderDAO;
import com.funbridge.server.pay.data.PayOrder;
import com.funbridge.server.player.PlayerMgr;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.presence.FBSession;
import com.funbridge.server.store.dao.ProductDAO;
import com.funbridge.server.store.data.Product;
import com.funbridge.server.ws.*;
import com.funbridge.server.ws.pay.alipay.AlipayConfig;
import com.funbridge.server.ws.pay.wxpay.MyConfig;
import com.funbridge.server.ws.pay.wxpay.PayConfig;
import com.funbridge.server.ws.store.WSNewTransactionReceipt;
import com.github.wxpay.sdk.WXPay;
import com.github.wxpay.sdk.WXPayConstants;
import com.github.wxpay.sdk.WXPayUtil;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

@Service(value = "orderService")
@Scope(value = "singleton")
public class OrderServiceRestImpl extends FunbridgeMgr implements OrderServiceRest {

    @Resource(name = "payOrderMgr")
    private PayOrderMgr payOrderMgr ;
    @Resource(name = "playerMgr")
    private PlayerMgr playerMgr;
    @Resource(name="productDAO")
    private ProductDAO productDAO = null;
    @Resource(name="payOrderDAO")
    private PayOrderDAO payOrderDAO = null;

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
    public FBWSResponse getOrderString(final String sessionId,GetOrderStringParam param) {
        FBWSResponse response = new FBWSResponse();
        if (param != null){
            try {
                if (sessionId == null || sessionId.isEmpty()){
                    throw new FBWSException(FBExceptionType.SESSION_INVALID_SESSION_ID);
                }
                //check shopId exit

                //获取session,并且获得session中用户的信息
                FBSession session = ServiceTools.getAndCheckSession(sessionId);
                Player player = session.getPlayer() ;
                if (!playerMgr.existPseudo(player.getNickname())){
                    log.error("The Play is not exist ! ProductId:{}",param.shopId);
                    throw new FBWSException(FBExceptionType.PLAYER_NOT_EXIST);
                }

                Product product = productDAO.getProductForProductID(String.valueOf(param.shopId));
                if (product == null){
                    log.error("The Product is not exist ! ProductId:{}",param.shopId);
                    throw new FBWSException(FBExceptionType.PRODUCT_NOT_EXIST);
                }

                //生成订单记录，并且插入库中
                log.info("Send it to me-------userId:{}",player.getID());
                log.info("Send it to me-------shopId:{}",param.shopId);
                String localOrderNo = payOrderMgr.insertPayOrder(player.getID(), param.shopId, Constantes.ALIPAY_TYPE) ;
                if (localOrderNo == null || localOrderNo.isEmpty()){
                    throw new FBWSException(FBExceptionType.LOCALORDERNO_MAKE_FAIL);
                }

                //加签订单信息，获取orderString
                String orderString = payOrderMgr.sendPay(sessionId);

                if (orderString == null || orderString.isEmpty()){
                    throw new FBWSException(FBExceptionType.ORDERSTRING_IS_NONE);
                }

                //返回orderString
                GetOrderStringResponse resp = new GetOrderStringResponse() ;
                resp.orderString = orderString ;
                resp.localOrderNo = localOrderNo ;
                log.info(orderString);
                response.setData(resp);
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        }else {
            log.warn("Parameter not valid");
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }


        return response ;
    }

    @Override
    public String aliCallBack(String notify_time,String notify_type,String notify_id,
                              String app_id,String charset,String version,
                              String sign_type,String sign,String trade_no,
                              String out_trade_no,String out_biz_no,String buyer_id,
                              String buyer_logon_id,String seller_id,String seller_email,
                              String trade_status,String total_amount,String receipt_amount,
                              String invoice_amount,String buyer_pay_amount,String point_amount,
                              String refund_fee,String subject,String body,
                              String gmt_create	,String gmt_payment,String gmt_refund,
                              String gmt_close,String fund_bill_list,String passback_params,
                              String voucher_detail_list) {
        try {
            passback_params = URLDecoder.decode(passback_params, AlipayConfig.CHARSET);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            log.error("SessionId URLDecoder Is ERROR ! LocalOrderNo:{}",out_trade_no);
        }

//        Map<String,String> params = new HashMap<>();
        try {
            body = new String(body.getBytes("ISO-8859-1"),"utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
//        //valueStr = new String(valueStr.getBytes("ISO-8859-1"), "utf-8");
//        params.put("notify_time",notify_time);
//        params.put("notify_type",notify_type);
//        params.put("notify_id",notify_id);
//        params.put("app_id",app_id);
//        params.put("charset",charset);
//        params.put("version",version);
//        params.put("sign_type",sign_type);
//        params.put("sign",sign);
//        params.put("trade_no",trade_no);
//        params.put("out_trade_no",out_trade_no);
//        params.put("out_biz_no",out_biz_no);
//        params.put("buyer_id",buyer_id);
//        params.put("buyer_logon_id",buyer_logon_id);
//        params.put("seller_id",seller_id);
//        params.put("seller_email",seller_email);
//        params.put("trade_status",trade_status);
//        params.put("total_amount",total_amount);
//        params.put("receipt_amount",receipt_amount);
//        params.put("invoice_amount",invoice_amount);
//        params.put("buyer_pay_amount",buyer_pay_amount);
//        params.put("point_amount",point_amount);
//        params.put("refund_fee",refund_fee);
//        params.put("subject",subject);
//        params.put("body",body);
//        params.put("gmt_create",gmt_create);
//        params.put("gmt_payment",gmt_payment);
//        params.put("gmt_refund",gmt_refund);
//        params.put("gmt_close",gmt_close);
//        params.put("fund_bill_list",fund_bill_list);
//        params.put("passback_params",passback_params);
//        params.put("voucher_detail_list",voucher_detail_list);
//        log.info("-------------------------------------------");
//        for (Map.Entry<String, String> entry : params.entrySet()){
//            log.info(entry.getKey() + " ->" + entry.getValue());
//        }
//        log.info("-------------------------------------------");

        if (trade_status.equals("TRADE_SUCCESS")){
            log.info("success->out_trade_no:{}",out_trade_no);

            payOrderMgr.updatePayOrderAndPlayer(out_trade_no,trade_no,gmt_payment,body,passback_params);

            return "success";
        }else if (trade_status.equals("TRADE_FINISHED") || trade_status.equals("TRADE_CLOSED")){
            //交易交易结束/关闭
            log.error("The PayOder status : {}",trade_status);
            return "success" ;
        }else { //if (trade_status.equals("WAIT_BUYER_PAY"))
            //交易创建，等待支付。
            log.error("The PayOder status : {}",trade_status);
            return "fail" ;
        }
    }

    @Override
    public FBWSResponse getCheckResult(final String sessionId, GetCheckResultParam param) {
        FBWSResponse response = new FBWSResponse();
        if (sessionId != null) {
            FBSession session = null;
            try {
                session = ServiceTools.getAndCheckSession(sessionId);
                if (param == null || !param.isValid()) {
                    log.error("Parameter not valid ! - param=["+param+"] - session="+session);
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
                if (log.isDebugEnabled()) {
                    log.debug("playerID="+session.getPlayer().getID()+" - param="+param);
                }
                NewTransactionResponse resp = new NewTransactionResponse();
                // TODO : change arguments
                PayOrder payOrder = payOrderDAO.getPayOrderforLocalOrderNo(param.localOrderNo) ;
                resp.checkTransactionReceipt = new WSNewTransactionReceipt();
                if (payOrder != null && payOrder.getOrdeStatus() == 1) {

                    resp.checkTransactionReceipt.creditAccount = session.getPlayer().getCreditAmount();
                    resp.checkTransactionReceipt.accountExpirationDate = session.getPlayer().getSubscriptionExpirationDate();
                    resp.checkTransactionReceipt.transactionValid = true;
                }else {
                    log.info("The PayOrder is not exist or status is 0 !  status:{}",payOrder.getOrdeStatus());
                }
                response.setData(resp);
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : "+e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameters not valid ... sessionId="+sessionId+" - param="+param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }



    @Override
    public FBWSResponse UnifiedOrderRun(final String sessionId,UnifiedOrderRunParam param) throws Exception {

        FBWSResponse response = new FBWSResponse();

        if (param != null){
            try {
                if (sessionId == null || sessionId.isEmpty()){
                    throw new FBWSException(FBExceptionType.SESSION_INVALID_SESSION_ID);
                }
                //check shopId exit

                //获取session,并且获得session中用户的信息
                FBSession session = ServiceTools.getAndCheckSession(sessionId);
                Player player = session.getPlayer() ;
                if (!playerMgr.existPseudo(player.getNickname())){
                    log.error("The Play is not exist ! ProductId:{}",param.shopId);
                    throw new FBWSException(FBExceptionType.PLAYER_NOT_EXIST);
                }

                Product product = productDAO.getProductForProductID(String.valueOf(param.shopId));
                if (product == null){
                    log.error("The Product is not exist ! ProductId:{}",param.shopId);
                    throw new FBWSException(FBExceptionType.PRODUCT_NOT_EXIST);
                }

                //生成订单记录，并且插入库中
                log.info("Send it to me-------userId:{}",player.getID());
                log.info("Send it to me-------shopId:{}",param.shopId);
                String localOrderNo = payOrderMgr.insertPayOrder(player.getID(), param.shopId, Constantes.WXPAY_TYPE) ;
                if (localOrderNo == null || localOrderNo.isEmpty()){
                    throw new FBWSException(FBExceptionType.LOCALORDERNO_MAKE_FAIL);
                }

                Map<String, String> respMap = payOrderMgr.WXStartOrder(sessionId);

                Map<String, String> finalresult = new HashMap<String, String>();
                finalresult.put("appid", PayConfig.APP_ID);
                finalresult.put("partnerid", PayConfig.MCH_ID);
                finalresult.put("prepayid", respMap.get("prepay_id"));
                finalresult.put("noncestr", respMap.get("nonce_str"));
                String tmpTM=String.valueOf(System.currentTimeMillis());
                tmpTM = tmpTM.substring(0,10);
                finalresult.put("timestamp",tmpTM);
                finalresult.put("package", "Sign=WXPay");
                finalresult.put("sign", WXPayUtil.generateSignature(finalresult, PayConfig.KEY, WXPayConstants.SignType.HMACSHA256));

                UnifiedOrderRunResponse resp = new UnifiedOrderRunResponse();
                resp.map = finalresult ;
                resp.localOrderNo = localOrderNo ;
                response.setData(resp);
                response.setData(resp);


            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        }else {
            log.warn("Parameter not valid");
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    @Override
    public String payCallBack(String xml) throws Exception {
        log.info("xml:{}",xml);
        MyConfig config = new MyConfig();
        WXPay wxpay = new WXPay(config);
        String resXml="";
        Map<String, String> notifyMap = WXPayUtil.xmlToMap(xml);

        String returnCode = (String) notifyMap.get("return_code");
        if("SUCCESS".equals(returnCode)){
            //测试
            if (wxpay.isPayResultNotifySignatureValid(notifyMap)) {

                String openid = (String) notifyMap.get("openid");
                String transaction_id = (String) notifyMap.get("transaction_id");
                String orderNumberMain = (String) notifyMap.get("out_trade_no");
                String total_fee = (String) notifyMap.get("total_fee");
                String time_end = (String) notifyMap.get("time_end");
                String attach = (String) notifyMap.get("attach");

                //tbd3检查out_trade_no，查看订单状态，决定是否处理
                //tbd4检查total_fee是不是和这单价格相同，是就给用户加房卡
                //tbd5 用户加房卡成功后，将顶状态改成1

                time_end = time_end.substring(0,4)+"-"+time_end.substring(4,6)+"-"+time_end.substring(6,8)+" "+time_end.substring(8,10)+":"+time_end.substring(10,12)+":"+time_end.substring(12,14);
                payOrderMgr.updatePayOrderAndPlayer(orderNumberMain,transaction_id,time_end,"",attach);

                resXml = "<xml>" + "<return_code><![CDATA[SUCCESS]]></return_code>"
                        + "<return_msg><![CDATA[OK]]></return_msg>" + "</xml> ";
            } else {
                log.info("sign check failed");
                resXml = "<xml>" + "<return_code><![CDATA[FAIL]]></return_code>"
                        + "<return_msg><![CDATA[FAILED]]></return_msg>" + "</xml> ";
            }
        }else{
            resXml = "<xml>" + "<return_code><![CDATA[FAIL]]></return_code>"
                    + "<return_msg><![CDATA[FAILED]]></return_msg>" + "</xml> ";
        }
        log.info(resXml);
        return resXml;
    }




}
