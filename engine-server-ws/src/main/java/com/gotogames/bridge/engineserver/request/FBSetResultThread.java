package com.gotogames.bridge.engineserver.request;

import com.gotogames.bridge.engineserver.user.UserVirtualFBServer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pserent on 13/04/2016.
 */
public class FBSetResultThread implements Runnable{
    private String result;
    private List<String> listAsyncID;
    private QueueMgr queueMgr;
    private long ts;
    private String urlSetResult;
    private UserVirtualFBServer userVirtualFBServer = null;
    private boolean logStat = false;

    public FBSetResultThread(QueueMgr queueMgr, String result, List<String> listAsyncID, String urlSetResult, UserVirtualFBServer userFBServer, long ts, boolean logStat) {
        this.result = result;
        this.listAsyncID = new ArrayList<>(listAsyncID);
        this.queueMgr = queueMgr;
        this.urlSetResult = urlSetResult;
        this.ts = ts;
        this.userVirtualFBServer = userFBServer;
        this.logStat = logStat;
    }

    @Override
    public void run() {
        queueMgr.sendResult(result, listAsyncID, urlSetResult, userVirtualFBServer, ts, logStat);
    }
}
