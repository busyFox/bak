package com.funbridge.server.texts;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.common.FunbridgeMgr;
import com.funbridge.server.message.MessageNotifMgr;
import com.funbridge.server.tournament.serie.TourSerieMgr;
import com.gotogames.common.tools.NumericalTools;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.FileConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Created by pserent on 06/08/2014.
 */
@Component(value = "textUIMgr")
@Scope(value = "singleton")
public class TextUIMgr extends FunbridgeMgr{
    private ConcurrentHashMap<String, TextUIData> mapTextUI = new ConcurrentHashMap<String, TextUIData>();
    private ConcurrentHashMap<String, TextUIData> mapTextPush = new ConcurrentHashMap<String, TextUIData>();
    private ConcurrentHashMap<String, TextUIData> mapTextNotif = new ConcurrentHashMap<String, TextUIData>();
    private ConcurrentHashMap<String, TextUIData> mapTextMsg = new ConcurrentHashMap<String, TextUIData>();
    List<String> listDataNameTextUI = null, listDataNamePush = null, listDataNameNotif = null, listDataNameMsg = null;
    private Map<String, List<String>> mapDataNameTextUIForProject = null, mapDataNamePushForProject = null, mapDataNameNotifForProject = null, mapDataNameMsgForProject = null;
    private final ReentrantReadWriteLock rwLockTemplate = new ReentrantReadWriteLock();
    private final Lock lockReadTemplate = rwLockTemplate.readLock();
    private final Lock lockWriteTemplate = rwLockTemplate.writeLock();
    public static Pattern pattern;
    @Resource(name = "tourSerieMgr")
    private TourSerieMgr tourSerieMgr = null;

    @PostConstruct
    @Override
    public void init() {
        try {
            pattern = Pattern.compile("\\{(.+?)\\}");
        } catch (PatternSyntaxException e) {
            log.error("Pattern not valid !",e);
        }
    }

    @PreDestroy
    @Override
    public void destroy() {
        mapTextUI.clear();
        mapTextNotif.clear();
        mapTextPush.clear();
        mapTextMsg.clear();
    }

    @Override
    public void startUp() {
        // load all textUI data
        loadAllTextUIData();
    }

    public String[] getSupportedLang() {
        return FBConfiguration.getInstance().getStringValue("textui.supportedLang", "fr;en;nl").split(Constantes.SEPARATOR_VALUE);
    }

    public boolean isLangSupported(String value){
        String[] supportedLang = getSupportedLang();
        for(String lang : supportedLang){
            if(lang.equalsIgnoreCase(value)) return true;
        }
        return false;
    }

    public String[] getProjects() {
        return FBConfiguration.getInstance().getStringValue("textui.projects", "others").split(Constantes.SEPARATOR_VALUE);
    }

