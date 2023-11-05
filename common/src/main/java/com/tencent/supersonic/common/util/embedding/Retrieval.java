package com.tencent.supersonic.common.util.embedding;

import com.tencent.supersonic.common.pojo.enums.DictWordType;
import lombok.Data;

import java.util.Map;
import org.apache.commons.lang3.StringUtils;

@Data
public class Retrieval {

    protected String id;

    protected double distance;

    protected String query;

    protected Map<String, String> metadata;


    public static Long getLongId(String id) {
        if (StringUtils.isBlank(id)) {
            return null;
        }
        String[] split = id.split(DictWordType.NATURE_SPILT);
        return Long.parseLong(split[0]);
    }
}
