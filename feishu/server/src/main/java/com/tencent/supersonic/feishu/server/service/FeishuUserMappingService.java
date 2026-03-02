package com.tencent.supersonic.feishu.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.feishu.api.config.FeishuProperties;
import com.tencent.supersonic.feishu.server.persistence.dataobject.FeishuQuerySessionDO;
import com.tencent.supersonic.feishu.server.persistence.dataobject.FeishuUserMappingDO;
import com.tencent.supersonic.feishu.server.persistence.mapper.FeishuQuerySessionMapper;
import com.tencent.supersonic.feishu.server.persistence.mapper.FeishuUserMappingMapper;
import com.tencent.supersonic.feishu.server.service.FeishuContactService.FeishuContactInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class FeishuUserMappingService {

    public record ResolvedMapping(User user, Integer agentId) {}


    private final FeishuUserMappingMapper userMappingMapper;
    private final FeishuQuerySessionMapper querySessionMapper;
    private final FeishuContactService feishuContactService;
    private final FeishuProperties feishuProperties;
    private final SuperSonicApiClient apiClient;

    /**
     * Resolve a SuperSonic User from a Feishu open_id. First checks the mapping table, then
     * attempts auto-match if enabled.
     *
     * @param openId the Feishu open_id
     * @return the resolved User, or null if no mapping found
     */
    public User resolveUser(String openId) {
        ResolvedMapping resolved = resolveMapping(openId);
        return resolved != null ? resolved.user() : null;
    }

    /**
     * Resolve both the User and the user's preferred agentId from a Feishu open_id. Returns null if
     * no mapping found (a pending mapping is created as a side effect).
     */
    public ResolvedMapping resolveMapping(String openId) {
        // 1. Check mapping table for an active mapping
        LambdaQueryWrapper<FeishuUserMappingDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeishuUserMappingDO::getFeishuOpenId, openId).eq(FeishuUserMappingDO::getStatus,
                1);
        log.debug("resolveMapping: openId={}, tenantId={}", openId, TenantContext.getTenantId());
        FeishuUserMappingDO mapping = userMappingMapper.selectOne(wrapper);
        log.debug("resolveMapping: mapping={}", mapping);

        if (mapping != null) {
            User user = buildUser(mapping.getS2UserId(), mapping.getTenantId());
            return user != null ? new ResolvedMapping(user, mapping.getDefaultAgentId()) : null;
        }

        // 2. Try auto-match if enabled (uses current TenantContext for user lookup)
        if (feishuProperties.getUserMapping().isAutoMatchEnabled()) {
            Long currentTenantId = TenantContext.getTenantIdOrDefault(1L);
            FeishuUserMappingDO autoMatched = tryAutoMatch(openId, currentTenantId);
            if (autoMatched != null) {
                User user = buildUser(autoMatched.getS2UserId(), autoMatched.getTenantId());
                return user != null ? new ResolvedMapping(user, autoMatched.getDefaultAgentId())
                        : null;
            }
        }

        // 3. No match found — create a pending record so admin can assign manually
        createPendingMapping(openId);
        log.info("No user mapping found for openId={}, created pending mapping", openId);
        return null;
    }

    /**
     * Create a pending (status=0) mapping record for an unmapped Feishu user, so the admin can see
     * it in the web UI and assign an s2_user. Skips if a record already exists for this openId.
     */
    private void createPendingMapping(String openId) {
        LambdaQueryWrapper<FeishuUserMappingDO> exists = new LambdaQueryWrapper<>();
        exists.eq(FeishuUserMappingDO::getFeishuOpenId, openId);
        if (userMappingMapper.selectCount(exists) > 0) {
            return;
        }
        FeishuUserMappingDO pending = new FeishuUserMappingDO();
        pending.setFeishuOpenId(openId);
        pending.setStatus(0);
        pending.setMatchType("PENDING");
        pending.setCreatedAt(new Date());
        pending.setUpdatedAt(new Date());

        // Try to fill in contact info for display in the admin UI
        try {
            FeishuContactInfo info = feishuContactService.getContactInfo(openId);
            if (info != null) {
                pending.setFeishuUserName(info.name());
                pending.setFeishuEmail(info.email());
                pending.setFeishuMobile(info.mobile());
                pending.setFeishuEmployeeId(info.employeeId());
            }
        } catch (Exception e) {
            log.debug("Could not fetch contact info for pending mapping: {}", e.getMessage());
        }

        userMappingMapper.insert(pending);
        log.info("Created pending mapping for openId={}, name={}", openId,
                pending.getFeishuUserName());
    }

    /**
     * Attempt to auto-match a Feishu user to a SuperSonic user based on contact info. Uses the
     * current TenantContext for user lookup.
     */
    public FeishuUserMappingDO tryAutoMatch(String openId) {
        return tryAutoMatch(openId, TenantContext.getTenantIdOrDefault(1L));
    }

    /**
     * Attempt to auto-match a Feishu user to a SuperSonic user based on contact info. Tries match
     * fields in configured priority order: EMPLOYEE_ID, EMAIL, MOBILE.
     *
     * @param openId the Feishu open_id
     * @param tenantId the tenant to search for matching users
     * @return the created mapping record, or null if no match found
     */
    public FeishuUserMappingDO tryAutoMatch(String openId, Long tenantId) {
        FeishuContactInfo contactInfo = feishuContactService.getContactInfo(openId);
        if (contactInfo == null) {
            log.info("Cannot auto-match: no contact info for openId={}", openId);
            return null;
        }

        List<String> matchFields = feishuProperties.getUserMapping().getMatchFields();
        for (String field : matchFields) {
            Long s2UserId = matchByField(field, contactInfo, tenantId);
            if (s2UserId != null) {
                log.info("Auto-matched openId={} to s2UserId={} via {}", openId, s2UserId, field);
                FeishuUserMappingDO mapping = new FeishuUserMappingDO();
                mapping.setFeishuOpenId(openId);
                mapping.setFeishuUserName(contactInfo.name());
                mapping.setFeishuEmail(contactInfo.email());
                mapping.setFeishuMobile(contactInfo.mobile());
                mapping.setFeishuEmployeeId(contactInfo.employeeId());
                mapping.setS2UserId(s2UserId);
                mapping.setTenantId(tenantId);
                mapping.setMatchType(field);
                mapping.setStatus(1);
                mapping.setCreatedAt(new Date());
                mapping.setUpdatedAt(new Date());
                userMappingMapper.insert(mapping);
                return mapping;
            }
        }

        log.info("Auto-match failed for openId={}: no matching s2_user found", openId);
        return null;
    }

    /**
     * Get a single mapping by ID.
     */
    public FeishuUserMappingDO getMappingById(Long id) {
        return userMappingMapper.selectById(id);
    }

    /**
     * List user mappings with pagination.
     */
    public IPage<FeishuUserMappingDO> listMappings(int pageNum, int pageSize) {
        Page<FeishuUserMappingDO> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<FeishuUserMappingDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(FeishuUserMappingDO::getUpdatedAt);
        return userMappingMapper.selectPage(page, wrapper);
    }

    /**
     * Create a new user mapping record.
     */
    public FeishuUserMappingDO createMapping(FeishuUserMappingDO mapping) {
        mapping.setCreatedAt(new Date());
        mapping.setUpdatedAt(new Date());
        if (mapping.getStatus() == null) {
            mapping.setStatus(1);
        }
        if (StringUtils.isBlank(mapping.getMatchType())) {
            mapping.setMatchType("MANUAL");
        }
        userMappingMapper.insert(mapping);
        return mapping;
    }

    /**
     * Update an existing user mapping record.
     */
    public FeishuUserMappingDO updateMapping(FeishuUserMappingDO mapping) {
        mapping.setUpdatedAt(new Date());
        userMappingMapper.updateById(mapping);
        return mapping;
    }

    /**
     * Update the default agentId for a user identified by Feishu openId.
     *
     * @return true if updated, false if no active mapping found
     */
    public boolean updateDefaultAgent(String openId, Integer agentId) {
        LambdaQueryWrapper<FeishuUserMappingDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeishuUserMappingDO::getFeishuOpenId, openId).eq(FeishuUserMappingDO::getStatus,
                1);
        FeishuUserMappingDO mapping = userMappingMapper.selectOne(wrapper);
        if (mapping == null) {
            return false;
        }
        mapping.setDefaultAgentId(agentId);
        mapping.setUpdatedAt(new Date());
        userMappingMapper.updateById(mapping);
        return true;
    }

    /**
     * Delete a user mapping by ID.
     */
    public void deleteMapping(Long id) {
        userMappingMapper.deleteById(id);
    }

    /**
     * Toggle the status of a user mapping (enable/disable).
     */
    public void toggleStatus(Long id, int status) {
        FeishuUserMappingDO mapping = userMappingMapper.selectById(id);
        if (mapping != null) {
            // When enabling a PENDING mapping, promote to MANUAL
            if (status == 1 && "PENDING".equals(mapping.getMatchType())) {
                if (mapping.getS2UserId() == null) {
                    throw new IllegalStateException("请先编辑并关联平台用户后再启用");
                }
                mapping.setMatchType("MANUAL");
            }
            mapping.setStatus(status);
            mapping.setUpdatedAt(new Date());
            userMappingMapper.updateById(mapping);
        }
    }

    /**
     * List query sessions with optional filters.
     */
    public IPage<FeishuQuerySessionDO> listSessions(int pageNum, int pageSize, String status,
            String startDate, String endDate) {
        Page<FeishuQuerySessionDO> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<FeishuQuerySessionDO> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.isNotBlank(status)) {
            wrapper.eq(FeishuQuerySessionDO::getStatus, status);
        }
        if (StringUtils.isNotBlank(startDate)) {
            wrapper.ge(FeishuQuerySessionDO::getCreatedAt, startDate);
        }
        if (StringUtils.isNotBlank(endDate)) {
            wrapper.le(FeishuQuerySessionDO::getCreatedAt, endDate);
        }

        wrapper.orderByDesc(FeishuQuerySessionDO::getCreatedAt);
        return querySessionMapper.selectPage(page, wrapper);
    }

    /**
     * Find a PENDING mapping record for a given openId.
     */
    public FeishuUserMappingDO findPendingByOpenId(String openId) {
        LambdaQueryWrapper<FeishuUserMappingDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeishuUserMappingDO::getFeishuOpenId, openId).eq(FeishuUserMappingDO::getStatus,
                0);
        return userMappingMapper.selectOne(wrapper);
    }

    /**
     * Find an active mapping by SuperSonic user ID.
     */
    public FeishuUserMappingDO findByS2UserId(Long s2UserId) {
        LambdaQueryWrapper<FeishuUserMappingDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeishuUserMappingDO::getS2UserId, s2UserId).eq(FeishuUserMappingDO::getStatus,
                1);
        return userMappingMapper.selectOne(wrapper);
    }

    /**
     * Complete a self-service binding: update the PENDING mapping to active with the given
     * s2UserId.
     */
    public void completeBinding(Long mappingId, Long s2UserId) {
        FeishuUserMappingDO mapping = userMappingMapper.selectById(mappingId);
        if (mapping == null) {
            throw new IllegalStateException("Mapping record not found: id=" + mappingId);
        }
        mapping.setS2UserId(s2UserId);
        mapping.setMatchType("OAUTH_BIND");
        mapping.setStatus(1);
        mapping.setUpdatedAt(new Date());
        userMappingMapper.updateById(mapping);
        log.info("Completed OAuth binding: mappingId={}, s2UserId={}, openId={}", mappingId,
                s2UserId, mapping.getFeishuOpenId());
    }

    /**
     * Get a User object via the auth API (supports independent deployment).
     */
    private User buildUser(Long s2UserId, Long tenantId) {
        if (s2UserId == null) {
            return null;
        }
        User user = apiClient.getUserById(s2UserId, tenantId);
        if (user == null) {
            log.warn("No s2_user found for id={}, tenantId={}", s2UserId, tenantId);
        }
        return user;
    }

    /**
     * Try to match a Feishu contact field to a SuperSonic user via the auth API.
     */
    private Long matchByField(String field, FeishuContactInfo contactInfo, Long tenantId) {
        try {
            List<User> users = apiClient.getUserList(tenantId);
            return switch (field) {
                case "EMPLOYEE_ID" ->
                    findUserIdByValue(users, contactInfo.employeeId(), User::getEmployeeId);
                case "EMAIL" -> findUserIdByValue(users, contactInfo.email(), User::getEmail);
                case "MOBILE" -> findUserIdByValue(users, contactInfo.mobile(), User::getPhone);
                default -> {
                    log.warn("Unknown match field: {}", field);
                    yield null;
                }
            };
        } catch (Exception e) {
            log.error("Error matching by field {}: {}", field, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Search a user list for a matching field value and return the user ID.
     */
    private Long findUserIdByValue(List<User> users, String value,
            java.util.function.Function<User, String> getter) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return users.stream().filter(u -> Objects.equals(value, getter.apply(u))).map(User::getId)
                .findFirst().orElse(null);
    }
}