    /**
     * Load textUI data from config file (for language FR, EN & NL)
     * @return
     */
    public boolean loadAllTextUIData() {
        boolean loadOK = false;
        // block reader during loading template
        lockWriteTemplate.lock();
        try {
            char c = 0;
            AbstractConfiguration.setDefaultListDelimiter(c);
            // load all data name
            String fileText = FBConfiguration.getInstance().getStringResolvEnvVariableValue("textui.define", null);
            String[] projects = getProjects();
            log.info("Projects to load : " + Arrays.toString(projects));
            try {
                // load notif properties
                if (fileText != null && new File(fileText).isFile()) {
                    FileConfiguration fcText = new PropertiesConfiguration(fileText);
                    mapDataNameTextUIForProject = new HashMap<>();
                    mapDataNameMsgForProject = new HashMap<>();
                    mapDataNamePushForProject = new HashMap<>();
                    mapDataNameNotifForProject = new HashMap<>();
                    listDataNameTextUI = new ArrayList<>();
                    listDataNameMsg = new ArrayList<>();
                    listDataNamePush = new ArrayList<>();
                    listDataNameNotif = new ArrayList<>();
                    for (String project : projects) {
                        String suffix = "";
                        if (!project.equals("others")) {
                            suffix = "."+project;
                        }
                        List<String> listDataNameTextUIForProject = Arrays.asList(fcText.getStringArray("textui.define"+suffix));
                        Collections.sort(listDataNameTextUIForProject);
                        log.info("Nb text "+project+" textui=" + listDataNameTextUIForProject.size());
                        List<String> listDataNameMsgForProject = Arrays.asList(fcText.getStringArray("msg.define"+suffix));
                        Collections.sort(listDataNameMsgForProject);
                        log.info("Nb text "+project+" msg=" + listDataNameMsgForProject.size());
                        List<String> listDataNamePushForProject = Arrays.asList(fcText.getStringArray("push.define"+suffix));
                        Collections.sort(listDataNamePushForProject);
                        log.info("Nb text "+project+" push=" + listDataNamePushForProject.size());
                        List<String> listDataNameNotifForProject = Arrays.asList(fcText.getStringArray("notif.define"+suffix));
                        Collections.sort(listDataNameNotifForProject);
                        log.info("Nb text "+project+" notif=" + listDataNameNotifForProject.size());

                        listDataNameTextUI.addAll(listDataNameTextUIForProject);
                        listDataNameMsg.addAll(listDataNameMsgForProject);
                        listDataNamePush.addAll(listDataNamePushForProject);
                        listDataNameNotif.addAll(listDataNameNotifForProject);

                        mapDataNameTextUIForProject.put(project, listDataNameTextUIForProject);
                        mapDataNameMsgForProject.put(project, listDataNameMsgForProject);
                        mapDataNamePushForProject.put(project, listDataNamePushForProject);
                        mapDataNameNotifForProject.put(project, listDataNameNotifForProject);
                    }
                } else {
                    log.error("Settings notif properties not found file=" + fileText);
                }
            } catch (ConfigurationException e) {
                log.error("ConfigurationException - Error to read properties file=" + fileText, e);
            } catch (Exception e) {
                log.error("Exception - Error to read properties file=" + fileText, e);
            }

            // load text data for each lang
            if (listDataNameTextUI != null && listDataNameTextUI.size() > 0) {
                // clear all current message
                mapTextUI.clear();
                mapTextNotif.clear();
                mapTextPush.clear();
                mapTextMsg.clear();

                String[] supportedLang = getSupportedLang();
                log.info("Lang to load : " + Arrays.toString(supportedLang));

                // for each lang
                for (String lang : supportedLang) {

                    for (String project : projects) {
                        String fileLang;
                        if (project.equals("others")) {
                            fileLang = FBConfiguration.getInstance().getStringResolvEnvVariableValue("textui.data_" + lang, null);
                        } else {
                            fileLang = FBConfiguration.getInstance().getStringResolvEnvVariableValue("textui.data_" + project + "_" + lang, null);
                        }
                        try {
                            FileConfiguration fcTextLang = null;
                            // load message text
                            if (fileLang != null && new File(fileLang).isFile()) {
                                fcTextLang = new PropertiesConfiguration();
                                fcTextLang.setEncoding("UTF-8");
                                fcTextLang.load(fileLang);
                            } else {
                                log.error("Data Text for lang " + lang + " - file not found file=" + fileLang);
                            }
                            if (fcTextLang != null) {
                                int nbDataText = 0;
                                // LOAD TEXTUI
                                for (String dataName : mapDataNameTextUIForProject.get(project)) {
                                    TextUIData textData = mapTextUI.get(dataName);
                                    if (textData == null) {
                                        textData = new TextUIData();
                                        textData.name = dataName;
                                        mapTextUI.put(dataName, textData);
                                    }
                                    String text = getDataText(fcTextLang, "textui." + dataName, null);
                                    if (text != null && text.length() > 0) {
                                        textData.addTextLang(lang, text);
                                        nbDataText++;
                                    } else {
                                        log.error("No text found for lang=" + lang + " - TEXTUI - dataName=" + dataName);
                                    }
                                }
                                // LOAD NOTIF
                                for (String dataName : mapDataNameNotifForProject.get(project)) {
                                    // Body
                                    TextUIData textData = mapTextNotif.get(dataName);
                                    if (textData == null) {
                                        textData = new TextUIData();
                                        textData.name = dataName;
                                        mapTextNotif.put(dataName, textData);
                                    }
                                    String text = getDataText(fcTextLang, "notif." + dataName + ".body", null);
                                    if (text != null && text.length() > 0) {
                                        textData.addTextLang(lang, text);
                                        nbDataText++;
                                    } else {
                                        log.error("No text found for lang=" + lang + " - NOTIF - dataName=" + dataName);
                                    }

                                    // Rich Body
                                    String dataNameRichBody = dataName + ".richbody";
                                    text = getDataText(fcTextLang, "notif." + dataNameRichBody, null);
                                    if (text != null && text.length() > 0) {
                                        textData = mapTextNotif.get(dataNameRichBody);
                                        if (textData == null) {
                                            textData = new TextUIData();
                                            textData.name = dataNameRichBody;
                                            mapTextNotif.put(dataNameRichBody, textData);
                                        }
                                        textData.addTextLang(lang, text);
                                        nbDataText++;
                                    }

                                    // Title
                                    String dataNameTitle = dataName + ".title";
                                    text = getDataText(fcTextLang, "notif." + dataNameTitle, null);
                                    if (text != null && text.length() > 0) {
                                        textData = mapTextNotif.get(dataNameTitle);
                                        if (textData == null) {
                                            textData = new TextUIData();
                                            textData.name = dataNameTitle;
                                            mapTextNotif.put(dataNameTitle, textData);
                                        }
                                        textData.addTextLang(lang, text);
                                        nbDataText++;
                                    }

                                    // Button
                                    String dataNameButton = dataName + ".button";
                                    text = getDataText(fcTextLang, "notif." + dataNameButton, null);
                                    if (text != null && text.length() > 0) {
                                        textData = mapTextNotif.get(dataNameButton);
                                        if (textData == null) {
                                            textData = new TextUIData();
                                            textData.name = dataNameButton;
                                            mapTextNotif.put(dataNameButton, textData);
                                        }
                                        textData.addTextLang(lang, text);
                                        nbDataText++;
                                    }

                                    // Action
                                    String dataNameAction = dataName + ".action";
                                    text = getDataText(fcTextLang, "notif." + dataNameAction, null);
                                    if (text != null && text.length() > 0) {
                                        textData = mapTextNotif.get(dataNameAction);
                                        if (textData == null) {
                                            textData = new TextUIData();
                                            textData.name = dataNameAction;
                                            mapTextNotif.put(dataNameAction, textData);
                                        }
                                        textData.addTextLang(lang, text);
                                        nbDataText++;
                                    }
                                }
                                // LOAD PUSH
                                for (String dataName : mapDataNamePushForProject.get(project)) {
                                    TextUIData textData = mapTextPush.get(dataName);
                                    if (textData == null) {
                                        textData = new TextUIData();
                                        textData.name = dataName;
                                        mapTextPush.put(dataName, textData);
                                    }
                                    String text = getDataText(fcTextLang, "push." + dataName, null);
                                    if (text != null && text.length() > 0) {
                                        textData.addTextLang(lang, text);
                                        nbDataText++;
                                    } else {
                                        log.error("No text found for lang=" + lang + " - PUSH - dataName=" + dataName);
                                    }
                                }
                                // LOAD MSG
                                for (String dataName : mapDataNameMsgForProject.get(project)) {
                                    TextUIData textData = mapTextMsg.get(dataName);
                                    if (textData == null) {
                                        textData = new TextUIData();
                                        textData.name = dataName;
                                        mapTextMsg.put(dataName, textData);
                                    }
                                    String text = getDataText(fcTextLang, "msg." + dataName + ".body", null);
                                    if (text != null && text.length() > 0) {
                                        textData.addTextLang(lang, text);
                                        nbDataText++;
                                    } else {
                                        log.error("No text found for lang=" + lang + " - MSG - dataName=" + dataName);
                                    }
                                }
                                log.info("Load data text for lang=" + lang + " from file=" + fileLang + " - nbDataText=" + nbDataText);
                            }
                        } catch (ConfigurationException e) {
                            log.error("ConfigurationException - Error to read properties file=" + fileLang, e);
                        } catch (Exception e) {
                            log.error("Exception - Error to read properties file=" + fileLang, e);
                        }
                    }
                }
                loadOK = true;
            } else {
                log.error("List of data is null or empty !!");
            }
        } finally {
            lockWriteTemplate.unlock();
        }
        return loadOK;
    }

