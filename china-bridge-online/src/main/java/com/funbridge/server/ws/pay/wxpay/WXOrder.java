package com.funbridge.server.ws.pay.wxpay;

import com.github.wxpay.sdk.WXPay;
import com.github.wxpay.sdk.WXPayUtil;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;


//??Controller?????????
@RestController
@RequestMapping("/pay")


public class WXOrder {
    Logger logger = LoggerFactory.getLogger(WXOrder.class);

    @RequestMapping(value = "/startorder", method = RequestMethod.GET)
    public Map<String, String> UnifiedOrderRun(@RequestParam("username") String username,@RequestParam("shopuid") String shopuid) throws Exception {
        //String returnStr="";
        MyConfig config = new MyConfig();
        WXPay wxpay = new WXPay(config);

        //tbd1???shopuid?????????
        //tbd2?????????????????????username??????0???????out_trade_no
        //tbd3???????????????????

        Map<String, String> data = new HashMap<String, String>();
        data.put("body", "123");
        data.put("out_trade_no", "123");
        data.put("device_info", "");
        data.put("fee_type", "CNY");
        data.put("total_fee", "???");
        data.put("spbill_create_ip", "1.1.1.1");
        data.put("notify_url", "http://www.example.com/wxpay/notify");
        data.put("trade_type", "APP");  // ?????APP??
        data.put("product_id", "1");

        try {
            Map<String, String> resp = wxpay.unifiedOrder(data);
            return resp;
        } catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    @RequestMapping(value = "/notify")
    public void wxNotify(HttpServletRequest request,HttpServletResponse response) throws Exception{
        MyConfig config = new MyConfig();

        BufferedReader br = new BufferedReader(new InputStreamReader((ServletInputStream)request.getInputStream()));
        String line = null;
        StringBuilder sb = new StringBuilder();
        while((line = br.readLine()) != null){
            sb.append(line);
        }
        br.close();
        //sb??????xml
        String notifyData = sb.toString();
        String resXml = "";
        logger.info("res:" + notifyData);

        WXPay wxpay = new WXPay(config);
        Map<String, String> notifyMap = WXPayUtil.xmlToMap(notifyData);  // ???map

        String returnCode = (String) notifyMap.get("return_code");
        if("SUCCESS".equals(returnCode)){
          //测试
            if (wxpay.isPayResultNotifySignatureValid(notifyMap)) {
                // ???????
                String openid = (String) notifyMap.get("openid");
                String transaction_id = (String) notifyMap.get("transaction_id");
                String orderNumberMain = (String) notifyMap.get("out_trade_no");
                String total_fee = (String) notifyMap.get("total_fee");

                //??????
                //tbd4??????out_trade_no???????????total_fee?????????????????????????
                //tbd5 ???????out_trade_no???????????????



                //?????????????
                resXml = "<xml>" + "<return_code><![CDATA[SUCCESS]]></return_code>"
                        + "<return_msg><![CDATA[OK]]></return_msg>" + "</xml> ";
            } else {
                logger.info("????????!?????");
            }
        }else{
            resXml = "<xml>" + "<return_code><![CDATA[FAIL]]></return_code>"
                    + "<return_msg><![CDATA[????]]></return_msg>" + "</xml> ";
        }
        logger.info(resXml);
        logger.info("??????????");

        BufferedOutputStream out = new BufferedOutputStream(
                response.getOutputStream());
        out.write(resXml.getBytes());
        out.flush();
        out.close();
    }

}