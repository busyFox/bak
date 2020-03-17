package com.funbridge.server.tournament;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;
import org.ini4j.Profile.Section;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.common.FunbridgeMgr;
import com.funbridge.server.tournament.dao.TournamentDAOJpa;
import com.funbridge.server.tournament.dao.TournamentDealDAOJpa;
import com.funbridge.server.tournament.dao.TournamentDistributionDAOJpa;
import com.funbridge.server.tournament.data.Tournament;
import com.funbridge.server.tournament.data.TournamentCategory;
import com.funbridge.server.tournament.data.TournamentDeal;
import com.funbridge.server.tournament.data.TournamentDistribution;
import com.funbridge.server.tournament.data.TournamentSettings;
import com.gotogames.common.bridge.BridgeDeal;
import com.gotogames.common.bridge.BridgeDealParam;

/**
 * Class to manage generation of tournament
 * @author pascal
 *
 */
@Component(value="tournamentGenerator")
@Scope(value="singleton")
public class TournamentGenerator extends FunbridgeMgr {
	@Resource(name="tournamentDAO")
	private TournamentDAOJpa tournamentDAO = null;
	@Resource(name="tournamentDistributionDAO")
	private TournamentDistributionDAOJpa tournamentDistributionDAO = null;
	@Resource(name="tournamentDealDAO")
	private TournamentDealDAOJpa tournamentDealDAO = null;
	private final String SEPARATOR_DISTRIB_PARAM = ";";
	
	private Object objSynchroGenerator = new Object();
	
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

	/**
	 * Convert string parameter value to int array
	 * @param val
	 * @return null if an exception to parse string to int occurs
	 */
	private int[] paramString2IntTab(String val) {
		if (val != null) {
			try {
				String[] temp = val.split(" ");
				if (temp.length == 6) {
					int[] tabVal = new int[6];
					tabVal[0] = Integer.parseInt(temp[0]);
					tabVal[1] = Integer.parseInt(temp[1]);
					tabVal[2] = Integer.parseInt(temp[2]);
					tabVal[3] = Integer.parseInt(temp[3]);
					tabVal[4] = Integer.parseInt(temp[4]);
					tabVal[5] = Integer.parseInt(temp[5]);
					return tabVal;
				}
			} catch (Exception e) {
				log.error("Exception on parsing param value to transform to tab int : "+val,e);
			}
		}
		return null;
	}
	
	/**
	 * get a random parameter
	 * @return
	 */
	private BridgeDealParam getRandomParamGenertor() {
		Random random = new Random();
		Ini ini = new Ini();
		try {
			ini.load(new File(FBConfiguration.getInstance().getStringResolvEnvVariableValue("tournament.generator.dealParamFile", "")));
			Section secList = ini.get("LIST");
			if (secList != null) {
				String[] keytab = secList.keySet().toArray(new String[secList.size()]);
				int randomListIdx = random.nextInt(keytab.length);
				String randomList = secList.get(keytab[randomListIdx]);
				
				if (randomList != null) {
					Section secListParam = ini.get(randomList);
					if (secListParam != null) {
						String[] tabListParam = secListParam.keySet().toArray(new String[secListParam.size()]);
						int randomParam = random.nextInt(tabListParam.length);
						String param = secListParam.get(tabListParam[randomParam]);
						
						Section secParam = ini.get(param);
						if (secParam != null) {
							BridgeDealParam dealParam = new BridgeDealParam();
							dealParam.ptsHonMin = paramString2IntTab(secParam.get("phmin"));
							dealParam.ptsHonMax = paramString2IntTab(secParam.get("phmax"));
							dealParam.nbCardCMin = paramString2IntTab(secParam.get("trmin"));
							dealParam.nbCardCMax = paramString2IntTab(secParam.get("trmax"));
							dealParam.nbCardDMin = paramString2IntTab(secParam.get("camin"));
							dealParam.nbCardDMax = paramString2IntTab(secParam.get("camax"));
							dealParam.nbCardHMin = paramString2IntTab(secParam.get("comin"));
							dealParam.nbCardHMax = paramString2IntTab(secParam.get("comax"));
							dealParam.nbCardSMin = paramString2IntTab(secParam.get("pimin"));
							dealParam.nbCardSMax = paramString2IntTab(secParam.get("pimax"));
							dealParam.paramName = param;
							dealParam.dealer = secParam.get("dealer");
							return dealParam;
						} else {
							log.error("Generator param file - no section found for"+param);
						}
					} else {
						log.error("Generator param file - no section found for"+randomList);
					}
				} else {
					log.error("Generator param file - no property found for "+keytab[randomListIdx]+" in section LIST");
				}
			} else {
				log.error("Generator param file - no section LIST found");
			}
		} catch (InvalidFileFormatException e) {
			log.error("Generator param file invalid format (no ini file)",e);
		} catch (IOException e) {
			log.error("Generator param file error loading ini",e);
		}
		return null;
	}
	
