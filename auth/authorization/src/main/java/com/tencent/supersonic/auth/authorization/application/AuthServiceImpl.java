package com.tencent.supersonic.auth.authorization.application;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthRes;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthResGrp;
import com.tencent.supersonic.auth.api.authorization.pojo.DimensionFilter;
import com.tencent.supersonic.auth.api.authorization.request.QueryAuthResReq;
import com.tencent.supersonic.auth.api.authorization.response.AuthorizedResourceResp;
import com.tencent.supersonic.auth.api.authorization.service.AuthService;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthGroup;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthRule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private JdbcTemplate jdbcTemplate;

    private UserService userService;

    public AuthServiceImpl(JdbcTemplate jdbcTemplate,
                           UserService userService) {
        this.jdbcTemplate = jdbcTemplate;
        this.userService = userService;
    }

    private List<AuthGroup> load() {
        List<String> rows = jdbcTemplate.queryForList("select config from s2_auth_groups", String.class);
        Gson g = new Gson();
        return rows.stream().map(row -> g.fromJson(row, AuthGroup.class)).collect(Collectors.toList());
    }

    @Override
    public List<AuthGroup> queryAuthGroups(String modelId, Integer groupId) {
        return load().stream()
                .filter(group -> (Objects.isNull(groupId) || groupId.equals(group.getGroupId()))
                        && modelId.equals(group.getModelId()))
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
            jdbcTemplate.update("insert into s2_auth_groups (group_id, config) values (?, ?);", nextGroupId,
                    g.toJson(group));
        } else {
            jdbcTemplate.update("update s2_auth_groups set config = ? where group_id = ?;", g.toJson(group),
                    group.getGroupId());
        }
    }

    @Override
    public void removeAuthGroup(AuthGroup group) {
        jdbcTemplate.update("delete from s2_auth_groups where group_id = ?", group.getGroupId());
    }


    @Override
    public AuthorizedResourceResp queryAuthorizedResources(QueryAuthResReq req, User user) {
        Set<String> userOrgIds = userService.getUserAllOrgId(user.getName());
        if (!CollectionUtils.isEmpty(userOrgIds)) {
            req.setDepartmentIds(new ArrayList<>(userOrgIds));
        }
        List<AuthGroup> groups = getAuthGroups(req, user.getName());
        AuthorizedResourceResp resource = new AuthorizedResourceResp();
        Map<String, List<AuthGroup>> authGroupsByModelId = groups.stream()
                .collect(Collectors.groupingBy(AuthGroup::getModelId));
        Map<String, List<AuthRes>> reqAuthRes = req.getResources().stream()
                .collect(Collectors.groupingBy(AuthRes::getModelId));

        for (String modelId : reqAuthRes.keySet()) {
            List<AuthRes> reqResourcesList = reqAuthRes.get(modelId);
            AuthResGrp rg = new AuthResGrp();
            if (authGroupsByModelId.containsKey(modelId)) {
                List<AuthGroup> authGroups = authGroupsByModelId.get(modelId);
                for (AuthRes reqRes : reqResourcesList) {
                    for (AuthGroup authRuleGroup : authGroups) {
                        List<AuthRule> authRules = authRuleGroup.getAuthRules();
                        List<String> allAuthItems = new ArrayList<>();
                        authRules.forEach(authRule -> allAuthItems.addAll(authRule.resourceNames()));

                        if (allAuthItems.contains(reqRes.getName())) {
                            rg.getGroup().add(reqRes);
                        }

                    }
                }
            }
            if (!CollectionUtils.isEmpty(rg.getGroup())) {
                resource.getResources().add(rg);
            }
        }

        if (StringUtils.isNotEmpty(req.getModelId())) {
            List<AuthGroup> authGroups = authGroupsByModelId.get(req.getModelId());
            if (!CollectionUtils.isEmpty(authGroups)) {
                for (AuthGroup group : authGroups) {
                    if (group.getDimensionFilters() != null
                            && group.getDimensionFilters().stream().anyMatch(expr -> !Strings.isNullOrEmpty(expr))) {
                        DimensionFilter df = new DimensionFilter();
                        df.setDescription(group.getDimensionFilterDescription());
                        df.setExpressions(group.getDimensionFilters());
                        resource.getFilters().add(df);
                    }
                }
            }
        }
        return resource;
    }

    private List<AuthGroup> getAuthGroups(QueryAuthResReq req, String userName) {
        List<AuthGroup> groups = load().stream()
                .filter(group -> {
                    if (!Objects.equals(group.getModelId(), req.getModelId())) {
                        return false;
                    }
                    if (!CollectionUtils.isEmpty(group.getAuthorizedUsers()) && group.getAuthorizedUsers()
                            .contains(userName)) {
                        return true;
                    }
                    for (String departmentId : req.getDepartmentIds()) {
                        if (!CollectionUtils.isEmpty(group.getAuthorizedDepartmentIds())
                                && group.getAuthorizedDepartmentIds().contains(departmentId)) {
                            return true;
                        }
                    }
                    return false;
                }).collect(Collectors.toList());
        log.info("user:{} department:{} authGroups:{}", userName, req.getDepartmentIds(), groups);
        return groups;
    }

}
