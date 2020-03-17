package com.funbridge.server.engine;

import com.funbridge.server.common.Constantes;

import java.util.HashMap;
import java.util.Map;

public class ArgineTextElement {
	public String name;
	public Map<String, String> mapTextLang = new HashMap<>();

	public String getTextForLang(String lang) {
		lang = lang.replaceAll("-", "_");
		String value = "";
		if (lang != null) {
			value = mapTextLang.get(lang);
		}
		if (lang != null && (value == null || value.length() == 0)) {
			if(lang.contains("_")){
				lang = lang.substring(0, lang.lastIndexOf("_"));
				value = mapTextLang.get(lang);
			}
		}
		if (value == null || value.length() == 0) {
			value = mapTextLang.get(Constantes.PLAYER_LANG_EN);
		}
		if (value != null && value.equals("NOP")) {
		    return "";
        }
		return value;
	}

	public void addTextLang(String lang, String text) {
		mapTextLang.put(lang, text);
	}
}