	/**
	 * Generate deal param associated to the settings
	 * @param settings
	 * @return
	 */
	private BridgeDealParam getParamGeneratorFromSettings(TournamentSettings settings) {
		BridgeDealParam dealParam = null;
		if (settings != null) {
			// case of mode RANDOM, POINTS_FOR_NS, ENCHERIR_A2, THEME
			if (settings.mode.equals(Constantes.TOURNAMENT_SETTINGS_MODE_RANDOM) ||
				settings.mode.equals(Constantes.TOURNAMENT_SETTINGS_MODE_POINTS_FOR_NS) ||
				settings.mode.equals(Constantes.TOURNAMENT_SETTINGS_MODE_ENCHERIE_A2)||
				settings.mode.equals(Constantes.TOURNAMENT_SETTINGS_MODE_THEME)) {
				Ini ini = new Ini();
				String iniFile = FBConfiguration.getInstance().getStringResolvEnvVariableValue("tournament.generator.trainingPartnerFile", "");
				try {
					ini.load(new File(iniFile));
					Random random = new Random();
					// select mode
					Section secMode = ini.get(settings.mode);
					if (secMode != null) {
						String modeTheme = null;
						if (settings.mode.equals(Constantes.TOURNAMENT_SETTINGS_MODE_THEME)) {
							modeTheme = settings.theme;
						} else {
							String[] keytab = secMode.keySet().toArray(new String[secMode.size()]);
							int randomModeIdx = random.nextInt(keytab.length);
							modeTheme = secMode.get(keytab[randomModeIdx]);
						}
						// select theme
						Section secTheme = ini.get(modeTheme);
						if (secTheme != null) {
							String[] tabListParam = secTheme.keySet().toArray(new String[secTheme.size()]);
							int randomParam = random.nextInt(tabListParam.length);
							String param = secTheme.get(tabListParam[randomParam]);
							// select param
							Section secParam = ini.get(param);
							if (secParam != null) {
								dealParam = new BridgeDealParam();
								dealParam.ptsHonMin = paramString2IntTab(secParam.get("phmin"));
								dealParam.ptsHonMax = paramString2IntTab(secParam.get("phmax"));
								dealParam.nbCardCMin = paramString2IntTab(secParam.get("trmin"));
								dealParam.nbCardCMax = paramString2IntTab(secParam.get("trmax"));
								dealParam.nbCardDMin = paramString2IntTab(secParam.get("camin"));
								dealParam.nbCardDMax = paramString2IntTab(secParam.get("camax"));
								dealParam.nbCardHMin = paramString2IntTab(secParam.get("comin"));
								dealParam.nbCardHMax = paramString2IntTab(secParam.get("comax"));
								dealParam.nbCardSMin = paramString2IntTab(secParam.get("pimin"));
								dealParam.nbCardSMax = paramString2IntTab(secParam.get("pimax"));
								dealParam.paramName = param;
								dealParam.dealer = secParam.get("dealer");
							} else {
								log.error("No section found for param="+param+" - settings="+settings);
							}
						} else {
							log.error("No section found for theme="+modeTheme+" - settings="+settings);
						}
					} else {
						log.error("No section found for mode - settings="+settings);
					}
				} catch (InvalidFileFormatException e) {
					log.error("InvalidFileFormatException to load ini file="+iniFile,e);
				} catch (IOException e) {
					log.error("IOException to load ini file="+iniFile,e);
				}
			}
			// case MODE ADVANCED
			else if (settings.mode.equals(Constantes.TOURNAMENT_SETTINGS_MODE_ADVANCED)) {
				if (settings.advancedSettings != null) {
					dealParam = settings.advancedSettings.toBridgeDealParam();
				}
			}
			// UNKNOWN MODE
			else {
				log.error("Unknown mode from settings ! settings="+settings);
			}
		}
		return dealParam;
	}
	
