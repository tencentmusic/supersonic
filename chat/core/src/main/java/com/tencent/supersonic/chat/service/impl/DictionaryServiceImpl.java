package com.tencent.supersonic.chat.service.impl;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.config.DefaultMetric;
import com.tencent.supersonic.chat.config.Dim4Dict;
import com.tencent.supersonic.chat.persistence.dataobject.DimValueDO;
import com.tencent.supersonic.chat.service.DictionaryService;
import com.tencent.supersonic.chat.utils.DictMetaHelper;
import com.tencent.supersonic.chat.utils.DictQueryHelper;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.TaskStatusEnum;
import com.tencent.supersonic.knowledge.dictionary.FileHandler;
import com.tencent.supersonic.knowledge.persistence.dataobject.DictTaskDO;
import com.tencent.supersonic.knowledge.utils.DictTaskConverter;
import com.tencent.supersonic.knowledge.dictionary.DictConfig;
import com.tencent.supersonic.knowledge.dictionary.DictTaskFilter;
import com.tencent.supersonic.knowledge.dictionary.DictUpdateMode;
import com.tencent.supersonic.knowledge.dictionary.DimValue2DictCommand;
import com.tencent.supersonic.knowledge.dictionary.DimValueDictInfo;
import com.tencent.supersonic.knowledge.persistence.repository.DictRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
public class DictionaryServiceImpl implements DictionaryService {

    private final DictMetaHelper metaUtils;
    private final DictQueryHelper dictQueryHelper;
    private final FileHandler fileHandler;
    private final DictRepository dictRepository;
    @Value("${dict.flush.enable:true}")
    private Boolean dictFlushEnable;
    @Value("${dict.file.type:txt}")
    private String dictFileType;
    private String dimValue = "DimValue_%d_%d";

    public DictionaryServiceImpl(DictMetaHelper metaUtils,
                                 DictQueryHelper dictQueryHelper,
                                 FileHandler fileHandler,
                                 DictRepository dictRepository) {
        this.metaUtils = metaUtils;
        this.dictQueryHelper = dictQueryHelper;
        this.fileHandler = fileHandler;
        this.dictRepository = dictRepository;
    }

    public Long addDictTask(DimValue2DictCommand dimValue2DictCommend, User user) {
        if (!dictFlushEnable) {
            return 0L;
        }
        DictTaskDO dictTaskDO = DictTaskConverter.generateDimValueDictTaskPO(dimValue2DictCommend,
                user);
        log.info("[addDictTask] dictTaskDO:{}", dictTaskDO);
        dictRepository.createDimValueDictTask(dictTaskDO);
        TaskStatusEnum finalStatus = TaskStatusEnum.SUCCESS;
        try {
            //1. construct internal dictionary requirements
            List<DimValueDO> dimValueDOList = metaUtils.generateDimValueInfo(dimValue2DictCommend);
            log.info("dimValueDOList:{}", dimValueDOList);
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
        } catch (Exception e) {
            log.warn("addDictInfo exception:", e);
            finalStatus = TaskStatusEnum.ERROR;
        }
        dictRepository.updateDictTaskStatus(finalStatus.getCode(),
                dictTaskDO);
        return 1L;
    }


    public Long deleteDictTask(DimValue2DictCommand dimValue2DictCommend, User user) {
        if (!dictFlushEnable) {
            return 0L;
        }
        if (Objects.isNull(dimValue2DictCommend) || DictUpdateMode.REALTIME_DELETE.equals(
                dimValue2DictCommend.getUpdateMode())) {
            throw new RuntimeException("illegal parameter");
        }
        Map<Long, List<Long>> modelAndDimPair = dimValue2DictCommend.getModelAndDimPair();
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

        return 1L;
    }

    public String getDictRootPath() {
        return fileHandler.getDictRootPath();
    }

    public List<DimValueDictInfo> searchDictTaskList(DictTaskFilter filter, User user) {
        return dictRepository.searchDictTaskList(filter);
    }

    public DictConfig getDictInfoByModelId(Long modelId) {
        return dictRepository.getDictInfoByModelId(modelId);
    }
}
