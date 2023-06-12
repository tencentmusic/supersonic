package com.tencent.supersonic.knowledge.domain.converter;

import com.google.common.base.Strings;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.enums.TaskStatusEnum;
import com.tencent.supersonic.common.util.json.JsonUtil;
import com.tencent.supersonic.knowledge.domain.dataobject.DictConfPO;
import com.tencent.supersonic.knowledge.domain.dataobject.DimValueDictTaskPO;
import com.tencent.supersonic.knowledge.domain.pojo.DictConfig;
import com.tencent.supersonic.knowledge.domain.pojo.DimValue2DictCommand;
import com.tencent.supersonic.knowledge.domain.pojo.DimValueInfo;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

public class DictTaskConverter {

    private static String dateTimeFormatter = "yyyyMMddHHmmss";

    public static DimValueDictTaskPO generateDimValueDictTaskPO(DimValue2DictCommand dimValue2DictCommend, User user) {
        DimValueDictTaskPO taskPO = new DimValueDictTaskPO();
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

    public static DictConfPO generateDictConfPO(DictConfig dictConfig, User user) {
        DictConfPO dictConfPO = new DictConfPO();
        dictConfPO.setDimValueInfos(JsonUtil.toString(dictConfig.getDimValueInfoList()));
        dictConfPO.setDomainId(dictConfig.getDomainId());

        dictConfPO.setCreatedBy(user.getName());
        dictConfPO.setUpdatedBy(user.getName());
        dictConfPO.setCreatedAt(new Date());
        dictConfPO.setUpdatedAt(new Date());

        return dictConfPO;
    }

    public static DictConfig dictConfPO2Config(DictConfPO dictConfPO) {
        DictConfig dictConfig = new DictConfig();
        dictConfig.setDomainId(dictConfPO.getDomainId());
        List<DimValueInfo> dimValueInfos = JsonUtil.toList(dictConfPO.getDimValueInfos(), DimValueInfo.class);
        dictConfig.setDimValueInfoList(dimValueInfos);
        return dictConfig;
    }
}