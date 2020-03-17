package com.funbridge.server.tournament;

import com.gotogames.common.bridge.BridgeDeal;
import com.gotogames.common.bridge.BridgeDealParam;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;
import org.ini4j.Profile.Section;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DealGenerator {
	protected Logger log = LogManager.getLogger(this.getClass());
	private IDealGeneratorCallback dealGeneratorCallback = null;
	private String fileParam = null;
	
	public DealGenerator(IDealGeneratorCallback dealCallback, String fileParamPath) {
		this.dealGeneratorCallback = dealCallback;
		this.fileParam = fileParamPath;
	}
	
	/**
	 * Deal generated
	 * @author pserent
	 *
	 */
	public class DealGenerate {
		public String distrib;
		public char vulnerability;
		public char dealer;
		public String paramGenerator;
		public int index;
	}
	
	/**
	 * Generate an array of deal
	 * @param nbDeal
	 * @param offsetIndex offset for index of deal. Used to generate a logical suite for vulnerability and dealer. -1 => random value
	 * @return
	 */
	public DealGenerate[] generateDeal(int nbDeal, int offsetIndex) {
		Ini ini = new Ini();
		if (fileParam != null) {
			try {
				ini.load(new File(fileParam));
				List<String> listDistribGen = new ArrayList<String>();
				DealGenerate[] tabDealGenerate = new DealGenerate[nbDeal];
				for (int i = 0; i < nbDeal; i++) {
					BridgeDealParam dealParam = null;
					int nbTry = 0;
					boolean dealParamValid = false;
					// get random BridgeDealParam
					while (!dealParamValid) {
						dealParam = getRandomParamGenerator(ini);
						nbTry++;
						if (dealParam == null) {
							throw new Exception("Failed to get random parameter");
						}
						if (!dealParam.isValid()) {
							log.error("BridgeDealParam generated not valid - param="+dealParam);
							if (nbTry >= 5) {
								throw new Exception("Failed 5x to generated valid BridgeDealParam ... stop");
							}
						} else {
							dealParamValid = true;
						}
					}
					dealParam.index = i+1; // set index of deal param to deal number
					if (offsetIndex == -1) {
						Random r = new Random();
						dealParam.index = r.nextInt(100);
					} else {
						dealParam.index = offsetIndex + i+1; // set index of deal param to deal number
					}
					// Generate deal
					String dealGen = null;
					boolean bRetry = false;
					do {
						dealGen = BridgeDeal.generateDeal(dealParam);
						if (dealGen == null) {
							throw new Exception("Deal generated is null");
						}
						nbTry++;
						// check no same deal already exists
						// in list of generate deal for previous index
						if (listDistribGen.contains(dealGen.substring(2))) {
							bRetry = true;
						}
						// and in list of existing deal
						else {
							bRetry = dealGeneratorCallback.distributionExists(dealGen.substring(2));
						}
						// must retry => wait a little ...
						if (bRetry) {
							log.error("Generate same distribution try to generate again ! dealGen="+dealGen);
							Thread.sleep(100); // wait 100 ms
						}
					} while (bRetry && nbTry < 10);
					
					// add list of generated distrib
					listDistribGen.add(dealGen.substring(2));
					tabDealGenerate[i] = new DealGenerate();
					tabDealGenerate[i].distrib = dealGen.substring(2);
					tabDealGenerate[i].dealer = dealGen.charAt(0);
					tabDealGenerate[i].vulnerability = dealGen.charAt(1);
					tabDealGenerate[i].paramGenerator = dealParam.paramName;
					tabDealGenerate[i].index = i+1;
				}
				return tabDealGenerate;
			} catch (InvalidFileFormatException e) {
				log.error("Generator param file invalid format (no ini file) - fileParam="+fileParam,e);
			} catch (IOException e) {
				log.error("Generator param file error loading ini - fileParam="+fileParam,e);
			} catch (Exception e) {
				log.error("Exception to build deal - fileParam="+fileParam,e);
			}
		} else {
			log.error("Parameter fileParam not valid ! fileParam="+fileParam);
		}
		return null;
	}
	
	/**
	 * get a random parameter from Ini data
	 * @return
	 */
	private BridgeDealParam getRandomParamGenerator(Ini iniData) {
		Random random = new Random(System.nanoTime());
		Section secList = iniData.get("LIST");
		if (secList != null) {
			String[] keytab = secList.keySet().toArray(new String[secList.size()]);
			int randomListIdx = random.nextInt(keytab.length);
			String randomList = secList.get(keytab[randomListIdx]);
				
			if (randomList != null) {
				Section secListParam = iniData.get(randomList);
				if (secListParam != null) {
					String[] tabListParam = secListParam.keySet().toArray(new String[secListParam.size()]);
					int randomParam = random.nextInt(tabListParam.length);
					String param = secListParam.get(tabListParam[randomParam]);
					
					Section secParam = iniData.get(param);
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
		return null;
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
}
