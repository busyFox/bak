package com.gotogames.bridge.engineserver.common;

import com.codahale.metrics.MetricRegistry;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Constantes {
	
	public static final long CRC_NOT_VALID = -1;
	
	/*****************************************************/
	/*
	 * REQUEST
	 */
	public static final int REQUEST_NB_FIELD = 7;
	public static final int REQUEST_INDEX_FIELD_DEAL = 0;
	public static final int REQUEST_INDEX_FIELD_GAME = 3;
	public static final int REQUEST_INDEX_FIELD_CONV = 2;
	public static final int REQUEST_INDEX_FIELD_OPTIONS = 1;
	public static final int REQUEST_INDEX_FIELD_TYPE = 4;
    public static final int REQUEST_INDEX_FIELD_NB_TRICKS_FOR_CLAIM = 5;
    public static final int REQUEST_INDEX_FIELD_CLAIM_PLAYER = 6;
	public static final String REQUEST_FIELD_SEPARATOR = ";";
	public static final int REQUEST_FIELD_OPTIONS_LENGTH = 10;
	public static final String REQUEST_BID_INFO_RESULT_SEPARATOR = ";";
	public static final String REQUEST_ENGINE_NO_RESULT = "NO_RESULT";
	
	public static final String REQUEST_DEAL_BIDINFO = "SSSSSSSSSSSSSWWWWWWWWWWWWWNNNNNNNNNNNNNEEEEEEEEEEEEE";
	public static final int REQUEST_TYPE_BID = 0;
	public static final int REQUEST_TYPE_CARD = 1;
	public static final int REQUEST_TYPE_BID_INFO = 2;
    public static final int REQUEST_TYPE_PAR = 3;
    public static final int REQUEST_TYPE_CLAIM = 5;
	
	/*****************************************************/
	/*
	 * USER
	 */
	public static final int USER_TYPE_ENGINE = 0;
	public static final int USER_TYPE_GAME_SERVER = 1;
	
	/*****************************************************/
	/*
	 * CONFIG
	 */
	private static final Map<String, Integer> mapConfigDeaultIntValue = new HashMap<String, Integer>();
	static {
		mapConfigDeaultIntValue.put("request.nbMaxListRequest",1);
		mapConfigDeaultIntValue.put("request.nbMaxEngineComputingRequest", 2);
		mapConfigDeaultIntValue.put("request.nbMaxRequestComputingByEngine", 1);
		mapConfigDeaultIntValue.put("session.timeoutSession", 3600);
		mapConfigDeaultIntValue.put("session.cleanSessionPeriod", 5);
		mapConfigDeaultIntValue.put("session.challengeRandomLength", 30);
		mapConfigDeaultIntValue.put("compute.test.automaticGenerateDeal", 0);
		mapConfigDeaultIntValue.put("general.check.userEngineConnected", 1);
	}
	public static int getConfigDefaultIntValue(String param) {
		Integer val = mapConfigDeaultIntValue.get(param);
		if (val != null) {
			return val.intValue();
		}
		return 0;
	}
	
	private static final Map<String, String> mapConfigDeaultStringValue = new HashMap<String, String>();
	static {
		//mapConfigDeaultStringValue.put("general.clientVersionRequired", "0.0.0");
	}
	
	public static String getConfigDefaultStringValue(String param) {
		String val = mapConfigDeaultStringValue.get(param);
		if (val != null) {
			return val;
		}
		return "";
	}

	private static final MetricRegistry metrics = new MetricRegistry();
	public static MetricRegistry getMetricRegistry() {
	    return metrics;
    }

    private static SimpleDateFormat sdfDateHour = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");
    private static SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy");
    public static String timestamp2StringDateHour(long ts) {
        return sdfDateHour.format(new Date(ts));
    }
    public static String timestamp2StringDate(long ts) {
        return sdfDate.format(new Date(ts));
    }
    public static long stringDateHour2Timestamp(String val) throws ParseException {
        return sdfDateHour.parse(val).getTime();
    }
    public static long stringDate2Timestamp(String val) throws ParseException {
        return sdfDate.parse(val).getTime();
    }

    /**
     * Convert int value to string hexa
     * @param value must be > 0
     * @return
     */
    public static String intToHexaString(int value) {
        StringBuilder sb = new StringBuilder();
        sb.append(Integer.toHexString(value));
        if (sb.length() %2 == 1) {
            sb.insert(0, "0");
        }
        return sb.toString().toUpperCase();
    }

    /**
     * Build options according to result type and engine version
     * @param resultType
     * @param engineVersion
     * @param engineSel
     * @param engineSpeed
     * @param spreadEnable
     * @return
     */
    public static String buildOptionsForEngine(int resultType, int engineVersion, int engineSel, int engineSpeed, int spreadEnable) {
        // options field : 0=engine selection - 1=engine speed - 2=result type - 3&4=engine version
        String result = "";
        result += intToHexaString(engineSel & 0xFF);
        result += intToHexaString(engineSpeed & 0xFF);
        result += intToHexaString(resultType & 0xFF);
        result += intToHexaString(engineVersion & 0xFF); // lowbyte
        result += intToHexaString((engineVersion & 0xFF00) >> 8); // highbyte
        result += intToHexaString(spreadEnable & 0xFF);
        return result;
    }
}
