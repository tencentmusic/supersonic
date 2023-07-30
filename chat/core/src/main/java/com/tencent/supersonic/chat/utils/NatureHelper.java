package com.tencent.supersonic.chat.utils;

import com.hankcs.hanlp.corpus.tag.Nature;
import com.hankcs.hanlp.seg.common.Term;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.mapper.DomainInfoStat;
import com.tencent.supersonic.knowledge.dictionary.DictWordType;
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

    private static boolean isDomainOrEntity(Term term, Integer domain) {
        return (DictWordType.NATURE_SPILT + domain).equals(term.nature.toString()) || term.nature.toString()
                .endsWith(DictWordType.ENTITY.getType());
    }

    public static Integer getDomainByNature(Nature nature) {
        if (nature.startsWith(DictWordType.NATURE_SPILT)) {
            String[] dimensionValues = nature.toString().split(DictWordType.NATURE_SPILT);
            if (StringUtils.isNumeric(dimensionValues[1])) {
                return Integer.valueOf(dimensionValues[1]);
            }
        }
        return 0;
    }

    public static Long getDomainId(String nature) {
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

    public static boolean isDimensionValueClassId(String nature) {
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

    public static DomainInfoStat getDomainStat(List<Term> terms) {
        return DomainInfoStat.builder()
                .domainCount(getDomainCount(terms))
                .dimensionDomainCount(getDimensionCount(terms))
                .metricDomainCount(getMetricCount(terms))
                .dimensionValueDomainCount(getDimensionValueCount(terms))
                .build();
    }


    private static long getDomainCount(List<Term> terms) {
        return terms.stream().filter(term -> isDomainOrEntity(term, getDomainByNature(term.nature))).count();
    }

    private static long getDimensionValueCount(List<Term> terms) {
        return terms.stream().filter(term -> isDimensionValueClassId(term.nature.toString())).count();
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
     * domainId -> (nature , natureCount)
     *
     * @param terms
     * @return
     */
    public static Map<Long, Map<DictWordType, Integer>> getDomainToNatureStat(List<Term> terms) {
        Map<Long, Map<DictWordType, Integer>> domainToNature = new HashMap<>();
        terms.stream().filter(
                term -> term.nature.startsWith(DictWordType.NATURE_SPILT)
        ).forEach(term -> {
            DictWordType dictWordType = DictWordType.getNatureType(String.valueOf(term.nature));
            Long domain = getDomainId(String.valueOf(term.nature));

            Map<DictWordType, Integer> natureTypeMap = new HashMap<>();
            natureTypeMap.put(dictWordType, 1);

            Map<DictWordType, Integer> original = domainToNature.get(domain);
            if (Objects.isNull(original)) {
                domainToNature.put(domain, natureTypeMap);
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
        return domainToNature;
    }

    public static List<Long> selectPossibleDomains(List<Term> terms) {
        Map<Long, Map<DictWordType, Integer>> domainToNatureStat = getDomainToNatureStat(terms);
        Integer maxDomainTypeSize = domainToNatureStat.entrySet().stream()
                .max(Comparator.comparingInt(o -> o.getValue().size())).map(entry -> entry.getValue().size())
                .orElse(null);
        if (Objects.isNull(maxDomainTypeSize) || maxDomainTypeSize == 0) {
            return new ArrayList<>();
        }
        return domainToNatureStat.entrySet().stream().filter(entry -> entry.getValue().size() == maxDomainTypeSize)
                .map(entry -> entry.getKey()).collect(Collectors.toList());
    }
}
