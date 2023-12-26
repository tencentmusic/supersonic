package com.tencent.supersonic.headless.materialization.rest;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.headless.common.materialization.pojo.MaterializationDateFilter;
import com.tencent.supersonic.headless.common.materialization.pojo.MaterializationRecordFilter;
import com.tencent.supersonic.headless.common.materialization.request.MaterializationRecordReq;
import com.tencent.supersonic.headless.common.materialization.response.MaterializationDateResp;
import com.tencent.supersonic.headless.common.materialization.response.MaterializationRecordResp;
import com.tencent.supersonic.headless.materialization.domain.MaterializationRecordService;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/api/semantic/materialization/record")
public class MaterializationRecordController {

    private final MaterializationRecordService recordService;

    public MaterializationRecordController(MaterializationRecordService recordService) {
        this.recordService = recordService;
    }

    @PostMapping
    public Boolean addMaterializationRecord(@RequestBody MaterializationRecordReq materializationRecord,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return recordService.addMaterializationRecord(materializationRecord, user);
    }

    @PutMapping
    public Boolean updateMaterializationRecord(@RequestBody MaterializationRecordReq materializationRecord,
                                               HttpServletRequest request,
                                               HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return recordService.updateMaterializationRecord(materializationRecord, user);
    }

    @PostMapping("/info")
    List<MaterializationRecordResp> getMaterializationRecordList(@RequestBody MaterializationRecordFilter filter,
                                                                 HttpServletRequest request,
                                                                 HttpServletResponse response) {

        User user = UserHolder.findUser(request, response);
        return recordService.getMaterializationRecordList(filter, user);
    }

    @PostMapping("/count")
    Long getMaterializationRecordCount(@RequestBody MaterializationRecordFilter filter,
            HttpServletRequest request,
            HttpServletResponse response) {

        User user = UserHolder.findUser(request, response);
        return recordService.getMaterializationRecordCount(filter, user);
    }

    @PostMapping("/info/date")
    List<MaterializationDateResp> fetchMaterializationDate(@RequestBody MaterializationDateFilter filter,
                                                           HttpServletRequest request,
                                                           HttpServletResponse response) {

        User user = UserHolder.findUser(request, response);
        return recordService.fetchMaterializationDate(filter, user);
    }

}
