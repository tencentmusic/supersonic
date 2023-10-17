package com.tencent.supersonic.semantic.materialization.rest;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.semantic.api.materialization.pojo.MaterializationConfFilter;
import com.tencent.supersonic.semantic.api.materialization.pojo.MaterializationFilter;
import com.tencent.supersonic.semantic.api.materialization.request.MaterializationElementReq;
import com.tencent.supersonic.semantic.api.materialization.request.MaterializationReq;
import com.tencent.supersonic.semantic.api.materialization.response.MaterializationResp;
import com.tencent.supersonic.semantic.api.materialization.response.MaterializationSourceResp;
import com.tencent.supersonic.semantic.materialization.domain.MaterializationConfService;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/semantic/materialization/conf")
public class MaterializationConfController {

    private final MaterializationConfService confService;

    public MaterializationConfController(MaterializationConfService confService) {
        this.confService = confService;
    }

    @PostMapping
    public Boolean addMaterializationConf(@RequestBody MaterializationReq materializationReq,
            HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return confService.addMaterializationConf(materializationReq, user);
    }

    @PutMapping
    public Boolean updateMaterializationConf(@RequestBody MaterializationReq materializationReq,
            HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return confService.updateMaterializationConf(materializationReq, user);
    }

    @PostMapping("/info")
    List<MaterializationResp> getMaterializationResp(@RequestBody MaterializationFilter filter,
            HttpServletRequest request,
            HttpServletResponse response) {

        User user = UserHolder.findUser(request, response);
        return confService.getMaterializationResp(filter, user);
    }


    @PostMapping("/element")
    public Boolean addMaterializationElementConf(@RequestBody MaterializationElementReq materializationElementReq,
            HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return confService.addMaterializationElementConf(materializationElementReq, user);
    }

    @PutMapping("/element")
    public Boolean updateMaterializationElementConf(@RequestBody MaterializationElementReq materializationElementReq,
            HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return confService.updateMaterializationElementConf(materializationElementReq, user);
    }

    @PutMapping("/element/init")
    public Boolean initMaterializationElementConf(@RequestBody MaterializationConfFilter filter,
            HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return confService.initMaterializationElementConf(filter, user);
    }

    @PostMapping("/element/info")
    List<MaterializationResp> queryMaterializationConf(@RequestBody MaterializationConfFilter filter,
            HttpServletRequest request,
            HttpServletResponse response) {

        User user = UserHolder.findUser(request, response);
        return confService.queryMaterializationConf(filter, user);
    }

    @GetMapping("/table/sql")
    String generateCreateSql(@RequestParam(value = "materializationId") Long materializationId,
            HttpServletRequest request,
            HttpServletResponse response) {

        User user = UserHolder.findUser(request, response);
        return confService.generateCreateSql(materializationId, user);
    }

    @PostMapping("/source")
    List<MaterializationSourceResp> queryElementModel(@RequestBody MaterializationFilter filter,
            HttpServletRequest request,
            HttpServletResponse response) {

        //User user = UserHolder.findUser(request, response);
        return confService.getMaterializationSourceResp(filter.getMaterializationId());
    }


}