    /**
     * Retrieve the value of key in config file
     * @param fileConfig
     * @param key
     * @param defaultValue
     * @return
     */
    private String getDataText(FileConfiguration fileConfig, String key, String defaultValue) {
        String value = defaultValue;
        if (fileConfig != null && key != null && key.length() > 0) {
            value = fileConfig.getString(key, defaultValue);
        } else {
            if (fileConfig == null) {
                log.error("File config is null key="+key+" - defaultValue="+defaultValue);
            } else {
                log.error("key not valid ! key="+key);
            }
        }
        return value;
    }

    /**
     * Get text for textUI with name
     * @param name
     * @return
     */
    public TextUIData getTextUI(String name) {
        lockReadTemplate.lock();
        try {
            return mapTextUI.get(name);
        } finally {
            lockReadTemplate.unlock();
        }
    }

    /**
     * Get text for notif with name
     * @param name
     * @return
     */
    public TextUIData getTextNotif(String name) {
        lockReadTemplate.lock();
        try {
            return mapTextNotif.get(name);
        } finally {
            lockReadTemplate.unlock();
        }
    }

    /**
     * Get text for push with name
     * @param name
     * @return
     */
    public TextUIData getTextPush(String name) {
        lockReadTemplate.lock();
        try {
            return mapTextPush.get(name);
        } finally {
            lockReadTemplate.unlock();
        }
    }

