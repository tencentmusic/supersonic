package com.tencent.supersonic.chat.domain.pojo.chat;

import java.io.Serializable;
import java.util.List;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class RecommendResponse {

    private List<Item> dimensions;
    private List<Item> metrics;


    public static class Item implements Serializable {

        private Integer domain;
        private String name;
        private String bizName;

        public Integer getDomain() {
            return domain;
        }

        public void setDomain(Integer domain) {
            this.domain = domain;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getBizName() {
            return bizName;
        }

        public void setBizName(String bizName) {
            this.bizName = bizName;
        }
    }
}