	/**
	 * Create a new tournament and persist it to DB
	 * @param cat
	 * @param dealCount
	 * @param resultType
	 * @param beginDate
	 * @param endDate
	 * @param engineVersion
	 * @param nbMaxPlayer
	 * @return
	 */
	private Tournament createTournament(TournamentCategory cat,
			String name, String serie, int dealCount, int resultType,
			long beginDate, long lastStartDate, long endDate,
			int engineVersion, int nbMaxPlayer, int nbCreditPlayDeal) {
		Tournament tour = new Tournament();
		tour.setCategory(cat);
		tour.setName(name);
		tour.setSerie(serie);
		tour.setCountDeal(dealCount);
		tour.setResultType(resultType);
		tour.setBeginDate(beginDate);
		tour.setLastStartDate(lastStartDate);
		tour.setEndDate(endDate);
		tour.setCreationDate(System.currentTimeMillis());
		int lastTourNumber = tournamentDAO.getLastTournamentNumber();
		tour.setNumber(lastTourNumber + 1);
		tour.setEngineVersion(engineVersion);
		tour.setNbMaxPlayer(nbMaxPlayer);
		tour.setNbCreditPlayDeal(nbCreditPlayDeal);
		if (tournamentDAO.addTournament(tour)) {
			return tour;
		}
		log.error("Error to add tournament="+tour.toString());
		return null;
	}
	
	/**
	 * Create a new distribution for each deal in array and persist all to DB
	 * @param tabDeal
	 * @return
	 */
	private TournamentDistribution[] createDistribution(String tabDeal[]) {
		if (tabDeal != null) {
			boolean bCreateOK = true;
			TournamentDistribution[] tabDistrib = new TournamentDistribution[tabDeal.length];
			for (int i = 0; i < tabDistrib.length; i++) {
				String[] temp = tabDeal[i].split(SEPARATOR_DISTRIB_PARAM);
				if (temp.length == 2) {
					String distrib = temp[0];
					TournamentDistribution td = new TournamentDistribution();
					td.setDealer(distrib.charAt(0));
					td.setVulnerability(distrib.charAt(1));
					td.setCards(distrib.substring(2));
					td.setParamGenerator(temp[1]);
					if (!tournamentDistributionDAO.addDistribution(td)) {
						log.error("Error to create add distribution : "+td.toString());
						bCreateOK = false;
						break;
					}
					tabDistrib[i] = td;
				}
				
			}
			
			if (bCreateOK) {
				return tabDistrib;
			}
		}
		return null;
	}
	