    /**
     * Get text for msg with name
     * @param name
     * @return
     */
    public TextUIData getTextMsg(String name) {
        lockReadTemplate.lock();
        try {
            return mapTextMsg.get(name);
        } finally {
            lockReadTemplate.unlock();
        }
    }

    /**
     * Return all texts (textui, notif, push & msg)
     * @return
     */
    public List<TextUIData> listAllTexts() {
        lockReadTemplate.lock();
        try {
            List<TextUIData> list =  new ArrayList<TextUIData>();
            list.addAll(mapTextUI.values());
            list.addAll(mapTextNotif.values());
            list.addAll(mapTextPush.values());
            list.addAll(mapTextMsg.values());
            Collections.sort(list, new Comparator<TextUIData>() {

                @Override
                public int compare(TextUIData o1, TextUIData o2) {
                    return o1.name.compareTo(o2.name);
                }
            });
            return list;
        } finally {
            lockReadTemplate.unlock();
        }
    }

    /**
     * Return all texts from a map
     * @return
     */
    public List<TextUIData> listText(Map<String, TextUIData> map) {
        lockReadTemplate.lock();
        try {
            List<TextUIData> list =  new ArrayList<TextUIData>(map.values());
            Collections.sort(list, new Comparator<TextUIData>() {

                @Override
                public int compare(TextUIData o1, TextUIData o2) {
                    return o1.name.compareTo(o2.name);
                }
            });
            return list;
        } finally {
            lockReadTemplate.unlock();
        }
    }

    /**
     * Return all texts type textUI
     * @return
     */
    public List<TextUIData> listTextUI() {
        return listText(mapTextUI);
    }

    /**
     * Return all texts type notif
     * @return
     */
    public List<TextUIData> listTextNotif() {
        return listText(mapTextNotif);
    }

    /**
     * Return all texts type push
     * @return
     */
    public List<TextUIData> listTextPush() {
        return listText(mapTextPush);
    }

    /**
     * Return all texts type msg
     * @return
     */
    public List<TextUIData> listTextMsg() {
        return listText(mapTextMsg);
    }

    public List<String> getListTemplateNameTextUI() {
        return listDataNameTextUI;
    }

    public List<String> getListTemplateNameMsg() {
        return listDataNameMsg;
    }

    public List<String> getListTemplateNameNotif() {
        return listDataNameNotif;
    }

    public List<String> getListTemplateNamePush() {
        return listDataNamePush;
    }

