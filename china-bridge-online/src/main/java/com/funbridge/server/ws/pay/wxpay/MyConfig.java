package com.funbridge.server.ws.pay.wxpay;

import com.github.wxpay.sdk.IWXPayDomain;
import com.github.wxpay.sdk.WXPayConfig;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;


public class MyConfig implements WXPayConfig {

    private byte[] certData;

    public MyConfig() throws Exception {
        String certPath = PayConfig.CERT_PARTH;
        File file = new File(certPath);
        InputStream certStream = new FileInputStream(file);
        this.certData = new byte[(int) file.length()];
        certStream.read(this.certData);
        certStream.close();
    }

    @Override
    public String getAppID() {

        return PayConfig.APP_ID;
    }

    @Override
    public String getMchID() {
        return PayConfig.MCH_ID;
    }

    @Override
    public String getKey() {
        return PayConfig.KEY;
    }

    @Override
    public InputStream getCertStream() {
        ByteArrayInputStream certBis = new ByteArrayInputStream(this.certData);
        return certBis;
    }


    public int getHttpConnectTimeoutMs() {
        return 8000;
    }


    public int getHttpReadTimeoutMs() {
        return 10000;
    }


    public IWXPayDomain getWXPayDomain() {

        return WXPayDomainSimpleImpl.instance();
    }


    public boolean shouldAutoReport() {
        return false;
    }


    public int getReportWorkerNum() {
        return 6;
    }


    public int getReportQueueMaxSize() {
        return 10000;
    }


    public int getReportBatchSize() {
        return 10;
    }
}
