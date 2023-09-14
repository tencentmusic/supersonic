package com.tencent.supersonic.chat.service.impl;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.request.DictLatestTaskReq;
import com.tencent.supersonic.chat.api.pojo.response.DictLatestTaskResp;
import com.tencent.supersonic.chat.config.DefaultMetric;
import com.tencent.supersonic.chat.config.Dim4Dict;
import com.tencent.supersonic.chat.persistence.dataobject.DimValueDO;
import com.tencent.supersonic.chat.service.ChatKnowledgeService;
import com.tencent.supersonic.chat.utils.DictMetaHelper;
import com.tencent.supersonic.chat.utils.DictQueryHelper;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.TaskStatusEnum;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.knowledge.dictionary.FileHandler;
import com.tencent.supersonic.knowledge.listener.ApplicationStartedListener;
import com.tencent.supersonic.knowledge.persistence.dataobject.DictTaskDO;
import com.tencent.supersonic.knowledge.utils.DictTaskConverter;
import com.tencent.supersonic.knowledge.dictionary.DictConfig;
import com.tencent.supersonic.chat.api.pojo.request.DictTaskFilterReq;
import com.tencent.supersonic.knowledge.dictionary.DictUpdateMode;
import com.tencent.supersonic.knowledge.dictionary.DimValue2DictCommand;
import com.tencent.supersonic.knowledge.dictionary.DimValueDictInfo;
import com.tencent.supersonic.knowledge.persistence.repository.DictRepository;



import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@Service
public class ChatKnowledgeServiceImpl implements ChatKnowledgeService {

    private final DictMetaHelper metaUtils;
    private final DictQueryHelper dictQueryHelper;
    private final FileHandler fileHandler;
    private final DictRepository dictRepository;
    private final ApplicationStartedListener applicationStartedListener;

    @Value("${dict.flush.enable:true}")
    private Boolean dictFlushEnable;
    @Value("${dict.flush.daily.enable:true}")
    private Boolean dictFlushDailyEnable;
    @Value("${dict.file.type:txt}")
    private String dictFileType;
    private String dimValue = "DimValue_%d_%d";

    public ChatKnowledgeServiceImpl(DictMetaHelper metaUtils,
                                    DictQueryHelper dictQueryHelper,
                                    FileHandler fileHandler,
                                    DictRepository dictRepository,
                                    ApplicationStartedListener applicationStartedListener) {
        this.metaUtils = metaUtils;
        this.dictQueryHelper = dictQueryHelper;
        this.fileHandler = fileHandler;
        this.dictRepository = dictRepository;
        this.applicationStartedListener = applicationStartedListener;
    }

    @Scheduled(cron = "${knowledge.dimension.value.cron:0 0 0 * * ?}")
    public Boolean dailyDictTask() {
        log.info("[dailyDictTask] start");
        if (!dictFlushDailyEnable) {
            log.info("dictFlushDailyEnable is false, now finish dailyDictTask");
        }
        DimValue2DictCommand dimValue2DictCommend = new DimValue2DictCommand();
        dimValue2DictCommend.setUpdateMode(DictUpdateMode.OFFLINE_FULL);

        User user = User.getFakeUser();
        addDictTask(dimValue2DictCommend, user);
        log.info("[dailyDictTask] finish");
        return true;
    }

    @Override
    public Long addDictTask(DimValue2DictCommand dimValue2DictCommend, User user) {
        if (!dictFlushEnable) {
            return 0L;
        }

        if (DictUpdateMode.REALTIME_DELETE.equals(dimValue2DictCommend.getUpdateMode())) {
            return deleteDictTask(dimValue2DictCommend, user);
        }

        DictTaskDO dictTaskDO = DictTaskConverter.generateDimValueDictTaskDO(dimValue2DictCommend, user);
        log.info("[addDictTask] dictTaskDO:{}", dictTaskDO);
        // todo check dimension can not be searched

        dictRepository.createDimValueDictTask(dictTaskDO);
        runDictTask(dictTaskDO, user);

        return dictTaskDO.getId();
    }

