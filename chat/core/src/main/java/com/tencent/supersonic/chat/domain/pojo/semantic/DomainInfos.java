package com.tencent.supersonic.chat.domain.pojo.semantic;

import com.tencent.supersonic.common.nlp.ItemDO;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;

/**
 * DomainInfos
 */
@Data
public class DomainInfos implements Serializable {

    private List<ItemDO> domains = new ArrayList<>();
    private List<ItemDO> dimensions = new ArrayList<>();
    private List<ItemDO> metrics = new ArrayList<>();
    private List<ItemDO> entities = new ArrayList<>();

    public Map<Integer, String> getDomainToName() {
        if (CollectionUtils.isEmpty(domains)) {
            return new HashMap();
        }
        return domains.stream().collect(
                Collectors.toMap(ItemDO::getDomain, ItemDO::getName, (value1, value2) -> value2)
        );
    }
}
