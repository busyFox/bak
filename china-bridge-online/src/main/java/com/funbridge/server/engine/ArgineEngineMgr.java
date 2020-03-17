package com.funbridge.server.engine;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.common.FunbridgeMgr;
import com.gotogames.common.bridge.*;
import com.gotogames.common.tools.NumericalTools;
import com.gotogames.common.tools.StringTools;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.FileConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component(value="argineEngineMgr")
@Scope(value="singleton")
public class ArgineEngineMgr extends FunbridgeMgr {
	
	private ArgineProfiles profiles;
    private ArgineProfiles profilesCards;
	
	private final ReentrantReadWriteLock rwLockProfiles = new ReentrantReadWriteLock();
	private final Lock lockReadProfiles = rwLockProfiles.readLock();
	private final Lock lockWriteProfiles = rwLockProfiles.writeLock();
	private final ReentrantReadWriteLock rwLockArgineText = new ReentrantReadWriteLock();
	private final Lock lockReadArgineText = rwLockArgineText.readLock();
	private final Lock lockWriteArgineText = rwLockArgineText.writeLock();
	
	private Map<String, ArgineTextElement> mapArgineText = new HashMap<String, ArgineTextElement>();
	
	@Override
	public void init() {
	}

	@Override
	public void destroy() {
	}

	@Override
	public void startUp() {
		log.debug("Load profiles from configuration");
		if (!loadProfiles()) {
			log.error("Failed to load profiles from configuration");
		}
        if (!loadProfilesCards()) {
            log.error("Failed to load profiles cards from configuration");
        }
		if (!loadArgineText()) {
			log.error("Failed to load argine texts from configuration");
		}
	}

    /**
	 * Read int value for parameter in name (paranName) in config file
	 * ??????????paranName???????int?
     * @param paramName
     * @param defaultValue
     * @return
     */
    public int getConfigIntValue(String paramName, int defaultValue) {
        return FBConfiguration.getInstance().getIntValue("engine." + paramName, defaultValue);
    }

    /**
	 * Read string value for parameter in name (paranName) in config file
     * @param paramName
     * @param defaultValue
     * @return
     */
    public String getConfigStringValue(String paramName, String defaultValue) {
        return FBConfiguration.getInstance().getStringValue("engine." + paramName, defaultValue);
    }
	
	/**
	 * Load profiles from json file
	 * @return
	 */
	public boolean loadProfiles() {
		boolean result = false;
		String fileProfiles = FBConfiguration.getInstance().getStringResolvEnvVariableValue("engine.argine.profilesFile", null);
		if (fileProfiles != null && fileProfiles.length() > 0) {
			try {
				ArgineProfiles temp = ContextManager.getJSONTools().mapDataFromFile(fileProfiles, ArgineProfiles.class);
				if (temp != null) {
					lockWriteProfiles.lock();
					try {
						if (profiles == null) {
							profiles = new ArgineProfiles();
						}
						profiles.profiles.clear();
						profiles.profiles.addAll(temp.profiles);
						result = true;
					} catch (Exception e) {
						log.error("Exception to change profiles ... ", e);
					} finally {
						lockWriteProfiles.unlock();
					}
				} else {
					log.error("Map data is null !");
				}
			} catch (JsonParseException e) {
				log.error("JsonParseException - failed to map profiles from file="+fileProfiles, e);
			} catch (JsonMappingException e) {
				log.error("JsonMappingException - failed to map profiles from file="+fileProfiles, e);
			} catch (IOException e) {
				log.error("IOException - failed to map profiles from file="+fileProfiles, e);
			}
		} else {
			log.error("No value found for param=engine.argine.profilesFile");
		}
		return result;
	}

    /**
     * Load profiles Card from json file
     * @return
     */
    public boolean loadProfilesCards() {
        boolean result = false;
        String fileProfiles = FBConfiguration.getInstance().getStringResolvEnvVariableValue("engine.argine.profilesCardsFile", null);
        if (fileProfiles != null && fileProfiles.length() > 0) {
            try {
                ArgineProfiles temp = ContextManager.getJSONTools().mapDataFromFile(fileProfiles, ArgineProfiles.class);
                if (temp != null) {
                    lockWriteProfiles.lock();
                    try {
                        if (profilesCards == null) {
                            profilesCards = new ArgineProfiles();
                        }
                        profilesCards.profiles.clear();
                        profilesCards.profiles.addAll(temp.profiles);
                        result = true;
                    } catch (Exception e) {
                        log.error("Exception to change profiles Cards ... ", e);
                    } finally {
                        lockWriteProfiles.unlock();
                    }
                } else {
                    log.error("Map data is null !");
                }
            } catch (JsonParseException e) {
                log.error("JsonParseException - failed to map profiles from file="+fileProfiles, e);
            } catch (JsonMappingException e) {
                log.error("JsonMappingException - failed to map profiles from file="+fileProfiles, e);
            } catch (IOException e) {
                log.error("IOException - failed to map profiles from file="+fileProfiles, e);
            }
        } else {
            log.error("No value found for param=engine.argine.profilesCardsFile");
        }
        return result;
    }

