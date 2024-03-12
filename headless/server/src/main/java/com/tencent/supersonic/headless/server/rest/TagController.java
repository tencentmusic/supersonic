package com.tencent.supersonic.headless.server.rest;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.headless.api.pojo.request.ItemValueReq;
import com.tencent.supersonic.headless.api.pojo.request.MetaBatchReq;
import com.tencent.supersonic.headless.api.pojo.request.TagBatchCreateReq;
import com.tencent.supersonic.headless.api.pojo.request.TagReq;
import com.tencent.supersonic.headless.api.pojo.response.ItemValueResp;
import com.tencent.supersonic.headless.api.pojo.response.TagResp;
import com.tencent.supersonic.headless.server.pojo.TagFilterPage;
import com.tencent.supersonic.headless.server.service.TagMetaService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tencent.supersonic.headless.server.service.TagQueryService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/semantic/tag")
public class TagController {

    private final TagMetaService tagMetaService;
    private final TagQueryService tagQueryService;
    public TagController(TagMetaService tagMetaService,
                         TagQueryService tagQueryService) {
        this.tagMetaService = tagMetaService;
        this.tagQueryService = tagQueryService;
    }

    /**
     * 新建标签
     * @param tagReq
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @PostMapping("/create")
    public TagResp create(@RequestBody TagReq tagReq,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        return tagMetaService.create(tagReq, user);
    }

    /**
     * 从现有维度/指标批量新建标签
     * @param tagBatchReq
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @PostMapping("/create/batch")
    public Integer createBatch(@RequestBody TagBatchCreateReq tagBatchReq,
                          HttpServletRequest request,
                          HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        return tagMetaService.createBatch(tagBatchReq, user);
    }

    /**
     * 编辑标签信息
     * @param tagReq
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @PostMapping("/update")
    public TagResp update(@RequestBody TagReq tagReq,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        return tagMetaService.update(tagReq, user);
    }

    /**
     * 批量更新标签状态
     * @param metaBatchReq
     * @param request
     * @param response
     * @return
     */
    @PostMapping("/batchUpdateStatus")
    public Boolean batchUpdateStatus(@RequestBody MetaBatchReq metaBatchReq,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return tagMetaService.batchUpdateStatus(metaBatchReq, user);
    }

    /**
     * 标签删除
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
        tagMetaService.delete(id, user);
        return true;
    }

    /**
     * 标签详情获取
     * @param id
     * @param request
     * @param response
     * @return
     */
    @GetMapping("getTag/{id}")
    public TagResp getTag(@PathVariable("id") Long id,
            HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return tagMetaService.getTag(id, user);
    }

    /**
     * 标签市场-分页查询
     * @param tagFilterPage
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @PostMapping("/queryTag")
    public PageInfo<TagResp> queryPage(@RequestBody TagFilterPage tagFilterPage,
                                         HttpServletRequest request,
                                         HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        return tagMetaService.queryPage(tagFilterPage, user);
    }


    /**
     * 获取标签值分布信息
     * @param itemValueReq
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @PostMapping("/value/distribution")
    public ItemValueResp queryTagValue(@RequestBody ItemValueReq itemValueReq,
                                       HttpServletRequest request,
                                       HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        return tagQueryService.queryTagValue(itemValueReq, user);
    }

}
