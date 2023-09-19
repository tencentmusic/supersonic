package com.tencent.supersonic.chat.api.pojo.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import java.util.List;

import static java.time.LocalDate.now;

@ToString
@Data
@NoArgsConstructor
public class DictLatestTaskReq {

    @NotNull
    private Long modelId;

    private List<Long> dimIds;

    private String createdAt = now().plusDays(-4).toString();
}