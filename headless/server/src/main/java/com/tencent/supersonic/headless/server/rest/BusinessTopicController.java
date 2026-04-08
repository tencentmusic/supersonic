package com.tencent.supersonic.headless.server.rest;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.server.persistence.dataobject.BusinessTopicDO;
import com.tencent.supersonic.headless.server.pojo.BusinessTopicVO;
import com.tencent.supersonic.headless.server.service.BusinessTopicService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/businessTopics")
@RequiredArgsConstructor
public class BusinessTopicController {

    private final BusinessTopicService businessTopicService;

    @GetMapping
    public Page<BusinessTopicVO> list(@RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Boolean enabled, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return businessTopicService.listTopics(new Page<>(current, pageSize), enabled, user);
    }

    @GetMapping("/{id}")
    public BusinessTopicVO getDetail(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return businessTopicService.getTopicDetail(id, user);
    }

    @PostMapping
    public BusinessTopicDO create(@RequestBody BusinessTopicDO topic, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return businessTopicService.createTopic(topic, user);
    }

    @PatchMapping("/{id}")
    public BusinessTopicDO update(@PathVariable Long id, @RequestBody BusinessTopicDO topic,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        topic.setId(id);
        return businessTopicService.updateTopic(topic, user);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        businessTopicService.deleteTopic(id, user);
    }

    @PostMapping("/{id}/items")
    public void addItems(@PathVariable Long id, @RequestBody Map<String, List<?>> body,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        @SuppressWarnings("unchecked")
        List<String> itemTypes = (List<String>) body.get("itemTypes");
        List<Long> itemIds =
                ((List<Number>) body.get("itemIds")).stream().map(Number::longValue).toList();
        businessTopicService.addItems(id, itemTypes, itemIds, user);
    }

    @DeleteMapping("/{id}/items/{itemType}/{itemId}")
    public void removeItem(@PathVariable Long id, @PathVariable String itemType,
            @PathVariable Long itemId, HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        businessTopicService.removeItem(id, itemType, itemId, user);
    }
}