    /**
     * Replace the variables in text by the values contain in the map. Variable are like '{VAR_NAME}' and map contain pair value (VAR_NAME, value)
     * @param msgText
     * @param varVal
     * @return
     */
    public static String replaceTextVariables(String msgText, Map<String, String> varVal) {
        String strReturn = msgText;
        if (msgText != null && msgText.indexOf("{") >= 0) {
            if (varVal != null && varVal.size() > 0) {
                // replace all token by the value
                Matcher matcher = pattern.matcher(msgText);
                StringBuffer sb = new StringBuffer();
                while (matcher.find()) {
                    String val = varVal.get(matcher.group(1));
                    if (val != null) {
                        matcher.appendReplacement(sb, "");
                        sb.append(val);
                    }
                }
                matcher.appendTail(sb);
                strReturn = sb.toString();
            }
        }
        return strReturn;
    }

    /**
     * Return the text for serieBonusFirst
     * @param lang
     * @param nbTourBeforeBonus
     * @return
     */
    public String getTextSerieBonusFirst(String lang, int nbTourBeforeBonus) {
        String dataText = getTextUIForLang("serieBonusFirst", lang);
        if (dataText != null) {
            Map<String, String> mapExtra = new HashMap<String, String>();
            mapExtra.put("NB_TOUR_BEFORE_BONUS", ""+nbTourBeforeBonus);
            dataText = replaceTextVariables(dataText, mapExtra);
            return dataText;
        }
        return null;
    }

    /**
     * Return the text for serieBonusNext
     * @param lang
     * @param nbTourRemoveBonus
     * @return
     */
    public String getTextSerieBonusNext(String lang, int nbTourRemoveBonus, int ruleNbTourRemoveBonus, int ruleNbTourBonus) {
        String dataText = getTextUIForLang("serieBonusNext", lang);
        if (dataText != null) {
            Map<String, String> mapExtra = new HashMap<String, String>();
            mapExtra.put("NB_TOUR_REMOVE_BONUS", ""+nbTourRemoveBonus);
            mapExtra.put("RULE_NB_TOUR_REMOVE_BONUS", ""+ruleNbTourRemoveBonus);
            mapExtra.put("RULE_NB_TOUR_BONUS", ""+ruleNbTourBonus);
            dataText = replaceTextVariables(dataText, mapExtra);
            return dataText;
        }
        return null;
    }

    /**
     * Return the text for serieBonusDescription
     * @param lang
     * @param ruleNbTourRemoveBonus
     * @param ruleNbTourBonus
     * @return
     */
    public String getTextSerieBonusDescription(String lang, int ruleNbTourRemoveBonus, int ruleNbTourBonus) {
        String dataText = getTextUIForLang("serieBonusDescription", lang);
        if (dataText != null) {
            Map<String, String> mapExtra = new HashMap<String, String>();
            mapExtra.put("RULE_NB_TOUR_REMOVE_BONUS", ""+ruleNbTourRemoveBonus);
            mapExtra.put("RULE_NB_TOUR_BONUS", ""+ruleNbTourBonus);
            dataText = replaceTextVariables(dataText, mapExtra);
            return dataText;
        }
        return null;
    }

    /**
     * Return the text for this lang
     * @param name
     * @param lang
     * @return
     */
    public String getTextUIForLang(String name, String lang) {
        TextUIData t = getTextUI(name);
        if (t != null) {
            String text = t.getText(lang);
            if (text != null && text.length() > 0) {
                Map<String, String> mapVarVal = new HashMap<String, String>();
                mapVarVal.put(MessageNotifMgr.VARIABLE_LINE_BREAK, FBConfiguration.getInstance().getStringValue("message.notifLineBreak", "\n"));
                text = replaceTextVariables(text, mapVarVal);
            }
            return text;
        } else {
            log.error("No textUIData found for name="+name);
        }
        return "";
    }

    public String getTextUIAndReplaceVariableForLang(String name, String lang, Map<String, String> mapExtra) {
        String textResult = getTextUIForLang(name, lang);
        textResult = replaceTextVariables(textResult, mapExtra);
        if (textResult == null) {
            textResult = "";
        }
        return textResult;
    }

