package com.tencent.supersonic.chat.rest;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.chat.application.knowledge.DictApplicationService;
import com.tencent.supersonic.knowledge.domain.pojo.DictTaskFilter;
import com.tencent.supersonic.knowledge.domain.pojo.DimValue2DictCommand;
import com.tencent.supersonic.knowledge.domain.pojo.DimValueDictInfo;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/chat/dict")
public class DictController {

    private final DictApplicationService dictApplicationService;

    public DictController(DictApplicationService dictApplicationService) {
        this.dictApplicationService = dictApplicationService;
    }

    /**
     * addDictInfo
     *
     * @param dimValue2DictCommend
     */
    @PostMapping("/task")
    public Long addDictTask(@RequestBody DimValue2DictCommand dimValue2DictCommend,
            HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return dictApplicationService.addDictTask(dimValue2DictCommend, user);
    }

    /**
     * deleteDictInfo
     *
     * @param dimValue2DictCommend
     */
    @DeleteMapping("/task")
    public Long deleteDictTask(@RequestBody DimValue2DictCommand dimValue2DictCommend,
            HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return dictApplicationService.deleteDictTask(dimValue2DictCommend, user);
    }

    /**
     * searchDictTaskList
     *
     * @param filter
     */
    @PostMapping("/task/search")
    public List<DimValueDictInfo> searchDictTaskList(@RequestBody DictTaskFilter filter,
            HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return dictApplicationService.searchDictTaskList(filter, user);
    }

    @GetMapping("/rootPath")
    public String getDictRootPath(HttpServletRequest request,
            HttpServletResponse response) {
        return dictApplicationService.getDictRootPath();
    }

}