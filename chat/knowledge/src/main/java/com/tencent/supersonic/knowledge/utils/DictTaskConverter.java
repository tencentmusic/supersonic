package com.tencent.supersonic.knowledge.utils;

import com.google.common.base.Strings;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.enums.TaskStatusEnum;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.knowledge.dictionary.DictConfig;
import com.tencent.supersonic.knowledge.dictionary.DimValue2DictCommand;
import com.tencent.supersonic.knowledge.dictionary.DimValueInfo;
import com.tencent.supersonic.knowledge.persistence.dataobject.DictConfDO;
import com.tencent.supersonic.knowledge.persistence.dataobject.DictTaskDO;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

public class DictTaskConverter {

    private static String dateTimeFormatter = "yyyyMMddHHmmss";

    public static DictTaskDO generateDimValueDictTaskPO(DimValue2DictCommand dimValue2DictCommend, User user) {
        DictTaskDO taskPO = new DictTaskDO();
        Date createAt = new Date();
        String date = DateTimeFormatter.ofPattern(dateTimeFormatter)
                .format(createAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        String creator = Strings.isNullOrEmpty(user.getName()) ? "" : user.getName();
        String updateMode = dimValue2DictCommend.getUpdateMode().getValue();
        String name = String.format("DimValue_dic_%s_%s_%s", updateMode, creator, date);
        taskPO.setName(name);

        taskPO.setCreatedAt(createAt);
        taskPO.setCommand(JsonUtil.toString(dimValue2DictCommend));
        taskPO.setStatus(TaskStatusEnum.RUNNING.getCode());
        taskPO.setCreatedBy(creator);

        return taskPO;
    }

    public static DictConfig dictConfPO2Config(DictConfDO dictConfDO) {
        DictConfig dictConfig = new DictConfig();
        dictConfig.setModelId(dictConfDO.getModelId());
        List<DimValueInfo> dimValueInfos = JsonUtil.toList(dictConfDO.getDimValueInfos(), DimValueInfo.class);
        dictConfig.setDimValueInfoList(dimValueInfos);
        return dictConfig;
    }
}
