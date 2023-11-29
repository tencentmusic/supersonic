package com.tencent.supersonic.chat.agent;


import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NL2SQLTool extends AgentTool {

    protected List<Long> modelIds;

}