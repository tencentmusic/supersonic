package com.tencent.supersonic.headless.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthRes;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthRule;
import com.tencent.supersonic.auth.api.authorization.pojo.DimensionFilter;
import com.tencent.supersonic.auth.api.authorization.request.QueryAuthResReq;
import com.tencent.supersonic.auth.api.authorization.response.AuthorizedResourceResp;
import com.tencent.supersonic.auth.api.authorization.service.AuthService;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AuthChangeType;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.common.service.AuthAuditService;
import com.tencent.supersonic.common.util.RowFilterValidator;
import com.tencent.supersonic.headless.api.pojo.DataSetAuthGroup;
import com.tencent.supersonic.headless.api.pojo.response.DataSetResp;
import com.tencent.supersonic.headless.api.pojo.response.DomainResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.api.service.DataSetService;
import com.tencent.supersonic.headless.server.persistence.dataobject.DataSetAuthGroupDO;
import com.tencent.supersonic.headless.server.persistence.mapper.DataSetAuthGroupDOMapper;
import com.tencent.supersonic.headless.server.service.DataSetAuthService;
import com.tencent.supersonic.headless.server.service.DomainService;
import com.tencent.supersonic.headless.server.service.ModelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DataSetAuthServiceImpl implements DataSetAuthService {

    private final DataSetAuthGroupDOMapper authGroupMapper;
    private final UserService userService;
    private final AuthAuditService authAuditService;
    private final Gson gson = new Gson();

    @Lazy
    @Autowired
    private DataSetService dataSetService;

    @Lazy
    @Autowired
    private DomainService domainService;

    @Lazy
    @Autowired
    private ModelService modelService;

    @Lazy
    @Autowired
    private AuthService authService;

    public DataSetAuthServiceImpl(DataSetAuthGroupDOMapper authGroupMapper, UserService userService,
            AuthAuditService authAuditService) {
        this.authGroupMapper = authGroupMapper;
        this.userService = userService;
        this.authAuditService = authAuditService;
    }

    @Override
    public List<DataSetAuthGroup> queryAuthGroups(Long datasetId, Long groupId) {
        LambdaQueryWrapper<DataSetAuthGroupDO> wrapper = new LambdaQueryWrapper<>();
        if (datasetId != null) {
            wrapper.eq(DataSetAuthGroupDO::getDatasetId, datasetId);
        }
        if (groupId != null) {
            wrapper.eq(DataSetAuthGroupDO::getGroupId, groupId);
        }
        return authGroupMapper.selectList(wrapper).stream().map(this::convertToGroup)
                .collect(Collectors.toList());
    }

    @Override
    public DataSetAuthGroup createAuthGroup(DataSetAuthGroup group, User user) {
        validateDimensionFilters(group.getDimensionFilters());
        DataSetAuthGroupDO groupDO = convertToDO(group);
        groupDO.setCreatedAt(new Date());
        groupDO.setCreatedBy(user.getName());
        groupDO.setUpdatedAt(new Date());
        groupDO.setUpdatedBy(user.getName());
        authGroupMapper.insert(groupDO);
        group.setGroupId(groupDO.getGroupId());

        // Audit log for create
        authAuditService.logAuthChange(AuthChangeType.CREATE, "DATASET", group.getDatasetId(),
                groupDO.getGroupId() != null ? groupDO.getGroupId().intValue() : null,
                user.getName(), null, gson.toJson(group),
                "Created dataset auth group: " + group.getName());

        return group;
    }

    @Override
    public void updateAuthGroup(DataSetAuthGroup group, User user) {
        validateDimensionFilters(group.getDimensionFilters());

        // Get old value for audit
        DataSetAuthGroupDO oldGroupDO = authGroupMapper.selectById(group.getGroupId());
        String oldValue = oldGroupDO != null ? gson.toJson(convertToGroup(oldGroupDO)) : null;

        DataSetAuthGroupDO groupDO = convertToDO(group);
        groupDO.setUpdatedAt(new Date());
        groupDO.setUpdatedBy(user.getName());
        authGroupMapper.updateById(groupDO);

        // Audit log for update
        authAuditService.logAuthChange(AuthChangeType.UPDATE, "DATASET", group.getDatasetId(),
                group.getGroupId() != null ? group.getGroupId().intValue() : null, user.getName(),
                oldValue, gson.toJson(group), "Updated dataset auth group: " + group.getName());
    }

    private void validateDimensionFilters(List<String> dimensionFilters) {
        if (CollectionUtils.isEmpty(dimensionFilters)) {
            return;
        }
        for (String filter : dimensionFilters) {
            RowFilterValidator.ValidationResult result = RowFilterValidator.validate(filter);
            if (!result.isValid()) {
                throw new RuntimeException(
                        "Invalid row filter expression: " + result.getErrorMessage());
            }
        }
    }

    @Override
    public void removeAuthGroup(Long groupId, User user) {
        // Get old value for audit
        DataSetAuthGroupDO oldGroupDO = authGroupMapper.selectById(groupId);
        Long datasetId = oldGroupDO != null ? oldGroupDO.getDatasetId() : null;
        String oldValue = oldGroupDO != null ? gson.toJson(convertToGroup(oldGroupDO)) : null;
        String groupName = oldGroupDO != null ? oldGroupDO.getName() : "unknown";

        authGroupMapper.deleteById(groupId);

        // Audit log for delete
        authAuditService.logAuthChange(AuthChangeType.DELETE, "DATASET", datasetId,
                groupId.intValue(), user.getName(), oldValue, null,
                "Deleted dataset auth group: " + groupName);
    }

    @Override
    public AuthorizedResourceResp queryAuthorizedResources(Long datasetId, User user) {
        AuthorizedResourceResp resource = new AuthorizedResourceResp();
        if (datasetId == null) {
            return resource;
        }

        Set<String> userOrgIds = userService.getUserAllOrgId(user.getName());
        List<DataSetAuthGroup> groups =
                getAuthGroupsForUser(datasetId, user.getName(), new ArrayList<>(userOrgIds));

        // Check if any group has inheritFromModel enabled
        boolean shouldInheritFromModel = groups.stream()
                .anyMatch(g -> g.getInheritFromModel() != null && g.getInheritFromModel() == 1);

        // If inherit from model is enabled, merge model-level permissions
        if (shouldInheritFromModel) {
            DataSetResp dataSet = dataSetService.getDataSet(datasetId);
            if (dataSet != null) {
                List<Long> modelIds = dataSet.getAllModels();
                if (!CollectionUtils.isEmpty(modelIds)) {
                    QueryAuthResReq queryAuthResReq = new QueryAuthResReq();
                    queryAuthResReq.setModelIds(modelIds);
                    AuthorizedResourceResp modelResource =
                            authService.queryAuthorizedResources(queryAuthResReq, user);
                    if (modelResource != null) {
                        resource.getAuthResList().addAll(modelResource.getAuthResList());
                        resource.getFilters().addAll(modelResource.getFilters());
                    }
                }
            }
        }

        // Add dataset-level permissions
        for (DataSetAuthGroup group : groups) {
            if (!CollectionUtils.isEmpty(group.getAuthRules())) {
                for (AuthRule authRule : group.getAuthRules()) {
                    for (String resBizName : authRule.resourceNames()) {
                        resource.getAuthResList().add(new AuthRes(datasetId, resBizName));
                    }
                }
            }

            if (!CollectionUtils.isEmpty(group.getDimensionFilters())) {
                DimensionFilter df = new DimensionFilter();
                df.setDescription(group.getDimensionFilterDescription());
                df.setExpressions(group.getDimensionFilters());
                resource.getFilters().add(df);
            }
        }

        return resource;
    }

    @Override
    public boolean checkDataSetViewPermission(Long datasetId, User user) {
        if (user.isSuperAdmin()) {
            return true;
        }

        DataSetResp dataSet = dataSetService.getDataSet(datasetId);
        if (dataSet == null) {
            return false;
        }

        if (checkDataSetAdminPermission(datasetId, user)) {
            return true;
        }

        if (dataSet.openToAll()) {
            return true;
        }

        String userName = user.getName();
        if (dataSet.getViewers().contains(userName)) {
            return true;
        }

        Set<String> userOrgIds = userService.getUserAllOrgId(userName);
        for (String orgId : userOrgIds) {
            if (dataSet.getViewOrgs().contains(orgId)) {
                return true;
            }
        }

        List<Long> modelIds = dataSet.getAllModels();
        if (!CollectionUtils.isEmpty(modelIds)) {
            List<ModelResp> models = modelService.getModelListWithAuth(user, null, AuthType.VIEWER);
            Set<Long> viewableModelIds =
                    models.stream().map(ModelResp::getId).collect(Collectors.toSet());
            for (Long modelId : modelIds) {
                if (viewableModelIds.contains(modelId)) {
                    return true;
                }
            }
        }

        Set<DomainResp> domains = domainService.getDomainAuthSet(user, AuthType.VIEWER);
        if (!CollectionUtils.isEmpty(domains)) {
            Set<Long> viewableDomainIds =
                    domains.stream().map(DomainResp::getId).collect(Collectors.toSet());
            if (viewableDomainIds.contains(dataSet.getDomainId())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean checkDataSetAdminPermission(Long datasetId, User user) {
        if (user.isSuperAdmin()) {
            return true;
        }

        DataSetResp dataSet = dataSetService.getDataSet(datasetId);
        if (dataSet == null) {
            return false;
        }

        String userName = user.getName();
        if (dataSet.getAdmins().contains(userName)
                || Objects.equals(dataSet.getCreatedBy(), userName)) {
            return true;
        }

        Set<String> userOrgIds = userService.getUserAllOrgId(userName);
        for (String orgId : userOrgIds) {
            if (dataSet.getAdminOrgs().contains(orgId)) {
                return true;
            }
        }

        Set<DomainResp> domains = domainService.getDomainAuthSet(user, AuthType.ADMIN);
        if (!CollectionUtils.isEmpty(domains)) {
            Set<Long> adminDomainIds =
                    domains.stream().map(DomainResp::getId).collect(Collectors.toSet());
            if (adminDomainIds.contains(dataSet.getDomainId())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public List<String> getRowFilters(Long datasetId, User user) {
        List<String> filters = new ArrayList<>();
        if (datasetId == null) {
            return filters;
        }

        Set<String> userOrgIds = userService.getUserAllOrgId(user.getName());
        List<DataSetAuthGroup> groups =
                getAuthGroupsForUser(datasetId, user.getName(), new ArrayList<>(userOrgIds));

        for (DataSetAuthGroup group : groups) {
            if (!CollectionUtils.isEmpty(group.getDimensionFilters())) {
                filters.addAll(group.getDimensionFilters());
            }
        }

        return filters;
    }

    private List<DataSetAuthGroup> getAuthGroupsForUser(Long datasetId, String userName,
            List<String> departmentIds) {
        List<DataSetAuthGroup> allGroups = queryAuthGroups(datasetId, null);
        return allGroups.stream().filter(group -> {
            if (!CollectionUtils.isEmpty(group.getAuthorizedUsers())
                    && group.getAuthorizedUsers().contains(userName)) {
                return true;
            }
            for (String deptId : departmentIds) {
                if (!CollectionUtils.isEmpty(group.getAuthorizedDepartmentIds())
                        && group.getAuthorizedDepartmentIds().contains(deptId)) {
                    return true;
                }
            }
            return false;
        }).collect(Collectors.toList());
    }

    private DataSetAuthGroup convertToGroup(DataSetAuthGroupDO groupDO) {
        DataSetAuthGroup group = new DataSetAuthGroup();
        group.setGroupId(groupDO.getGroupId());
        group.setDatasetId(groupDO.getDatasetId());
        group.setName(groupDO.getName());
        group.setDimensionFilterDescription(groupDO.getDimensionFilterDescription());
        group.setInheritFromModel(groupDO.getInheritFromModel());

        if (groupDO.getAuthRules() != null) {
            group.setAuthRules(gson.fromJson(groupDO.getAuthRules(),
                    new TypeToken<List<AuthRule>>() {}.getType()));
        }
        if (groupDO.getDimensionFilters() != null) {
            group.setDimensionFilters(gson.fromJson(groupDO.getDimensionFilters(),
                    new TypeToken<List<String>>() {}.getType()));
        }
        if (groupDO.getAuthorizedUsers() != null) {
            group.setAuthorizedUsers(gson.fromJson(groupDO.getAuthorizedUsers(),
                    new TypeToken<List<String>>() {}.getType()));
        }
        if (groupDO.getAuthorizedDepartmentIds() != null) {
            group.setAuthorizedDepartmentIds(gson.fromJson(groupDO.getAuthorizedDepartmentIds(),
                    new TypeToken<List<String>>() {}.getType()));
        }

        return group;
    }

    private DataSetAuthGroupDO convertToDO(DataSetAuthGroup group) {
        DataSetAuthGroupDO groupDO = new DataSetAuthGroupDO();
        groupDO.setGroupId(group.getGroupId());
        groupDO.setDatasetId(group.getDatasetId());
        groupDO.setName(group.getName());
        groupDO.setDimensionFilterDescription(group.getDimensionFilterDescription());
        groupDO.setInheritFromModel(group.getInheritFromModel());

        if (group.getAuthRules() != null) {
            groupDO.setAuthRules(gson.toJson(group.getAuthRules()));
        }
        if (group.getDimensionFilters() != null) {
            groupDO.setDimensionFilters(gson.toJson(group.getDimensionFilters()));
        }
        if (group.getAuthorizedUsers() != null) {
            groupDO.setAuthorizedUsers(gson.toJson(group.getAuthorizedUsers()));
        }
        if (group.getAuthorizedDepartmentIds() != null) {
            groupDO.setAuthorizedDepartmentIds(gson.toJson(group.getAuthorizedDepartmentIds()));
        }

        return groupDO;
    }
}
