package com.tencent.supersonic.headless.server.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.exception.InvalidArgumentException;
import com.tencent.supersonic.common.pojo.exception.InvalidPermissionException;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.common.util.PageUtils;
import com.tencent.supersonic.headless.api.pojo.enums.AppStatus;
import com.tencent.supersonic.headless.api.pojo.AppConfig;
import com.tencent.supersonic.headless.api.pojo.request.AppQueryReq;
import com.tencent.supersonic.headless.api.pojo.request.AppReq;
import com.tencent.supersonic.headless.api.pojo.response.AppDetailResp;
import com.tencent.supersonic.headless.api.pojo.response.AppResp;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.AppDO;
import com.tencent.supersonic.headless.server.persistence.mapper.AppMapper;
import com.tencent.supersonic.headless.server.pojo.MetaFilter;
import com.tencent.supersonic.headless.server.service.AppService;
import com.tencent.supersonic.headless.server.service.DimensionService;
import com.tencent.supersonic.headless.server.service.MetricService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, AppDO> implements AppService {

    private AppMapper appMapper;

    private MetricService metricService;

    private DimensionService dimensionService;

    public AppServiceImpl(AppMapper appMapper, MetricService metricService,
                          DimensionService dimensionService) {
        this.appMapper = appMapper;
        this.metricService = metricService;
        this.dimensionService = dimensionService;
    }

    @Override
    public AppDetailResp save(AppReq app, User user) {
        app.createdBy(user.getName());
        AppDO appDO = new AppDO();
        BeanMapper.mapper(app, appDO);
        appDO.setStatus(AppStatus.INIT.getCode());
        appDO.setConfig(JSONObject.toJSONString(app.getConfig()));
        appDO.setAppSecret(getUniqueId());
        appMapper.insert(appDO);
        return convertDetail(appDO);
    }

    @Override
    public AppDetailResp update(AppReq app, User user) {
        app.updatedBy(user.getName());
        AppDO appDO = getById(app.getId());
        checkAuth(appDO, user);
        BeanMapper.mapper(app, appDO);
        appDO.setConfig(JSONObject.toJSONString(app.getConfig()));
        appMapper.updateById(appDO);
        return convertDetail(appDO);
    }

    @Override
    public void online(Integer id, User user) {
        AppDO appDO = getAppDO(id);
        checkAuth(appDO, user);
        appDO.setStatus(AppStatus.ONLINE.getCode());
        appDO.setUpdatedAt(new Date());
        appDO.setUpdatedBy(user.getName());
        updateById(appDO);
    }

    @Override
    public void offline(Integer id, User user) {
        AppDO appDO = getAppDO(id);
        checkAuth(appDO, user);
        appDO.setStatus(AppStatus.OFFLINE.getCode());
        appDO.setUpdatedAt(new Date());
        appDO.setUpdatedBy(user.getName());
        updateById(appDO);
    }

    @Override
    public void delete(Integer id, User user) {
        AppDO appDO = getAppDO(id);
        checkAuth(appDO, user);
        appDO.setStatus(AppStatus.DELETED.getCode());
        appDO.setUpdatedAt(new Date());
        appDO.setUpdatedBy(user.getName());
        updateById(appDO);
    }

    @Override
    public PageInfo<AppResp> pageApp(AppQueryReq appQueryReq, User user) {
        PageInfo<AppDO> appDOPageInfo = PageHelper.startPage(appQueryReq.getCurrent(),
                        appQueryReq.getPageSize())
                .doSelectPageInfo(() -> queryApp(appQueryReq));
        PageInfo<AppResp> appPageInfo = PageUtils.pageInfo2PageInfoVo(appDOPageInfo);
        Map<Long, MetricResp> metricResps = metricService.getMetrics(new MetaFilter())
                .stream().collect(Collectors.toMap(MetricResp::getId, m -> m));
        Map<Long, DimensionResp> dimensionResps = dimensionService.getDimensions(new MetaFilter())
                .stream().collect(Collectors.toMap(DimensionResp::getId, m -> m));
        appPageInfo.setList(appDOPageInfo.getList().stream().map(appDO
                        -> convert(appDO, dimensionResps, metricResps, user))
                .collect(Collectors.toList()));
        return appPageInfo;
    }

    public List<AppDO> queryApp(AppQueryReq appQueryReq) {
        QueryWrapper<AppDO> appDOQueryWrapper = new QueryWrapper<>();
        appDOQueryWrapper.lambda().ne(AppDO::getStatus, AppStatus.DELETED.getCode());
        if (StringUtils.isNotBlank(appQueryReq.getName())) {
            appDOQueryWrapper.lambda().like(AppDO::getName, appQueryReq.getName());
        }
        if (!CollectionUtils.isEmpty(appQueryReq.getStatus())) {
            appDOQueryWrapper.lambda().in(AppDO::getStatus, appQueryReq.getStatus());
        }
        if (StringUtils.isNotBlank(appQueryReq.getCreatedBy())) {
            appDOQueryWrapper.lambda().eq(AppDO::getCreatedBy, appQueryReq.getCreatedBy());
        }
        return list(appDOQueryWrapper);
    }

    @Override
    public AppDetailResp getApp(Integer id, User user) {
        AppDO appDO = getAppDO(id);
        Map<Long, MetricResp> metricResps = metricService.getMetrics(new MetaFilter())
                .stream().collect(Collectors.toMap(MetricResp::getId, m -> m));
        Map<Long, DimensionResp> dimensionResps = dimensionService.getDimensions(new MetaFilter())
                .stream().collect(Collectors.toMap(DimensionResp::getId, m -> m));
        checkAuth(appDO, user);
        return convertDetail(appDO, dimensionResps, metricResps);
    }

    @Override
    public AppDetailResp getApp(Integer id) {
        AppDO appDO = getAppDO(id);
        return convertDetail(appDO, new HashMap<>(), new HashMap<>());
    }

    private AppDO getAppDO(Integer id) {
        AppDO appDO = getById(id);
        if (appDO == null) {
            throw new InvalidArgumentException("该应用不存在");
        }
        return appDO;
    }

    private void checkAuth(AppDO appDO, User user) {
        if (!hasAuth(appDO, user)) {
            throw new InvalidPermissionException("您不是该应用的负责人, 暂无权查看或者修改");
        }
    }

    private boolean hasAuth(AppDO appDO, User user) {
        if (appDO.getCreatedBy().equalsIgnoreCase(user.getName())) {
            return true;
        }
        return StringUtils.isNotBlank(appDO.getOwner())
                && appDO.getOwner().contains(user.getName());
    }

    private AppResp convert(AppDO appDO, Map<Long, DimensionResp> dimensionMap,
                            Map<Long, MetricResp> metricMap, User user) {
        AppResp app = new AppResp();
        BeanMapper.mapper(appDO, app);
        AppConfig appConfig = JSONObject.parseObject(appDO.getConfig(), AppConfig.class);
        fillItemName(appConfig, dimensionMap, metricMap);
        app.setConfig(appConfig);
        app.setAppStatus(AppStatus.fromCode(appDO.getStatus()));
        app.setHasAdminRes(hasAuth(appDO, user));
        return app;
    }

    private AppDetailResp convertDetail(AppDO appDO) {
        return convertDetail(appDO, new HashMap<>(), new HashMap<>());
    }

    private AppDetailResp convertDetail(AppDO appDO, Map<Long, DimensionResp> dimensionMap,
                                        Map<Long, MetricResp> metricMap) {
        AppDetailResp app = new AppDetailResp();
        BeanMapper.mapper(appDO, app);
        AppConfig appConfig = JSONObject.parseObject(appDO.getConfig(), AppConfig.class);
        fillItemName(appConfig, dimensionMap, metricMap);
        app.setConfig(appConfig);
        app.setAppStatus(AppStatus.fromCode(appDO.getStatus()));
        return app;
    }

    private void fillItemName(AppConfig appConfig, Map<Long, DimensionResp> dimensionMap,
                              Map<Long, MetricResp> metricMap) {
        appConfig.getItems().forEach(metricItem -> {
            metricItem.setName(metricMap.getOrDefault(metricItem.getId(), new MetricResp()).getName());
            metricItem.setBizName(metricMap.getOrDefault(metricItem.getId(), new MetricResp()).getBizName());
            metricItem.setCreatedBy(metricMap.getOrDefault(metricItem.getId(), new MetricResp()).getCreatedBy());
            metricItem.getRelateItems().forEach(dimensionItem -> {
                dimensionItem.setName(dimensionMap.getOrDefault(dimensionItem.getId(), new DimensionResp()).getName());
                dimensionItem.setBizName(dimensionMap.getOrDefault(dimensionItem.getId(),
                        new DimensionResp()).getBizName());
                dimensionItem.setCreatedBy(dimensionMap.getOrDefault(dimensionItem.getId(),
                        new DimensionResp()).getCreatedBy());
            });
        });
    }

    private String getUniqueId() {
        return UUID.randomUUID().toString().replaceAll("_", "");
    }

}
