package com.tencent.supersonic.headless.server.rest;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import com.tencent.supersonic.headless.api.pojo.request.DictItemFilter;
import com.tencent.supersonic.headless.api.pojo.request.DictItemReq;
import com.tencent.supersonic.headless.api.pojo.request.DictSingleTaskReq;
import com.tencent.supersonic.headless.api.pojo.response.DictItemResp;
import com.tencent.supersonic.headless.api.pojo.response.DictTaskResp;
import com.tencent.supersonic.headless.server.service.DictConfService;
import com.tencent.supersonic.headless.server.service.DictTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api/semantic/knowledge")
public class KnowledgeController {

    @Autowired
    private DictTaskService taskService;

    @Autowired
    private DictConfService confService;

    /**
     * @Autowired
     * private ApplicationStartedListener applicationStartedListener;
     */


    /**
     * addDictConf
     * Add configuration information for dictionary entries
     *
     * @param dictItemReq
     */
    @PostMapping("/conf")
    public Long addDictConf(@RequestBody @Valid DictItemReq dictItemReq,
                            HttpServletRequest request,
                            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return confService.addDictConf(dictItemReq, user);
    }

    /**
     * editDictConf
     * Edit configuration information for dictionary entries
     *
     * @param dictItemReq
     */
    @PutMapping("/conf")
    public Long editDictConf(@RequestBody @Valid DictItemReq dictItemReq,
                             HttpServletRequest request,
                             HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return confService.editDictConf(dictItemReq, user);
    }

    /**
     * queryDictConf
     * query configuration information for dictionary entries
     *
     * @param filter
     */
    @PostMapping("/conf/query")
    public List<DictItemResp> queryDictConf(@RequestBody @Valid DictItemFilter filter,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return confService.queryDictConf(filter, user);
    }


    /**
     * addDictTask
     * write specific item values to the knowledge base
     *
     * @param taskReq
     */
    @PostMapping("/task")
    public Long addDictTask(@RequestBody DictSingleTaskReq taskReq,
                            HttpServletRequest request,
                            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return taskService.addDictTask(taskReq, user);
    }

    /**
     * deleteDictInfo
     * remove specific item values from the knowledge base
     *
     * @param taskReq
     */
    @PutMapping("/task/delete")
    public Long deleteDictTask(@RequestBody DictSingleTaskReq taskReq,
                               HttpServletRequest request,
                               HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return taskService.deleteDictTask(taskReq, user);
    }

    /**
     * 手动离线更新所以字典问题
     */
    @PutMapping("/task/all")
    public Boolean deleteDictTask(
            HttpServletRequest request,
            HttpServletResponse response) {
        return taskService.dailyDictTask();
    }

    /**
     * 返回最新的字典任务执行情况
     *
     * @param taskReq
     */
    @PostMapping("/task/search")
    public DictTaskResp queryLatestDictTask(@RequestBody DictSingleTaskReq taskReq,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return taskService.queryLatestDictTask(taskReq, user);
    }

}
