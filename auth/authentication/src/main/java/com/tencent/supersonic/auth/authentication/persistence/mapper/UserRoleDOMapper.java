package com.tencent.supersonic.auth.authentication.persistence.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserRoleDOMapper {

    /**
     * 获取用户的角色ID列表
     */
    @Select("SELECT role_id FROM s2_user_role WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    /**
     * 获取用户的角色名称列表
     */
    @Select("SELECT r.name FROM s2_role r INNER JOIN s2_user_role ur ON r.id = ur.role_id WHERE ur.user_id = #{userId}")
    List<String> selectRoleNamesByUserId(@Param("userId") Long userId);

    /**
     * 获取用户的角色信息（ID和名称）
     */
    @Select("SELECT r.id, r.name FROM s2_role r INNER JOIN s2_user_role ur ON r.id = ur.role_id WHERE ur.user_id = #{userId}")
    List<Map<String, Object>> selectRolesByUserId(@Param("userId") Long userId);

    /**
     * 删除用户的所有角色
     */
    @Delete("DELETE FROM s2_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 只删除用户的租户级角色，保留平台级角色
     */
    @Delete("DELETE FROM s2_user_role WHERE user_id = #{userId} AND role_id IN "
            + "(SELECT id FROM s2_role WHERE scope = 'TENANT')")
    int deleteTenantRolesByUserId(@Param("userId") Long userId);

    /**
     * 插入用户-角色关联
     */
    @Insert("INSERT INTO s2_user_role (user_id, role_id, created_at, created_by) VALUES (#{userId}, #{roleId}, CURRENT_TIMESTAMP, #{createdBy})")
    int insert(@Param("userId") Long userId, @Param("roleId") Long roleId,
            @Param("createdBy") String createdBy);

    /**
     * 批量插入用户-角色关联
     */
    @Insert("<script>"
            + "INSERT INTO s2_user_role (user_id, role_id, created_at, created_by) VALUES "
            + "<foreach collection='roleIds' item='roleId' separator=','>"
            + "(#{userId}, #{roleId}, CURRENT_TIMESTAMP, #{createdBy})" + "</foreach>"
            + "</script>")
    int batchInsert(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds,
            @Param("createdBy") String createdBy);
}
