package com.tencent.supersonic.chat.domain.utils;

import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.common.nlp.NatureType;

/***
 * nature type to schemaType converter
 */
public class NatureConverter {

    public static SchemaElementType convertTo(String nature) {
        NatureType natureType = NatureType.getNatureType(nature);
        SchemaElementType result = null;
        switch (natureType) {
            case METRIC:
                result = SchemaElementType.METRIC;
                break;
            case DIMENSION:
                result = SchemaElementType.DIMENSION;
                break;
            case ENTITY:
                result = SchemaElementType.ENTITY;
                break;
            case DOMAIN:
                result = SchemaElementType.DOMAIN;
                break;
            case VALUE:
                result = SchemaElementType.VALUE;
                break;
            default:
                break;
        }
        return result;
    }
}