    /**
     * Build text serie trend using all parameters
     * @param lang
     * @param currentSerie
     * @param trend
     * @param nbTournamentPlayed
     * @param lastPeriodPlayed
     * @param thresholdNbTourUp
     * @param thresholdNbTourMaintain
     * @param thresholdUp
     * @param thresholdMaintain
     * @return
     */
    public String getTextSerieTrend(String lang, String currentSerie, int trend, int nbTournamentPlayed, String lastPeriodPlayed, int thresholdNbTourUp, int thresholdNbTourMaintain, double thresholdUp, double thresholdMaintain) {
        String textResult = null;
        Map<String, String> mapExtra = new HashMap<String, String>();
        String serieUp = TourSerieMgr.computeSerieEvolution(currentSerie, 1, false);
        String textSerieUp = "", textSerieCurrent = "";
        textSerieUp = Constantes.MESSAGE_SERIE_PARAM.get("SERIE_"+serieUp);
        textSerieCurrent = Constantes.MESSAGE_SERIE_PARAM.get("SERIE_"+currentSerie);
        mapExtra.put("SERIE_UP", textSerieUp);
        mapExtra.put("SERIE_CURRENT", textSerieCurrent);
        mapExtra.put("SERIE_THRESHOLD_UP", ""+ NumericalTools.round(thresholdUp * 100, 2)+"%");
        mapExtra.put("SERIE_THRESHOLD_MAINTAIN", ""+NumericalTools.round(thresholdMaintain*100, 2)+"%");
        mapExtra.put("SERIE_THRESHOLD_NB_TOUR_UP", ""+thresholdNbTourUp);
        mapExtra.put("SERIE_THRESHOLD_NB_TOUR_MAINTAIN", ""+thresholdNbTourMaintain);
        // SERIE NC
        if (currentSerie.equals(TourSerieMgr.SERIE_NC)) {
            if (trend > 0) {
                textResult = getTextUIForLang("serieTrendG", lang);
            } else {
                textResult = getTextUIForLang("serieTrendF", lang);
            }
        }
        // SERIE TOP
        else if (currentSerie.equals(TourSerieMgr.SERIE_TOP)) {
            if (nbTournamentPlayed == 0 && trend == 0) {
                textResult = ""; // nothing to say ...
            }
            else if (nbTournamentPlayed == 0 && trend < 0) {
                textResult = getTextUIForLang("serieTrendB", lang);
            }
            else {
                if (trend < 0) {
                    textResult = getTextUIForLang("serieTrendC", lang);
                } else {
                    textResult = getTextUIForLang("serieTrendD", lang);
                }
            }
        }
        // SERIE 11
        else if (currentSerie.equals(TourSerieMgr.SERIE_11)) {
            if (nbTournamentPlayed == 0) {
                if (tourSerieMgr.isPlayerReserve(currentSerie, lastPeriodPlayed, false)) {
                    textResult = getTextUIForLang("serieTrendJ", lang);
                } else {
                    textResult = getTextUIForLang("serieTrendA", lang);
                }
            }
            else {
                if (nbTournamentPlayed < thresholdNbTourUp) {
                    textResult = getTextUIForLang("serieTrendA", lang);
                } else {
                    if (trend > 0) {
                        textResult = getTextUIForLang("serieTrendE", lang);
                    } else {
                        textResult = getTextUIForLang("serieTrendI", lang);
                    }
                }
            }
        }
        // OTHER SERIE
        else {
            if (nbTournamentPlayed == 0) {
                if (tourSerieMgr.isPlayerReserve(currentSerie, lastPeriodPlayed, false)) {
                    textResult = getTextUIForLang("serieTrendJ", lang);
                } else {
                    if (trend == 0) {
                        if (currentSerie.equals(TourSerieMgr.SERIE_01)) {
                            textResult = getTextUIForLang("serieTrendAElite", lang);
                        } else {
                            textResult = getTextUIForLang("serieTrendA", lang);
                        }
                    } else {
                        textResult = getTextUIForLang("serieTrendB", lang);
                    }
                }
            } else {
                if (nbTournamentPlayed < thresholdNbTourUp) {
                    if (trend < 0) {
                        textResult = getTextUIForLang("serieTrendC", lang);
                    } else if (trend == 0) {
                        if (currentSerie.equals(TourSerieMgr.SERIE_01)) {
                            textResult = getTextUIForLang("serieTrendAElite", lang)+ " "+getTextUIForLang("serieTrendL", lang);
                        } else {
                            textResult = getTextUIForLang("serieTrendA", lang)+ " "+getTextUIForLang("serieTrendL", lang);
                        }
                    }
                } else {
                    if (trend < 0) {
                        textResult = getTextUIForLang("serieTrendC", lang);
                    } else if (trend == 0) {
                        if (currentSerie.equals(TourSerieMgr.SERIE_01)) {
                            textResult = getTextUIForLang("serieTrendKElite", lang);
                        } else {
                            textResult = getTextUIForLang("serieTrendK", lang);
                        }
                    } else {
                        if (currentSerie.equals(TourSerieMgr.SERIE_01)) {
                            textResult = getTextUIForLang("serieTrendEElite", lang);
                        } else {
                            textResult = getTextUIForLang("serieTrendE", lang);
                        }
                    }
                }
            }
        }
        if (textResult != null) {
            textResult = replaceTextVariables(textResult, mapExtra);
        } else {
            textResult = "";
            log.error("No text found for parameters : lang="+lang+" - serie="+currentSerie+" - trend="+trend+" - nbTournamentPlayed="+nbTournamentPlayed+" - thresholdNbTourUp="+thresholdNbTourUp+" - thresholdNbTourMaintain="+thresholdNbTourMaintain+" - thresholdUp="+thresholdUp+" - thresholdMaintain="+thresholdMaintain);
        }
        return textResult;
    }

