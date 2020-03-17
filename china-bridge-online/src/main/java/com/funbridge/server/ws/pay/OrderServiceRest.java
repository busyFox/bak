package com.funbridge.server.ws.pay;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.funbridge.server.ws.FBWSResponse;
import com.funbridge.server.ws.store.WSNewTransactionReceipt;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Map;

@Path("/pay")
public interface OrderServiceRest {
    @POST
    @Path("/alipay")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getOrderString(@HeaderParam("sessionId") String sessionId,GetOrderStringParam param);
    class GetOrderStringParam {
        public int shopId ;
        @JsonIgnore
        public String toString() {
            return  "shopid="+ shopId;
        }
    }
    class GetOrderStringResponse {
        public String orderString ;
        public String localOrderNo ;
    }

    /**
     * 支付宝支付回调接口
     * @param
     */
    @POST
    @Path("/alicallback")
    @Consumes("application/x-www-form-urlencoded")
    @Produces("application/json")
    String aliCallBack(@FormParam("notify_time") String notify_time,
                       @FormParam("notify_type") String notify_type,
                       @FormParam("notify_id") String notify_id,
                       @FormParam("app_id") String app_id,
                       @FormParam("charset") String charset,
                       @FormParam("version") String version,
                       @FormParam("sign_type") String sign_type,
                       @FormParam("sign") String sign,
                       @FormParam("trade_no") String trade_no,
                       @FormParam("out_trade_no") String out_trade_no,
                       @FormParam("out_biz_no") String out_biz_no,
                       @FormParam("buyer_id") String buyer_id,
                       @FormParam("buyer_logon_id") String buyer_logon_id,
                       @FormParam("seller_id") String seller_id,
                       @FormParam("seller_email") String seller_email,
                       @FormParam("trade_status") String trade_status,
                       @FormParam("total_amount") String total_amount,
                       @FormParam("receipt_amount") String receipt_amount,
                       @FormParam("invoice_amount") String invoice_amount,
                       @FormParam("buyer_pay_amount") String buyer_pay_amount,
                       @FormParam("point_amount") String point_amount,
                       @FormParam("refund_fee") String refund_fee,
                       @FormParam("subject") String subject,
                       @FormParam("body") String body,
                       @FormParam("gmt_create") String gmt_create	,
                       @FormParam("gmt_payment") String gmt_payment,
                       @FormParam("gmt_refund") String gmt_refund,
                       @FormParam("gmt_close") String gmt_close,
                       @FormParam("fund_bill_list") String fund_bill_list,
                       @FormParam("passback_params") String passback_params,
                       @FormParam("voucher_detail_list") String voucher_detail_list);

    @POST
    @Path("/getCheckResult")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getCheckResult(@HeaderParam("sessionId") String sessionId, GetCheckResultParam param);
    class GetCheckResultParam {
        public String localOrderNo;

        @JsonIgnore
        public boolean isValid() {
            return localOrderNo != null && localOrderNo.length() != 0;
        }

        @JsonIgnore
        public String toString() {
            return "localOrderNo=" + localOrderNo;
        }
    }

    class NewTransactionResponse {
        public WSNewTransactionReceipt checkTransactionReceipt;
    }

    @POST
    @Path("/wxstartorder")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse UnifiedOrderRun(@HeaderParam("sessionId") String sessionId,UnifiedOrderRunParam param) throws Exception;

    class UnifiedOrderRunParam {
        public int shopId ;
        @JsonIgnore
        public String toString() {
            return shopId + "+ shopId";
        }
    }
    class UnifiedOrderRunResponse {
        public Map<String, String> map ;
        public String localOrderNo ;
    }


    @Path("wxnotify")
    @POST
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.WILDCARD)
    String payCallBack(String xml) throws Exception;


}
