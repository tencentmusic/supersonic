package com.tencent.supersonic.headless.server.rest;

import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.request.ClassReq;
import com.tencent.supersonic.headless.api.pojo.response.ClassResp;
import com.tencent.supersonic.headless.server.pojo.ClassFilter;
import com.tencent.supersonic.headless.server.service.ClassService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/semantic/class")
public class ClassController {

    private final ClassService classService;

    public ClassController(ClassService classService) {
        this.classService = classService;
    }

    /**
     * 新建目录
     *
     * @param classReq
     * @param request
     * @param response
     * @return
     */
    @PostMapping("/create")
    public ClassResp create(@RequestBody @Valid ClassReq classReq, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return classService.create(classReq, user);
    }

    /**
     * 修改目录
     *
     * @param classReq
     * @param request
     * @param response
     * @return
     */
    @PutMapping("/update")
    public ClassResp update(@RequestBody @Valid ClassReq classReq, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return classService.update(classReq, user);
    }

    /**
     * 删除目录
     *
     * @param id
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @DeleteMapping("delete/{id}/{force}")
    public Boolean delete(@PathVariable("id") Long id, @PathVariable("force") Boolean force,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        return classService.delete(id, force, user);
    }

    /**
     * 删除目录
     *
     * @param filter
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @GetMapping("delete/{id}/{force}")
    public List<ClassResp> get(@RequestBody @Valid ClassFilter filter, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return classService.getClassList(filter, user);
    }
}
