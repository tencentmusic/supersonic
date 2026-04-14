package com.tencent.supersonic.headless.server.service.delivery;

import com.tencent.supersonic.headless.api.service.delivery.DeliveryContext;
import com.tencent.supersonic.headless.server.pojo.DeliveryType;

/**
 * Strategy interface for report delivery channels. Each channel implementation handles a specific
 * delivery type (email, webhook, Feishu, DingTalk).
 */
public interface ReportDeliveryChannel {

    /**
     * Get the delivery type this channel handles.
     */
    DeliveryType getType();

    /**
     * Deliver the report using this channel.
     *
     * @param configJson JSON configuration for the channel (recipients, URLs, etc.)
     * @param context delivery context (file location, report metadata)
     * @throws DeliveryException if delivery fails
     */
    void deliver(String configJson, DeliveryContext context) throws DeliveryException;

    /**
     * Validate the channel configuration.
     *
     * @param configJson JSON configuration to validate
     * @return true if valid
     * @throws IllegalArgumentException if invalid with details
     */
    boolean validateConfig(String configJson);
}