	public String[] getSupportedLang() {
		return getConfigStringValue("argine.supportedLang", "fr;en;nl").split(Constantes.SEPARATOR_VALUE);
	}

	/**
	 * Load argine text from configuration file
	 * @return
	 */
	public boolean loadArgineText() {
		boolean result = false;
		lockWriteArgineText.lock();
		try {
			char c = 0;
			AbstractConfiguration.setDefaultListDelimiter(c);
			String[] supportedLang = getSupportedLang();
			//log.info("Lang to load : " + Arrays.toString(supportedLang));
			mapArgineText.clear();
			for (String lang : supportedLang) {
				// Message Text in different lang
				String fileLang = FBConfiguration.getInstance().getStringResolvEnvVariableValue("engine.argine.fileTexts_" + lang, null);
				FileConfiguration fcTextLang = null;
				// load message text
				if (fileLang != null && new File(fileLang).isFile()) {
					try {
						fcTextLang = new PropertiesConfiguration();
                        fcTextLang.setEncoding("UTF-8");
                        fcTextLang.load(fileLang);
						int nbText = 0;
						for (Iterator<String> it = fcTextLang.getKeys(); it.hasNext(); ) {
							String k = it.next();
							String v = fcTextLang.getString(k);
							if (v != null) {
								ArgineTextElement ate = mapArgineText.get(k.toLowerCase());
								if (ate == null) {
									ate = new ArgineTextElement();
									ate.name = k.toLowerCase();
									mapArgineText.put(k.toLowerCase(), ate);
								}
								ate.addTextLang(lang, v);
								nbText++;
							}
						}
						//log.info("Lang=" + lang + " - file=" + fileLang + " - nbText=" + nbText);
					} catch (ConfigurationException e) {
						log.error("ConfigurationException - lang="+lang+" - Error to read properties file="+fileLang, e);
					} catch (Exception e) {
						log.error("Exception - lang="+lang+" - Error to read properties file="+fileLang, e);
					}
				} else {
					log.error("Text for lang " + lang + " - file not found file=" + fileLang);
				}
			}
			result = true;
			//log.info("Load argine text with success - size="+mapArgineText.size());
		} catch (Exception e) {
			log.error("Exception - to load engine text", e);
		} finally {
			lockWriteArgineText.unlock();
		}
		return result;
	}
	
	/**
	 * Retrieve argine profile from id
	 * ?ID??argine??
	 * @param id
	 * @return
	 */
	public ArgineProfile getProfile(int id) {
        if (profiles != null) {
            lockReadProfiles.lock();
            try {
                if (id >= 0) {
                    for (ArgineProfile ap : profiles.profiles) {
                        if (ap != null && ap.id == id) {
                            return ap;
                        }
                    }
                }

                // no profile found ... return default
                int defaultProfile = getDefaultProfile();
                for (ArgineProfile ap : profiles.profiles) {
                    if (ap != null && ap.id == defaultProfile) {
                        return ap;
                    }
                }
            } finally {
                lockReadProfiles.unlock();
            }
        }
        // no profile found
        log.error("No profile found for id="+id+" and no default profile defined");
        return null;
	}

    /**
     * Retrieve default profile bids from configuration
	 * ??????????????
     * @return
     */
    public int getDefaultProfile() {
        return getConfigIntValue("argine.defaultProfile", 0);
    }


    /**
     * Retrieve argine profile Card from id
	 * ?ID??argine?????
     * @param id
     * @return
     */
    public ArgineProfile getProfileCards(int id) {
        if (profilesCards != null) {
            lockReadProfiles.lock();
            try {
                if (id >= 0) {
                    for (ArgineProfile ap : profilesCards.profiles) {
                        if (ap != null && ap.id == id) {
                            return ap;
                        }
                    }
                }
                // no profile found ... return default
                int defaultProfile = getDefaultProfileCards();
                for (ArgineProfile ap : profilesCards.profiles) {
                    if (ap != null && ap.id == defaultProfile) {
                        return ap;
                    }
                }
            } finally {
                lockReadProfiles.unlock();
            }
        }

        // no profile found
        log.error("No profile found for id="+id+" and no default profile defined");
        return null;
    }

