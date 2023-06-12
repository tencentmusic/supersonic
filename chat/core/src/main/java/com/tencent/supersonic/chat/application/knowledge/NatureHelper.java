package com.tencent.supersonic.chat.application.knowledge;

import com.hankcs.hanlp.corpus.tag.Nature;
import com.hankcs.hanlp.seg.common.Term;
import com.tencent.supersonic.chat.domain.pojo.search.DomainInfoStat;
import com.tencent.supersonic.common.nlp.NatureType;
import com.tencent.supersonic.knowledge.application.online.BaseWordNature;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * nature parse helper
 */
public class NatureHelper {


    private static boolean isDomainOrEntity(Term term, Integer domain) {
        return (NatureType.NATURE_SPILT + domain).equals(term.nature.toString()) || term.nature.toString()
                .endsWith(NatureType.ENTITY.getType());
    }

    public static Integer getDomainByNature(Nature nature) {
        if (nature.startsWith(NatureType.NATURE_SPILT)) {
            String[] dimensionValues = nature.toString().split(NatureType.NATURE_SPILT);
            if (StringUtils.isNumeric(dimensionValues[1])) {
                return Integer.valueOf(dimensionValues[1]);
            }
        }
        return 0;
    }

    public static Integer getDomain(String nature) {
        String[] split = nature.split(NatureType.NATURE_SPILT);
        return Integer.valueOf(split[1]);
    }

    public static boolean isDimensionValueClassId(String nature) {
        if (StringUtils.isEmpty(nature)) {
            return false;
        }
        if (!nature.startsWith(NatureType.NATURE_SPILT)) {
            return false;
        }
        String[] split = nature.split(NatureType.NATURE_SPILT);
        if (split.length <= 1) {
            return false;
        }
        return !nature.endsWith(NatureType.METRIC.getType()) && !nature.endsWith(NatureType.DIMENSION.getType())
                && StringUtils.isNumeric(split[1]);
    }

    public static DomainInfoStat getDomainStat(List<Term> terms) {
        DomainInfoStat stat = new DomainInfoStat();
        stat.setDimensionDomainCount(getDimensionCount(terms));
        stat.setMetricDomainCount(getMetricCount(terms));
        stat.setDomainCount(getDomainCount(terms));
        stat.setDimensionValueDomainCount(getDimensionValueCount(terms));
        return stat;
    }


    private static long getDomainCount(List<Term> terms) {
        return terms.stream().filter(term -> isDomainOrEntity(term, getDomainByNature(term.nature))).count();
    }

    private static long getDimensionValueCount(List<Term> terms) {
        return terms.stream().filter(term -> isDimensionValueClassId(term.nature.toString())).count();
    }

    private static long getDimensionCount(List<Term> terms) {
        return terms.stream().filter(term -> term.nature.startsWith(NatureType.NATURE_SPILT) && term.nature.toString()
                .endsWith(NatureType.DIMENSION.getType())).count();
    }

    private static long getMetricCount(List<Term> terms) {
        return terms.stream().filter(term -> term.nature.startsWith(NatureType.NATURE_SPILT) && term.nature.toString()
                .endsWith(NatureType.METRIC.getType())).count();
    }

    /**
     * Get the number of types of class parts of speech
     * classId -> (nature , natureCount)
     *
     * @param terms
     * @return
     */
    public static Map<Integer, Map<NatureType, Integer>> getDomainToNatureStat(List<Term> terms) {
        Map<Integer, Map<NatureType, Integer>> domainToNature = new HashMap<>();
        terms.stream().filter(
                term -> term.nature.startsWith(NatureType.NATURE_SPILT)
        ).forEach(term -> {
            NatureType natureType = NatureType.getNatureType(String.valueOf(term.nature));
            Integer domain = BaseWordNature.getDomain(String.valueOf(term.nature));

            Map<NatureType, Integer> natureTypeMap = new HashMap<>();
            natureTypeMap.put(natureType, 1);

            Map<NatureType, Integer> original = domainToNature.get(domain);
            if (Objects.isNull(original)) {
                domainToNature.put(domain, natureTypeMap);
            } else {
                Integer count = original.get(natureType);
                if (Objects.isNull(count)) {
                    count = 1;
                } else {
                    count = count + 1;
                }
                original.put(natureType, count);
            }
        });
        return domainToNature;
    }

    public static List<Integer> selectPossibleDomains(List<Term> terms) {
        Map<Integer, Map<NatureType, Integer>> domainToNatureStat = getDomainToNatureStat(terms);
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
