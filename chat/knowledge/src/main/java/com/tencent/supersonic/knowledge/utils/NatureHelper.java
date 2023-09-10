package com.tencent.supersonic.knowledge.utils;

import com.hankcs.hanlp.corpus.tag.Nature;
import com.hankcs.hanlp.seg.common.Term;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.common.pojo.enums.DictWordType;
import com.tencent.supersonic.knowledge.dictionary.ModelInfoStat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * nature parse helper
 */
@Slf4j
public class NatureHelper {

    public static SchemaElementType convertToElementType(String nature) {
        DictWordType dictWordType = DictWordType.getNatureType(nature);
        SchemaElementType result = null;
        switch (dictWordType) {
            case METRIC:
                result = SchemaElementType.METRIC;
                break;
            case DIMENSION:
                result = SchemaElementType.DIMENSION;
                break;
            case ENTITY:
                result = SchemaElementType.ENTITY;
                break;
            case MODEL:
                result = SchemaElementType.MODEL;
                break;
            case VALUE:
                result = SchemaElementType.VALUE;
                break;
            default:
                break;
        }
        return result;
    }

    private static boolean isModelOrEntity(Term term, Integer model) {
        return (DictWordType.NATURE_SPILT + model).equals(term.nature.toString()) || term.nature.toString()
                .endsWith(DictWordType.ENTITY.getType());
    }

    public static Integer getModelByNature(Nature nature) {
        if (nature.startsWith(DictWordType.NATURE_SPILT)) {
            String[] dimensionValues = nature.toString().split(DictWordType.NATURE_SPILT);
            if (StringUtils.isNumeric(dimensionValues[1])) {
                return Integer.valueOf(dimensionValues[1]);
            }
        }
        return 0;
    }

    public static Long getModelId(String nature) {
        try {
            String[] split = nature.split(DictWordType.NATURE_SPILT);
            if (split.length <= 1) {
                return null;
            }
            return Long.valueOf(split[1]);
        } catch (NumberFormatException e) {
            log.error("", e);
        }
        return null;
    }

    public static boolean isDimensionValueModelId(String nature) {
        if (StringUtils.isEmpty(nature)) {
            return false;
        }
        if (!nature.startsWith(DictWordType.NATURE_SPILT)) {
            return false;
        }
        String[] split = nature.split(DictWordType.NATURE_SPILT);
        if (split.length <= 1) {
            return false;
        }
        return !nature.endsWith(DictWordType.METRIC.getType()) && !nature.endsWith(DictWordType.DIMENSION.getType())
                && StringUtils.isNumeric(split[1]);
    }

    public static ModelInfoStat getModelStat(List<Term> terms) {
        return ModelInfoStat.builder()
                .modelCount(getModelCount(terms))
                .dimensionModelCount(getDimensionCount(terms))
                .metricModelCount(getMetricCount(terms))
                .dimensionValueModelCount(getDimensionValueCount(terms))
                .build();
    }


    private static long getModelCount(List<Term> terms) {
        return terms.stream().filter(term -> isModelOrEntity(term, getModelByNature(term.nature))).count();
    }

    private static long getDimensionValueCount(List<Term> terms) {
        return terms.stream().filter(term -> isDimensionValueModelId(term.nature.toString())).count();
    }

    private static long getDimensionCount(List<Term> terms) {
        return terms.stream().filter(term -> term.nature.startsWith(DictWordType.NATURE_SPILT) && term.nature.toString()
                .endsWith(DictWordType.DIMENSION.getType())).count();
    }

    private static long getMetricCount(List<Term> terms) {
        return terms.stream().filter(term -> term.nature.startsWith(DictWordType.NATURE_SPILT) && term.nature.toString()
                .endsWith(DictWordType.METRIC.getType())).count();
    }

    /**
     * Get the number of types of class parts of speech
     * modelId -> (nature , natureCount)
     *
     * @param terms
     * @return
     */
    public static Map<Long, Map<DictWordType, Integer>> getModelToNatureStat(List<Term> terms) {
        Map<Long, Map<DictWordType, Integer>> modelToNature = new HashMap<>();
        terms.stream().filter(
                term -> term.nature.startsWith(DictWordType.NATURE_SPILT)
        ).forEach(term -> {
            DictWordType dictWordType = DictWordType.getNatureType(String.valueOf(term.nature));
            Long model = getModelId(String.valueOf(term.nature));

            Map<DictWordType, Integer> natureTypeMap = new HashMap<>();
            natureTypeMap.put(dictWordType, 1);

            Map<DictWordType, Integer> original = modelToNature.get(model);
            if (Objects.isNull(original)) {
                modelToNature.put(model, natureTypeMap);
            } else {
                Integer count = original.get(dictWordType);
                if (Objects.isNull(count)) {
                    count = 1;
                } else {
                    count = count + 1;
                }
                original.put(dictWordType, count);
            }
        });
        return modelToNature;
    }

    public static List<Long> selectPossibleModels(List<Term> terms) {
        Map<Long, Map<DictWordType, Integer>> modelToNatureStat = getModelToNatureStat(terms);
        Integer maxModelTypeSize = modelToNatureStat.entrySet().stream()
                .max(Comparator.comparingInt(o -> o.getValue().size())).map(entry -> entry.getValue().size())
                .orElse(null);
        if (Objects.isNull(maxModelTypeSize) || maxModelTypeSize == 0) {
            return new ArrayList<>();
        }
        return modelToNatureStat.entrySet().stream().filter(entry -> entry.getValue().size() == maxModelTypeSize)
                .map(entry -> entry.getKey()).collect(Collectors.toList());
    }
}
