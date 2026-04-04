package com.tencent.supersonic.auth.authentication.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.TenantUsageDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;

/**
 * MyBatis mapper for tenant usage table.
 */
@Mapper
public interface TenantUsageDOMapper extends BaseMapper<TenantUsageDO> {

    @Update("UPDATE s2_tenant_usage SET api_calls = api_calls + 1, updated_at = NOW() "
            + "WHERE tenant_id = #{tenantId} AND usage_date = #{usageDate}")
    int incrementApiCalls(@Param("tenantId") Long tenantId,
            @Param("usageDate") LocalDate usageDate);

    @Update("UPDATE s2_tenant_usage SET tokens_used = tokens_used + #{tokenCount}, "
            + "updated_at = NOW() WHERE tenant_id = #{tenantId} AND usage_date = #{usageDate}")
    int incrementTokensUsed(@Param("tenantId") Long tenantId,
            @Param("usageDate") LocalDate usageDate, @Param("tokenCount") long tokenCount);

    @Update("UPDATE s2_tenant_usage SET query_count = query_count + 1, updated_at = NOW() "
            + "WHERE tenant_id = #{tenantId} AND usage_date = #{usageDate}")
    int incrementQueryCount(@Param("tenantId") Long tenantId,
            @Param("usageDate") LocalDate usageDate);

    @Update("UPDATE s2_tenant_usage SET storage_bytes = storage_bytes + #{bytes}, "
            + "updated_at = NOW() WHERE tenant_id = #{tenantId} AND usage_date = #{usageDate}")
    int incrementStorageBytes(@Param("tenantId") Long tenantId,
            @Param("usageDate") LocalDate usageDate, @Param("bytes") long bytes);

    @Update("UPDATE s2_tenant_usage SET active_users = active_users + 1, updated_at = NOW() "
            + "WHERE tenant_id = #{tenantId} AND usage_date = #{usageDate}")
    int incrementActiveUsers(@Param("tenantId") Long tenantId,
            @Param("usageDate") LocalDate usageDate);

    @Select("SELECT COALESCE(api_calls, 0) FROM s2_tenant_usage "
            + "WHERE tenant_id = #{tenantId} AND usage_date = #{usageDate}")
    int selectApiCallsForDate(@Param("tenantId") Long tenantId,
            @Param("usageDate") LocalDate usageDate);

    @Select("SELECT COALESCE(SUM(tokens_used), 0) FROM s2_tenant_usage "
            + "WHERE tenant_id = #{tenantId} AND usage_date BETWEEN #{startDate} AND #{endDate}")
    long sumTokensUsedInRange(@Param("tenantId") Long tenantId,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
