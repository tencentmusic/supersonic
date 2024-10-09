package com.tencent.supersonic.headless.chat.utils;

import com.hankcs.hanlp.algorithm.EditDistance;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Data
@Service
@Slf4j
public class EditDistanceUtils {

    /**
     * * get similarity
     *
     * @param detectSegment
     * @param matchName
     * @return
     */
    public static double getSimilarity(String detectSegment, String matchName) {
        String detectSegmentLower = detectSegment == null ? null : detectSegment.toLowerCase();
        String matchNameLower = matchName == null ? null : matchName.toLowerCase();
        return 1 - (double) EditDistance.compute(detectSegmentLower, matchNameLower)
                / Math.max(matchName.length(), detectSegment.length());
    }
}