    /**
     * Retrieve default profile cards from configuration
     * @return
     */
    public int getDefaultProfileCards() {
        return getConfigIntValue("argine.defaultProfileCards", 0);
    }

	/**
	 * Retrieve the argine text element associated to this rule
	 * @param rule
	 * @return
	 */
	public ArgineTextElement getArgineText(String rule) {
		lockReadArgineText.lock();
		try {
			return mapArgineText.get(rule.toLowerCase());
		} finally {
			lockReadArgineText.unlock();
		}
	}
	
	/**
	 * List all profiles
	 * @return
	 */
	public ArgineProfiles getAllProfiles() {
		return profiles;
	}

    /**
     * List all profiles Cards
     * @return
     */
    public ArgineProfiles getAllProfilesCards() {
        return profilesCards;
    }
	
	public List<ArgineTextElement> getAllArgineText() {
		List<ArgineTextElement> listText = new ArrayList<ArgineTextElement>(mapArgineText.values());
		Collections.sort(listText, new Comparator<ArgineTextElement>() {
			@Override
			public int compare(ArgineTextElement o1, ArgineTextElement o2) {
				return o1.name.compareTo(o2.name);
			}
		});
		return listText;
	}

	/**
	 * Decode the text for reglette
	 * ??reglette???
	 * @param value
	 * @return
	 */
	public String decodeText(String value, String lang) {
        String result = "";
		try {
			int i = 0;
			int n = value.length();
			String rule = "";
			List<String> listArg = new ArrayList<String>();
			// parse the value
			while (i<n) {
				// operator & |
				if (value.charAt(i) == '&' || value.charAt(i) == '|' || value.charAt(i) == '+') {
					if (rule.length() > 0) {
						result += getRuleText(rule, listArg, lang);
						rule = "";
						listArg.clear();
					}
					ArgineTextElement ate = getArgineText(""+value.charAt(i));
					if (ate != null) {
					    result += " ";
					    String t = ate.getTextForLang(lang);
					    if (t != null && t.length() > 0) {
					        result += t + " ";
                        }
					}
					i++;
				}
				// bracket for parameters
				else if (value.charAt(i) == '(') {
					i++;
					listArg.clear();
					StringBuffer tempArg = new StringBuffer();
					boolean bracketOK = false;
					while (i<n) {
						// separator parameters
						if (value.charAt(i) == ',') {
							// build current parameter before next
							String t = tempArg.toString();
							if (t.equalsIgnoreCase("t") ||
									t.equalsIgnoreCase("k") ||
									t.equalsIgnoreCase("c") ||
									t.equalsIgnoreCase("p")) {
								ArgineTextElement ateParam = getArgineText(t);
								if (ateParam != null) {
									t = ateParam.getTextForLang(lang);
								}
							}
							listArg.add(t);
							// reset buffer for parameter
							tempArg = new StringBuffer();
							i++;
						}
						// end of parameters
						else if (value.charAt(i) == ')') {
							String t = tempArg.toString();
							if (t.equalsIgnoreCase("t") ||
									t.equalsIgnoreCase("k") ||
									t.equalsIgnoreCase("c") ||
									t.equalsIgnoreCase("p")) {
								ArgineTextElement ateParam = getArgineText(t);
								if (ateParam != null) {
									t = ateParam.getTextForLang(lang);
								}
							}
							listArg.add(t);
							tempArg = new StringBuffer();
							result += getRuleText(rule, listArg, lang);
							i++;
							bracketOK = true;
							break;
						}
						// values of parameters
						else {
							tempArg.append(value.charAt(i));
							i++;
						}
					}
					// bracket parameters ok : find ( and )
					if (!bracketOK) {
						result = "";
						break;
					}
					rule = "";
					listArg.clear();
				}
				// value not parse between ""
				else if (value.charAt(i) == '"') {
					i++;
					while (i<n) {
						if (value.charAt(i) == '"') {
							i++;
							break;
						} else {
							result += ""+value.charAt(i);
							i++;
						}
					}
				}
				// value of rule
				else {
					rule+=value.charAt(i);
					i++;
				}
			}
			if (rule.length() > 0) {
				result += getRuleText(rule, listArg, lang);
			}
		} catch (Exception e) {
			log.error("Failed to decode value="+value+" - lang="+lang, e);
		}
		if (result != null && result.length() > 0) {
			// set upper case for first character and add on '.' if not present
			result = Character.toUpperCase(result.charAt(0)) + result.substring(1);
			if (result.charAt(result.length()-1) != '.') {
				result += ".";
			}
		}
		return result;
	}
	
