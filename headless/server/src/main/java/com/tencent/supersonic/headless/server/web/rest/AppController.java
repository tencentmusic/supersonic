package com.tencent.supersonic.headless.server.web.rest;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.headless.api.pojo.request.AppQueryReq;
import com.tencent.supersonic.headless.api.pojo.request.AppReq;
import com.tencent.supersonic.headless.api.pojo.response.AppDetailResp;
import com.tencent.supersonic.headless.api.pojo.response.AppResp;
import com.tencent.supersonic.headless.server.web.service.AppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/semantic/app")
public class AppController {

    @Autowired
    private AppService appService;

    @PostMapping
    public boolean save(@RequestBody AppReq app,
                     HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        appService.save(app, user);
        return true;
    }

    @PutMapping
    public boolean update(@RequestBody AppReq app,
                       HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        appService.update(app, user);
        return true;
    }

    @PutMapping("/online/{id}")
    public boolean online(@PathVariable("id") Integer id,
                       HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        appService.online(id, user);
        return true;
    }

    @PutMapping("/offline/{id}")
    public boolean offline(@PathVariable("id") Integer id,
                        HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        appService.offline(id, user);
        return true;
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable("id") Integer id,
                       HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        appService.delete(id, user);
        return true;
    }

    @GetMapping("/{id}")
    public AppDetailResp getApp(@PathVariable("id") Integer id,
                                HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return appService.getApp(id, user);
    }

    @PostMapping("/page")
    public PageInfo<AppResp> pageApp(@RequestBody AppQueryReq appQueryReq,
                                     HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return appService.pageApp(appQueryReq, user);
    }

}
