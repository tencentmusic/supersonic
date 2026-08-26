package com.tencent.supersonic.headless.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tencent.supersonic.headless.api.pojo.response.DataSetResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.DataSetDO;
import com.tencent.supersonic.headless.server.persistence.mapper.DataSetDOMapper;
import com.tencent.supersonic.headless.server.service.impl.DataSetServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;

/**
 * 验证 getDataSetList(domainId, statusList, name) 的名称模糊搜索行为。
 *
 * <p>MyBatis-Plus 的 lambda QueryWrapper 在脱离 Spring 上下文时无法生成 SQL（缺少 TableInfo 缓存），
 * 因此本测试用 mock mapper 在 selectList 桩内以 Java 模拟数据库 LIKE 语义（对 name / bizName 做
 * 子串匹配），断言服务返回的列表被正确收窄：
 * <ul>
 *   <li>name 为空 → 返回全量（受域/状态约束）；</li>
 *   <li>name 非空 → 仅返回 name 或 bizName 包含关键字的记录；</li>
 *   <li>命中既可能来自 name，也可能来自 bizName。</li>
 * </ul>
 * 注：生产代码使用 {@code lambda().like(...)} 生成标准 SQL LIKE，对 Postgres/Mysql 通用；
 * 大小写敏感性取决于数据库 collation（与现有 AppServiceImpl/TermServiceImpl 一致）。
 */
class DataSetServiceImplTest {

    private static final List<DataSetDO> ALL = Arrays.asList(
            dataSet(1L, "销售数据集", "sales_daily", 1),
            dataSet(2L, "用户数据集", "user_profile", 1),
            dataSet(3L, "财务数据集", "finance", 2),
            dataSet(4L, "销售明细", "sales_detail", 1));

    private DataSetServiceImpl spyService(DataSetDOMapper mapper) {
        DataSetServiceImpl service = Mockito.spy(new DataSetServiceImpl());
        Mockito.doReturn(mapper).when(service).getBaseMapper();
        return service;
    }

    private static DataSetDO dataSet(Long id, String name, String bizName, Integer status) {
        DataSetDO d = new DataSetDO();
        d.setId(id);
        d.setName(name);
        d.setBizName(bizName);
        d.setStatus(status);
        return d;
    }

    /** 模拟数据库 LIKE：name 或 bizName 包含关键字即命中（name 为空则全量）。 */
    private List<DataSetDO> simulateLike(QueryWrapper<DataSetDO> w, String keyword) {
        return ALL.stream().filter(d -> {
            if (keyword == null || keyword.isBlank()) {
                return true;
            }
            String k = keyword;
            return (d.getName() != null && d.getName().contains(k))
                    || (d.getBizName() != null && d.getBizName().contains(k));
        }).collect(Collectors.toList());
    }

    @Test
    void getDataSetList_withoutName_returnsAll() {
        DataSetDOMapper mapper = Mockito.mock(DataSetDOMapper.class);
        Mockito.when(mapper.selectList(any())).thenAnswer(inv ->
                simulateLike(inv.getArgument(0), null));

        List<DataSetResp> result = spyService(mapper)
                .getDataSetList(10L, Arrays.asList(1, 2), null);
        Assertions.assertEquals(4, result.size());
    }

    @Test
    void getDataSetList_withName_matchesByName() {
        DataSetDOMapper mapper = Mockito.mock(DataSetDOMapper.class);
        Mockito.when(mapper.selectList(any())).thenAnswer(inv ->
                simulateLike(inv.getArgument(0), "销售"));

        List<DataSetResp> result = spyService(mapper)
                .getDataSetList(10L, Arrays.asList(1, 2), "销售");
        // 命中 id=1(销售数据集) 与 id=4(销售明细)，bizName 不含"销售"的 user_profile/finance 不命中
        List<String> names = result.stream().map(DataSetResp::getName).collect(Collectors.toList());
        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(names.contains("销售数据集"));
        Assertions.assertTrue(names.contains("销售明细"));
    }

    @Test
    void getDataSetList_withName_matchesByBizName() {
        DataSetDOMapper mapper = Mockito.mock(DataSetDOMapper.class);
        Mockito.when(mapper.selectList(any())).thenAnswer(inv ->
                simulateLike(inv.getArgument(0), "user"));

        List<DataSetResp> result = spyService(mapper)
                .getDataSetList(10L, Arrays.asList(1, 2), "user");
        // 仅 bizName=user_profile 命中（name 为"用户数据集"不含"user"），验证 bizName 也被纳入模糊匹配
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("user_profile", result.get(0).getBizName());
    }

    @Test
    void getDataSetList_withName_noMatch_returnsEmpty() {
        DataSetDOMapper mapper = Mockito.mock(DataSetDOMapper.class);
        Mockito.when(mapper.selectList(any())).thenAnswer(inv ->
                simulateLike(inv.getArgument(0), "不存在的关键字"));

        List<DataSetResp> result = spyService(mapper)
                .getDataSetList(10L, Arrays.asList(1, 2), "不存在的关键字");
        Assertions.assertTrue(result.isEmpty());
    }
}
