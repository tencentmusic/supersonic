package com.tencent.supersonic.chat.rest;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.chat.api.pojo.request.DictLatestTaskReq;
import com.tencent.supersonic.chat.api.pojo.response.DictLatestTaskResp;
import com.tencent.supersonic.chat.service.ChatKnowledgeService;
import com.tencent.supersonic.knowledge.listener.ApplicationStartedListener;
import com.tencent.supersonic.chat.api.pojo.request.DictTaskFilterReq;
import com.tencent.supersonic.knowledge.dictionary.DimValue2DictCommand;
import com.tencent.supersonic.knowledge.dictionary.DimValueDictInfo;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;


@RestController
@RequestMapping("/api/chat/dict")
public class KnowledgeController {

    @Autowired
    private ChatKnowledgeService knowledgeService;

    @Autowired
    private ApplicationStartedListener applicationStartedListener;

    /**
     * addDictInfo
     * write specific dimension values to the knowledge base
     *
     * @param dimValue2DictCommend
     */
    @PostMapping("/task")
    public Long addDictTask(@RequestBody DimValue2DictCommand dimValue2DictCommend,
                            HttpServletRequest request,
                            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return knowledgeService.addDictTask(dimValue2DictCommend, user);
    }

    /**
     * deleteDictInfo
     * remove specific dimension values from the knowledge base
     *
     * @param dimValue2DictCommend
     */
    @PostMapping("/task/delete")
    public Long deleteDictTask(@RequestBody DimValue2DictCommand dimValue2DictCommend,
                               HttpServletRequest request,
                               HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return knowledgeService.deleteDictTask(dimValue2DictCommend, user);
    }

    /**
     * searchDictTaskList
     *
     * @param filter
     */
    @PostMapping("/task/search")
    public List<DimValueDictInfo> searchDictTaskList(@RequestBody DictTaskFilterReq filter,
                                                     HttpServletRequest request,
                                                     HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return knowledgeService.searchDictTaskList(filter, user);
    }

    /**
     * searchDictLatestTaskList
     */
    @PostMapping("/task/search/latest")
    public List<DictLatestTaskResp> searchDictLatestTaskList(@RequestBody @Valid DictLatestTaskReq filter,
                                                             HttpServletRequest request,
                                                             HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return knowledgeService.searchDictLatestTaskList(filter, user);
    }

    /**
     * getDictRootPath
     * get knowledge base file directory
     *
     * @return
     */
    @GetMapping("/rootPath")
    public String getDictRootPath(HttpServletRequest request,
                                  HttpServletResponse response) {
        return knowledgeService.getDictRootPath();
    }

    /**
     * updateDimValue
     * update in-memory dictionary files in real time
     *
     * @param request
     * @param response
     * @return
     */
    @PutMapping("/knowledge/dimValue")
    public Boolean updateDimValue(HttpServletRequest request,
                                  HttpServletResponse response) {
        return applicationStartedListener.updateKnowledgeDimValue();
    }

}
