package com.tencent.supersonic.auth.authentication.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tencent.supersonic.auth.api.authentication.pojo.Tenant;
import com.tencent.supersonic.auth.api.authentication.service.TenantService;
import com.tencent.supersonic.auth.api.authentication.service.UsageTrackingService;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.TenantDO;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.UserDO;
import com.tencent.supersonic.auth.authentication.persistence.mapper.TenantDOMapper;
import com.tencent.supersonic.auth.authentication.persistence.mapper.UserDOMapper;
import com.tencent.supersonic.common.pojo.PlanQuota;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.service.SubscriptionInfoProvider;
import com.tencent.supersonic.common.util.BeanMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

/**
 * Implementation of TenantService.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_SUSPENDED = "SUSPENDED";
    private static final String STATUS_DELETED = "DELETED";

    private final TenantDOMapper tenantDOMapper;
    private final SubscriptionInfoProvider subscriptionInfoProvider;
    private final UserDOMapper userDOMapper;
    private final UsageTrackingService usageTrackingService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public Tenant createTenant(Tenant tenant) {
        tenant.setCode(normalizeTenantCode(tenant.getCode()));
        TenantDO tenantDO = convertToTenantDO(tenant);
        tenantDO.setStatus(STATUS_ACTIVE);
        tenantDO.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        tenantDO.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        tenantDOMapper.insert(tenantDO);
        log.info("Created tenant: {} with ID: {}", tenant.getName(), tenantDO.getId());

        return convertToTenant(tenantDO);
    }

    @Override
    @Transactional
    public Tenant updateTenant(Tenant tenant) {
        tenant.setCode(normalizeTenantCode(tenant.getCode()));
        TenantDO tenantDO = convertToTenantDO(tenant);
        tenantDO.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        tenantDOMapper.updateById(tenantDO);
        log.info("Updated tenant: {}", tenant.getId());

        return convertToTenant(tenantDO);
    }

    @Override
    public Optional<Tenant> getTenantById(Long id) {
        TenantDO tenantDO = tenantDOMapper.selectById(id);
        return Optional.ofNullable(tenantDO).map(this::convertToTenant);
    }

    @Override
    public Optional<Tenant> getTenantByCode(String code) {
        String normalizedCode = normalizeTenantCode(code);
        if (StringUtils.isBlank(normalizedCode)) {
            return Optional.empty();
        }
        LambdaQueryWrapper<TenantDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.apply("LOWER(code) = {0}", normalizedCode);
        TenantDO tenantDO = tenantDOMapper.selectOne(wrapper);
        return Optional.ofNullable(tenantDO).map(this::convertToTenant);
    }

    @Override
    public List<Tenant> getAllTenants() {
        LambdaQueryWrapper<TenantDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(TenantDO::getStatus, STATUS_DELETED);
        return tenantDOMapper.selectList(wrapper).stream().map(this::convertToTenant)
                .collect(Collectors.toList());
    }

    @Override
    public List<Tenant> getActiveTenants() {
        LambdaQueryWrapper<TenantDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TenantDO::getStatus, STATUS_ACTIVE);
        return tenantDOMapper.selectList(wrapper).stream().map(this::convertToTenant)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteTenant(Long id) {
        TenantDO tenantDO = tenantDOMapper.selectById(id);
        if (tenantDO != null) {
            tenantDO.setStatus(STATUS_DELETED);
            tenantDO.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
            tenantDOMapper.updateById(tenantDO);
            log.info("Deleted tenant: {}", id);
        }
    }

    @Override
    @Transactional
    public void suspendTenant(Long id) {
        TenantDO tenantDO = tenantDOMapper.selectById(id);
        if (tenantDO != null) {
            tenantDO.setStatus(STATUS_SUSPENDED);
            tenantDO.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
            tenantDOMapper.updateById(tenantDO);
            log.info("Suspended tenant: {}", id);
        }
    }

    @Override
    @Transactional
    public void activateTenant(Long id) {
        TenantDO tenantDO = tenantDOMapper.selectById(id);
        if (tenantDO != null) {
            tenantDO.setStatus(STATUS_ACTIVE);
            tenantDO.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
            tenantDOMapper.updateById(tenantDO);
            log.info("Activated tenant: {}", id);
        }
    }

    @Override
    public boolean isUserLimitReached(Long tenantId) {
        return isResourceLimitReached(tenantId, PlanQuota::getMaxUsers, this::countUserByTenantId);
    }

    @Override
    public boolean isDatasetLimitReached(Long tenantId) {
        return isResourceLimitReached(tenantId, PlanQuota::getMaxDatasets,
                this::countDatasetByTenantId);
    }

    @Override
    public boolean isModelLimitReached(Long tenantId) {
        return isResourceLimitReached(tenantId, PlanQuota::getMaxModels,
                this::countModelByTenantId);
    }

    @Override
    public boolean isAgentLimitReached(Long tenantId) {
        return isResourceLimitReached(tenantId, PlanQuota::getMaxAgents,
                this::countAgentByTenantId);
    }

    @Override
    public boolean isApiCallLimitReached(Long tenantId) {
        Optional<PlanQuota> quota = subscriptionInfoProvider.getActivePlanQuota(tenantId);
        if (quota.isEmpty()) {
            return false;
        }
        Integer maxApiCalls = quota.get().getMaxApiCallsPerDay();
        if (quota.get().isUnlimited(maxApiCalls)) {
            return false;
        }
        int currentApiCalls = usageTrackingService.getTodayApiCalls(tenantId);
        // API calls are recorded before checking to reduce concurrent under-counting, so the
        // request that exactly reaches the limit should still be allowed.
        return currentApiCalls > maxApiCalls;
    }

    @Override
    public int getApiCallUsagePercent(Long tenantId) {
        Optional<PlanQuota> quota = subscriptionInfoProvider.getActivePlanQuota(tenantId);
        if (quota.isEmpty()) {
            return 0;
        }
        Integer maxApiCalls = quota.get().getMaxApiCallsPerDay();
        if (quota.get().isUnlimited(maxApiCalls)) {
            return 0;
        }
        int currentApiCalls = usageTrackingService.getTodayApiCalls(tenantId);
        return (int) Math.min(100, (long) currentApiCalls * 100 / maxApiCalls);
    }

    @Override
    public int getTokenUsagePercent(Long tenantId) {
        Optional<PlanQuota> quota = subscriptionInfoProvider.getActivePlanQuota(tenantId);
        if (quota.isEmpty()) {
            return 0;
        }
        Long maxTokens = quota.get().getMaxTokensPerMonth();
        if (quota.get().isUnlimited(maxTokens)) {
            return 0;
        }
        long currentTokens = usageTrackingService.getMonthlyTokenUsage(tenantId);
        return (int) Math.min(100, currentTokens * 100 / maxTokens);
    }

    @Override
    public boolean isTenantCodeAvailable(String code) {
        String normalizedCode = normalizeTenantCode(code);
        if (StringUtils.isBlank(normalizedCode)) {
            return false;
        }
        LambdaQueryWrapper<TenantDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.apply("LOWER(code) = {0}", normalizedCode);
        return tenantDOMapper.selectCount(wrapper) == 0;
    }

    private TenantDO convertToTenantDO(Tenant tenant) {
        TenantDO tenantDO = new TenantDO();
        BeanMapper.mapper(tenant, tenantDO);
        return tenantDO;
    }

    private Tenant convertToTenant(TenantDO tenantDO) {
        Tenant tenant = new Tenant();
        BeanMapper.mapper(tenantDO, tenant);
        return tenant;
    }

    private boolean isResourceLimitReached(Long tenantId, Function<PlanQuota, Integer> maxExtractor,
            ToLongFunction<Long> counter) {
        Optional<PlanQuota> quota = subscriptionInfoProvider.getActivePlanQuota(tenantId);
        if (quota.isEmpty()) {
            return false;
        }
        Integer max = maxExtractor.apply(quota.get());
        if (quota.get().isUnlimited(max)) {
            return false;
        }
        long current = counter.applyAsLong(tenantId);
        return current >= max;
    }

    private long countUserByTenantId(Long tenantId) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDO::getTenantId, tenantId).eq(UserDO::getStatus, 1);
        return userDOMapper.selectCount(wrapper);
    }

    private long countDatasetByTenantId(Long tenantId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM s2_data_set WHERE tenant_id = ? AND status <> ?", Long.class,
                tenantId, StatusEnum.DELETED.getCode());
        return count == null ? 0L : count;
    }

    private long countModelByTenantId(Long tenantId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM s2_model WHERE tenant_id = ? AND status <> ?", Long.class,
                tenantId, StatusEnum.DELETED.getCode());
        return count == null ? 0L : count;
    }

    private long countAgentByTenantId(Long tenantId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM s2_agent WHERE tenant_id = ?", Long.class, tenantId);
        return count == null ? 0L : count;
    }

    private String normalizeTenantCode(String code) {
        return StringUtils.trimToEmpty(code).toLowerCase();
    }
}
