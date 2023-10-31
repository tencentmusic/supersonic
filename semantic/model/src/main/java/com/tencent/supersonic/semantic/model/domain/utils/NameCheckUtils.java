package com.tencent.supersonic.semantic.model.domain.utils;

import java.util.regex.Pattern;

public class NameCheckUtils {

    public static boolean containsSpecialCharacters(String str) {
        if (str == null) {
            return false;
        }
        String regex = "^[^a-zA-Z\\u4E00-\\u9FA5_\\d].*|^\\d.*";
        return Pattern.compile(regex).matcher(str).find();
    }

}