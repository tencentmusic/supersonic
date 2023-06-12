package com.tencent.supersonic.chat.application.knowledge;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.domain.dataobject.DimValueDO;
import com.tencent.supersonic.chat.domain.pojo.config.DefaultMetric;
import com.tencent.supersonic.chat.domain.pojo.config.Dim4Dict;
import com.tencent.supersonic.chat.domain.utils.DictMetaUtils;
import com.tencent.supersonic.chat.domain.utils.DictQueryUtils;
import com.tencent.supersonic.common.constant.Constants;
import com.tencent.supersonic.common.enums.TaskStatusEnum;
import com.tencent.supersonic.knowledge.domain.FileHandler;
import com.tencent.supersonic.knowledge.domain.converter.DictTaskConverter;
import com.tencent.supersonic.knowledge.domain.dataobject.DimValueDictTaskPO;
import com.tencent.supersonic.knowledge.domain.pojo.DictConfig;
import com.tencent.supersonic.knowledge.domain.pojo.DictTaskFilter;
import com.tencent.supersonic.knowledge.domain.pojo.DictUpdateMode;
import com.tencent.supersonic.knowledge.domain.pojo.DimValue2DictCommand;
import com.tencent.supersonic.knowledge.domain.pojo.DimValueDictInfo;
import com.tencent.supersonic.knowledge.domain.repository.DictRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
public class DictApplicationService {

    @Value("${dict.flush.enable:true}")
    private Boolean dictFlushEnable;

    @Value("${dict.file.type:txt}")
    private String dictFileType;
    private String dimValue = "DimValue_%d_%d";
    private String dateTimeFormatter = "yyyyMMddHHmmss";


    private final DictMetaUtils metaUtils;
    private final DictQueryUtils dictQueryUtils;
    private final FileHandler fileHandler;
    private final DictRepository dictRepository;

    public DictApplicationService(DictMetaUtils metaUtils,
            DictQueryUtils dictQueryUtils,
            FileHandler fileHandler,
            DictRepository dictRepository) {
        this.metaUtils = metaUtils;
        this.dictQueryUtils = dictQueryUtils;
        this.fileHandler = fileHandler;
        this.dictRepository = dictRepository;
    }

    public Long addDictTask(DimValue2DictCommand dimValue2DictCommend, User user) {
        if (!dictFlushEnable) {
            return 0L;
        }
        DimValueDictTaskPO dimValueDictTaskPO = DictTaskConverter.generateDimValueDictTaskPO(dimValue2DictCommend,
                user);
        log.info("[addDictTask] dimValueDictTaskPO:{}", dimValueDictTaskPO);
        dictRepository.createDimValueDictTask(dimValueDictTaskPO);
        TaskStatusEnum finalStatus = TaskStatusEnum.SUCCESS;
        try {
            //1. construct internal dictionary requirements
            List<DimValueDO> dimValueDOList = metaUtils.generateDimValueInfo(dimValue2DictCommend);
            log.info("dimValueDOList:{}", dimValueDOList);
            //2. query dimension value information
            for (DimValueDO dimValueDO : dimValueDOList) {
                Long domainId = dimValueDO.getDomainId();
                DefaultMetric defaultMetricDesc = dimValueDO.getDefaultMetricDescList().get(0);
                for (Dim4Dict dim4Dict : dimValueDO.getDimensions()) {
                    List<String> data = dictQueryUtils.fetchDimValueSingle(domainId, defaultMetricDesc, dim4Dict, user);
                    //3. local file changes
                    String fileName = String.format(dimValue + Constants.DOT + dictFileType, domainId,
                            dim4Dict.getDimId());
                    fileHandler.writeFile(data, fileName, false);
                }
            }
        } catch (Exception e) {
            log.warn("addDictInfo exception:", e);
            finalStatus = TaskStatusEnum.ERROR;
        }
        dictRepository.updateDictTaskStatus(finalStatus.getCode(), dimValueDictTaskPO);
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
        Map<Long, List<Long>> domainAndDimPair = dimValue2DictCommend.getDomainAndDimPair();
        if (CollectionUtils.isEmpty(domainAndDimPair)) {
            return 0L;
        }
        for (Long domainId : domainAndDimPair.keySet()) {
            if (CollectionUtils.isEmpty(domainAndDimPair.get(domainId))) {
                continue;
            }
            for (Long dimId : domainAndDimPair.get(domainId)) {
                String fileName = String.format(dimValue + Constants.DOT + dictFileType, domainId, dimId);
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

    public DictConfig getDictInfoByDomainId(Long domainId) {
        return dictRepository.getDictInfoByDomainId(domainId);
    }
}