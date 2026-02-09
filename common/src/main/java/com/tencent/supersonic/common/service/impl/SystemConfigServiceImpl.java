package com.tencent.supersonic.common.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.supersonic.common.config.SystemConfig;
import com.tencent.supersonic.common.config.TenantConfig;
import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.persistence.dataobject.SystemConfigDO;
import com.tencent.supersonic.common.persistence.mapper.SystemConfigMapper;
import com.tencent.supersonic.common.pojo.Parameter;
import com.tencent.supersonic.common.service.SystemConfigService;
import com.tencent.supersonic.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfigDO>
        implements SystemConfigService {

    private final TenantConfig tenantConfig;

    private final Environment environment;

    // Cache field to store the system configuration per tenant
    private Map<Long, SystemConfig> tenantConfigCache = new ConcurrentHashMap<>();

    @Override
    public SystemConfig getSystemConfig() {
        Long tenantId = TenantContext.getTenantIdOrDefault(tenantConfig.getDefaultTenantId());
        SystemConfig cachedConfig = tenantConfigCache.get(tenantId);
        if (cachedConfig != null) {
            return cachedConfig;
        }
        SystemConfig systemConfigDb = getSystemConfigFromDB(tenantId);
        tenantConfigCache.put(tenantId, systemConfigDb);
        return systemConfigDb;
    }

    private SystemConfig getSystemConfigFromDB(Long tenantId) {
        List<SystemConfigDO> list =
                this.lambdaQuery().eq(SystemConfigDO::getTenantId, tenantId).list();
        if (CollectionUtils.isEmpty(list)) {
            SystemConfig systemConfig = new SystemConfig();
            systemConfig.setTenantId(tenantId);
            systemConfig.init();
            // use system property to initialize system parameter
            systemConfig.getParameters().forEach(p -> {
                if (environment.containsProperty(p.getName())) {
                    p.setValue(environment.getProperty(p.getName()));
                }
            });
            save(systemConfig);
            return systemConfig;
        }

        return convert(list.getFirst());
    }

    @Override
    public void save(SystemConfig sysConfig) {
        Long tenantId = sysConfig.getTenantId();
        if (tenantId == null) {
            tenantId = TenantContext.getTenantIdOrDefault(tenantConfig.getDefaultTenantId());
            sysConfig.setTenantId(tenantId);
        }
        SystemConfigDO systemConfigDO = convert(sysConfig);
        saveOrUpdate(systemConfigDO);
        tenantConfigCache.put(tenantId, sysConfig);
    }

    /**
     * Clears the cache for a specific tenant.
     *
     * @param tenantId the tenant ID to clear cache for
     */
    public void clearCache(Long tenantId) {
        if (tenantId != null) {
            tenantConfigCache.remove(tenantId);
        }
    }

    /**
     * Clears all tenant caches.
     */
    public void clearAllCache() {
        tenantConfigCache.clear();
    }

    private SystemConfig convert(SystemConfigDO systemConfigDO) {
        SystemConfig sysParameter = new SystemConfig();
        sysParameter.setId(systemConfigDO.getId());
        sysParameter.setTenantId(systemConfigDO.getTenantId());
        List<Parameter> parameters = JsonUtil.toObject(systemConfigDO.getParameters(),
                new TypeReference<List<Parameter>>() {});
        sysParameter.setParameters(parameters);
        sysParameter.setAdminList(systemConfigDO.getAdmin());
        return sysParameter;
    }

    private SystemConfigDO convert(SystemConfig sysParameter) {
        SystemConfigDO sysParameterDO = new SystemConfigDO();
        sysParameterDO.setId(sysParameter.getId());
        sysParameterDO.setTenantId(sysParameter.getTenantId());
        sysParameterDO.setParameters(JSONObject.toJSONString(sysParameter.getParameters()));
        sysParameterDO.setAdmin(sysParameter.getAdmin());
        return sysParameterDO;
    }
}