    /**
     * Build text analyze for bid
     * @param analyseBid
     * @param lang
     * @return
     */
    public String getTextAnalyzeBid(String analyseBid, String lang) {
        if (analyseBid == null || analyseBid.length() == 0) {
            analyseBid = "bidAnalyzeNotAvailable";
        }
        String[] tabData = analyseBid.split(";");
        String textResult = getTextUIForLang(tabData[0], lang);
        if (textResult != null) {
            if (tabData.length > 1) {
                Map<String, String> mapExtra = new HashMap<String, String>();
                for (int i = 1; i < tabData.length; i++) {
                    String[] temp = tabData[i].split("=");
                    if (temp.length == 2) {
                        mapExtra.put(temp[0], temp[1]);
                    }
                }
                textResult = replaceTextVariables(textResult, mapExtra);
            }
            return textResult;
        }
        return "";
    }

    /**
     * Build text analyze for play
     * @param analysePlay
     * @param lang
     * @return
     */
    public String getTextAnalyzePlay(String analysePlay, String lang) {
        if (analysePlay == null || analysePlay.length() == 0) {
            analysePlay = "cardAnalyzeNotAvailable";
        }
        String[] tabData = analysePlay.split(";");
        String textResult = getTextUIForLang(tabData[0], lang);
        if (textResult != null) {
            if (tabData.length > 1) {
                Map<String, String> mapExtra = new HashMap<String, String>();
                for (int i = 1; i < tabData.length; i++) {
                    String[] temp = tabData[i].split("=");
                    if (temp.length == 2) {
                        mapExtra.put(temp[0], temp[1]);
                    }
                }
                textResult = replaceTextVariables(textResult, mapExtra);
            }
            return textResult;
        }
        return "";
    }