	/**
	 * Get the text for rule and list of arguments
	 * @param rule
	 * @param args
	 * @return
	 */
	private String getRuleText(String rule, List<String> args, String lang) {
		String result = "";
		if (rule != null && rule.length() > 0) {
			// no arguments
			if (args == null || args.size() == 0) {
				ArgineTextElement ate = getArgineText(rule);
				if (ate != null) {
					result = ate.getTextForLang(lang);
				} else {
					log.error("No argine text found for rule="+rule);
				}
			}
			// with arguments
			else {
				
				// build rule name with parameters to find it in the map
				String temp = rule + "(";
				for (int i=0; i< args.size(); i++) {
					if (i > 0) temp += ",";
					temp +="p"+(i+1);
				}
				temp += ")";
				ArgineTextElement ate = getArgineText(temp);
				if (ate != null) {
					result = ate.getTextForLang(lang);
					// replace parameters with arguments
					for (int i=0; i< args.size(); i++) {
						result = result.replaceAll("%p"+(i+1)+"%", args.get(i));
					}
				} else {
					log.error("No argine text found for rule="+temp);
				}
			}
		} else {
			log.error("Rule is null or empty");
		}
		return result;
	}

    /**
     * Build text for point
     * @param strMin
     * @param strMax
     * @param lang
     * @return
     */
    public String getTextPoint(String strMin, String strMax, String lang) {
        String key = "";
        int min = 0, max = 40;
        try {
            min = Integer.parseInt(strMin);
            max = Integer.parseInt(strMax);
        } catch (Exception e) {}
        if (min == 0 && max > 27) {
            key = "_pointNoIndication";
        } else if (min > 0 && max > 27) {
            key = "_pointAlLeast("+min+")";
        } else if (min == 0 && max < 28) {
            key = "_pointAtMost("+max+")";
        } else if (min == max) {
            key = "_pointExact("+min+")";
        } else {
            key = "_pointBetween("+min+","+max+")";
        }

        return decodeText(key, lang);
    }

    /**
     * Build text for color
     * @param strMin
     * @param strMax
     * @param lang
     * @return
     */
    public String getTextColor(String strMin, String strMax, String lang) {
        String key = "";
        int min = 0, max = 40;
        try {
            min = Integer.parseInt(strMin);
            max = Integer.parseInt(strMax);
        } catch (Exception e) {}
        if (min == 0 && max > 8) {
            key = "_colorNoIndication";
        } else if (min > 0 && max > 8) {
            key = "_colorAlLeast("+min+")";
        } else if (min == 0 && max < 9) {
            key = "_colorAtMost("+max+")";
        } else if (min == max) {
            key = "_colorExact("+min+")";
        } else {
            key = "_colorBetween("+min+","+max+")";
        }
        return decodeText(key, lang);
    }

