package com.tencent.supersonic.headless.server.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NameCheckUtils {

    public static final String forbiddenCharactersRegex = "[（）%#()]";
    public static final String identifierRegex = "^[_a-zA-Z0-9]+$";

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

    public static Boolean isValidIdentifier(String str) {
        if (StringUtils.isBlank(str)) {
            return false;
        }
        Pattern pattern = Pattern.compile(identifierRegex);
        Matcher matcher = pattern.matcher(str);
        return matcher.find();
    }
}
