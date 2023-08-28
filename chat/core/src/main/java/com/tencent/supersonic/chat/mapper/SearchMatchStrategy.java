package com.tencent.supersonic.chat.mapper;

import com.google.common.collect.Lists;
import com.hankcs.hanlp.seg.common.Term;
import com.tencent.supersonic.knowledge.dictionary.DictWordType;
import com.tencent.supersonic.knowledge.dictionary.MapResult;
import com.tencent.supersonic.knowledge.service.SearchService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * match strategy implement
 */
@Service
public class SearchMatchStrategy implements MatchStrategy {

    private static final int SEARCH_SIZE = 3;

    @Override
    public Map<MatchText, List<MapResult>> match(String text, List<Term> originals,
            Long detectModelId) {

        Map<Integer, Integer> regOffsetToLength = originals.stream()
                .filter(entry -> !entry.nature.toString().startsWith(DictWordType.NATURE_SPILT))
                .collect(Collectors.toMap(Term::getOffset, value -> value.word.length(),
                        (value1, value2) -> value2));

        List<Integer> detectIndexList = Lists.newArrayList();

        for (Integer index = 0; index < text.length(); ) {

            if (index < text.length()) {
                detectIndexList.add(index);
            }
            Integer regLength = regOffsetToLength.get(index);
            if (Objects.nonNull(regLength)) {
                index = index + regLength;
            } else {
                index++;
            }
        }
        Map<MatchText, List<MapResult>> regTextMap = new ConcurrentHashMap<>();
        detectIndexList.stream().parallel().forEach(detectIndex -> {
                    String regText = text.substring(0, detectIndex);
                    String detectSegment = text.substring(detectIndex);

                    if (StringUtils.isNotEmpty(detectSegment)) {
                        List<MapResult> mapResults = SearchService.prefixSearch(detectSegment);
                        List<MapResult> suffixMapResults = SearchService.suffixSearch(detectSegment, SEARCH_SIZE);
                        mapResults.addAll(suffixMapResults);
                        // remove entity name where search
                        mapResults = mapResults.stream().filter(entry -> {
                            List<String> natures = entry.getNatures().stream()
                                    .filter(nature -> !nature.endsWith(DictWordType.ENTITY.getType()))
                                    .filter(nature -> {
                                                if (Objects.isNull(detectModelId) || detectModelId <= 0) {
                                                    return true;
                                                }
                                                if (nature.startsWith(DictWordType.NATURE_SPILT + detectModelId)
                                                        && nature.startsWith(DictWordType.NATURE_SPILT)) {
                                                    return true;
                                                }
                                                return false;
                                            }
                                    ).collect(Collectors.toList());
                            if (CollectionUtils.isEmpty(natures)) {
                                return false;
                            }
                            return true;
                        }).collect(Collectors.toList());
                        MatchText matchText = MatchText.builder()
                                .regText(regText)
                                .detectSegment(detectSegment)
                                .build();
                        regTextMap.put(matchText, mapResults);
                    }
                }
        );
        return regTextMap;
    }
}