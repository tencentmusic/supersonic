package com.tencent.supersonic.headless.server.web.rest;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.headless.api.pojo.request.TagObjectReq;
import com.tencent.supersonic.headless.api.pojo.response.TagObjectResp;
import com.tencent.supersonic.headless.server.pojo.TagObjectFilter;
import com.tencent.supersonic.headless.server.web.service.TagObjectService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/api/semantic/tagObject")
public class TagObjectController {

    private final TagObjectService tagObjectService;

    public TagObjectController(TagObjectService tagObjectService) {
        this.tagObjectService = tagObjectService;
    }

    /**
     * 新建标签对象
     *
     * @param tagObjectReq
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @PostMapping("/create")
    public TagObjectResp create(@RequestBody TagObjectReq tagObjectReq,
                                HttpServletRequest request,
                                HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        return tagObjectService.create(tagObjectReq, user);
    }

    /**
     * 编辑标签对象
     *
     * @param tagObjectReq
     * @param request
     * @param response
     * @return
     */
    @PostMapping("/update")
    public TagObjectResp update(@RequestBody TagObjectReq tagObjectReq,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return tagObjectService.update(tagObjectReq, user);
    }

    /**
     * 删除标签对象
     *
     * @param id
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @DeleteMapping("delete/{id}")
    public Boolean delete(@PathVariable("id") Long id,
                          HttpServletRequest request,
                          HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        tagObjectService.delete(id, user, true);
        return true;
    }

    /**
     * 标签对象-查询
     * @param filter
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @PostMapping("/query")
    public List<TagObjectResp> queryTagObject(@RequestBody TagObjectFilter filter,
                                         HttpServletRequest request,
                                         HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        return tagObjectService.getTagObjects(filter, user);
    }
}