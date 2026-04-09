package com.tencent.supersonic.feishu.server.service;

import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.feishu.api.config.FeishuProperties;
import com.tencent.supersonic.feishu.server.persistence.dataobject.FeishuUserMappingDO;
import com.tencent.supersonic.feishu.server.persistence.mapper.FeishuQuerySessionMapper;
import com.tencent.supersonic.feishu.server.persistence.mapper.FeishuUserMappingMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeishuUserMappingServiceTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createMappingShouldUseCurrentTenantWhenMissing() {
        TenantContext.setTenantId(9L);
        MapperState state = new MapperState();
        FeishuUserMappingService service = new FeishuUserMappingService(newUserMappingMapper(state),
                newQuerySessionMapper(state), null, new FeishuProperties(), null);

        FeishuUserMappingDO mapping = new FeishuUserMappingDO();
        service.createMapping(mapping);

        assertEquals(9L, state.lastInserted.getTenantId());
    }

    @Test
    void completeBindingShouldRewriteTenantId() {
        MapperState state = new MapperState();
        FeishuUserMappingDO existing = new FeishuUserMappingDO();
        existing.setId(12L);
        existing.setTenantId(1L);
        state.recordsById.put(12L, existing);

        FeishuUserMappingService service = new FeishuUserMappingService(newUserMappingMapper(state),
                newQuerySessionMapper(state), null, new FeishuProperties(), null);

        service.completeBinding(12L, 34L, 8L);

        assertEquals(34L, state.lastUpdated.getS2UserId());
        assertEquals(8L, state.lastUpdated.getTenantId());
    }

    @Test
    void listSessionsShouldReturnEmptyWhenTenantHasNoMappedOpenIds() {
        TenantContext.setTenantId(11L);
        MapperState state = new MapperState();
        FeishuUserMappingService service = new FeishuUserMappingService(newUserMappingMapper(state),
                newQuerySessionMapper(state), null, new FeishuProperties(), null);

        var result = service.listSessions(1, 20, null, null, null);

        assertTrue(result.getRecords().isEmpty());
        assertEquals(0, result.getTotal());
        assertEquals(0, state.querySessionSelectPageCalls);
    }

    private FeishuUserMappingMapper newUserMappingMapper(MapperState state) {
        return (FeishuUserMappingMapper) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[] {FeishuUserMappingMapper.class}, (proxy, method, args) -> {
                    String name = method.getName();
                    if ("insert".equals(name)) {
                        state.lastInserted = (FeishuUserMappingDO) args[0];
                        return 1;
                    }
                    if ("selectById".equals(name)) {
                        return state.recordsById.get(args[0]);
                    }
                    if ("selectList".equals(name)) {
                        return List.of();
                    }
                    if ("updateById".equals(name)) {
                        state.lastUpdated = (FeishuUserMappingDO) args[0];
                        return 1;
                    }
                    return null;
                });
    }

    private FeishuQuerySessionMapper newQuerySessionMapper(MapperState state) {
        return (FeishuQuerySessionMapper) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[] {FeishuQuerySessionMapper.class}, (proxy, method, args) -> {
                    if ("selectPage".equals(method.getName())) {
                        state.querySessionSelectPageCalls++;
                    }
                    return null;
                });
    }

    private static class MapperState {
        private final Map<Long, FeishuUserMappingDO> recordsById = new HashMap<>();
        private FeishuUserMappingDO lastInserted;
        private FeishuUserMappingDO lastUpdated;
        private int querySessionSelectPageCalls;
    }
}