    public Long runDictTask(DictTaskDO dictTaskDO, User user) {
        if (Objects.isNull(dictTaskDO)) {
            return -1L;
        }
        DimValue2DictCommand command = JsonUtil.toObject(dictTaskDO.getCommand(), DimValue2DictCommand.class);
        try {
            //1. construct internal dictionary requirements
            List<DimValueDO> dimValueDOList = metaUtils.generateDimValueInfo(command);
            Set<Long> dimIds = generateDimSet(dimValueDOList);
            dictTaskDO.setDimIds(JsonUtil.toString(dimIds));
            dictRepository.updateDictTaskStatus(TaskStatusEnum.RUNNING.getCode(), dictTaskDO);
            log.debug("dimValueDOList:{}", dimValueDOList);
            //2. query dimension value information
            for (DimValueDO dimValueDO : dimValueDOList) {
                Long modelId = dimValueDO.getModelId();
                DefaultMetric defaultMetricDesc = dimValueDO.getDefaultMetricDescList().get(0);
                for (Dim4Dict dim4Dict : dimValueDO.getDimensions()) {
                    List<String> data = dictQueryHelper.fetchDimValueSingle(modelId, defaultMetricDesc, dim4Dict, user);
                    //3. local file changes
                    String fileName = String.format(dimValue + Constants.DOT + dictFileType, modelId,
                            dim4Dict.getDimId());
                    fileHandler.writeFile(data, fileName, false);
                }
            }
            applicationStartedListener.updateKnowledgeDimValue();
            log.debug("updateDictTaskStatus to SUCCESS");
            dictRepository.updateDictTaskStatus(TaskStatusEnum.SUCCESS.getCode(), dictTaskDO);
        } catch (Exception e) {
            log.warn("addDictInfo exception:", e);
            dictRepository.updateDictTaskStatus(TaskStatusEnum.ERROR.getCode(), dictTaskDO);
        }
        return 1L;
    }

    private Set<Long> generateDimSet(List<DimValueDO> dimValueDOList) {
        Set<Long> dimIds = new HashSet<>();
        if (!CollectionUtils.isEmpty(dimValueDOList)) {
            dimValueDOList.stream().forEach(dimValueDO -> {
                if (!CollectionUtils.isEmpty(dimValueDO.getDimensions())) {
                    dimValueDO.getDimensions().stream().forEach(dim4Dict -> dimIds.add(dim4Dict.getDimId()));
                }
            });
        }
        return dimIds;
    }

    @Override
    public Long deleteDictTask(DimValue2DictCommand dimValue2DictCommand, User user) {
        if (!dictFlushEnable) {
            return 0L;
        }
        if (Objects.isNull(dimValue2DictCommand) || !DictUpdateMode.REALTIME_DELETE.equals(
                dimValue2DictCommand.getUpdateMode())) {
            throw new RuntimeException("illegal parameter");
        }

        DictTaskDO dictTaskDO = DictTaskConverter.generateDimValueDictTaskDO(dimValue2DictCommand, user);
        log.info("[deleteDictTask] dictTaskDO:{}", dictTaskDO);
        Set<Long> dimIds = generateDimSetFromCommand(dimValue2DictCommand.getModelAndDimPair());
        dictTaskDO.setDimIds(JsonUtil.toString(dimIds));
        dictRepository.createDimValueDictTask(dictTaskDO);

        Map<Long, List<Long>> modelAndDimPair = dimValue2DictCommand.getModelAndDimPair();
        if (CollectionUtils.isEmpty(modelAndDimPair)) {
            return 0L;
        }
        for (Long modelId : modelAndDimPair.keySet()) {
            if (CollectionUtils.isEmpty(modelAndDimPair.get(modelId))) {
                continue;
            }
            for (Long dimId : modelAndDimPair.get(modelId)) {
                String fileName = String.format(dimValue + Constants.DOT + dictFileType, modelId, dimId);
                fileHandler.deleteDictFile(fileName);
            }
        }
        applicationStartedListener.updateKnowledgeDimValue();
        dictRepository.updateDictTaskStatus(TaskStatusEnum.SUCCESS.getCode(), dictTaskDO);
        applicationStartedListener.updateKnowledgeDimValue();

        return 1L;
    }

