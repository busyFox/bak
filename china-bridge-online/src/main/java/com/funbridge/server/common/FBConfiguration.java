package com.funbridge.server.common;

import com.gotogames.common.tools.StringTools;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.FileConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * read config-*.properties file
 */

public class FBConfiguration {
	private static FBConfiguration _instance = null;
	private Logger log = LogManager.getLogger(this.getClass());
	private FileConfiguration config;
    private String configFilePath = "";
    private String versionTxt = "";

	public static FBConfiguration getInstance() {
		if (_instance == null) {
			_instance = new FBConfiguration();
		}
		return _instance;
	}
	
	private FBConfiguration() {
		load();
	}
	
	private void load() {
		// try to set value from properties file
		configFilePath = "";
		try {
		    //获取classpath路径下的settings.properties文件
			ResourceBundle bundle = ResourceBundle.getBundle("settings");
			configFilePath = bundle.getString("configurationFilePath");
            log.debug("ConfigFilePath before resolved="+configFilePath);
            configFilePath = resolveEnvVariable(configFilePath);
            log.debug("ConfigFilePath after resolved="+configFilePath);
			config = new PropertiesConfiguration();
			((PropertiesConfiguration)config).setListDelimiter('\0');
			config.load(new File(configFilePath));

            //定时reload配置文件 默认reload每5s一次
			config.setReloadingStrategy(new FileChangedReloadingStrategy());

		} catch (MissingResourceException e) {
			log.fatal("ERROR loading from bundle settings.properties", e);
		} catch (ConfigurationException e) {
			log.fatal("ERROR loading configuration from file : "+configFilePath, e);
		}

        // load version.txt 加载 version.txt
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
	
	/**
	 * Return the value for this key. If not found return defaultValue
	 * @param param
	 * @param defaultValue
	 * @return
	 */
	public int getIntValue(String param, int defaultValue) {
		int val = defaultValue;
		try {
			val = config.getInt(param, defaultValue);
		} catch (Exception e) {
			log.debug("PARAMETER "+param+" not found - use defautl value : "+val);
		}
		return val;
	}

    /**
     * Return the boolean value for tihs key. If not found, return the default value.
     * The value must be an integer value : 0 => false, >=1 => true
     * @param param
     * @param defaultValue
     * @return
     */
    public boolean getConfigBooleanValue(String param, boolean defaultValue) {
        boolean val = defaultValue;
        try {
            if (config.containsKey(param)) {
                return getIntValue(param, 0) >= 1;
            }
        } catch (Exception e) {
            log.debug("PARAMETER "+param+" not found - use defautl value : "+val);
        }
        return val;
    }

    /**
     * Return the value for this key. If not found return defaultValue
     * @param param
     * @param defaultValue
     * @return
     */
    public long getLongValue(String param, long defaultValue) {
        long val = defaultValue;
        try {
            val = config.getLong(param, defaultValue);
        } catch (Exception e) {
            log.debug("PARAMETER "+param+" not found - use defautl value : "+val);
        }
        return val;
    }
	
	/**
	 * Return the value for this key. It not found, return default value
	 * @param param
	 * @param defaultValue
	 * @return
	 */
	public double getDoubleValue(String param, double defaultValue) {
		double val = defaultValue;
		try {
			val = config.getDouble(param, defaultValue);
		} catch (Exception e) {
			log.debug("PARAMETER "+param+" not found - use defautl value : "+val);
		}
		return val;
	}
	
	/**
	 * Return the value for this key. If not found return defaultValue
	 * @param param
	 * @param defaultValue
	 * @return
	 */
	public String getStringValue(String param, String defaultValue) {
		String val = defaultValue;
		try {
			val = config.getString(param, defaultValue);
		} catch (Exception e) {
			log.debug("PARAMETER "+param+" not found - use defautl value : "+val);
		}
		if (val == null) {
			val = defaultValue;
		}
		return val;
	}
	
	/**
	 * Return the list of key with this prefix
	 * @param prefix
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<String> getKeys(String prefix) {
		List<String> listKey = new ArrayList<String>();
		Iterator<String> keyIt = config.getKeys(prefix);
		while (keyIt.hasNext()) {
			listKey.add(keyIt.next());
		}
		return listKey;
	}

    public List<String> getKeysStartWith(String prefix) {
        List<String> listKey = new ArrayList<String>();
        Iterator<String> keyIt = config.getKeys();
        while (keyIt.hasNext()) {
            String k = keyIt.next();
            if (k.startsWith(prefix)) {
                listKey.add(k);
            }
        }
        return listKey;
    }
	
	/**
	 * Get a List of strings associated with the given configuration key.
     * If the key doesn't map to an existing object an empty List is returned.
	 * @param key
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<String> getList(String key) {
//		return config.getList(key);
        return Arrays.asList(config.getStringArray(key));
	}

    /**
     * Return the file path of configuration
     * @return
     */
    public String getConfigFilePath() {
        return configFilePath;
    }

    /**
     * Set the value for each key contained in the list and save the config file
     * @param listKeyValue string list with "key=value" - char '=' to separate key from value
     * @return
     */
    public boolean saveValuesKeys(List<String> listKeyValue) {
        if (listKeyValue == null) {
            log.error("List keyValue null !");
            return false;
        }
        // check the list of key-value
        Iterator<String> it = listKeyValue.iterator();
        while (it.hasNext()) {
            String keyValue = it.next();
            if (keyValue.indexOf('=') <= 0) {
                log.error("Item key value not valid, no char '=' found to delimiter key and value : keyValue="+keyValue);
                it.remove();
            }
        }
        if (listKeyValue.isEmpty()) {
            log.error("List keyValue empty !");
            return false;
        }

        // DO BACKUP of the existing config file
        //对现有配置文件进行备份
        boolean backupOK = false;
        String configFilePathBackup = configFilePath+"-backup-ts"+System.currentTimeMillis();
        try {
            FileUtils.copyFile(new File(configFilePath), new File(configFilePathBackup));
            backupOK = true;
        } catch (IOException e) {
            log.error("IOException to copy config file to backup config file fileConfigPath=" + configFilePath + " - fileConfigBackupPath=" + configFilePathBackup);
        }

        boolean bSaveOK = false;
        if (backupOK) {
            // DO CHANGE
            try{
                List<String> configData = FileUtils.readLines(new File(configFilePath));
                for (String keyValue : listKeyValue) {
                    String[] temp = keyValue.split("=");
                    if (temp.length == 2) {
                        String key = temp[0], value=temp[1];
                        for (int i = 0; i < configData.size(); i++) {
                            String data = configData.get(i);
                            if (data.startsWith(key+"=")) {
                                configData.set(i, key+"="+value);
                                break;
                            }
                        }
                    }
                }
                FileUtils.writeLines(new File(configFilePath), configData);
                bSaveOK = true;
            } catch (IOException e) {
                log.error("IOException to change config file configFile=" + configFilePath);
            }

            // ERROR IN CHANGE => USE BACKUP
            if (!bSaveOK) {
                log.error("Change failed => need to use backup");
                try {
                    FileUtils.copyFile(new File(configFilePathBackup), new File(configFilePath));
                } catch (IOException e) {
                    log.error("IOException to copy backup config file to config file configFile=" + configFilePath + " - configFileBackup=" + configFilePathBackup);
                }
            }
            // REMOVE BACKUP (no more need)
            else {
                FileUtils.deleteQuietly(new File(configFilePathBackup));
            }
        } else {
            log.error("Failed to backup !");
        }
        if (log.isDebugEnabled()) {
            log.debug("Change config file listKeyValue=" + StringTools.listToString(listKeyValue) + " - bSaveOK=" + bSaveOK + " - fileConfigPath=" + configFilePath);
        }
        return bSaveOK;
    }

    /**
     * Return the string associated to this key. If the value contains environment variables, the value is resolved
     * 返回与此键关联的字符串。 如果该值包含环境变量，则将解析该值
     * @param param
     * @param defaultValue
     * @return
     */
    public String getStringResolvEnvVariableValue(String param, String defaultValue) {
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
                // match ${ENV_VAR_NAME} ??????
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

    public boolean checkPlayerIDForParam(String param, long playerID, boolean defaultValue) {
        try {
            String value = getStringValue(param, null);
            if (value != null) {
                String[] temp = value.split(";");
                for (int i = 0; i < temp.length; i++) {
                    if (temp[i].equals(""+playerID)) {
                        return true;
                    }
                }
                return false;
            }
        } catch (Exception e) {
            log.debug("PARAMETER "+param+" not found - return "+defaultValue);
        }
        return defaultValue;
    }

    public String getVersionTxt() {
        return versionTxt;
    }
}
