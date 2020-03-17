package com.funbridge.server.common;

import com.gotogames.common.bridge.BridgeConstantes;
import org.apache.commons.lang.StringUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Constantes {
	
	public static final String CRYPT_KEY = "hXswKjF.wpi@sLl>):-8";
	public static final String SEPARATOR_VALUE = ";";
	public static final String VALUE_TEST = "test";
	public static final String APPID = "wx13e01543680ef9bb" ;
	public static final String SECRET = "958dbf8ffed5d40c196150a6f874b15d" ;
	public static final String LOGIN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token";
	public static final String ACCESSTOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token";
	public static final String USERINFO_URL = "https://api.weixin.qq.com/sns/userinfo";

	public static final int SMSCODE_VALID_TIME = 110 ;
	public static final int PHONE_TYPE = 0 ;
	public static final int OPPENID_TYPE = 1 ;
	public static final int START_HANDS = 10 ;
	public static final int Day_credit = 888; // 活动赠送牌局数
	public static final int ALIPAY_TYPE = 2 ;
    public static final int WXPAY_TYPE = 1 ;
	public static final int IOS_PAYORDER_TYPE = 0 ;
	public static final int NO_PAY = 0 ;
	public static final int YES_PAY = 1 ;


	public static long TIMESTAMP_SECOND = 1000;
    public static long TIMESTAMP_MINUTE = TIMESTAMP_SECOND*60;
    public static long TIMESTAMP_HOUR = TIMESTAMP_MINUTE*60;
    public static long TIMESTAMP_DAY = 24*TIMESTAMP_HOUR;

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
	 * Return timestamp for a measure unit (default minute) 度量单位的时间戳（默认分钟）
	 * @param unit
	 * @return
	 */
	public static long getTimestampByTimeUnit(String unit) {
		TimeUnit timeUnit = TimeUnit.valueOf(unit);
		if (timeUnit !=  null) {
			return TimeUnit.MILLISECONDS.convert(1, timeUnit);
		}
		return TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);
	}

	public static String getStringDateForNextDelayScheduler(ScheduledFuture<?> future) {
		if (future != null) {
			long delay = future.getDelay(TimeUnit.MILLISECONDS);
			if (delay > 0) {
				return timestamp2StringDateHour(System.currentTimeMillis() + delay);
			} else {
				return "expired , no future execution !";
			}
		}
		return null;
	}
	
	public static Calendar getNextDateForPeriod(Calendar ori, int periodMinute) {
		if (ori != null) {
			if (periodMinute > 0 && periodMinute <= 30) {
				ori.set(Calendar.SECOND, 0);
				ori.set(Calendar.MILLISECOND, 0);
				int nextMinute = ori.get(Calendar.MINUTE);
				nextMinute = ((nextMinute/periodMinute)+1)*periodMinute;
				if (nextMinute >= 60){
					nextMinute = 0;
					ori.add(Calendar.HOUR_OF_DAY, 1);
				}
				ori.set(Calendar.MINUTE, nextMinute);
				return ori;
			}
		}
		return Calendar.getInstance();
	}

	/**
	 * Format date for locale with SHORT format (ex : dd/MM/yyyy)
	 * 格式为简短格式的语言环境的日期（例如：dd / MM / yyyy）
	 * @param date
	 * @param lang
	 * @param country
	 * @return
	 */
	public static String formatShortDate(long date, String lang, String country) {
		SimpleDateFormat sdf = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT, getLocale(lang, country));
		String pattern = sdf.toPattern().replaceAll("\\byy\\b", "yyyy");
		sdf.applyPattern(pattern);
		return sdf.format(date);
	}

	/**
	 * format date for locale with MMMM yyyy (ex: may 2019)
	 * @param date
	 * @param lang
	 * @param country
	 * @return
	 */
	public static String formatMonthYear(long date, String lang, String country){
		SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", getLocale(lang, country));
		return sdf.format(date);
	}

	/**
	 * format date for locale with MMMM (ex: may)
	 * @param date
	 * @param lang
	 * @param country
	 * @return
	 */
	public static String formatMonth(long date, String lang, String country){
		SimpleDateFormat sdf = new SimpleDateFormat("MMMM", getLocale(lang, country));
		return sdf.format(date);
	}

	/**
	 * Format date for locale with SHORT format (ex : dd/MM)
	 * @param date
	 * @param lang
	 * @param country
	 * @return
	 */
	public static String formatDayMonthDate(long date, String lang, String country) {
		SimpleDateFormat sdf = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT, getLocale(lang, country));
		String pattern = sdf.toPattern().replaceAll("\\byy\\b", "yyyy")
				.replaceAll(".{1}\\byyyy\\b", "")
				.replaceAll("\\byyyy\\b.{1}", "");
		sdf.applyPattern(pattern);
		return sdf.format(date);
	}

	/**
	 * Format date and time for locale with SHORT format (ex : dd/MM/yyyy hh:mm)
	 * @param date
	 * @param lang
	 * @param country
	 * @return
	 */
    public static String formatShortDateTime(long date, String lang, String country) {
        TimeZone timeZone = getTimeZoneByCountry(country);
        SimpleDateFormat sdf;
        if (!timeZone.getID().equals("GMT")) {
            sdf = (SimpleDateFormat) DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, getLocale(lang, country));
        } else {
            sdf = (SimpleDateFormat) DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG, getLocale(lang, country));
        }
        String pattern = sdf.toPattern().replaceAll("\\byy\\b", "yyyy")
                .replaceAll("HH:mm:ss","HH:mm")
                .replaceAll("hh:mm:ss","hh:mm")
                .replaceAll("H:mm:ss","H:mm")
                .replaceAll("h:mm:ss","h:mm");
        sdf.applyPattern(pattern);
        sdf.setTimeZone(timeZone);
        return sdf.format(date);
    }

	/**
	 * Format time for locale with SHORT format (ex : hh:mm)
	 * 使用简短格式（例如：hh：mm）格式化语言环境的时间
	 * @param date
	 * @param lang
	 * @param country
	 * @return
	 */
    public static String formatShortTime(long date, String lang, String country) {
        TimeZone timeZone = getTimeZoneByCountry(country);
        SimpleDateFormat sdf;
        if (!timeZone.getID().equals("GMT")) {
            sdf = (SimpleDateFormat) DateFormat.getTimeInstance(DateFormat.SHORT, getLocale(lang, country));
        } else {
            sdf = (SimpleDateFormat) DateFormat.getTimeInstance(DateFormat.LONG, getLocale(lang, country));
        }
        String pattern = sdf.toPattern().replaceAll("HH:mm:ss","HH:mm")
                .replaceAll("hh:mm:ss","hh:mm")
                .replaceAll("H:mm:ss","H:mm")
                .replaceAll("h:mm:ss","h:mm");
        sdf.applyPattern(pattern);
        sdf.setTimeZone(timeZone);
        return sdf.format(date);
    }

	/**
	 * Get locale for lang and country
	 * 获取语言和国家/地区的语言环境
	 * @param lang
	 * @param country
	 * @return
	 */
	private static Locale getLocale(String lang, String country) {
		if (lang != null || country != null) {
			if (country.equalsIgnoreCase("gb")) {
				return Locale.UK;
			}
			if (lang.equals("zh_hans")) {
				return Locale.SIMPLIFIED_CHINESE;
			}
			if (lang.equals("zh_hant")) {
				return Locale.TRADITIONAL_CHINESE;
			}
			if (lang.equals("nb")) {
				return new Locale("no", country);
			}
			return new Locale(lang, country);
		}
		return Locale.UK;
	}

	/**
	 * List of countries in the same timezone as France
	 * 与法国相同时区的国家列表
	 */
	private static final List<String> frenchTimezoneCountries = new ArrayList<>();
	static {
		frenchTimezoneCountries.add("FR"); // France
		frenchTimezoneCountries.add("AL"); // Albania
		frenchTimezoneCountries.add("DE"); // Germany
		frenchTimezoneCountries.add("AD"); // Andorra
		frenchTimezoneCountries.add("AT"); // Austria
		frenchTimezoneCountries.add("BE"); // Belgium
		frenchTimezoneCountries.add("BA"); // Bosnia and Herzegovina
		frenchTimezoneCountries.add("HR"); // Croatia
		frenchTimezoneCountries.add("DK"); // Denmark
		frenchTimezoneCountries.add("ES"); // Spain
		frenchTimezoneCountries.add("HU"); // Hungary
		frenchTimezoneCountries.add("IT"); // Italy
		frenchTimezoneCountries.add("LI"); // Liechtenstein
		frenchTimezoneCountries.add("LU"); // Luxembourg
		frenchTimezoneCountries.add("MK"); // Republic of Macedonia
		frenchTimezoneCountries.add("MT"); // Malta
		frenchTimezoneCountries.add("MC"); // Monaco
		frenchTimezoneCountries.add("ME"); // Montenegro
		frenchTimezoneCountries.add("NO"); // Norway
		frenchTimezoneCountries.add("NL"); // Netherlands
		frenchTimezoneCountries.add("PL"); // Poland
		frenchTimezoneCountries.add("CS"); // Czech Republic
		frenchTimezoneCountries.add("SM"); // San Marino
		frenchTimezoneCountries.add("RS"); // Serbia
		frenchTimezoneCountries.add("SK"); // Slovakia
		frenchTimezoneCountries.add("SI"); // Slovenia
		frenchTimezoneCountries.add("SE"); // Sweden
		frenchTimezoneCountries.add("CH"); // Switzerland
		frenchTimezoneCountries.add("VA"); // Vatican
	}

	/**
	 * Get timezone of a country. If multiple timezones in the country, returns timezone GMT
	 * 获取一个国家的时区。 如果该国家/地区有多个时区，则返回格林尼治标准时间
	 * @param country
	 * @return
	 */
    private static TimeZone getTimeZoneByCountry(String country){
        if (frenchTimezoneCountries.contains(country)) {
        	return TimeZone.getDefault();
		}
        return TimeZone.getTimeZone("GMT");
    }



    public static long getIDLongValue(String IDstr, long ID) {
        if (IDstr != null && IDstr.length() > 0 && StringUtils.isNumeric(IDstr)) {
            return Long.parseLong(IDstr);
        }
        return ID;
    }
	
	/***********************************************/
	/*
	 * CARD and BID
	 */
	
	public static final int CONTRACT_LEAVE = -1;
	public static final int CONTRACT_TYPE_PASS = 0;
	public static final int CONTRACT_TYPE_NORMAL = 1;
	public static final int CONTRACT_TYPE_X1 = 2;
	public static final int CONTRACT_TYPE_X2 = 3;
	/**
	 * Return the string representation of this contract (contract and contractType).
	 * 返回此合同的字符串表示形式（contract和contractType）。
	 * Exemple : contract=1H contractType=X1, return "1HX1"
	 * @param contract
	 * @param contractType
	 * @return
	 */
	public static String contractToString(String contract, int contractType) {
		String result = "";
		if (contractType == CONTRACT_LEAVE) {
			result = "";
		}
		else if (contractType == CONTRACT_TYPE_PASS) {
			result = BridgeConstantes.BID_PASS;
		} else {
			result = contract;
			if (contractType == Constantes.CONTRACT_TYPE_X1) {
				result = result+"X1";
			} else if (contractType == Constantes.CONTRACT_TYPE_X2){
				result = result+"X2";
			}
		}
		return result;
	}
	
	/**
	 * Return the contract from the contractString. Exemple "1HX1" => "1H"
	 * 通过contractString返回contract。 范例“ 1HX1” =>“ 1H”
	 * @param contractString
	 * @return
	 */
	public static String contractStringToContract(String contractString) {
		if(contractString != null){
			if (contractString.length() == 2 || contractString.length() == 4) {
				return contractString.substring(0, 2);
			}
		}
		return "";
	}
	
	/**
	 * Return the contract type for the contractString. Exemple "1HX1" => 2 (X1)
	 * 通过contractString返回contract type
	 * @param contractString
	 * @return
	 */
	public static int contractStringToType(String contractString) {
		if (contractString.equals(BridgeConstantes.BID_PASS)) {
			return CONTRACT_TYPE_PASS;
		} else if ((contractString.length() == 2) || (contractString.length() == 4)) {
			if (contractString.length() == 2) {
				return CONTRACT_TYPE_NORMAL;
			} else if (contractString.substring(2).equals(BridgeConstantes.BID_X1)) {
				return CONTRACT_TYPE_X1;
			} else if (contractString.substring(2).equals(BridgeConstantes.BID_X2)) {
				return CONTRACT_TYPE_X2;
			}
		}
		return -1;
	}
	
	/***********************************************/
	/*
	 * GAME
	 */
	public static final int GAME_SCORE_INVALID = -99999;
	public static final int GAME_SCORE_LEAVE = -32000;
	public static final int GAME_SCORE_PASS = 0;
	public static final char GAME_INDICATOR_CLAIM = '!'; // claim all remaining cards
    public static final String GAME_BIDCARD_SEPARATOR = "-";
	
	public static final int GAME_MODE_ROBOT_UNKNOWN = 0;
	public static final int GAME_MODE_ROBOT_NORMAL = 1;
	public static final int GAME_MODE_ROBOT_PASS = 2;
	
	public static final String GAME_CONVENTION_SEPARATOR = ";";
	public static final String GAME_CONVENTION_ENGINE_COSTEL = "cs";
	public static final String GAME_CONVENTION_ENGINE_ARGINE = "ar";
	
	/***********************************************/
	/*
	 * TABLE
	 */
	public static final int TABLE_POSITION_SOUTH = 0;
	public static final int TABLE_POSITION_WEST = 1;
	public static final int TABLE_POSITION_NORTH = 2;
	public static final int TABLE_POSITION_EAST = 3;
	
	public static final int TABLE_PLAYER_STATUS_NOT_PRESENT = 0;
	public static final int TABLE_PLAYER_STATUS_PRESENT = 1;
	
	/**
	 * Return the index of the table for a position : S => 0, N => 2 ...
	 * 返回表的位置索引：S => 0，N => 2 ...
	 * @param position
	 * @return
	 */
	public static int getTablePosition(char position) {
		if (position == BridgeConstantes.POSITION_SOUTH){
			return TABLE_POSITION_SOUTH;
		} else if (position == BridgeConstantes.POSITION_NORTH){
			return TABLE_POSITION_NORTH;
		} else if (position == BridgeConstantes.POSITION_WEST){
			return TABLE_POSITION_WEST;
		} else if (position == BridgeConstantes.POSITION_EAST){
			return TABLE_POSITION_EAST;
		} 
		return -1;
	}
	/**
	 * Return the string position for the table index
	 * 返回表索引的字符串位置
	 * @param idx
	 * @return
	 */
	public static char getPositionForTableIndex(int idx) {
		switch (idx) {
		case TABLE_POSITION_SOUTH:
			return BridgeConstantes.POSITION_SOUTH;
		case TABLE_POSITION_WEST:
			return BridgeConstantes.POSITION_WEST;
		case TABLE_POSITION_NORTH:
			return BridgeConstantes.POSITION_NORTH;
		case TABLE_POSITION_EAST:
			return BridgeConstantes.POSITION_EAST;
		default:
			return BridgeConstantes.POSITION_NOT_VALID;
		}
	}
	/**
	 * Check if position is a valid value (N, S, E or W)
	 * 检查位置是否为有效值（N，S，E或W）
	 * @param position
	 * @return
	 */
	public static boolean isTablePositionValid(char position) {
		return getTablePosition(position) != -1;
	}
	
	/***********************************************/
	/*
	 * TOURNAMENT 比赛
	 */
	public static final int TOURNAMENT_RESULT_UNKNOWN = 0;
	public static final int TOURNAMENT_RESULT_PAIRE = 1;
	public static final int TOURNAMENT_RESULT_IMP = 2;
	public static final int TOURNAMENT_RESULT_IMP2 = 3;
	public static final int TOURNAMENT_CATEGORY_TRAINING = 1;
	public static final String TOURNAMENT_CATEGORY_NAME_TRAINING = "TRAINING";
	public static final int TOURNAMENT_CATEGORY_TOURDAY = 2;
	public static final String TOURNAMENT_CATEGORY_NAME_TOURDAY = "TOUR_4_6";
	public static final int TOURNAMENT_CATEGORY_TRAINING_PARTNER = 4;
	public static final String TOURNAMENT_CATEGORY_NAME_TRAINING_PARTNER = "TRAINING_PARTNER";
	public static final int TOURNAMENT_CATEGORY_DUEL = 5;
	public static final String TOURNAMENT_CATEGORY_NAME_DUEL = "DUEL";
	public static final int TOURNAMENT_CATEGORY_TIMEZONE = 6;
	public static final String TOURNAMENT_CATEGORY_NAME_TIMEZONE = "TIMEZONE";
	public static final String TOURNAMENT_CATEGORY_NAME_TRIAL = "TRIAL";
    public static final int TOURNAMENT_CATEGORY_NEWSERIE = 8;
    public static final String TOURNAMENT_CATEGORY_NAME_NEWSERIE = "NEWSERIE";
	public static final int TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE = 10;
	public static final String TOURNAMENT_CATEGORY_NAME_SERIE_TOP_CHALLENGE = "SERIE_TOP_CHALLENGE";
	public static final int TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE = 29;
	public static final String TOURNAMENT_CATEGORY_NAME_SERIE_EASY_CHALLENGE = "SERIE_EASY_CHALLENGE";
    public static final int TOURNAMENT_CATEGORY_TEAM = 12;
    public static final String TOURNAMENT_CATEGORY_NAME_TEAM = "TEAM";
    public static final int TOURNAMENT_CATEGORY_PRIVATE = 15;
    public static final String TOURNAMENT_CATEGORY_NAME_PRIVATE= "PRIVATE";
	public static final int TOURNAMENT_CATEGORY_TOUR_CBO = 20;
	public static final String TOURNAMENT_CATEGORY_NAME_TOUR_CBO = "TOUR_CBO";
	public static final String TOURNAMENT_CBO = "CBO";
	public static final int TOURNAMENT_CATEGORY_LEARNING = 22;
	public static final String TOURNAMENT_CATEGORY_NAME_LEARNING = "LEARNING";

    public static String tourCategory2Name(int catID) {
		switch (catID) {
		case TOURNAMENT_CATEGORY_TRAINING:
			return TOURNAMENT_CATEGORY_NAME_TRAINING;
		case TOURNAMENT_CATEGORY_TOURDAY:
			return TOURNAMENT_CATEGORY_NAME_TOURDAY;
		case TOURNAMENT_CATEGORY_TRAINING_PARTNER:
			return TOURNAMENT_CATEGORY_NAME_TRAINING_PARTNER;
		case TOURNAMENT_CATEGORY_DUEL:
			return TOURNAMENT_CATEGORY_NAME_DUEL;
		case TOURNAMENT_CATEGORY_TIMEZONE:
			return TOURNAMENT_CATEGORY_NAME_TIMEZONE;
		case TOURNAMENT_CATEGORY_NEWSERIE:
            return TOURNAMENT_CATEGORY_NAME_NEWSERIE;
        case TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE:
            return TOURNAMENT_CATEGORY_NAME_SERIE_TOP_CHALLENGE;
        case TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE:
			return TOURNAMENT_CATEGORY_NAME_SERIE_EASY_CHALLENGE;
        case TOURNAMENT_CATEGORY_TEAM:
            return TOURNAMENT_CATEGORY_NAME_TEAM;
        case TOURNAMENT_CATEGORY_PRIVATE:
            return TOURNAMENT_CATEGORY_NAME_PRIVATE;
        case TOURNAMENT_CATEGORY_TOUR_CBO:
            return TOURNAMENT_CATEGORY_NAME_TOUR_CBO;
		case TOURNAMENT_CATEGORY_LEARNING:
			return TOURNAMENT_CATEGORY_NAME_LEARNING;
		default:
			return "UNKNOWN";
		}
	}
	public static int tournamentResult2option(int tournamentResult) {
		switch (tournamentResult) {
		case TOURNAMENT_RESULT_PAIRE:
			return 0;
		case TOURNAMENT_RESULT_IMP:
			return 1;
		case TOURNAMENT_RESULT_IMP2:
			return 1;
		default:
			return 0;
		}
	}

    public static String tournamentResultType2String(int resultType) {
        switch (resultType) {
            case TOURNAMENT_RESULT_IMP:
                return "IMP";
            case TOURNAMENT_RESULT_IMP2:
                return "IMP";
            case TOURNAMENT_RESULT_PAIRE:
                return "PAIRE";
            default:
                return "UNKNOWN";
        }
    }
	
	public static final int TOURNAMENT_CHALLENGE_STATUS_INIT = 0;
	public static final int TOURNAMENT_CHALLENGE_STATUS_WAITING = 1;
	public static final int TOURNAMENT_CHALLENGE_STATUS_PLAY = 2;
	public static final int TOURNAMENT_CHALLENGE_STATUS_END = 3;
	
	public static final String TOURNAMENT_SETTINGS_MODE_RANDOM = "TrainingModeRandom";
	public static final String TOURNAMENT_SETTINGS_MODE_POINTS_FOR_NS = "TrainingModePointsForNS";
	public static final String TOURNAMENT_SETTINGS_MODE_ENCHERIE_A2 = "TrainingModeEncherireA2";
	public static final String TOURNAMENT_SETTINGS_MODE_THEME = "TrainingModeByTheme";
	public static final String TOURNAMENT_SETTINGS_MODE_ADVANCED = "TrainingModeAdvanced";
	
	public static final String TOURNAMENT_TIMEZONE_EUROPE = "EUROPE";
	public static final String TOURNAMENT_TIMEZONE_AMERICA_WEST = "AMERICA_WEST";
	public static final String TOURNAMENT_TIMEZONE_AMERICA_EAST = "AMERICA_EAST";
	public static final String TOURNAMENT_TIMEZONE_ASIA = "ASIA";


    public static int computeTeamPoints(int nbTotal, int rank, int nbSameRank) {
        return 0;
    }
	/**
	 * Compute Paire result : 计算配对结果
				val = NbTotalPlayer - NbPlayerBefore - (NbPLayerSameScore + 1)/2
				percent = val / (NbTotalPlayer - 1)
	 * @param nbTotalPlayer number of player who have played
	 * @param nbPlayerBefore number of player with best score
	 * @param nbPlayerSameScore number of player with same scores
	 * @return
	 */
	public static double computeResultPaire(int nbTotalPlayer, int nbPlayerBefore, int nbPlayerSameScore) {
		if (nbTotalPlayer < 1) {
			return 0;
		}
		// only one player => 50%
		if (nbTotalPlayer == 1) {
			return 0.5;
		}
		// First and alone ! => 1
		if (nbPlayerBefore == 0 && nbPlayerSameScore == 0) {
			return 1;
		}
		double val = nbTotalPlayer - nbPlayerBefore - (((double)nbPlayerSameScore+1)/2);
		if (val < 0) {
			return 0;
		}
		val = val / (nbTotalPlayer - 1);
		return val;
	}
	
	/**
	 * Compute IMP result:计算IMP结果
	 *  diff = scoreAverage - scorePlayer
	 *  val = table[diff]
	 *  0 - 10 : 0 IMP
	 *  20 - 40 : 1 IMP
	 *  50 - 80 : 2 IMP
	 *  90 - 120 : 3 IMP
	 *  130 - 160 : 4 IMP
	 *  170 - 210 : 5 IMP
	 *  220 - 260 : 6 IMP
	 *  270 - 310 : 7 IMP
	 *  320 - 360 : 8 IMP
	 *  370 - 420 : 9 IMP
	 *  430 - 490 : 10 IMP
	 *  500 - 590 : 11 IMP
	 *  600 - 740 : 12 IMP
	 *  750 - 890 : 13 IMP
	 *  900 - 1090 : 14 IMP
	 *  1100 - 1290 : 15 IMP
	 *  1300 - 1490 : 16 IMP
	 *  1500 - 1740 : 17 IMP
	 *  1750 - 1990 : 18 IMP
	 *  2000 - 2240 : 19 IMP
	 *  2250 - 2490 : 20 IMP
	 *  2500 - 2990 : 21 IMP
	 *  3000 - 3490 : 22 IMP
	 *  3500 - 3990 : 23 IMP
	 *  4000 et + : 24 IMP
	 * @param scoreAverage
	 * @param scorePlayer
	 * @return
	 */
	public static int computeResultIMP(int scoreAverage, int scorePlayer) {
		int diff = 0;
		boolean bNegative = false;
		if (scoreAverage > scorePlayer) {
			diff = scoreAverage - scorePlayer;
			bNegative = true;
		} else {
			diff = scorePlayer - scoreAverage;
		}
		int val = 0;
		if (0 <= diff && diff < 10){val = 0;}
		else if (10 < diff && diff <= 40) {val = 1;}
		else if (40 < diff && diff <= 80) {val = 2;}
		else if (80 < diff && diff <= 120) {val = 3;}
		else if (120 < diff && diff <= 160) {val = 4;}
		else if (160 < diff && diff <= 210) {val = 5;}
		else if (210 < diff && diff <= 260) {val = 6;}
		else if (260 < diff && diff <= 310) {val = 7;}
		else if (310 < diff && diff <= 360) {val = 8;}
		else if (360 < diff && diff <= 420) {val = 9;}
		else if (420 < diff && diff <= 490) {val = 10;}
		else if (490 < diff && diff <= 590) {val = 11;}
		else if (590 < diff && diff <= 740) {val = 12;}
		else if (740 < diff && diff <= 890) {val = 13;}
		else if (890 < diff && diff <= 1090) {val = 14;}
		else if (1090 < diff && diff <= 1290) {val = 15;}
		else if (1290 < diff && diff <= 1490) {val = 16;}
		else if (1490 < diff && diff <= 1740) {val = 17;}
		else if (1740 < diff && diff <= 1990) {val = 18;}
		else if (1990 < diff && diff <= 2240) {val = 19;}
		else if (2240 < diff && diff <= 2490) {val = 20;}
		else if (2490 < diff && diff <= 2990) {val = 21;}
		else if (2990 < diff && diff <= 3490) {val = 22;}
		else if (3490 < diff && diff <= 3990) {val = 23;}
		else if (3990 < diff ) {val = 24;}
		
		if (bNegative) {
			val = -val;
		}
		return val;
	}
	
	/***********************************************/
	/*
	 * PLAYER
	 */
	public static final int PLAYER_TYPE_HUMAN = 0;
	public static final int PLAYER_TYPE_ROBOT = 1;
	public static final int PLAYER_ID_ROBOT = -1;
	public static final String PLAYER_ROBOT_PSEUDO = "ROBOT";
	public static final int PLAYER_STATUS_UNDEFINED = 0;//"UNDEFINED";
	public static final int PLAYER_STATUS_PRESENT = 1;//"PRESENT";
	public static final int PLAYER_STATUS_ABSENT = 2;//"ABSENT";
	public static final String PLAYER_COUNTRY_NOT_VISIBLE = "00";
    public static final String PLAYER_DISABLE_PATTERN = "DELETED_";
    public static final String PLAYER_DISABLE_PSEUDO = "*** Deleted Player ***";

	public static final int PLAYER_DESCRIPTION_MAX_LENGTH = 255;
	public static final int PLAYER_PSEUDO_MAX_LENGTH = 255;
	public static final int PLAYER_MAIL_MAX_LENGTH = 255;
	public static final int PLAYER_LANG_MAX_LENGTH = 10;
	public static final int PLAYER_PASSWORD_MAX_LENGTH = 255;
	public static final int PLAYER_FIRSTNAME_MAX_LENGTH = 45;
	public static final int PLAYER_LASTNAME_MAX_LENGTH = 45;
	public static final int PLAYER_TOWN_MAX_LENGTH = 45;
	public static final int PLAYER_COUNTRY_MAX_LENGTH = 10;
	public static final int PLAYER_LINK_MESSAGE_MAX_LENGTH = 256;
	
	public static final int PLAYER_LINK_TYPE_FOLLOWER = 1;
	public static final int PLAYER_LINK_TYPE_FRIEND = 2;
	public static final int PLAYER_LINK_TYPE_FRIEND_PENDING = 4;
	public static final int PLAYER_LINK_TYPE_BLOCKED = 8;
	public static final int PLAYER_LINK_TYPE_WAY = 16;
	public static final int PLAYER_FRIEND_MAX = 500;
	
	public static final int PLAYER_FUNBRIDGE_ID = -1;
	public static final String PLAYER_FUNBRIDGE_PSEUDO = "Funbridge";
    public static final String PLAYER_FUNBRIDGE_COUNTRY = "01";
    public static final int PLAYER_ARGINE_ID = -2;
    public static final int DEVICE_ID_PLAYER_ARGINE = -1;
	
	public static final String PLAYER_LANG_FR = "fr";
	public static final String PLAYER_LANG_NL = "nl";
	public static final String PLAYER_LANG_EN = "en";
	
	public static final int PLAYER_SEX_NOT_DEFINED = 0;
	public static final int PLAYER_SEX_WOMAN = 1;
	public static final int PLAYER_SEX_MAN = 2;
	
	public static final int PLAYER_DUEL_STATUS_NONE = 0;
	public static final int PLAYER_DUEL_STATUS_PLAYING = 1;
	public static final int PLAYER_DUEL_STATUS_REQUEST_PLAYER1 = 2;
	public static final int PLAYER_DUEL_STATUS_REQUEST_PLAYER2 = 3;
	public static final int PLAYER_DUEL_STATUS_PLAYED = 4;
	
	public static final int PLAYER_DUEL_RESET_REQUEST_NONE = 0;
	public static final int PLAYER_DUEL_RESET_REQUEST_PLAYER1 = 1;
	public static final int PLAYER_DUEL_RESET_REQUEST_PLAYER2 = 2;

    public static final int PLAYER_CREDENTIALS_FLAG_PSEUDO = (int)Math.pow(2, 0); // 1
    public static final int PLAYER_CREDENTIALS_FLAG_MAIL = (int)Math.pow(2, 1); // 2
    public static final int PLAYER_CREDENTIALS_FLAG_PASSWORD = (int)Math.pow(2, 2); // 4

	public static final String PLAYER_DELETED="player.deleted";

	/***********************************************/
	/*
	 * RESULT ATTRIBUT 结果属性
	 */
	public static final String RESULT_ATTRIBUT_PLAYER_CONTRACT = "PlayerContract";
	public static final String RESULT_ATTRIBUT_INDEX_PLAYER = "IndexResultPlayer";
	public static final String RESULT_ATTRIBUT_TOTAL_PLAYER = "NbTotalPlayer";
	public static final String RESULT_ATTRIBUT_DEAL_INDEX = "DealIndex";
	public static final String RESULT_ATTRIBUT_SCORE_AVERAGE = "ScoreAverage";
	public static final String RESULT_ATTRIBUT_RESULT_TYPE = "ResultType";
	public static final String RESULT_ATTRIBUT_TOURNAMENT_NB_PLAYER = "TournamentNbPlayer";
	public static final String RESULT_ATTRIBUT_TOURNAMENT_RANK = "TournamentRank";
	public static final String RESULT_ATTRIBUT_TOURNAMENT_RESULT = "TournamentResult";
	public static final String RESULT_ATTRIBUT_TOURNAMENT_NB_DEAL = "TournamentNbDeal";
	
	/***********************************************/
	/*
	 * EVENT 事件
	 */
	public static final int EVENT_RECEIVER_ALL = -2; 
	public static final int EVENT_PLAYER_ID_SERVER = -1;
	public static final String EVENT_FIELD_CATEGORY = "CATEGORY";
	public static final String EVENT_FIELD_TYPE = "TYPE";
	public static final String EVENT_CATEGORY_GAME = "GAME";
	public static final String EVENT_CATEGORY_MESSAGE = "MESSAGE";
	public static final String EVENT_CATEGORY_CHAT_MESSAGE = "CHAT_MESSAGE";
	public static final String EVENT_CATEGORY_CHAT = "CHAT";
	public static final String EVENT_CATEGORY_CHALLENGE = "CHALLENGE";
	public static final String EVENT_CATEGORY_PLAYER = "PLAYER";
	public static final String EVENT_CATEGORY_DUEL = "DUEL";
	public static final String EVENT_CATEGORY_TEAM = "TEAM";
    public static final String EVENT_CATEGORY_GENERAL = "GENERAL";
    public static final String EVENT_CATEGORY_RPC = "RPC";
	public static final String EVENT_FIELD_TABLE_ID = "TABLE_ID";
	public static final String EVENT_FIELD_GAME_ID = "GAME_ID";
    /* TYPE GENERAL  一般类型 */
    public static final String EVENT_TYPE_GENERAL_DISCONNECT = "DISCONNECT";
    public static final String EVENT_VALUE_DISCONNECT_CONNECT_ANOTHER_DEVICE = "CONNECT_ANOTHER_DEVICE";
	public static final String EVENT_VALUE_SUPPORT_CONNECT_DEVICE = "CONNECT_SUPPORT_DEVICE";
    public static final String EVENT_VALUE_DISCONNECT_MAINTENANCE = "MAINTENANCE";
    public static final String EVENT_VALUE_DISCONNECT_UPDATE_PLAYER = "UPDATE_PLAYER";
	/* TYPE GAME  游戏类型*/
	public static final String EVENT_TYPE_GAME_CHANGE_PLAYER_STATUS = "CHANGE_PLAYER_STATUS";
	public static final String EVENT_TYPE_GAME_CHANGE_PLAYER_STATUS_2 = "CHANGE_PLAYER_STATUS_2";
	public static final String EVENT_TYPE_GAME_BEGIN_GAME = "BEGIN_GAME";
	public static final String EVENT_TYPE_GAME_END_GAME = "END_GAME";
	public static final String EVENT_TYPE_GAME_CURRENT_PLAYER = "CURRENT_PLAYER";
	public static final String EVENT_TYPE_GAME_BID = "BID_PLAYED";
	public static final String EVENT_TYPE_GAME_CARD = "CARD_PLAYED";
	public static final String EVENT_TYPE_GAME_END_TRICK = "END_TRICK";
	public static final String EVENT_TYPE_GAME_END_BIDS = "END_BIDS";
	public static final String EVENT_TYPE_GAME_END_TOURNAMENT = "END_TOURNAMENT";
	public static final String EVENT_TYPE_GAME_ENGINE_ERROR = "GAME_ENGINE_ERROR";
	public static final String EVENT_TYPE_GAME_SPREAD = "GAME_SPREAD";
	public static final String EVENT_TYPE_GAME_LEAVE = "GAME_LEAVE";
	public static final String EVENT_TYPE_GAME_SPREAD_RESULT = "GAME_SPREAD_RESULT";
	public static final String EVENT_TYPE_GAME_CLAIM = "GAME_CLAIM";
    public static final String EVENT_TYPE_GAME_REMAINING_CARDS = "REMAINING_CARDS";
    public static final String EVENT_TYPE_GAME_ADVICE = "ADVICE";

	/* TYPE CHALLENGE 挑战类型*/
	public static final String EVENT_TYPE_CHALLENGE_REQUEST = "CHALLENGE_REQUEST";
	public static final String EVENT_TYPE_CHALLENGE_RESPONSE = "CHALLENGE_RESPONSE";
	
	/* TYPE PLAYER  玩家类型*/
	public static final String EVENT_TYPE_PLAYER_CHANGE_LINK = "CHANGE_LINK";
	public static final String EVENT_TYPE_PLAYER_FRIEND_CONNECTED = "FRIEND_CONNECTED";
    public static final String EVENT_TYPE_PLAYER_UPDATE = "PLAYER_UPDATE";
	
	public static final int EVENT_TYPE_DUEL_REQUEST = 0;
	public static final int EVENT_TYPE_DUEL_RESET = 1;
	public static final int EVENT_TYPE_DUEL_ACCEPT = 2;
	public static final int EVENT_TYPE_DUEL_REFUSE = 3;
	public static final int EVENT_TYPE_DUEL_UPDATE = 4;
    public static final int EVENT_TYPE_DUEL_MATCH_MAKING = 5;
	
	public static final double EVENT_FRQUENCY_FAST = 2;
	public static final double EVENT_FRQUENCY_MEDIUM = 5;
	public static final double EVENT_FRQUENCY_SLOW = 10;

	/* TYPE CHAT  聊天类型*/
	public static final String EVENT_TYPE_CHAT_KICKED_FROM_CHATROOM = "KICKED_FROM_CHATROOM";
	
	/*****************************************************/
	/* 
	 * ENGINE 引擎
	 */
	public static final int ENGINE_REQUEST_TYPE_BID = 0;
	public static final int ENGINE_REQUEST_TYPE_CARD = 1;
	public static final int ENGINE_REQUEST_TYPE_BID_INFO = 2;
    public static final int ENGINE_REQUEST_TYPE_PAR = 3;
    public static final int ENGINE_REQUEST_TYPE_ADVICE = 4;
    public static final int ENGINE_REQUEST_TYPE_CLAIM = 5;
	public static final String ENGINE_RESULT_NULL = "null";
    public static final String ENGINE_RESULT_EMPTY = "EMPTY";
	
	/*********************************************************/
	/*
	 * SERIE
	 */
	public static final String SERIE_NOT_DEFINED = "??";

	
	/******************************************************/
	/*
	 * PERIOD 时期
	 */
	public static final String PERIOD_TYPE_DAY = "day";
	public static final String PERIOD_TYPE_WEEK = "week";
	public static final String PERIOD_TYPE_2WEEK = "2week";
	public static final String PERIOD_TYPE_HALFDAY = "halfday";
	public static final int PERIOD_TIME_LAST = 10; //temps limite avant la fin pour démarrer un tournoi (en minute)
	
	/*****************************************************/
	/*
	 * DEVICE 设备
	 */
	public static final String DEVICE_TYPE_IOS = "ios";
	public static final String DEVICE_TYPE_MAC = "mac";
	public static final String DEVICE_TYPE_ANDROID = "android";
	public static final String DEVICE_TYPE_WEB = "web";
    public static final String DEVICE_TYPE_AMAZON = "amazon";
    public static final String DEVICE_TYPE_WINPHONE = "winphone";
    public static final String DEVICE_TYPE_WINRT = "winrt";
    public static final String DEVICE_TYPE_WINPC = "winpc";

	/*****************************************************/
	/*
	 * CREDIT 信用
	 */
	public static final String PRODUCT_TYPE_BONUS = "BONUS";
	public static final String PRODUCT_TYPE_SUPPORT = "SUPPORT";
	public static final String PRODUCT_TYPE_WEB = "WEB";
	public static final String PRODUCT_TYPE_IOS = "IOS";
	public static final String PRODUCT_TYPE_ANDROID = "ANDROID";
 	
	public static final int PRODUCT_CATEGORY_DEAL = 0;
	public static final int PRODUCT_CATEGORY_SUBSCRIPTION = 1;
	public static final int PRODUCT_CATEGORY_TOUR_CBO = 10;

	public static final String PRODUCT_CREDIT_TYPE_DEAL = "DEAL";
	public static final String PRODUCT_CREDIT_TYPE_DAY = "DAY";
	public static final String PRODUCT_CREDIT_TYPE_MONTH = "MONTH";
	public static final String PRODUCT_CREDIT_TYPE_YEAR = "YEAR";

	/************************************************************/
	/*
	 * MESSAGE 信息
	 */
	public static final int MESSAGE_TYPE_ALERT = (int)Math.pow(2, 0); // 1
	public static final int MESSAGE_TYPE_MSG = (int)Math.pow(2, 1); // 2
	public static final int MESSAGE_TYPE_ALL = MESSAGE_TYPE_ALERT | MESSAGE_TYPE_MSG;

	public static final String MESSAGE_TEMPLATE_EVENT_CHALLENGE_REQUEST = "event_challenge_request";
	public static final String MESSAGE_TEMPLATE_EVENT_CHALLENGE_REQUEST_RESET = "event_challenge_request_reset";
	public static final String MESSAGE_TEMPLATE_EVENT_CHALLENGE_RESPONSE_OK = "event_challenge_response_ok";
	public static final String MESSAGE_TEMPLATE_EVENT_CHALLENGE_RESPONSE_KO = "event_challenge_response_ko";
	public static final String MESSAGE_TEMPLATE_EVENT_DUEL_REQUEST = "event_duel_request";
	public static final String MESSAGE_TEMPLATE_EVENT_DUEL_REQUEST_ACCEPT = "event_duel_request_answer_accept";
	public static final String MESSAGE_TEMPLATE_EVENT_DUEL_REQUEST_REFUSE = "event_duel_request_answer_refuse";
	public static final String MESSAGE_TEMPLATE_EVENT_DUEL_RESET_REQUEST = "event_duel_reset_request";

	public static final int SUBSCRIPTION_MAIL_NEWSLETTER = (int)Math.pow(2, 0); // 1
	public static final int SUBSCRIPTION_MAIL_PERIOD_REPORT = (int)Math.pow(2, 1); // 2
	public static final int SUBSCRIPTION_MAIL_COMMERCIAL = (int)Math.pow(2, 2); // 4
	
	public static final int SUBSCRIPTION_PUSH_FB_NOTIFICATION = (int)Math.pow(2, 0); // 1
	public static final int SUBSCRIPTION_PUSH_FRIENDS = (int)Math.pow(2, 1); // 2

	public static final int SUBSCRIPTION_PUSH_TOURNAMENT = (int)Math.pow(2, 5); // 32
    public static final String SUBSCRIPTION_PUSH_SELECT_ALL = "all";
    public static final String SUBSCRIPTION_PUSH_SELECT_FRIENDS = "friends";
    public static final String SUBSCRIPTION_PUSH_SELECT_NONE = "none";

	public static final int FRIEND_MASK_NOTIFICATION_DUEL = (int)Math.pow(2, 0); // 1
	public static final int FRIEND_MASK_DUEL_ONLY_FRIEND = (int)Math.pow(2, 1); // 2
	public static final int FRIEND_MASK_MESSAGE_ONLY_FRIEND = (int)Math.pow(2, 2); // 4

	public static final int PUSH_ENVRIONMENT_DISTRIBUTION = 1;
	public static final int PUSH_ENVRIONMENT_SANDBOX = 0;
	public static final String PUSH_TEMPLATE_DUEL_REQUEST = "duel_request";
    public static final String PUSH_TEMPLATE_DUEL_REQUEST_ACCEPT = "duel_request_accept";
	public static final String PUSH_TEMPLATE_DUEL_FINISH = "duel_finish";
    public static final String PUSH_TEMPLATE_DUEL_PARTNER_FINISH = "duel_partner_finish";
	public static final String PUSH_TEMPLATE_FRIEND_MESSAGE = "friend_message";
	public static final String PUSH_TEMPLATE_FRIEND_PENDING = "friend_pending";
	public static final String PUSH_TEMPLATE_MESSAGE_SUPPORT = "message_support";
	
	public static final Map<String, String> MESSAGE_SERIE_PARAM = new HashMap<String, String>();
    public static final Map<String, String> MESSAGE_TEAM_DIVISION_PARAM = new HashMap<>();
	static {
		buildMapSeries();
		buildMapTeamDivision();
	}
	public static void buildMapSeries() {
		MESSAGE_SERIE_PARAM.clear();
		MESSAGE_SERIE_PARAM.put("SERIE_SNC", "NEW");
        MESSAGE_SERIE_PARAM.put("SERIE_S01", "1");
        MESSAGE_SERIE_PARAM.put("SERIE_S02", "2");
        MESSAGE_SERIE_PARAM.put("SERIE_S03", "3");
        MESSAGE_SERIE_PARAM.put("SERIE_S04", "4");
        MESSAGE_SERIE_PARAM.put("SERIE_S05", "5");
        MESSAGE_SERIE_PARAM.put("SERIE_S06", "6");
        MESSAGE_SERIE_PARAM.put("SERIE_S07", "7");
        MESSAGE_SERIE_PARAM.put("SERIE_S08", "8");
        MESSAGE_SERIE_PARAM.put("SERIE_S09", "9");
        MESSAGE_SERIE_PARAM.put("SERIE_S10", "10");
        MESSAGE_SERIE_PARAM.put("SERIE_S11", "11");
        MESSAGE_SERIE_PARAM.put("SERIE_TOP", "Elite");
	}

    public static void buildMapTeamDivision() {
        MESSAGE_TEAM_DIVISION_PARAM.clear();
        MESSAGE_TEAM_DIVISION_PARAM.put("DIVISION_DNO", "NO");
        MESSAGE_TEAM_DIVISION_PARAM.put("DIVISION_D01", "1");
        MESSAGE_TEAM_DIVISION_PARAM.put("DIVISION_D02", "2");
        MESSAGE_TEAM_DIVISION_PARAM.put("DIVISION_D03", "3");
        MESSAGE_TEAM_DIVISION_PARAM.put("DIVISION_D04", "4");
        MESSAGE_TEAM_DIVISION_PARAM.put("DIVISION_D05", "5");
        MESSAGE_TEAM_DIVISION_PARAM.put("DIVISION_D06", "6");
    }

	/************************************************************/
	/*
	 * CHAT MESSAGE (NEW SERVICE) 聊天消息（新服务）
	 */
	public static final String CHAT_MESSAGE_TYPE_PLAYER = "PLAYER";
	public static final String CHAT_MESSAGE_TYPE_SYSTEM = "SYSTEM";
	public static final String CHAT_MESSAGE_TYPE_MODERATED = "MODERATED";
	public static final String CHAT_MESSAGE_TYPE_DELETED = "DELETED";

	public static final String CHATROOM_TYPE_SINGLE = "SINGLE";
	public static final String CHATROOM_TYPE_GROUP = "GROUP";
	public static final String CHATROOM_TYPE_TEAM = "TEAM";
	public static final String CHATROOM_TYPE_TOURNAMENT = "TOURNAMENT";

	/************************************************************/
	/*
	 * NOTIF/PUSH 通知/推送
	 */

	/** NOTIF PARAM 通知参数**/
	public static final String NOTIF_PARAM_ACTION = "ACTION";
	public static final String NOTIF_PARAM_PLAYER_ID = "PLAYER_ID";
	public static final String NOTIF_PARAM_PSEUDO = "PSEUDO";
	public static final String NOTIF_PARAM_AVATAR = "AVATAR";
	public static final String NOTIF_PARAM_TOURNAMENT_ID = "TOURNAMENT_ID";
	public static final String NOTIF_PARAM_TOURNAMENT_CATGEORY_ID = "TOURNAMENT_CATEGORY_ID";
	public static final String NOTIF_PARAM_URL_FULL = "URL_FULL";
	public static final String NOTIF_PARAM_PREVIOUS_SERIE = "PREVIOUS_SERIE";
	public static final String NOTIF_PARAM_SERIE = "SERIE";
	public static final String NOTIF_PARAM_PERIOD = "PERIOD";
	public static final String NOTIF_PARAM_TEMPLATE = "TEMPLATE";
	public static final String NOTIF_PARAM_MATCH_MAKING = "MATCH_MAKING";
	public static final String NOTIF_PARAM_TEAM_TYPE = "TEAM_TYPE";
	public static final String NOTIF_PARAM_RANKING_TYPE = "RANKING_TYPE";
	public static final String NOTIF_PARAM_RANKING_OPTIONS = "RANKING_OPTIONS";
	public static final String NOTIF_PARAM_CATEGORY_ID = "CATEGORY_ID";
	public static final String NOTIF_PARAM_PAGE = "PAGE";
	public static final String NOTIF_PARAM_CHATROOM_ID = "CHATROOM_ID";

	/** NOTIF PARAM VALUE 通知参数值 **/
	public static final String NOTIF_ACTION_OPEN_PROFILE = "OPEN_PROFILE";
	public static final String NOTIF_ACTION_OPEN_URL = "OPEN_URL";
	public static final String NOTIF_ACTION_OPEN_CATEGORY_PAGE = "OPEN_CATEGORY_PAGE";
	public static final String NOTIF_ACTION_OPEN_TOURNAMENT = "OPEN_TOURNAMENT";
	public static final String NOTIF_ACTION_SERIE_PREVIOUS_RANKING = "SERIES_PREVIOUS_RANKING";
	public static final String NOTIF_ACTION_TEAM_TYPE_CHECK = "CHECK";
	public static final String NOTIF_ACTION_TEAM_TYPE_CROSS = "CROSS";
	public static final String NOTIF_ACTION_TEAM_TYPE_CAPTAIN = "CAPTAIN";
	public static final String NOTIF_ACTION_TEAM_TYPE_DELETE = "DELETE";
	public static final String NOTIF_ACTION_TEAM_TYPE_LEAVE = "LEAVE";
	public static final String NOTIF_ACTION_TEAM_TYPE_RANKING = "RANKING";
	public static final String NOTIF_ACTION_OPEN_CATEGORY = "OPEN_CATEGORY";
	public static final String NOTIF_ACTION_OPEN_SETTINGS = "OPEN_SETTINGS";
	public static final String NOTIF_ACTION_OPEN_FRIENDS = "OPEN_FRIENDS";
	public static final String NOTIF_ACTION_OPEN_CHATROOM = "OPEN_CHATROOM";

	/************************************************************/
	/*
	 * RANKING 排行
	 */
	public static final String RANKING_TYPE_DUEL = "DUEL";
	public static final String RANKING_TYPE_DUEL_ARGINE = "DUEL_ARGINE";
	public static final String RANKING_TYPE_SERIE = "SERIE";
	public static final String RANKING_TYPE_PTS_CBO = "PTS_CBO";
	public static final String RANKING_TYPE_COUNTRY = "COUNTRY";
	public static final String RANKING_TYPE_PERFORMANCE = "PERFORMANCE";

	public static final String RANKING_OPTIONS_CURRENT_PERIOD = "CURRENT_PERIOD";
	public static final String RANKING_OPTIONS_PREVIOUS_PERIOD = "PREVIOUS_PERIOD";
	public static final String RANKING_OPTIONS_TOTAL = "TOTAL";
	public static final String RANKING_OPTIONS_FRIENDS = "FRIENDS";
	public static final String RANKING_OPTIONS_COUNTRY = "COUNTRY";
	public static final String RANKING_OPTIONS_COUNTRY_FILTER = "COUNTRY_FILTER";
}