    /**
     * Build text for analyze play at color
     * @param analyzeCardColor
     * @param bid
     * @param nbTrickGameColor
     * @param nbPlayerSameColorExceptPlayer
     * @param nbPlayerBestTrickColor
	 * @param nbPlayerWorseTrickColor
     * @return
     */
    public String buildTextAnalyzePlayColor(boolean analyzeCardColor, BridgeBid bid, int nbTrickGameColor, int nbPlayerSameColorExceptPlayer, int nbPlayerBestTrickColor, int nbPlayerWorseTrickColor) {
		if (analyzeCardColor && bid != null && nbTrickGameColor >= 0 && nbTrickGameColor <= 13) {
            if (nbPlayerSameColorExceptPlayer > 0) {
				double percentPlayerBest = ((double)(nbPlayerBestTrickColor*100))/(nbPlayerSameColorExceptPlayer);
				double percentPlayerWorse = ((double)(nbPlayerWorseTrickColor*100))/(nbPlayerSameColorExceptPlayer);
				int valueBest = (int)NumericalTools.round(percentPlayerBest, 0); if (valueBest == 0) {valueBest=1;}
				int valueWorse = (int)NumericalTools.round(percentPlayerWorse, 0); if (valueWorse == 0) {valueWorse=1;}
				if ((percentPlayerBest > 0) && (percentPlayerWorse > 0)) {
					if (bid.getColor() == BidColor.NoTrump) {
						return "cardAnalyzeContractNTPercentBestAndWorse;NB_PLAYER_SAME_COLOR="+nbPlayerSameColorExceptPlayer+";PERCENT_BEST="+valueBest+";PERCENT_WORSE="+valueWorse;
					} else {
						return "cardAnalyzeContractPercentBestAndWorse;BID_COLOR={"+bid.getColor().getChar()+"};NB_PLAYER_SAME_COLOR="+nbPlayerSameColorExceptPlayer+";PERCENT_BEST="+valueBest+";PERCENT_WORSE="+valueWorse;
					}
				}
				else if (percentPlayerBest == 0 && percentPlayerWorse > 0) {
					if (bid.getColor() == BidColor.NoTrump) {
						return "cardAnalyzeContractNTPercentOnlyWorse;NB_PLAYER_SAME_COLOR="+nbPlayerSameColorExceptPlayer+";PERCENT_WORSE="+valueWorse;
					} else {
						return "cardAnalyzeContractPercentOnlyWorse;BID_COLOR={"+bid.getColor().getChar()+"};NB_PLAYER_SAME_COLOR="+nbPlayerSameColorExceptPlayer+";PERCENT_WORSE="+valueWorse;
					}
				}
				else if (percentPlayerBest > 0 && percentPlayerWorse == 0) {
					if (bid.getColor() == BidColor.NoTrump) {
						return "cardAnalyzeContractNTPercentOnlyBest;NB_PLAYER_SAME_COLOR="+nbPlayerSameColorExceptPlayer+";PERCENT_BEST="+valueBest;
					} else {
						return "cardAnalyzeContractPercentOnlyBest;BID_COLOR={"+bid.getColor().getChar()+"};NB_PLAYER_SAME_COLOR="+nbPlayerSameColorExceptPlayer+";PERCENT_BEST="+valueBest;
					}
				}
				else {
					if (bid.getColor() == BidColor.NoTrump) {
						return "cardAnalyzeContractNTSame;NB_PLAYER_SAME_COLOR="+nbPlayerSameColorExceptPlayer;
					} else {
						return "cardAnalyzeContractSame;BID_COLOR={"+bid.getColor().getChar()+"};NB_PLAYER_SAME_COLOR="+nbPlayerSameColorExceptPlayer;
					}
				}
            } else {
				if (bid.getColor() == BidColor.NoTrump) {
					return "cardAnalyzeNoPlayerContractNT";
				} else {
					return "cardAnalyzeNoPlayerContract;BID_COLOR={"+bid.getColor().getChar()+"}";
				}
            }
        }
		return "cardAnalyzeNotAvailable";
    }

    public static final String CHECK_BIDINFO_FAILED = "failed";
    public static final String CHECK_BIDINFO_OK = "OK";

