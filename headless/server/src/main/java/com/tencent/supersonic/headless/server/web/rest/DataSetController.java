package com.tencent.supersonic.headless.server.web.rest;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.headless.api.pojo.request.DataSetReq;
import com.tencent.supersonic.headless.api.pojo.response.DataSetResp;
import com.tencent.supersonic.headless.server.pojo.MetaFilter;
import com.tencent.supersonic.headless.server.web.service.DataSetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/api/semantic/dataSet")
public class DataSetController {

    @Autowired
    private DataSetService dataSetService;

    @PostMapping
    public DataSetResp save(@RequestBody DataSetReq dataSetReq,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return dataSetService.save(dataSetReq, user);
    }

    @PutMapping
    public DataSetResp update(@RequestBody DataSetReq dataSetReq,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return dataSetService.update(dataSetReq, user);
    }

    @GetMapping("/{id}")
    public DataSetResp getDataSet(@PathVariable("id") Long id) {
        return dataSetService.getDataSet(id);
    }

    @GetMapping("/getDataSetList")
    public List<DataSetResp> getDataSetList(@RequestParam("domainId") Long domainId) {
        MetaFilter metaFilter = new MetaFilter();
        metaFilter.setDomainId(domainId);
        return dataSetService.getDataSetList(metaFilter);
    }

    @DeleteMapping("/{id}")
    public Boolean delete(@PathVariable("id") Long id,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        dataSetService.delete(id, user);
        return true;
    }

}
