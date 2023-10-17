package com.tencent.supersonic.semantic.materialization.domain.utils;

import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.semantic.api.materialization.enums.ElementFrequencyEnum;
import com.tencent.supersonic.semantic.api.materialization.enums.ElementTypeEnum;
import com.tencent.supersonic.semantic.api.materialization.enums.MaterializedTypeEnum;
import com.tencent.supersonic.semantic.api.materialization.enums.UpdateCycleEnum;
import com.tencent.supersonic.semantic.api.materialization.request.MaterializationElementReq;
import com.tencent.supersonic.semantic.api.materialization.request.MaterializationReq;
import com.tencent.supersonic.semantic.api.materialization.response.MaterializationElementResp;
import com.tencent.supersonic.semantic.api.materialization.response.MaterializationResp;
import com.tencent.supersonic.semantic.materialization.domain.dataobject.MaterializationDOWithBLOBs;
import com.tencent.supersonic.semantic.materialization.domain.dataobject.MaterializationElementDOWithBLOBs;
import com.tencent.supersonic.semantic.materialization.domain.pojo.Materialization;
import com.tencent.supersonic.semantic.materialization.domain.pojo.MaterializationElement;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Objects;

@Component
public class MaterializationConverter {

    public static Materialization materializationReq2Bean(MaterializationReq materializationReq) {
        Materialization materialization = new Materialization();
        BeanUtils.copyProperties(materializationReq, materialization);
        return materialization;
    }

    public static MaterializationDOWithBLOBs materialization2DO(Materialization materialization) {
        MaterializationDOWithBLOBs materializationDOWithBLOBs = new MaterializationDOWithBLOBs();
        BeanUtils.copyProperties(materialization, materializationDOWithBLOBs);
        if (Objects.nonNull(materialization.getMaterializedType())) {
            materializationDOWithBLOBs.setMaterializedType(materialization.getMaterializedType().name());
        }
        if (Objects.nonNull(materialization.getUpdateCycle())) {
            materializationDOWithBLOBs.setUpdateCycle(materialization.getUpdateCycle().name());
        }
        if (Objects.nonNull(materialization.getStatus())) {
            materializationDOWithBLOBs.setStatus(materialization.getStatus().getCode());
        }
        if (!CollectionUtils.isEmpty(materialization.getPrincipals())) {
            materializationDOWithBLOBs.setPrincipals(JsonUtil.toString(materialization.getPrincipals()));
        }
        return materializationDOWithBLOBs;
    }

    public static MaterializationElementDOWithBLOBs materialization2DO(MaterializationElement materializationElement) {
        MaterializationElementDOWithBLOBs materializationElementDO = new MaterializationElementDOWithBLOBs();
        BeanUtils.copyProperties(materializationElement, materializationElementDO);
        if (Objects.nonNull(materializationElement.getElementType())) {
            materializationElementDO.setElementType(materializationElement.getElementType().name());
        }
        if (Objects.nonNull(materializationElement.getType())) {
            materializationElementDO.setType(materializationElement.getType().getName());
        }
        if (Objects.nonNull(materializationElement.getStatus())) {
            materializationElementDO.setStatus(materializationElement.getStatus().getCode());
        }
        if (Objects.nonNull(materializationElement.getFrequency())) {
            materializationElementDO.setFrequency(materializationElement.getFrequency().name());
        }

        return materializationElementDO;
    }

    public static MaterializationDOWithBLOBs convert(MaterializationDOWithBLOBs materializationDO,
                                                     Materialization materialization) {
        BeanMapper.mapper(materialization, materializationDO);
        if (Objects.nonNull(materialization.getMaterializedType())) {
            materializationDO.setMaterializedType(materialization.getMaterializedType().name());
        }
        if (Objects.nonNull(materialization.getUpdateCycle())) {
            materializationDO.setUpdateCycle(materialization.getUpdateCycle().name());
        }
        if (Objects.nonNull(materialization.getStatus())) {
            materializationDO.setStatus(materialization.getStatus().getCode());
        }
        if (!CollectionUtils.isEmpty(materialization.getPrincipals())) {
            materializationDO.setPrincipals(JsonUtil.toString(materialization.getPrincipals()));
        }
        return materializationDO;
    }

    public static MaterializationElementDOWithBLOBs convert(MaterializationElementDOWithBLOBs materializationElementDO,
                                                            MaterializationElement materializationElement) {
        BeanMapper.mapper(materializationElement, materializationElementDO);
        if (Objects.nonNull(materializationElement.getType())) {
            materializationElementDO.setType(materializationElement.getType().name());
        }
        if (Objects.nonNull(materializationElement.getElementType())) {
            materializationElementDO.setElementType(materializationElement.getElementType().name());
        }
        if (Objects.nonNull(materializationElement.getFrequency())) {
            materializationElementDO.setFrequency(materializationElement.getFrequency().name());
        }
        if (Objects.nonNull(materializationElement.getStatus())) {
            materializationElementDO.setStatus(materializationElement.getStatus().getCode());
        }

        return materializationElementDO;
    }

    public static MaterializationElement materializationElementReq2Bean(MaterializationElementReq elementReq) {
        MaterializationElement materializationElement = new MaterializationElement();
        BeanUtils.copyProperties(elementReq, materializationElement);
        return materializationElement;
    }

    public static MaterializationResp convert2Resp(MaterializationDOWithBLOBs materializationDO) {
        MaterializationResp materializationResp = new MaterializationResp();
        BeanUtils.copyProperties(materializationDO, materializationResp);
        if (Strings.isNotEmpty(materializationDO.getMaterializedType())) {
            materializationResp.setMaterializedType(Enum.valueOf(MaterializedTypeEnum.class,
                    materializationDO.getMaterializedType()));
        }
        if (Strings.isNotEmpty(materializationDO.getUpdateCycle())) {
            materializationResp.setUpdateCycle(Enum.valueOf(UpdateCycleEnum.class, materializationDO.getUpdateCycle()));
        }
        if (Strings.isNotEmpty(materializationDO.getPrincipals())) {
            materializationResp.setPrincipals(JsonUtil.toList(materializationDO.getPrincipals(), String.class));
        }
        return materializationResp;
    }

    public static MaterializationElementResp elementDO2Resp(MaterializationElementDOWithBLOBs elementDO) {
        MaterializationElementResp materializationElementResp = new MaterializationElementResp();
        BeanUtils.copyProperties(elementDO, materializationElementResp);
        if (Strings.isNotEmpty(elementDO.getType())) {
            materializationElementResp.setType(TypeEnums.of(elementDO.getType()));
        }
        if (Strings.isNotEmpty(elementDO.getElementType())) {
            materializationElementResp.setElementType(Enum.valueOf(ElementTypeEnum.class,
                    elementDO.getElementType()));
        }
        if (Strings.isNotEmpty(elementDO.getFrequency())) {
            materializationElementResp.setFrequency(Enum.valueOf(ElementFrequencyEnum.class,
                    elementDO.getFrequency()));
        }
        return materializationElementResp;
    }
}