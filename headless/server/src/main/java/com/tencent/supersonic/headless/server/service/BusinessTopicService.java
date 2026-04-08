package com.tencent.supersonic.headless.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.server.persistence.dataobject.BusinessTopicDO;
import com.tencent.supersonic.headless.server.pojo.BusinessTopicVO;

import java.util.List;

public interface BusinessTopicService {

    Page<BusinessTopicVO> listTopics(Page<BusinessTopicDO> page, Boolean enabled, User user);

    BusinessTopicVO getTopicDetail(Long id, User user);

    BusinessTopicDO createTopic(BusinessTopicDO topic, User user);

    BusinessTopicDO updateTopic(BusinessTopicDO topic, User user);

    void deleteTopic(Long id, User user);

    void addItems(Long topicId, List<String> itemTypes, List<Long> itemIds, User user);

    void removeItem(Long topicId, String itemType, Long itemId, User user);
}