    /**
     * Check bid is correct with bidinfo and player game (nb card for each color and nb points)
     * process order : point, pique, coeur, carreau, tr?fle
     * @param nbCardClub
     * @param nbCardDiamond
     * @param nbCardHeart
     * @param nbCardSpade
     * @param nbPoints
     * @param bid
     * @param bidInfo
     * @return
     */
    public String checkBidInfoWithGame(int nbCardClub, int nbCardDiamond, int nbCardHeart, int nbCardSpade, int nbPoints, String bid, String bidInfo) {
        String result = CHECK_BIDINFO_FAILED;
        String[] dataBidInfo = bidInfo.split(";");
        if (dataBidInfo.length == 14) {
            try {
                int dataBidInfoNbCardClubMin = Integer.parseInt(dataBidInfo[0]);
                int dataBidInfoNbCardClubMax = Integer.parseInt(dataBidInfo[1]);
                int dataBidInfoNbCardDiamondMin = Integer.parseInt(dataBidInfo[2]);
                int dataBidInfoNbCardDiamondMax = Integer.parseInt(dataBidInfo[3]);
                int dataBidInfoNbCardHeartMin = Integer.parseInt(dataBidInfo[4]);
                int dataBidInfoNbCardHeartMax = Integer.parseInt(dataBidInfo[5]);
                int dataBidInfoNbCardSpadeMin = Integer.parseInt(dataBidInfo[6]);
                int dataBidInfoNbCardSpadeMax = Integer.parseInt(dataBidInfo[7]);
                int dataBidInfoNbPointsMin = Integer.parseInt(dataBidInfo[8]);
                int dataBidInfoNbPointsMax = Integer.parseInt(dataBidInfo[9]);
				int tolerancePoints = getConfigIntValue("analyzeBidTolerancePoints", 1);
				int toleranceCards = getConfigIntValue("analyzeBidToleranceCards", 1);

                if ((nbPoints + tolerancePoints) < dataBidInfoNbPointsMin) {
                    result =  "bidAnalyzePointMin;BID_PLAY={"+bid+"};BID_POINT_MIN="+dataBidInfoNbPointsMin+";GAME_POINT="+nbPoints;
                }
                else if ((nbPoints - tolerancePoints) > dataBidInfoNbPointsMax) {
                    result =  "bidAnalyzePointMax;BID_PLAY={"+bid+"};BID_POINT_MAX="+dataBidInfoNbPointsMax+";GAME_POINT="+nbPoints;
                }
                else if ((nbCardSpade + toleranceCards) < dataBidInfoNbCardSpadeMin) {
                    result =  "bidAnalyzeColorMin;BID_PLAY={"+bid+"};BID_COLOR_MIN="+dataBidInfoNbCardSpadeMin+";BID_COLOR={S};GAME_NB_COLOR="+nbCardSpade;
                }
                else if ((nbCardSpade - toleranceCards) > dataBidInfoNbCardSpadeMax) {
                    result =  "bidAnalyzeColorMax;BID_PLAY={"+bid+"};BID_COLOR_MAX="+dataBidInfoNbCardSpadeMax+";BID_COLOR={S};GAME_NB_COLOR="+nbCardSpade;
                }
                else if ((nbCardHeart + toleranceCards) < dataBidInfoNbCardHeartMin) {
                    result =  "bidAnalyzeColorMin;BID_PLAY={"+bid+"};BID_COLOR_MIN="+dataBidInfoNbCardHeartMin+";BID_COLOR={H};GAME_NB_COLOR="+nbCardHeart;
                }
                else if ((nbCardHeart - toleranceCards) > dataBidInfoNbCardHeartMax) {
                    result =  "bidAnalyzeColorMax;BID_PLAY={"+bid+"};BID_COLOR_MAX="+dataBidInfoNbCardHeartMax+";BID_COLOR={H};GAME_NB_COLOR="+nbCardHeart;
                }
                else if ((nbCardDiamond + toleranceCards) < dataBidInfoNbCardDiamondMin) {
                    result =  "bidAnalyzeColorMin;BID_PLAY={"+bid+"};BID_COLOR_MIN="+dataBidInfoNbCardDiamondMin+";BID_COLOR={D};GAME_NB_COLOR="+nbCardDiamond;
                }
                else if ((nbCardDiamond - toleranceCards) > dataBidInfoNbCardDiamondMax) {
                    result =  "bidAnalyzeColorMax;BID_PLAY={"+bid+"};BID_COLOR_MAX="+dataBidInfoNbCardDiamondMax+";BID_COLOR={D};GAME_NB_COLOR="+nbCardDiamond;
                }
                else if ((nbCardClub + toleranceCards) < dataBidInfoNbCardClubMin) {
                    result =  "bidAnalyzeColorMin;BID_PLAY={"+bid+"};BID_COLOR_MIN="+dataBidInfoNbCardClubMin+";BID_COLOR={C};GAME_NB_COLOR="+nbCardClub;
                }
                else if ((nbCardClub - toleranceCards) > dataBidInfoNbCardClubMax) {
                    result =  "bidAnalyzeColorMax;BID_PLAY={"+bid+"};BID_COLOR_MAX="+dataBidInfoNbCardClubMax+";BID_COLOR={C};GAME_NB_COLOR="+nbCardClub;
                } else {
                    result = CHECK_BIDINFO_OK;
                }
            } catch (Exception e) {
                log.error("Failed to parse data from bidInfo="+bidInfo, e);
            }
        } else {
            log.error("bidInfo not valid .... bidInfo="+bidInfo);
        }
        log.debug("nbCardClub="+nbCardClub+" -  nbCardDiamond="+nbCardDiamond+" - nbCardHeart="+nbCardHeart+" - nbCardSpade="+nbCardSpade+" - nbPoints="+nbPoints+" - bid="+bid+" - bidInfo="+bidInfo+" - result="+result);
        return result;
    }

