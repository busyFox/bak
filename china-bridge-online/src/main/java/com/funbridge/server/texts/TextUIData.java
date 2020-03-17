package com.funbridge.server.texts;

import com.funbridge.server.common.Constantes;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by pserent on 06/08/2014.
 */
public class TextUIData {
    public String name;
    public Map<String, String> mapTextLang = new HashMap<>();

    public String toString() {
        return "name="+name;
    }

    public String getText(String lang) {
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
        return value;
    }

    public void addTextLang(String lang, String text) {
        mapTextLang.put(lang, text);
    }
}
