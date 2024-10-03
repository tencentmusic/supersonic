package com.tencent.supersonic.common.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
/**
 * 1.Password Field:
 *
 * <p>
 * dataType: string name: password require: true/false or any value/empty placeholder: 'Please enter
 * the relevant configuration information' value: initial value Text Input Field:
 *
 * <p>
 * 2.dataType: string require: true/false or any value/empty placeholder: 'Please enter the relevant
 * configuration information' value: initial value Long Text Input Field:
 *
 * <p>
 * 3.dataType: longText require: true/false or any value/empty placeholder: 'Please enter the
 * relevant configuration information' value: initial value Number Input Field:
 *
 * <p>
 * 4.dataType: number require: true/false or any value/empty placeholder: 'Please enter the relevant
 * configuration information' value: initial value Switch Component:
 *
 * <p>
 * 5.dataType: bool require: true/false or any value/empty value: initial value Select Dropdown
 * Component:
 *
 * <p>
 * 6.dataType: list candidateValues: ["OPEN_AI", "OLLAMA"] or [{label: 'Model Name 1', value:
 * 'OPEN_AI'}, {label: 'Model Name 2', value: 'OLLAMA'}] require: true/false or any value/empty
 * placeholder: 'Please enter the relevant configuration information' value: initial value
 */
public class Parameter {
    private String name;
    private String defaultValue;
    private String comment;
    private String description;
    private String dataType;
    private String module;
    private String value;
    private List<String> candidateValues;
    private List<Dependency> dependencies;

    public Parameter(String name, String defaultValue, String comment, String description,
            String dataType, String module) {
        this(name, defaultValue, comment, description, dataType, module, null, null);
    }

    public Parameter(String name, String defaultValue, String comment, String description,
            String dataType, String module, List<String> candidateValues) {
        this(name, defaultValue, comment, description, dataType, module, candidateValues, null);
    }

    public Parameter(String name, String defaultValue, String comment, String description,
            String dataType, String module, List<String> candidateValues,
            List<Dependency> dependencies) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.comment = comment;
        this.description = description;
        this.dataType = dataType;
        this.module = module;
        this.candidateValues = candidateValues;
        this.dependencies = dependencies;
    }

    public String getValue() {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Data
    public static class Dependency {
        private String name;
        private Show show;
        private Map<String, String> setDefaultValue;

        @Data
        public static class Show {
            private List<String> includesValue;
        }
    }
}
