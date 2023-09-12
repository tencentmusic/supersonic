package com.tencent.supersonic.chat.plugin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PluginRecallResult {

    private Plugin plugin;

    private Set<Long> modelIds;

    private double score;

    private double distance;

}
