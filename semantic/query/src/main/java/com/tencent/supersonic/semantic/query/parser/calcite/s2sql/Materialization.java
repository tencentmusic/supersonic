package com.tencent.supersonic.semantic.query.parser.calcite.s2sql;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Materialization {

    public enum TimePartType {
        /**
         * partition time type
         * 1 - FULL, not use partition
         * 2 - PARTITION , use  time list
         * 3 - ZIPPER, use [startDate, endDate]  range time
         */
        FULL("FULL"),
        PARTITION("PARTITION"),
        ZIPPER("ZIPPER"),
        None("");
        private String name;

        TimePartType(String name) {
            this.name = name;
        }

        public static TimePartType of(String name) {
            for (TimePartType typeEnum : TimePartType.values()) {
                if (typeEnum.name.equalsIgnoreCase(name)) {
                    return typeEnum;
                }
            }
            return TimePartType.None;
        }
    }

    private TimePartType timePartType;
    private String destinationTable;
    private String dateInfo;
    private String entities;
    private Long modelId;
    private Long dataBase;
    private Long materializationId;
    private Integer level;
    private List<MaterializationElement> dimensions = new ArrayList<>();
    private List<MaterializationElement> metrics = new ArrayList<>();


}
