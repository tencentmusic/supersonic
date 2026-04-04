package com.tencent.supersonic.auth.authentication.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tencent.supersonic.auth.api.authentication.pojo.TenantUsage;
import com.tencent.supersonic.auth.api.authentication.service.UsageTrackingService;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.TenantUsageDO;
import com.tencent.supersonic.auth.authentication.persistence.mapper.TenantUsageDOMapper;
import com.tencent.supersonic.common.util.BeanMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of UsageTrackingService.
 * <p>
 * Write methods (record*) use atomic SQL UPDATE for concurrency safety, avoiding read-modify-write
 * race conditions. A row for today is lazily created on first access via INSERT with
 * DuplicateKeyException guard.
 */
@Service
@Slf4j
public class UsageTrackingServiceImpl implements UsageTrackingService {

    private final TenantUsageDOMapper tenantUsageDOMapper;

    public UsageTrackingServiceImpl(TenantUsageDOMapper tenantUsageDOMapper) {
        this.tenantUsageDOMapper = tenantUsageDOMapper;
    }

    @Override
    public void recordApiCall(Long tenantId) {
        LocalDate today = LocalDate.now();
        int updated = tenantUsageDOMapper.incrementApiCalls(tenantId, today);
        if (updated == 0) {
            ensureTodayRowExists(tenantId);
            tenantUsageDOMapper.incrementApiCalls(tenantId, today);
        }
    }

    @Override
    public void recordTokenUsage(Long tenantId, long tokenCount) {
        LocalDate today = LocalDate.now();
        int updated = tenantUsageDOMapper.incrementTokensUsed(tenantId, today, tokenCount);
        if (updated == 0) {
            ensureTodayRowExists(tenantId);
            tenantUsageDOMapper.incrementTokensUsed(tenantId, today, tokenCount);
        }
    }

    @Override
    public void recordQuery(Long tenantId) {
        LocalDate today = LocalDate.now();
        int updated = tenantUsageDOMapper.incrementQueryCount(tenantId, today);
        if (updated == 0) {
            ensureTodayRowExists(tenantId);
            tenantUsageDOMapper.incrementQueryCount(tenantId, today);
        }
    }

    @Override
    public void recordStorageUsage(Long tenantId, long bytes) {
        LocalDate today = LocalDate.now();
        int updated = tenantUsageDOMapper.incrementStorageBytes(tenantId, today, bytes);
        if (updated == 0) {
            ensureTodayRowExists(tenantId);
            tenantUsageDOMapper.incrementStorageBytes(tenantId, today, bytes);
        }
    }

    @Override
    public void recordActiveUser(Long tenantId) {
        LocalDate today = LocalDate.now();
        int updated = tenantUsageDOMapper.incrementActiveUsers(tenantId, today);
        if (updated == 0) {
            ensureTodayRowExists(tenantId);
            tenantUsageDOMapper.incrementActiveUsers(tenantId, today);
        }
    }

    @Override
    public Optional<TenantUsage> getUsage(Long tenantId, LocalDate date) {
        LambdaQueryWrapper<TenantUsageDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TenantUsageDO::getTenantId, tenantId).eq(TenantUsageDO::getUsageDate, date);
        TenantUsageDO usageDO = tenantUsageDOMapper.selectOne(wrapper);
        return Optional.ofNullable(usageDO).map(this::convertToUsage);
    }

    @Override
    public TenantUsage getTodayUsage(Long tenantId) {
        TenantUsageDO usageDO = getOrCreateTodayUsage(tenantId);
        return convertToUsage(usageDO);
    }

    @Override
    public List<TenantUsage> getUsageRange(Long tenantId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<TenantUsageDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TenantUsageDO::getTenantId, tenantId).ge(TenantUsageDO::getUsageDate, startDate)
                .le(TenantUsageDO::getUsageDate, endDate).orderByAsc(TenantUsageDO::getUsageDate);
        return tenantUsageDOMapper.selectList(wrapper).stream().map(this::convertToUsage)
                .collect(Collectors.toList());
    }

    @Override
    public int getTodayApiCalls(Long tenantId) {
        return tenantUsageDOMapper.selectApiCallsForDate(tenantId, LocalDate.now());
    }

    @Override
    public long getMonthlyTokenUsage(Long tenantId) {
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        return tenantUsageDOMapper.sumTokensUsedInRange(tenantId, firstDayOfMonth, today);
    }

    @Override
    public TenantUsage getMonthlyUsage(Long tenantId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<TenantUsage> usages = getUsageRange(tenantId, startDate, endDate);

        // Aggregate usage

        return TenantUsage.builder().tenantId(tenantId).usageDate(startDate)
                .apiCalls(usages.stream()
                        .mapToInt(u -> u.getApiCalls() != null ? u.getApiCalls() : 0).sum())
                .tokensUsed(usages.stream()
                        .mapToLong(u -> u.getTokensUsed() != null ? u.getTokensUsed() : 0).sum())
                .queryCount(usages.stream()
                        .mapToInt(u -> u.getQueryCount() != null ? u.getQueryCount() : 0).sum())
                .storageBytes(usages.stream()
                        .mapToLong(u -> u.getStorageBytes() != null ? u.getStorageBytes() : 0).max()
                        .orElse(0))
                .activeUsers(usages.stream()
                        .mapToInt(u -> u.getActiveUsers() != null ? u.getActiveUsers() : 0).max()
                        .orElse(0))
                .build();
    }

    /**
     * Ensures a usage row exists for the given tenant and today's date. Uses INSERT with
     * DuplicateKeyException guard so concurrent threads don't fail — the loser simply catches the
     * exception.
     */
    private void ensureTodayRowExists(Long tenantId) {
        LocalDate today = LocalDate.now();
        try {
            TenantUsageDO usageDO = new TenantUsageDO();
            usageDO.setTenantId(tenantId);
            usageDO.setUsageDate(today);
            usageDO.setApiCalls(0);
            usageDO.setTokensUsed(0L);
            usageDO.setQueryCount(0);
            usageDO.setStorageBytes(0L);
            usageDO.setActiveUsers(0);
            usageDO.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            usageDO.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
            tenantUsageDOMapper.insert(usageDO);
        } catch (DuplicateKeyException e) {
            // Row already created by concurrent thread — safe to ignore
        }
    }

    /**
     * Used by read-only methods (getTodayUsage, etc.) that need the full row.
     */
    private TenantUsageDO getOrCreateTodayUsage(Long tenantId) {
        LocalDate today = LocalDate.now();

        LambdaQueryWrapper<TenantUsageDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TenantUsageDO::getTenantId, tenantId).eq(TenantUsageDO::getUsageDate, today);
        TenantUsageDO usageDO = tenantUsageDOMapper.selectOne(wrapper);

        if (usageDO == null) {
            ensureTodayRowExists(tenantId);
            // Re-select after insert
            usageDO = tenantUsageDOMapper.selectOne(wrapper);
        }

        return usageDO;
    }

    private TenantUsage convertToUsage(TenantUsageDO usageDO) {
        TenantUsage usage = new TenantUsage();
        BeanMapper.mapper(usageDO, usage);
        return usage;
    }
}
