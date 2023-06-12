package com.tencent.supersonic.semantic.core.rest;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.semantic.api.core.request.DatabaseReq;
import com.tencent.supersonic.semantic.api.core.request.SqlExecuteReq;
import com.tencent.supersonic.semantic.api.core.response.DatabaseResp;
import com.tencent.supersonic.semantic.api.core.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.core.domain.DatabaseService;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/semantic/database")
public class DatabaseController {


    private DatabaseService databaseService;

    public DatabaseController(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @PostMapping("/testConnect")
    public boolean testConnect(@RequestBody DatabaseReq databaseReq,
            HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return databaseService.testConnect(databaseReq, user);
    }


    @PostMapping("/createOrUpdateDatabase")
    public DatabaseResp createOrUpdateDatabase(@RequestBody DatabaseReq databaseReq,
            HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return databaseService.createOrUpdateDatabase(databaseReq, user);
    }

    @GetMapping("/{id}")
    public DatabaseResp getDatabase(@PathVariable("id") Long id) {
        return databaseService.getDatabase(id);
    }

    @GetMapping("/getDatabaseByDomainId/{domainId}")
    public DatabaseResp getDatabaseByDomainId(@PathVariable("domainId") Long domainId) {
        return databaseService.getDatabaseByDomainId(domainId);
    }

    @PostMapping("/executeSql")
    public QueryResultWithSchemaResp executeSql(@RequestBody SqlExecuteReq sqlExecuteReq) {
        return databaseService.executeSql(sqlExecuteReq.getSql(), sqlExecuteReq.getDomainId());
    }


}
