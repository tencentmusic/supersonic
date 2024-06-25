package com.tencent.supersonic.headless.server.persistence.dataobject;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@Getter
@AllArgsConstructor
public class StatisticsDO {
    /**
     * questionId
     */
    private Long questionId;

    /**
     * chatId
     */
    private Long chatId;

    /**
     * createTime
     */
    private Date createTime;

    /**
     * queryText
     */
    private String queryText;

    /**
     * userName
     */
    private String userName;


    /**
     * interface
     */
    private String interfaceName;

    /**
     * cost
     */
    private Integer cost;

    private Integer type;
}
