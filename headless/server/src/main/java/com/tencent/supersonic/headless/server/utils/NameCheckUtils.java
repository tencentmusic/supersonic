package com.tencent.supersonic.headless.server.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NameCheckUtils {
    public static final String forbiddenCharactersRegex = "[（）%#()]";

    public static String findForbiddenCharacters(String str) {
        if (StringUtils.isBlank(str)) {
            return "";
        }
        Pattern pattern = Pattern.compile(forbiddenCharactersRegex);
        Matcher matcher = pattern.matcher(str);

        StringBuilder foundCharacters = new StringBuilder();
        while (matcher.find()) {
            foundCharacters.append(matcher.group()).append(" ");
        }
        return foundCharacters.toString().trim();
    }

}