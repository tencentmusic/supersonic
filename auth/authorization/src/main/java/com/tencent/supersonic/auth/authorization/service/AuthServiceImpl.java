package com.tencent.supersonic.auth.authorization.service;

import com.google.gson.Gson;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthGroup;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthRes;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthRule;
import com.tencent.supersonic.auth.api.authorization.pojo.DimensionFilter;
import com.tencent.supersonic.auth.api.authorization.request.QueryAuthResReq;
import com.tencent.supersonic.auth.api.authorization.response.AuthorizedResourceResp;
import com.tencent.supersonic.auth.api.authorization.service.AuthService;
import com.tencent.supersonic.common.pojo.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private JdbcTemplate jdbcTemplate;

    private UserService userService;

    public AuthServiceImpl(JdbcTemplate jdbcTemplate, UserService userService) {
        this.jdbcTemplate = jdbcTemplate;
        this.userService = userService;
    }

    private List<AuthGroup> load() {
        List<String> rows =
                jdbcTemplate.queryForList("select config from s2_auth_groups", String.class);
        Gson g = new Gson();
        return rows.stream().map(row -> g.fromJson(row, AuthGroup.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<AuthGroup> queryAuthGroups(String modelId, Integer groupId) {
        return load().stream()
                .filter(group -> (Objects.isNull(groupId) || groupId.equals(group.getGroupId()))
                        && modelId.equals(group.getModelId().toString()))
                .collect(Collectors.toList());
    }

    @Override
    public void addOrUpdateAuthGroup(AuthGroup group) {
        Gson g = new Gson();
        if (group.getGroupId() == null) {
            int nextGroupId = 1;
            String sql = "select max(group_id) as group_id from s2_auth_groups";
            Integer obj = jdbcTemplate.queryForObject(sql, Integer.class);
            if (obj != null) {
                nextGroupId = obj + 1;
            }
            group.setGroupId(nextGroupId);
            jdbcTemplate.update("insert into s2_auth_groups (group_id, config) values (?, ?);",
                    nextGroupId, g.toJson(group));
        } else {
            jdbcTemplate.update("update s2_auth_groups set config = ? where group_id = ?;",
                    g.toJson(group), group.getGroupId());
        }
    }

    @Override
    public void removeAuthGroup(AuthGroup group) {
        jdbcTemplate.update("delete from s2_auth_groups where group_id = ?", group.getGroupId());
    }

    @Override
    public AuthorizedResourceResp queryAuthorizedResources(QueryAuthResReq req, User user) {
        if (CollectionUtils.isEmpty(req.getModelIds())) {
            return new AuthorizedResourceResp();
        }
        Set<String> userOrgIds = userService.getUserAllOrgId(user.getName());
        List<AuthGroup> groups =
                getAuthGroups(req.getModelIds(), user.getName(), new ArrayList<>(userOrgIds));
        AuthorizedResourceResp resource = new AuthorizedResourceResp();
        Map<Long, List<AuthGroup>> authGroupsByModelId =
                groups.stream().collect(Collectors.groupingBy(AuthGroup::getModelId));
        for (Long modelId : req.getModelIds()) {
            if (authGroupsByModelId.containsKey(modelId)) {
                List<AuthGroup> authGroups = authGroupsByModelId.get(modelId);
                for (AuthGroup authRuleGroup : authGroups) {
                    List<AuthRule> authRules = authRuleGroup.getAuthRules();
                    for (AuthRule authRule : authRules) {
                        for (String resBizName : authRule.resourceNames()) {
                            resource.getAuthResList().add(new AuthRes(modelId, resBizName));
                        }
                    }
                }
            }
        }
        Set<Map.Entry<Long, List<AuthGroup>>> entries = authGroupsByModelId.entrySet();
        for (Map.Entry<Long, List<AuthGroup>> entry : entries) {
            List<AuthGroup> authGroups = entry.getValue();
            for (AuthGroup authGroup : authGroups) {
                DimensionFilter df = new DimensionFilter();
                df.setDescription(authGroup.getDimensionFilterDescription());
                df.setExpressions(authGroup.getDimensionFilters());
                resource.getFilters().add(df);
            }
        }
        return resource;
    }

    private List<AuthGroup> getAuthGroups(List<Long> modelIds, String userName,
            List<String> departmentIds) {
        List<AuthGroup> groups = load().stream().filter(group -> {
            if (!modelIds.contains(group.getModelId())) {
                return false;
            }
            if (!CollectionUtils.isEmpty(group.getAuthorizedUsers())
                    && group.getAuthorizedUsers().contains(userName)) {
                return true;
            }
            for (String departmentId : departmentIds) {
                if (!CollectionUtils.isEmpty(group.getAuthorizedDepartmentIds())
                        && group.getAuthorizedDepartmentIds().contains(departmentId)) {
                    return true;
                }
            }
            return false;
        }).collect(Collectors.toList());
        log.info("user:{} department:{} authGroups:{}", userName, departmentIds, groups);
        return groups;
    }
}
