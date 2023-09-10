package com.tencent.supersonic.chat.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.tencent.supersonic.chat.api.pojo.request.ItemNameVisibilityInfo;
import com.tencent.supersonic.chat.service.ConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class VisibilityListener implements ApplicationListener<VisibilityEvent> {

    @Autowired
    @Qualifier("searchCaffeineCache")
    private Cache<Long, Object> caffeineCache;

    @Autowired
    private ConfigService configService;

    @Override
    public void onApplicationEvent(VisibilityEvent event) {
        log.info("visibility has changed,so update cache!");
        ItemNameVisibilityInfo itemNameVisibility = configService.getItemNameVisibility(event.getChatConfig());
        log.info("itemNameVisibility :{}", itemNameVisibility);
        caffeineCache.put(event.getChatConfig().getModelId(), itemNameVisibility);
    }
}