    public String analyzeGameBid(String dealDistrib, char dealDealer, char dealVulnerability,
                                 List<BridgeBid> gameBids, Map<String, String> mapBidInfoSouth,
                                 int gameConventionProfile, String gameConventionData,
                                 int resultType,
                                 int engineVersion,
                                 EngineRest engine) {
        if (gameBids != null && GameBridgeRule.isBidsFinished(gameBids)) {
            String analyzeResult = null;
            String bidSequence = "";
            String bidProblem = "";
            int nbCardClub = BridgeDeal.getNbCardColorForPlayer(dealDistrib, BridgeConstantes.POSITION_SOUTH, BridgeConstantes.CARD_COLOR_CLUB);
            int nbCardDiamond = BridgeDeal.getNbCardColorForPlayer(dealDistrib, BridgeConstantes.POSITION_SOUTH, BridgeConstantes.CARD_COLOR_DIAMOND);
            int nbCardHeart = BridgeDeal.getNbCardColorForPlayer(dealDistrib, BridgeConstantes.POSITION_SOUTH, BridgeConstantes.CARD_COLOR_HEART);
            int nbCardSpade = BridgeDeal.getNbCardColorForPlayer(dealDistrib, BridgeConstantes.POSITION_SOUTH, BridgeConstantes.CARD_COLOR_SPADE);
            int nbPoints = BridgeDeal.getNbPointsHonForPlayer(dealDistrib, BridgeConstantes.POSITION_SOUTH);
			int nbBidPAS = 0, nbBidX1 = 0, nbBidX2 = 0;
            for (BridgeBid bid : gameBids) {
                bidSequence += bid.getString();
				if (bid.getOwner() == BridgeConstantes.POSITION_SOUTH) {
					if (bid.isPass()) {
						nbBidPAS++;
					}
					if (bid.isX1()) {
						nbBidX1++;
					}
					if (bid.isX2()) {
						nbBidX2++;
					}
					if ((bid.isPass() && nbBidPAS > 1) ||
							(bid.isX1() && nbBidX1 > 1) ||
							(bid.isX2() && nbBidX2 > 1)) {
						// this bid has already been processed => continue with the next
						continue;
					}
					// get reglette info
					String bidInfo = mapBidInfoSouth.get(bidSequence);
					if (bidInfo == null) {
						try {
							bidInfo = engine.getBidInfoFullData(dealDealer,
									dealVulnerability,
									bidSequence,
									gameConventionProfile,
									gameConventionData,
                                    getDefaultProfileCards(),
                                    null,
									resultType,
									engineVersion);
						} catch (Exception e) {
							log.error("Failed to get bid info for bidSequence=" + bidSequence, e);
						}
					}
					if (bidInfo != null) {
						analyzeResult = checkBidInfoWithGame(nbCardClub, nbCardDiamond, nbCardHeart, nbCardSpade, nbPoints, bid.getString(), bidInfo);
						if (!analyzeResult.equals(CHECK_BIDINFO_FAILED) && !analyzeResult.equals(CHECK_BIDINFO_OK)) {
							bidProblem = bid.getString();
							break;
						}
					}

                }
            }

            if (analyzeResult == null) {
                analyzeResult = CHECK_BIDINFO_FAILED;
            }
            if (!analyzeResult.equals(CHECK_BIDINFO_FAILED) && !analyzeResult.equals(CHECK_BIDINFO_OK)) {
                if (bidProblem.length() == 0) {
                    analyzeResult = CHECK_BIDINFO_FAILED;
                }
            }

            if (analyzeResult.equals(CHECK_BIDINFO_OK)) {
                return "bidAnalyzeOK";
            }
            if (analyzeResult.equals(CHECK_BIDINFO_FAILED)) {
                return "bidAnalyzeNotAvailable";
            }

            return analyzeResult;
        }
        else {
            log.error("Bids null or not ended - gameBids="+ StringTools.listToString(gameBids));
        }
        return "bidAnalyzeNotAvailable";
    }

    /**
     * Return the engine version to use
     * @param tournamentCategory
     * @return
     */
    public int getEngineVersion(int tournamentCategory) {
        int engineVersion = 0;
        if (tournamentCategory > 0) {
            engineVersion = getConfigIntValue("argine.version." + Constantes.tourCategory2Name(tournamentCategory), 0);
        }
        if (engineVersion == 0) {
            // no engine version set on tournament - use default value
            engineVersion = getConfigIntValue("argine.version.ALL",0);
        }
        return engineVersion;
    }

    /**
     * build convention for ARGINE engine
	 * ARGINE???????
     * @param profile
     * @param conv
     * @return
     */
    public String buildConvention(int profile, String conv) {
        String convData = "";
        ArgineProfile arginePro = getProfile(profile);
        if (arginePro != null) {
            if (arginePro.isFree()) {
                // use convention from parameters
                convData = conv;
            } else {
                // use convention from profile
                convData = arginePro.value;
            }
        }
        if (convData == null || convData.length() == 0) {
            log.error("No convention bids define for profile="+profile);
            arginePro = getProfile(getDefaultProfile());
            if (arginePro != null) {
                convData = arginePro.value;
            } else {
                log.error("No default profile found !");
            }
        }
        return convData;
    }