	/**
	 * Create all deal for each distribution and linked it to the tournament
	 * @param tabDistrib
	 * @param tour
	 * @return true if all deal has been created with success or false
	 */
	private boolean createDeal(TournamentDistribution[] tabDistrib, Tournament tour) {
		if (tabDistrib != null && tour != null) {
			TournamentDeal[] tabDeal = new TournamentDeal[tabDistrib.length];
			for (int i = 0; i < tabDeal.length; i++) {
				TournamentDeal deal = new TournamentDeal();
				deal.setDistribution(tabDistrib[i]);
				deal.setIndex(i + 1);
				deal.setTournament(tour);
				if (!tournamentDealDAO.addDeal(deal)) {
					log.error("Error to create add deal : "+deal);
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Create all deal for each distribution and linked it to the tournament
	 * @param distrib
	 * @param tour
	 * @return true if all deal has been created with success or false
	 */
	private boolean createDeal(TournamentDistribution distrib, Tournament tour) {
		if (distrib != null && tour != null) {
			TournamentDeal deal = new TournamentDeal();
			deal.setDistribution(distrib);
			deal.setIndex(1);
			deal.setTournament(tour);
			if (!tournamentDealDAO.addDeal(deal)) {
				log.error("Error to create add deal : "+deal);
				return false;
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Set the field valid on tournament to true
	 * @param tour
	 */
	private void validTournament(Tournament tour) {
		if (tour != null) {
			tour.setValid(true);
		}
	}
	
	/**
	 * Create a tournament for category
	 * @param cat category for this tournament
	 * @param name name of tournament
	 * @param serie serie of tournament
	 * @param resultType IMP or PAIRE
	 * @param dealCount nb deal to play in this tournament
	 * @param offsetIndex offset for index of deal. Used to generate a logical suite for vulnerability and dealer. -1 => random value
	 * @param tsStart available date to play tournament
	 * @param tsLastStart last date to start playing tournament
	 * @param tsEnd date of end tournament
	 * @param engineVersion version of engine to use
	 * @param nbMaxPlayer nb max player on the tournament
	 * @return
	 */
	@Transactional
	public Tournament createTournamentForCategory(TournamentCategory cat,
			String name, String serie, int resultType, int dealCount, int offsetIndex,
			long tsStart, long tsLastStart, long tsEnd,
			int engineVersion, int nbMaxPlayer, int nbCreditPlayDeal, TournamentSettings settings) {
		if (cat != null) {
			String[] tabDeal = new String[dealCount];
			boolean bDealOK = true;
			List<String> listDistribGen = new ArrayList<String>(); 
			for (int i = 0; i < dealCount; i++) {
				// LOAD DEAL PARAM
				BridgeDealParam param = null;
				if (settings == null) {
					// BY DEFAULT, COME FROM GENERATOR FILE
					param = getRandomParamGenertor();
				} else {
					param = getParamGeneratorFromSettings(settings);
				}
				
				if (param != null && param.isValid()) {
					if (offsetIndex == -1) {
						Random r = new Random();
						param.index = r.nextInt(100);
					} else {
						param.index = offsetIndex + i+1; // set index of deal param to deal number
					}
					// GENERATE DEAL
					String dealGen = null;
					int nbTry = 0;
					try {
						boolean bRetry = false;
						do {
							dealGen = BridgeDeal.generateDeal(param);
							nbTry++;
							// check no same deal already exists
							// in list of generate deal for previous index
							if (listDistribGen.contains(dealGen.substring(2))) {
								bRetry = true;
							}
							// and in list of existing deal
							else {
								List<TournamentDistribution> listSameDistrib = tournamentDistributionDAO.listForCards(dealGen.substring(2), 1);
								if (listSameDistrib != null && listSameDistrib.size() > 0) {
									bRetry = true;
								}
							}
							// must retry => wait a little ...
							if (bRetry) {
								log.error("Generate same distribution try to generate again ! dealGen="+dealGen);
								Thread.sleep(100); // wait 100 ms
							}
						} while (bRetry && nbTry < 10);
					} catch (Exception e) {
						log.error("Exception to generate deal", e);
					}
					if (log.isDebugEnabled()) {
						log.debug("Param="+param.toString());
						log.debug("DealGen="+dealGen+" - nbPtsS="+BridgeDeal.getNbPointsHonForPlayer(dealGen.substring(2), 'S')+" - nbPtsN="+BridgeDeal.getNbPointsHonForPlayer(dealGen.substring(2), 'N'));
						log.debug("Deal check param="+BridgeDeal.checkDistribution(dealGen.substring(2), param));
					}
					tabDeal[i] = dealGen;
					if (tabDeal[i] == null) {
						bDealOK = false;
						break;
					}
					// add list of generated distrib
					listDistribGen.add(dealGen.substring(2));
					tabDeal[i] += SEPARATOR_DISTRIB_PARAM+param.paramName;
				} else {
					log.error("BridgeDealParam is not valid param="+param);
					bDealOK = false;
					break;
				}
			}
			// clear list of generated distrib ... no longer uses
			listDistribGen.clear();
			if (bDealOK) {
				// create only one tournament at a time
				synchronized (objSynchroGenerator) {
					// CREATE DISTRIB
					TournamentDistribution[] tabDistrib = createDistribution(tabDeal);
					
					if (tabDistrib != null) {
						// CREATE TOURNAMENT
						Tournament tour = createTournament(cat, name, serie,
								dealCount, resultType,
								tsStart, tsLastStart, tsEnd,
								engineVersion, nbMaxPlayer, nbCreditPlayDeal);
						if (settings != null) {
							ContextManager.getTournamentMgr().setTournamentSettings(tour, settings);
						}
						// CREATE DEAL
						if (createDeal(tabDistrib, tour)) {
							// SET TOURNAMENT VALID
							validTournament(tour);
							return tour;
						} else {
							log.error("Failed to create deal for tour="+tour);
						}
					} else {
						log.error("Tab Distribution is null !");
					}
				} // fin synchronize
			} else {
				log.error("Deal is not OK");
			}
		}
		log.error("Failed to create tournament - Cat="+cat+" - name="+name+" - serie="+serie+" - resultType="+resultType+" - dealCount="+dealCount+" - settings="+settings);
		return null;
	}
}
