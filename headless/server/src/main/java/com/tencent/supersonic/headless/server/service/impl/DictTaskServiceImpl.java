package com.tencent.supersonic.headless.server.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.pojo.enums.TaskStatusEnum;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.headless.api.pojo.DimValueMap;
import com.tencent.supersonic.headless.api.pojo.request.DictItemFilter;
import com.tencent.supersonic.headless.api.pojo.request.DictSingleTaskReq;
import com.tencent.supersonic.headless.api.pojo.request.DictValueReq;
import com.tencent.supersonic.headless.api.pojo.request.ValueTaskQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.DictItemResp;
import com.tencent.supersonic.headless.api.pojo.response.DictTaskResp;
import com.tencent.supersonic.headless.api.pojo.response.DictValueDimResp;
import com.tencent.supersonic.headless.api.pojo.response.DictValueResp;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.chat.knowledge.DictWord;
import com.tencent.supersonic.headless.chat.knowledge.file.FileHandler;
import com.tencent.supersonic.headless.server.persistence.dataobject.DictTaskDO;
import com.tencent.supersonic.headless.server.persistence.repository.DictRepository;
import com.tencent.supersonic.headless.server.service.DictTaskService;
import com.tencent.supersonic.headless.server.service.DimensionService;
import com.tencent.supersonic.headless.server.utils.DictUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DictTaskServiceImpl implements DictTaskService {

    @Value("${dict.flush.enable:true}")
    private Boolean dictFlushEnable;

    @Value("${dict.flush.daily.enable:true}")
    private Boolean dictFlushDailyEnable;

    @Value("${dict.file.type:txt}")
    private String dictFileType;

    private String dimValue = "DimValue_%d_%d";

    private final DictRepository dictRepository;
    private final DictUtils dictConverter;
    private final DictUtils dictUtils;
    private final FileHandler fileHandler;
    private final DictWordService dictWordService;
    private final DimensionService dimensionService;

    public DictTaskServiceImpl(DictRepository dictRepository, DictUtils dictConverter,
            DictUtils dictUtils, FileHandler fileHandler, DictWordService dictWordService,
            DimensionService dimensionService) {
        this.dictRepository = dictRepository;
        this.dictConverter = dictConverter;
        this.dictUtils = dictUtils;
        this.fileHandler = fileHandler;
        this.dictWordService = dictWordService;
        this.dimensionService = dimensionService;
    }

    @Override
    public Long addDictTask(DictSingleTaskReq taskReq, User user) {
        if (!dictFlushEnable) {
            return 0L;
        }
        DictItemResp dictItemResp = fetchDictItemResp(taskReq);
        return handleDictTaskByItemResp(dictItemResp, user);
    }

    private Long handleDictTaskByItemResp(DictItemResp dictItemResp, User user) {
        DictTaskDO dictTaskDO =
                dictConverter.generateDictTaskDO(dictItemResp, user, TaskStatusEnum.PENDING);
        log.info("[addDictTask] dictTaskDO:{}", dictTaskDO);
        dictRepository.addDictTask(dictTaskDO);
        Long idInDb = dictTaskDO.getId();
        dictItemResp.setId(idInDb);
        runDictTask(dictItemResp, user);
        return idInDb;
    }

    private DictItemResp fetchDictItemResp(DictSingleTaskReq taskReq) {
        DictItemFilter dictItemFilter = DictItemFilter.builder().itemId(taskReq.getItemId())
                .type(taskReq.getType()).build();
        List<DictItemResp> dictItemRespList = dictRepository.queryDictConf(dictItemFilter);
        if (!CollectionUtils.isEmpty(dictItemRespList)) {
            return dictItemRespList.get(0);
        }
        return null;
    }

    private void runDictTask(DictItemResp dictItemResp, User user) {
        if (Objects.isNull(dictItemResp)) {
            return;
        }

        DictTaskDO dictTaskDO = dictRepository.queryDictTaskById(dictItemResp.getId());
        dictTaskDO.setStatus(TaskStatusEnum.RUNNING.getStatus());
        dictRepository.editDictTask(dictTaskDO);

        // 1.Generate item dictionary data
        List<String> data = dictUtils.fetchItemValue(dictItemResp);

        // 2.Change dictionary file
        String fileName = dictItemResp.fetchDictFileName() + Constants.DOT + dictFileType;
        fileHandler.writeFile(data, fileName, false);

        // 3.Change in-memory dictionary data in real time
        String status = TaskStatusEnum.SUCCESS.getStatus();
        try {
            dictWordService.loadDictWord();
        } catch (Exception e) {
            log.error("reloadCustomDictionary error", e);
            status = TaskStatusEnum.ERROR.getStatus();
            dictTaskDO.setDescription(e.toString());
        }
        dictTaskDO.setStatus(status);
        dictTaskDO.setElapsedMs(DateUtils.calculateDiffMs(dictTaskDO.getCreatedAt()));
        dictRepository.editDictTask(dictTaskDO);
    }

    @Override
    public Long deleteDictTask(DictSingleTaskReq taskReq, User user) {
        DictItemResp dictItemResp = fetchDictItemResp(taskReq);
        String fileName = dictItemResp.fetchDictFileName() + Constants.DOT + dictFileType;
        fileHandler.deleteDictFile(fileName);

        try {
            dictWordService.loadDictWord();
        } catch (Exception e) {
            log.error("reloadCustomDictionary error", e);
        }
        // Add a clear dictionary file record
        DictTaskDO dictTaskDO =
                dictConverter.generateDictTaskDO(dictItemResp, user, TaskStatusEnum.INITIAL);
        log.info("[addDictTask] dictTaskDO:{}", dictTaskDO);
        dictRepository.addDictTask(dictTaskDO);
        return 0L;
    }

    @Override
    @Scheduled(cron = "${knowledge.dimension.value.cron:0 0 0 * * ?}")
    public Boolean dailyDictTask() {
        log.info("[dailyDictTask] start");
        if (!dictFlushDailyEnable) {
            log.info("dictFlushDailyEnable is false, now finish dailyDictTask");
        }
        DictItemFilter filter = DictItemFilter.builder().status(StatusEnum.ONLINE).build();
        List<DictItemResp> dictItemRespList = dictRepository.queryDictConf(filter);
        dictItemRespList.stream().forEach(item -> handleDictTaskByItemResp(item, null));
        log.info("[dailyDictTask] finish");
        return true;
    }

    @Override
    public DictTaskResp queryLatestDictTask(DictSingleTaskReq taskReq, User user) {
        return dictRepository.queryLatestDictTask(taskReq);
    }

    @Override
    public PageInfo<DictTaskResp> queryDictTask(ValueTaskQueryReq taskQueryReq, User user) {
        PageInfo<DictTaskDO> dictTaskDOPageInfo =
                PageHelper.startPage(taskQueryReq.getCurrent(), taskQueryReq.getPageSize())
                        .doSelectPageInfo(() -> dictRepository.queryAllDictTask(taskQueryReq));
        PageInfo<DictTaskResp> dictTaskRespPageInfo = new PageInfo<>();
        BeanMapper.mapper(dictTaskDOPageInfo, dictTaskRespPageInfo);
        dictTaskRespPageInfo.setList(dictConverter.taskDO2Resp(dictTaskDOPageInfo.getList()));
        return dictTaskRespPageInfo;
    }

    @Override
    public PageInfo<DictValueDimResp> queryDictValue(DictValueReq dictValueReq, User user) {
        // todo 优化读取内存结构
        // return getDictValuePageFromMemory(dictValueReq);
        return getDictValuePageFromFile(dictValueReq);
    }

    private PageInfo<DictValueDimResp> getDictValuePageFromFile(DictValueReq dictValueReq) {
        String fileName = String.format("dic_value_%d_%s_%s", dictValueReq.getModelId(),
                dictValueReq.getType().name(), dictValueReq.getItemId()) + Constants.DOT
                + dictFileType;
        PageInfo<DictValueResp> dictValueRespList =
                fileHandler.queryDictValue(fileName, dictValueReq);
        PageInfo<DictValueDimResp> result = convert2DictValueDimRespPage(dictValueRespList);
        fillDimMapInfo(result.getList(), dictValueReq.getItemId());
        return result;
    }

    private void fillDimMapInfo(List<DictValueDimResp> list, Long dimId) {
        DimensionResp dimResp = dimensionService.getDimension(dimId);
        if (CollectionUtils.isEmpty(dimResp.getDimValueMaps())) {
            return;
        }
        Map<String, DimValueMap> valueAndMap = dimResp.getDimValueMaps().stream()
                .collect(Collectors.toMap(dim -> dim.getValue(), v -> v, (v1, v2) -> v2));
        if (CollectionUtils.isEmpty(valueAndMap)) {
            return;
        }
        list.stream().forEach(dictValueDimResp -> {
            String dimValue = dictValueDimResp.getValue();
            if (valueAndMap.containsKey(dimValue) && Objects.nonNull(valueAndMap.get(dimValue))) {
                dictValueDimResp.setAlias(valueAndMap.get(dimValue).getAlias());
            }
        });
    }

    private PageInfo<DictValueDimResp> convert2DictValueDimRespPage(
            PageInfo<DictValueResp> dictValueRespPage) {
        PageInfo<DictValueDimResp> result = new PageInfo<>();
        BeanMapper.mapper(dictValueRespPage, result);
        if (CollectionUtils.isEmpty(dictValueRespPage.getList())) {
            return result;
        }

        List<DictValueDimResp> list = getDictValueDimRespList(dictValueRespPage.getList());
        result.setList(list);
        return result;
    }

    private List<DictValueDimResp> getDictValueDimRespList(List<DictValueResp> dictValueRespList) {
        List<DictValueDimResp> list =
                dictValueRespList.stream().map(dictValue -> convert2DictValueInternal(dictValue))
                        .collect(Collectors.toList());
        return list;
    }

    private List<DictValueDimResp> getDictValueDimRespList(List<DictWord> dictWords, Long dimId) {
        DimensionResp dimResp = dimensionService.getDimension(dimId);
        List<DictValueDimResp> list =
                dictWords.stream().map(dictWord -> convert2DictValueInternal(dictWord, dimResp))
                        .collect(Collectors.toList());
        return list;
    }

    private DictValueDimResp convert2DictValueInternal(DictWord dictWord, DimensionResp dimResp) {
        DictValueDimResp dictValueDimResp = new DictValueDimResp();
        BeanMapper.mapper(dictWord, dictValueDimResp);
        if (Objects.nonNull(dimResp.getDimValueMaps())) {
            Map<String, DimValueMap> techAndAliasMap = dimResp.getDimValueMaps().stream().collect(
                    Collectors.toMap(dimValue -> dimValue.getTechName(), v -> v, (v1, v2) -> v2));
            if (techAndAliasMap.containsKey(dictWord.getWord())) {
                DimValueMap dimValueMap = techAndAliasMap.get(dictWord.getWord());
                BeanMapper.mapper(dimValueMap, dictValueDimResp);
            }
        }
        return dictValueDimResp;
    }

    private DictValueDimResp convert2DictValueInternal(DictValueResp dictValue) {
        DictValueDimResp dictValueDimResp = new DictValueDimResp();
        BeanMapper.mapper(dictValue, dictValueDimResp);
        return dictValueDimResp;
    }

    private PageInfo<DictValueDimResp> getDictValuePageFromMemory(DictValueReq dictValueReq) {
        PageInfo<DictValueDimResp> dictValueRespPageInfo = new PageInfo<>();
        Set<Long> dimSet = new HashSet<>();
        dimSet.add(dictValueReq.getItemId());
        List<DictWord> dimDictWords = dictWordService.getDimDictWords(dimSet);
        if (CollectionUtils.isEmpty(dimDictWords)) {
            return dictValueRespPageInfo;
        }
        if (StringUtils.isNotEmpty(dictValueReq.getKeyValue())) {
            dimDictWords = dimDictWords.stream()
                    .filter(dimValue -> dimValue.getWord().contains(dictValueReq.getKeyValue()))
                    .collect(Collectors.toList());
        }

        Integer pageSize = dictValueReq.getPageSize();
        Integer current = dictValueReq.getCurrent();
        dictValueRespPageInfo.setTotal(dimDictWords.size());
        dictValueRespPageInfo.setPageSize(pageSize);
        dictValueRespPageInfo.setPageNum(dictValueReq.getCurrent());

        // 分页
        int startIndex = (current - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, dimDictWords.size());
        List<DictWord> data = dimDictWords.subList(startIndex, endIndex);
        dictValueRespPageInfo.setList(getDictValueDimRespList(data, dictValueReq.getItemId()));
        return dictValueRespPageInfo;
    }

    @Override
    public String queryDictFilePath(DictValueReq dictValueReq, User user) {
        String fileName = String.format("dic_value_%d_%s_%s", dictValueReq.getModelId(),
                dictValueReq.getType().name(), dictValueReq.getItemId()) + Constants.DOT
                + dictFileType;
        return fileHandler.queryDictFilePath(fileName);
    }
}
