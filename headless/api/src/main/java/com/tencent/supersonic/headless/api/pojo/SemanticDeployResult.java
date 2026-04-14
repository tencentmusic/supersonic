package com.tencent.supersonic.headless.api.pojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class SemanticDeployResult {

    private Long domainId;

    private String domainName;

    private List<CreatedModel> models = new ArrayList<>();

    private List<CreatedMetric> metrics = new ArrayList<>();

    private List<CreatedDimension> dimensions = new ArrayList<>();

    private Long dataSetId;

    private String dataSetName;

    /**
     * Agent configuration for creating Agent through chat module. Agent is not created by headless
     * module to keep modules independent.
     */
    private AgentConfigResult agentConfig;

    private List<CreatedTerm> terms = new ArrayList<>();

    /**
     * Agent configuration that can be used to create an Agent through the chat module. This allows
     * the headless module to remain independent from the chat module.
     */
    @Data
    public static class AgentConfigResult {
        private String name;
        private String description;
        private Boolean enableSearch;
        private List<String> examples;
        private Long dataSetId;
        private String dataSetName;
        /** Agent ID if auto-created, null if creation failed or skipped */
        private Integer agentId;
        /** ChatApp enable/disable overrides keyed by APP_KEY */
        private Map<String, Boolean> chatAppOverrides;
    }

    @Data
    public static class CreatedModel {
        private Long id;
        private String name;
        private String bizName;
    }

    @Data
    public static class CreatedMetric {
        private Long id;
        private String name;
        private String bizName;
        private Long modelId;
    }

    @Data
    public static class CreatedDimension {
        private Long id;
        private String name;
        private String bizName;
        private Long modelId;
    }

    @Data
    public static class CreatedTerm {
        private Long id;
        private String name;
    }
}
