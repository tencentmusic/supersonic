package com.tencent.supersonic.chat.server.userplugin.base.utils;

import com.hankcs.hanlp.seg.common.Term;
import com.tencent.supersonic.headless.chat.knowledge.helper.HanlpHelper;

import java.util.List;

public class SegmentUtils {
    public static String getTerm(String name, String prefix) {
        List<Term> terms = HanlpHelper.getSegment().seg(name);
        Term term =
                terms.stream().filter(e -> e.getNature().startsWith(prefix)).findAny().orElse(null);
        if (term == null) {
            return "";
        }
        return term.getNature().toString().substring(prefix.length());
    }
}
