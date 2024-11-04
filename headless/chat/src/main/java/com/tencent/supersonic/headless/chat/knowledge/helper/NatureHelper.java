package com.tencent.supersonic.headless.chat.knowledge.helper;

import com.hankcs.hanlp.corpus.tag.Nature;
import com.tencent.supersonic.common.pojo.enums.DictWordType;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.response.S2Term;
import com.tencent.supersonic.headless.chat.knowledge.DataSetInfoStat;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * nature parse helper
 */
@Slf4j
public class NatureHelper {

    public static SchemaElementType convertToElementType(String nature) {
        DictWordType dictWordType = DictWordType.getNatureType(nature);
        if (Objects.isNull(dictWordType)) {
            return null;
        }
        SchemaElementType result = null;
        switch (dictWordType) {
            case METRIC:
                result = SchemaElementType.METRIC;
                break;
            case DIMENSION:
                result = SchemaElementType.DIMENSION;
                break;
            case DATASET:
                result = SchemaElementType.DATASET;
                break;
            case VALUE:
                result = SchemaElementType.VALUE;
                break;
            case TERM:
                result = SchemaElementType.TERM;
                break;
            default:
                break;
        }
        return result;
    }

    private static boolean isDataSet(S2Term term, Integer model) {
        String natureStr = term.nature.toString();
        return (DictWordType.NATURE_SPILT + model).equals(natureStr);
    }

    public static Integer getDataSetByNature(Nature nature) {
        if (!nature.startsWith(DictWordType.NATURE_SPILT)) {
            return 0;
        }
        String[] dimensionValues = nature.toString().split(DictWordType.NATURE_SPILT);
        return StringUtils.isNumeric(dimensionValues[1]) ? Integer.valueOf(dimensionValues[1]) : 0;
    }

    public static Long getDataSetId(String nature) {
        return parseIdFromNature(nature, 1);
    }

    private static Long getModelId(String nature) {
        return parseIdFromNature(nature, 1);
    }

    private static String changeModel2DataSet(String nature, Long dataSetId) {
        try {
            String[] split = nature.split(DictWordType.NATURE_SPILT);
            if (split.length <= 1) {
                return null;
            }
            split[1] = String.valueOf(dataSetId);
            return String.join(DictWordType.NATURE_SPILT, split);
        } catch (NumberFormatException e) {
            log.error("", e);
        }
        return null;
    }

    public static List<String> changeModel2DataSet(String nature,
            Map<Long, List<Long>> modelIdToDataSetIds) {
        if (isTerm(nature)) {
            return Collections.singletonList(nature);
        }
        Long modelId = getModelId(nature);
        List<Long> dataSetIds = modelIdToDataSetIds.get(modelId);
        if (CollectionUtils.isEmpty(dataSetIds)) {
            return Collections.emptyList();
        }
        return dataSetIds.stream().map(dataSetId -> changeModel2DataSet(nature, dataSetId))
                .filter(Objects::nonNull).map(String::valueOf).collect(Collectors.toList());
    }

    public static boolean isTerm(String nature) {
        return SchemaElementType.TERM.equals(NatureHelper.convertToElementType(nature));
    }

    public static boolean isDimensionValueDataSetId(String nature) {
        return isNatureValid(nature)
                && !isNatureType(nature, DictWordType.METRIC, DictWordType.DIMENSION,
                        DictWordType.TERM)
                && StringUtils.isNumeric(nature.split(DictWordType.NATURE_SPILT)[1]);
    }

    public static DataSetInfoStat getDataSetStat(List<S2Term> terms) {
        return DataSetInfoStat.builder().dataSetCount(getDataSetCount(terms))
                .dimensionDataSetCount(getDimensionCount(terms))
                .metricDataSetCount(getMetricCount(terms))
                .dimensionValueDataSetCount(getDimensionValueCount(terms)).build();
    }

    private static long getDataSetCount(List<S2Term> terms) {
        return terms.stream().filter(term -> isDataSet(term, getDataSetByNature(term.nature)))
                .count();
    }

    private static long getDimensionValueCount(List<S2Term> terms) {
        return terms.stream().filter(term -> isDimensionValueDataSetId(term.nature.toString()))
                .count();
    }

    private static long getDimensionCount(List<S2Term> terms) {
        return terms.stream()
                .filter(term -> term.nature.startsWith(DictWordType.NATURE_SPILT)
                        && term.nature.toString().endsWith(DictWordType.DIMENSION.getType()))
                .count();
    }

    private static long getMetricCount(List<S2Term> terms) {
        return terms.stream().filter(term -> term.nature.startsWith(DictWordType.NATURE_SPILT)
                && term.nature.toString().endsWith(DictWordType.METRIC.getType())).count();
    }

    public static Map<Long, Map<DictWordType, Integer>> getDataSetToNatureStat(List<S2Term> terms) {
        Map<Long, Map<DictWordType, Integer>> modelToNature = new HashMap<>();
        terms.stream().filter(term -> term.nature.startsWith(DictWordType.NATURE_SPILT))
                .forEach(term -> {
                    DictWordType dictWordType = DictWordType.getNatureType(term.nature.toString());
                    Long model = getDataSetId(term.nature.toString());

                    modelToNature.computeIfAbsent(model, k -> new HashMap<>()).merge(dictWordType,
                            1, Integer::sum);
                });
        return modelToNature;
    }

    public static List<Long> selectPossibleDataSets(List<S2Term> terms) {
        Map<Long, Map<DictWordType, Integer>> modelToNatureStat = getDataSetToNatureStat(terms);
        return modelToNatureStat.entrySet().stream()
                .max(Comparator.comparingInt(entry -> entry.getValue().size()))
                .map(entry -> modelToNatureStat.entrySet().stream()
                        .filter(e -> e.getValue().size() == entry.getValue().size())
                        .map(Map.Entry::getKey).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    public static Long getElementID(String nature) {
        return parseIdFromNature(nature, 2);
    }

    public static Set<Long> getModelIds(Map<Long, List<Long>> modelIdToDataSetIds,
            Set<Long> detectDataSetIds) {
        if (CollectionUtils.isEmpty(detectDataSetIds)) {
            return modelIdToDataSetIds.keySet();
        }
        return modelIdToDataSetIds.entrySet().stream()
                .filter(entry -> !Collections.disjoint(entry.getValue(), detectDataSetIds))
                .map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    public static Long parseIdFromNature(String nature, int index) {
        try {
            String[] split = nature.split(DictWordType.NATURE_SPILT);
            if (split.length > index) {
                return Long.valueOf(split[index]);
            }
        } catch (NumberFormatException e) {
            log.error("Error parsing long from nature: {}", nature, e);
        }
        return null;
    }

    private static boolean isNatureValid(String nature) {
        return StringUtils.isNotEmpty(nature) && nature.startsWith(DictWordType.NATURE_SPILT);
    }

    private static boolean isNatureType(String nature, DictWordType... types) {
        for (DictWordType type : types) {
            if (nature.endsWith(type.getType())) {
                return true;
            }
        }
        return false;
    }
}
