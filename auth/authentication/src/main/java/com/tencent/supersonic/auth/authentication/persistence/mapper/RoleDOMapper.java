package com.tencent.supersonic.auth.authentication.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.RoleDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RoleDOMapper extends BaseMapper<RoleDO> {

    /**
     * 根据租户ID查询角色列表
     */
    @Select("SELECT id, name, code, description, scope, tenant_id, is_system, status, created_at, created_by, updated_at, updated_by "
            + "FROM s2_role WHERE (tenant_id = #{tenantId} OR (scope = 'PLATFORM' AND tenant_id IS NULL)) AND status = 1 ORDER BY id")
    List<RoleDO> selectByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 根据角色code和租户ID查询
     */
    @Select("SELECT id, name, code, description, scope, tenant_id, is_system, status, created_at, created_by, updated_at, updated_by "
            + "FROM s2_role WHERE code = #{code}"
            + " AND (tenant_id = #{tenantId} OR (scope = 'PLATFORM' AND tenant_id IS NULL))")
    RoleDO selectByCodeAndTenantId(@Param("code") String code, @Param("tenantId") Long tenantId);

    /**
     * 根据作用域和租户ID查询角色列表
     */
    @Select("SELECT id, name, code, description, scope, tenant_id, is_system, status, created_at, created_by, updated_at, updated_by "
            + "FROM s2_role WHERE scope = #{scope} AND (tenant_id = #{tenantId} OR scope = 'PLATFORM') AND status = 1 ORDER BY id")
    List<RoleDO> selectByScopeAndTenantId(@Param("scope") String scope,
            @Param("tenantId") Long tenantId);
}
