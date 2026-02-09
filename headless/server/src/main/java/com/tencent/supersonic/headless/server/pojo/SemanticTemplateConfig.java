package com.tencent.supersonic.headless.server.pojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class SemanticTemplateConfig {

    private DomainConfig domain;

    private List<ModelConfig> models = new ArrayList<>();

    private List<ModelRelationConfig> modelRelations = new ArrayList<>();

    private DataSetConfig dataSet;

    private AgentConfig agent;

    private List<TermConfig> terms = new ArrayList<>();

    private List<PluginConfig> plugins = new ArrayList<>();

    private List<ConfigParam> configParams = new ArrayList<>();

    private List<ExemplarConfig> exemplars = new ArrayList<>();

    @Data
    public static class DomainConfig {
        private String name;
        private String bizName;
        private String description;
        private List<String> viewers = new ArrayList<>();
        private List<String> viewOrgs = new ArrayList<>();
        private List<String> admins = new ArrayList<>();
        private List<String> adminOrgs = new ArrayList<>();
        private Integer isOpen = 0;
    }

    @Data
    public static class ModelConfig {
        private String name;
        private String bizName;
        private String description;
        private String tableName;
        private String sqlQuery;
        private List<String> viewers = new ArrayList<>();
        private List<String> viewOrgs = new ArrayList<>();
        private List<String> admins = new ArrayList<>();
        private List<String> adminOrgs = new ArrayList<>();
        private List<IdentifyConfig> identifiers = new ArrayList<>();
        private List<DimensionConfig> dimensions = new ArrayList<>();
        private List<MeasureConfig> measures = new ArrayList<>();
    }

    @Data
    public static class IdentifyConfig {
        private String name;
        private String bizName;
        private String fieldName;
        private String type; // primary, foreign
    }

    @Data
    public static class DimensionConfig {
        private String name;
        private String bizName;
        private String fieldName;
        private String type; // categorical, time, partition_time
        private String expr;
        private String dateFormat;
        private String alias;
        private Boolean enableDictValue = false;
    }

    @Data
    public static class MeasureConfig {
        private String name;
        private String bizName;
        private String fieldName;
        private String aggOperator; // SUM, COUNT, MAX, MIN, AVG, etc.
        private String expr;
        private String constraint;
        private Boolean createMetric = true;
    }

    @Data
    public static class ModelRelationConfig {
        private String fromModelBizName;
        private String toModelBizName;
        private String joinType; // left join, inner join
        private List<JoinCondition> joinConditions = new ArrayList<>();
    }

    @Data
    public static class JoinCondition {
        private String leftField;
        private String rightField;
        private String operator; // =, >, <, etc.
    }

    @Data
    public static class DataSetConfig {
        private String name;
        private String bizName;
        private String description;
        private List<String> admins = new ArrayList<>();
        private List<String> adminOrgs = new ArrayList<>();
        private List<String> viewers = new ArrayList<>();
        private List<String> viewOrgs = new ArrayList<>();
        private Integer isOpen = 0;
        private QueryConfigTemplate queryConfig;
    }

    @Data
    public static class QueryConfigTemplate {
        private TimeDefaultConfig timeDefaultConfig;
        private AggregateDefaultConfig aggregateDefaultConfig;
        private DetailDefaultConfig detailDefaultConfig;
    }

    @Data
    public static class TimeDefaultConfig {
        private Integer unit; // Calendar.DAY, etc.
        private Integer period;
        private String timeMode; // RECENT, LAST
    }

    @Data
    public static class AggregateDefaultConfig {
        // Aggregate query defaults
    }

    @Data
    public static class DetailDefaultConfig {
        // Detail query defaults
    }

    @Data
    public static class AgentConfig {
        private String name;
        private String description;
        private Boolean enableSearch = true;
        private List<String> examples = new ArrayList<>();
        private List<String> admins = new ArrayList<>();
        private List<String> viewers = new ArrayList<>();
        /**
         * Whether to auto-create the REPORT_SCHEDULE plugin and attach it to the agent. Defaults to
         * true for backward compatibility.
         */
        private Boolean enableReportSchedulePlugin = true;
        /**
         * Override ChatApp enable/disable per APP_KEY. Key is the ChatApp APP_KEY (e.g.
         * "PLAIN_TEXT", "S2SQL_SC"), value is whether to enable it. Apps not in the map retain
         * their defaults.
         */
        private Map<String, Boolean> chatAppOverrides = new HashMap<>();
    }

    @Data
    public static class TermConfig {
        private String name;
        private String description;
        private List<String> alias = new ArrayList<>();
    }

    @Data
    public static class PluginConfig {
        private String type;
        private String name;
        private String description;
        private String pattern;
        private List<String> examples;
        private List<Long> dataSetIds;
        private Object config;
    }

    @Data
    public static class ConfigParam {
        private String key;
        private String name;
        private String type; // DATABASE, TABLE, FIELD, TEXT
        private String defaultValue;
        private boolean required;
        private String description;
    }

    @Data
    public static class ExemplarConfig {
        private String question;
        private String sql;
    }
}