    /**
     * build convention for ARGINE engine
     * @param profile
     * @param conv
     * @param profileCards
     * @param convCards
     * @param gameString
     * @return
     */
	public String buildConvention(int profile, String conv, int profileCards, String convCards, String gameString) {
		int conventionTotalSize = getConfigIntValue("argine.conventionTotalSize", 56);
		int conventionBidsSize = getConfigIntValue("argine.conventionBidsSize", 50);
		int conventionCardsSize = getConfigIntValue("argine.conventionCardsSize", 6);

		// BIDS
		String convDataBids = "";
		ArgineProfile arginePro = getProfile(profile);
		if (arginePro != null) {
			if (arginePro.isFree()) {
				// use convention from parameters
				convDataBids = conv;
			} else {
				// use convention from profile
				convDataBids = arginePro.value;
			}
		}
		if (convDataBids == null || convDataBids.length() == 0) {
			Log log = LogFactory.getLog(BridgeEngineParam.class);
			log.error("No convention bids define for profile="+profile+" - gameString="+gameString);
			arginePro = getProfile(getDefaultProfile());
			if (arginePro != null) {
				convDataBids = arginePro.value;
			} else {
				log.error("No default profile bids found !");
			}
		}
		// if convention bids length = conventionTotalSize take only the bids part
		if (convDataBids.length() == conventionTotalSize) {
			convDataBids = convDataBids.substring(0, conventionBidsSize);
		}
		// if convention bids size is not valid => alert
		if (convDataBids.length() < conventionBidsSize) {
			if (log.isDebugEnabled()) {
				log.debug("Convention bids size not valid => full with 0 ! convDataBids=" + convDataBids + " - profile=" + profile + " - conv=" + conv + " - profileCards=" + profileCards + " - convCards=" + convCards+" - gameString="+gameString);
			}
			// Complete with trailing zeros (except the 3 last ones)
			while (convDataBids.length() < conventionBidsSize - 3) {
				convDataBids += "0";
			}
			// Add 001 for the 3 last characters
			convDataBids += "011";
		}
		if (convDataBids.length() > conventionBidsSize) {
			if (log.isDebugEnabled()) {
				log.debug("Convention bids size not valid => remove extra characters ! convDataBids=" + convDataBids + " - profile=" + profile + " - conv=" + conv + " - profileCards=" + profileCards + " - convCards=" + convCards+" - gameString="+gameString);
			}
			convDataBids = convDataBids.substring(0, conventionBidsSize);
		}

		// CARDS
		String convDataCards = "";
		ArgineProfile argineCardPro = getProfileCards(profileCards);
		if (argineCardPro != null) {
			if (argineCardPro.isFree()) {
				// use convention from parameters
				convDataCards = convCards;
			} else {
				// use convention from profile
				convDataCards = argineCardPro.value;
			}
		}
		if (convDataCards == null || convDataCards.length() == 0) {
			log.warn("No convention cards define for profile="+profile+" - conv="+conv+" - profileCards="+profileCards+" - convCards="+convCards+" - gameString="+gameString);
			argineCardPro = getProfileCards(getDefaultProfileCards());
			if (argineCardPro != null) {
				convDataCards = argineCardPro.value;
			} else {
				log.warn("No default profile cards found !");
			}
		}
		// if convention cards size is not valid => alert
		if (convDataCards.length() < conventionCardsSize) {
			log.warn("Convention cards size not valid => full with 0 ! convDataCards="+convDataCards+" - profile="+profile+" - conv="+conv+" - profileCards="+profileCards+" - convCards="+convCards+" - gameString="+gameString);
			while (convDataCards.length() < conventionCardsSize) {
				convDataCards += "0";
			}
		}
		if (convDataCards.length() > conventionCardsSize) {
			log.warn("Convention cards size not valid => remove extra characters ! convDataCards="+convDataCards+" - profile="+profile+" - conv="+conv+" - profileCards="+profileCards+" - convCards="+convCards+" - gameString="+gameString);
			convDataCards = convDataCards.substring(0, conventionCardsSize);
		}

		// concat the two data
		String convData = convDataBids + convDataCards;
		if (convData.length() < conventionTotalSize) {
			log.warn("Convention final size not valid => fill with 0 ! convData="+convData+" - profile="+profile+" - conv="+conv+" - profileCards="+profileCards+" - convCards="+convCards+" - gameString="+gameString);
			while (convData.length() < conventionTotalSize) {
				convData += "0";
			}
		}
		if (convData.length() > conventionTotalSize) {
			log.warn("Convention final size not valid => remove extra characters ! convData="+convData+" - profile="+profile+" - conv="+conv+" - profileCards="+profileCards+" - convCards="+convCards+" - gameString="+gameString);
			convData = convData.substring(0, conventionTotalSize);
		}
		return convData;
	}
}