    /**
     * Build text analyze for PAR : Le moteur retourne le par de la donne ainsi que la valeur du contrat possible dans chaque couleur et pour chaque joueur.
     * Les groupes de données sont séparés par des espaces.
     * Exemple : "3;4;4;2;460 2;5;5;5;5 0;0;0;0;0 2;5;5;5;5 0;0;0;0;0"
     * Tous ces champs sont à la sauce Jer :
     *  - Hauteur pour la hauteur du contrat : [0...7] (0 pour le passe général sinon la hauteur)
     *  - Couleur pour la couleur du contrat : [0...4] (TKCPN)
     *  - Le déclarant : [0...5],  soit [0...3] en NESO ou 4(pour Nord-Sud) ou 5(pour Est-Ouest)
     *  - Les levées : [-7...+7] (en négatif le contrat est contré, sinon la valeur absolue donne le nombre de levées de mieux réalisées)
     *  - score
     * Ensuite 4 groupes de valeurs correspondant à chaque main. Les valeurs correspondent à la valeur du contrat possible dans les couleurs Trèfle, Carreau, Coeur, Pique et Sans-Atout. L'ordre des mains est Nord, Est, Sud, Ouest.
     * @param par
     * @param lang
     * @return
     */
    public String getTextAnalyzePar(String par, String lang) {
        if (par == null) {
            par="";
        }
        String[] tabData = par.split(" ");
        String[] tempPar = tabData[0].split(";");
        String[] tempDataNorth = null, tempDataSouth = null;
        if (tabData.length == 5) {
            tempDataNorth = tabData[1].split(";");
            tempDataSouth = tabData[3].split(";");
        }

        String pattern = "parAnalyzeNotAvailable";
        Map<String, String> mapExtra = new HashMap<String, String>();
        if (tempPar.length == 5) {
            int contractHight = 0, contractColor = 0, contractDeclarer = 0, contractScore = 0, contractTrick = 0;
            try {
                contractHight = Integer.parseInt(tempPar[0]);
                contractColor = Integer.parseInt(tempPar[1]);
                contractDeclarer = Integer.parseInt(tempPar[2]);
                contractTrick = Integer.parseInt(tempPar[3]);
                contractScore = Integer.parseInt(tempPar[4]);
                String contract = "";
                // contract success on NS
                if (contractHight > 0 && contractScore > 0 && contractTrick >= 0) {
                    // contract declarer must be N, S or NS
                    if (contractDeclarer == 0 || contractDeclarer == 2 || contractDeclarer == 4) {
                        if (contractHight+contractTrick > 7) {
                            contract = "" + contractHight;
                        } else {
                            contract = "" + (contractHight + contractTrick);
                        }
                        if (contractColor == 0) {
                            contract+="C";
                        } else if (contractColor == 1) {
                            contract+="D";
                        } else if (contractColor == 2) {
                            contract+="H";
                        } else if (contractColor == 3) {
                            contract+="S";
                        } else if (contractColor == 4) {
                            contract+="N";
                        }
                        pattern = "parAnalyzeContract";
                        mapExtra.put("PAR_CONTRACT", "{"+contract+"}");
                    }
                }

                // contract with the the 4 hands
                if (contract.length() == 0) {
                    if (tempDataNorth.length == 5 && tempDataSouth.length == 5) {
                        int contractClub = Math.max(Integer.parseInt(tempDataNorth[0]), Integer.parseInt(tempDataSouth[0]));
                        int contractDiamond = Math.max(Integer.parseInt(tempDataNorth[1]), Integer.parseInt(tempDataSouth[1]));
                        int contractHeart = Math.max(Integer.parseInt(tempDataNorth[2]), Integer.parseInt(tempDataSouth[2]));
                        int contractSpade = Math.max(Integer.parseInt(tempDataNorth[3]), Integer.parseInt(tempDataSouth[3]));
                        int contractNoTrump = Math.max(Integer.parseInt(tempDataNorth[4]), Integer.parseInt(tempDataSouth[4]));
                        if (contractNoTrump > 0 && contractNoTrump >= contractSpade && contractNoTrump >= contractHeart && contractNoTrump >= contractDiamond && contractNoTrump >= contractClub) {
                            contract = contractNoTrump+"N";
                        }
                        else if (contractSpade > 0 && contractSpade >= contractHeart && contractSpade >= contractDiamond && contractSpade >= contractClub) {
                            contract = contractSpade+"S";
                        }
                        else if (contractHeart > 0 && contractHeart >= contractDiamond && contractHeart >= contractClub) {
                            contract = contractHeart+"H";
                        }
                        else if (contractDiamond > 0 && contractDiamond >= contractClub) {
                            contract = contractDiamond+"D";
                        }
                        else if (contractClub > 0) {
                            contract = contractClub+"C";
                        }
                        if (contract.length() > 0) {
                            pattern = "parAnalyzeContract";
                            mapExtra.put("PAR_CONTRACT", "{"+contract+"}");
                        }
                        else if (contract.length() == 0) {
                            pattern = "parAnalyzeNoContract";
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to parse data for par="+par, e);
            }
        }
        return getTextUIAndReplaceVariableForLang(pattern, lang, mapExtra);
    }

    public String formatDiscountForChinese(String value){
        if(value != null){
            if(StringUtils.isNumeric(value)){
                int discount = Integer.parseInt(value);
                value = ""+(100 - discount);
                if(value.length() == 2 && value.substring(1).equalsIgnoreCase("0")){
                    value = value.substring(0, 1);
                }
            }
        }
        return value;
    }


}
