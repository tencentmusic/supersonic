package com.tencent.supersonic.auth.authorization.application;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthRes;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthResGrp;
import com.tencent.supersonic.auth.api.authorization.pojo.DimensionFilter;
import com.tencent.supersonic.auth.api.authorization.request.QueryAuthResReq;
import com.tencent.supersonic.auth.api.authorization.response.AuthorizedResourceResp;
import com.tencent.supersonic.auth.authorization.domain.pojo.AuthGroup;
import com.tencent.supersonic.auth.authorization.domain.pojo.AuthRule;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
@Slf4j
public class AuthApplicationService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private List<AuthGroup> load() {
        List<String> rows = jdbcTemplate.queryForList("select config from s2_auth_groups", String.class);
        Gson g = new Gson();
        return rows.stream().map(row -> g.fromJson(row, AuthGroup.class)).collect(Collectors.toList());
    }

    public List<AuthGroup> queryAuthGroups(String domainId, Integer groupId) {
        return load().stream()
                .filter(group -> (Objects.isNull(groupId) || groupId.equals(group.getGroupId()))
                        && domainId.equals(group.getDomainId()))
                .collect(Collectors.toList());
    }

    public void updateAuthGroup(AuthGroup group) {
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

    public AuthorizedResourceResp queryAuthorizedResources(QueryAuthResReq req, HttpServletRequest request) {
        List<AuthGroup> groups = load().stream().
                filter(group -> group.getAuthorizedUsers().contains(req.getUser()) && req.getDomainId()
                        .equals(group.getDomainId())).
                collect(Collectors.toList());
        AuthorizedResourceResp resource = new AuthorizedResourceResp();
        Map<String, List<AuthGroup>> authGroupsByDomainId = groups.stream()
                .collect(Collectors.groupingBy(AuthGroup::getDomainId));
        Map<String, List<AuthRes>> reqAuthRes = req.getResources().stream()
                .collect(Collectors.groupingBy(AuthRes::getDomainId));

        for (String domainId : reqAuthRes.keySet()) {
            List<AuthRes> reqResourcesList = reqAuthRes.get(domainId);
            AuthResGrp rg = new AuthResGrp();
            if (authGroupsByDomainId.containsKey(domainId)) {
                List<AuthGroup> authGroups = authGroupsByDomainId.get(domainId);
                for (AuthRes reqRes : reqResourcesList) {
                    for (AuthGroup authRuleGroup : authGroups) {
                        List<AuthRule> authRules = authRuleGroup.getAuthRules();
                        List<String> allAuthItems = new ArrayList<>();
                        authRules.stream().forEach(authRule -> allAuthItems.addAll(authRule.resourceNames()));

                        if (allAuthItems.contains(reqRes.getName())) {
                            rg.getGroup().add(reqRes);
                        }

                    }
                }
            }
            if (Objects.nonNull(rg) && !CollectionUtils.isEmpty(rg.getGroup())) {
                resource.getResources().add(rg);
            }
        }

        if (StringUtils.isNotEmpty(req.getDomainId())) {
            List<AuthGroup> authGroups = authGroupsByDomainId.get(req.getDomainId());
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

    public void removeAuthGroup(AuthGroup group) {
        jdbcTemplate.update("delete from s2_auth_groups where group_id = ?", group.getGroupId());
    }
}