    private Set<Long> generateDimSetFromCommand(Map<Long, List<Long>> modelAndDimPair) {
        Set<Long> dimIds = new HashSet<>();
        if (!CollectionUtils.isEmpty(modelAndDimPair)) {
            modelAndDimPair.forEach((k, v) -> dimIds.addAll(v));
        }
        return dimIds;
    }

    @Override
    public String getDictRootPath() {
        return fileHandler.getDictRootPath();
    }

    @Override
    public List<DictLatestTaskResp> searchDictLatestTaskList(DictLatestTaskReq latestFilter, User user) {
        DictTaskFilterReq filter = new DictTaskFilterReq();
        BeanUtils.copyProperties(latestFilter, filter);
        List<DimValueDictInfo> dimValueDictInfoList = searchDictTaskList(filter, user);
        return extractLatestTask(dimValueDictInfoList, latestFilter.getDimIds());
    }

    private List<DictLatestTaskResp> extractLatestTask(List<DimValueDictInfo> dimValueDictInfoList, List<Long> dimIds) {
        List<DictLatestTaskResp> dictLatestTaskRespList = new ArrayList<>();
        Map<Long, DictLatestTaskResp> dimAndTaskPair = new HashMap<>(50);
        for (DimValueDictInfo dimValueDictInfo : dimValueDictInfoList) {
            //1. filter
            if (Objects.isNull(dimValueDictInfo) || CollectionUtils.isEmpty(dimValueDictInfo.getDimIds())) {
                continue;
            }
            if (!CollectionUtils.isEmpty(dimIds)) {
                Set<Long> tmp = dimValueDictInfo.getDimIds();
                tmp.retainAll(dimIds);
                dimValueDictInfo.setDimIds(tmp);
                if (tmp.size() <= 0) {
                    continue;
                }
            }

            // 2. extract
            Set<Long> dimIdList = dimValueDictInfo.getDimIds();
            for (Long dimId : dimIdList) {
                DictLatestTaskResp dictLatestTaskResp = new DictLatestTaskResp();
                if (!dimAndTaskPair.containsKey(dimId)) {
                    BeanUtils.copyProperties(dimValueDictInfo, dictLatestTaskResp);
                    dictLatestTaskResp.setDimId(dimId);
                } else {
                    DictLatestTaskResp dictLatestTaskExist = dimAndTaskPair.get(dimId);
                    if (dictLatestTaskExist.getCreatedAt().before(dimValueDictInfo.getCreatedAt())) {
                        BeanUtils.copyProperties(dimValueDictInfo, dictLatestTaskResp);
                        dictLatestTaskResp.setDimId(dimId);
                    } else {
                        dictLatestTaskResp = dictLatestTaskExist;
                    }
                }
                dimAndTaskPair.put(dimId, dictLatestTaskResp);
            }

        }

        if (dimAndTaskPair.size() >= 0 && !CollectionUtils.isEmpty(dimAndTaskPair.values())) {
            dimAndTaskPair.values().stream()
                    .filter(v -> !v.getCommand().contains(DictUpdateMode.REALTIME_DELETE.name()))
                    .forEach(v -> dictLatestTaskRespList.add(v));
        }


        return dictLatestTaskRespList;
    }

    @Override
    public List<DimValueDictInfo> searchDictTaskList(DictTaskFilterReq filter, User user) {
        return dictRepository.searchDictTaskList(filter);
    }

    @Override
    public DictConfig getDictInfoByModelId(Long modelId) {
        return dictRepository.getDictInfoByModelId(modelId);
    }
}
