package com.gotogames.bridge.engineserver.common;

import com.gotogames.common.tools.JSONTools;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.FileConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EngineConfiguration {
	private static EngineConfiguration _instance = null;
	private Logger log = LogManager.getLogger(this.getClass());
	private FileConfiguration config;
    private String versionTxt = "";
    private JSONTools jsonTools;
    private ArgineConventions argineConventionsBids = null;
    private ArgineConventions argineConventionsCards = null;

	public static EngineConfiguration getInstance() {
		if (_instance == null) {
			_instance = new EngineConfiguration();
		}
		return _instance;
	}
	
	private EngineConfiguration() {
	    jsonTools = new JSONTools();
		load();
	}
	
	private void load() {
		// try to set value from properties file
		String configFilePath = "";
		try {
			ResourceBundle bundle = ResourceBundle.getBundle("settings");
			configFilePath = bundle.getString("configurationFilePath");
            configFilePath = resolveEnvVariable(configFilePath);
			config = new PropertiesConfiguration();
            ((PropertiesConfiguration)config).setListDelimiter('\0');
            config.load(new File(configFilePath));
			config.setReloadingStrategy(new FileChangedReloadingStrategy());
		} catch (MissingResourceException e) {
			log.fatal("ERROR loading from bundle settings.properties", e);
		} catch (ConfigurationException e) {
			log.fatal("ERROR loading configuration from file : "+configFilePath, e);
		}

        // load version.txt
        try {
            InputStream inputStream = this.getClass().getResourceAsStream("/version.txt");
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            String line = "";
            versionTxt = "";
            while ((line=br.readLine()) != null) {
                if (versionTxt.length() > 0) {
                    versionTxt += " - ";
                }
                versionTxt += line;
            }
            br.close();
        } catch (Exception e) {
            log.error("Failed to load version.txt", e);
        }
	}
	
	public int getIntValue(String param, int defaultValue) {
		int val = defaultValue;
		try {
			val = config.getInt(param, defaultValue);
		} catch (Exception e) {
			log.error("PARAMETER "+param+" not found - use defautl value : "+val);
		}
		return val;
	}

    public long getLongValue(String param, long defaultValue) {
        long val = defaultValue;
        try {
            val = config.getLong(param, defaultValue);
        } catch (Exception e) {
            log.error("PARAMETER "+param+" not found - use defautl value : "+val);
        }
        return val;
    }
	
	public String getStringValue(String param, String defaultValue) {
		String val = defaultValue;
		try {
			val = config.getString(param, defaultValue);
		} catch (Exception e) {
			log.error("PARAMETER "+param+" not found - use defautl value : "+val);
		}
		return val;
	}

    public String getStringResolveEnvVariableValue(String param, String defaultValue){
        String stringValue = getStringValue(param, defaultValue);
        String stringValueResolved = resolveEnvVariable(stringValue);
        if (stringValueResolved != null) {
            // no exception to resolve
            return stringValueResolved;
        } else {
            return stringValue;
        }
    }

    /**
     * Resolve environment variables in string input with the value define in the environment.
     * The variable must have this format : ${SOME_VAR}
     * @param input string including environment variables (toto${SOME_VAR}tutu)
     * @return
     */
    public String resolveEnvVariable(String input) {
        if (null == input) {
            return null;
        }
        if (input.indexOf("${") >= 0) {
            try {
                // match ${ENV_VAR_NAME}
                Pattern p = Pattern.compile("\\$\\{([A-Za-z0-9-_]+)\\}");
                Matcher m = p.matcher(input); // get a matcher object
                StringBuffer sb = new StringBuffer();
                while (m.find()) {
                    String envVarName = m.group(1);
                    String envVarValue = System.getenv(envVarName);
                    m.appendReplacement(sb, envVarValue == null ? "" : envVarValue);
                }
                m.appendTail(sb);
                return sb.toString();
            } catch (Exception e) {
                log.error("Failed to resolveEnvVariable for input=" + input, e);
            }
            return null;
        } else {
            return input;
        }
    }

    public String getVersionTxt() {
        return versionTxt;
    }

    public JSONTools getJsonTools() {
        return jsonTools;
    }

    public ArgineConventions getArgineConventionsBids() {
        if (argineConventionsBids == null) {
            loadArgineConventions();
        }
        return argineConventionsBids;
    }

    public ArgineConventions getArgineConventionsCards() {
        if (argineConventionsCards == null) {
            loadArgineConventions();
        }
        return argineConventionsCards;
    }

    public void loadArgineConventions() {
        String argineConventionsBidsFilePath = getStringResolveEnvVariableValue("general.argineConventionsBidsFilePath", null);
        if (argineConventionsBidsFilePath != null) {
            try {
                argineConventionsBids = jsonTools.mapDataFromFile(argineConventionsBidsFilePath, ArgineConventions.class);
            } catch (Exception e) {
                log.error("Failed to load bids conventions file="+argineConventionsBidsFilePath);
            }
        }
        String argineConventionsCardsFilePath = getStringResolveEnvVariableValue("general.argineConventionsCardsFilePath", null);
        if (argineConventionsCardsFilePath != null) {
            try {
                argineConventionsCards = jsonTools.mapDataFromFile(argineConventionsCardsFilePath, ArgineConventions.class);
            } catch (Exception e) {
                log.error("Failed to load cards conventions file="+argineConventionsCardsFilePath);
            }
        }
    }
}
