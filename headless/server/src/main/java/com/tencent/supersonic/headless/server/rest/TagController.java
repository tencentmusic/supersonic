package com.tencent.supersonic.headless.server.rest;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.request.ItemValueReq;
import com.tencent.supersonic.headless.api.pojo.request.TagDeleteReq;
import com.tencent.supersonic.headless.api.pojo.request.TagFilterPageReq;
import com.tencent.supersonic.headless.api.pojo.request.TagReq;
import com.tencent.supersonic.headless.api.pojo.response.ItemValueResp;
import com.tencent.supersonic.headless.api.pojo.response.TagResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.TagDO;
import com.tencent.supersonic.headless.server.pojo.TagFilter;
import com.tencent.supersonic.headless.server.service.TagMetaService;
import com.tencent.supersonic.headless.server.service.TagQueryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/semantic/tag")
public class TagController {

    private final TagMetaService tagMetaService;
    private final TagQueryService tagQueryService;

    public TagController(TagMetaService tagMetaService, TagQueryService tagQueryService) {
        this.tagMetaService = tagMetaService;
        this.tagQueryService = tagQueryService;
    }

    /**
     * 新建标签
     *
     * @param tagReq
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @PostMapping("/create")
    public TagResp create(@RequestBody TagReq tagReq, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        return tagMetaService.create(tagReq, user);
    }

    /**
     * 从现有维度/指标批量新建标签
     *
     * @param tagReqList
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @PostMapping("/create/batch")
    public Integer createBatch(@RequestBody @Valid List<TagReq> tagReqList,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        return tagMetaService.createBatch(tagReqList, user);
    }

    /**
     * 批量删除标签
     *
     * @param tagDeleteReqList
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @PostMapping("/delete/batch")
    public Boolean deleteBatch(@RequestBody @Valid List<TagDeleteReq> tagDeleteReqList,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        return tagMetaService.deleteBatch(tagDeleteReqList, user);
    }

    /**
     * 标签删除
     *
     * @param id
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @DeleteMapping("delete/{id}")
    public Boolean delete(@PathVariable("id") Long id, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        tagMetaService.delete(id, user);
        return true;
    }

    /**
     * 标签详情获取
     *
     * @param id
     * @param request
     * @param response
     * @return
     */
    @GetMapping("getTag/{id}")
    public TagResp getTag(@PathVariable("id") Long id, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return tagMetaService.getTag(id, user);
    }

    /**
     * 标签查询
     *
     * @param tagFilter
     * @return
     */
    @PostMapping("/queryTag")
    public List<TagDO> queryPage(@RequestBody TagFilter tagFilter) {
        return tagMetaService.getTagDOList(tagFilter);
    }

    /**
     * 获取标签值分布信息
     *
     * @param itemValueReq
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @PostMapping("/value/distribution")
    public ItemValueResp queryTagValue(@RequestBody ItemValueReq itemValueReq,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        return tagQueryService.queryTagValue(itemValueReq, user);
    }

    /**
     * 标签市场-分页查询
     *
     * @param tagMarketPageReq
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @PostMapping("/queryTag/market")
    public PageInfo<TagResp> queryTagMarketPage(@RequestBody TagFilterPageReq tagMarketPageReq,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        return tagMetaService.queryTagMarketPage(tagMarketPageReq, user);
    }
}
