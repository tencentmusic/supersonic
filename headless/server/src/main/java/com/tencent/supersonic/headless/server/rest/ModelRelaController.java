package com.tencent.supersonic.headless.server.rest;

import com.tencent.supersonic.common.pojo.ModelRela;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.server.service.ModelRelaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/semantic/modelRela")
@RequiredArgsConstructor
public class ModelRelaController {

    private final ModelRelaService modelRelaService;

    @PostMapping
    public boolean save(@RequestBody ModelRela modelRela, User user) {
        modelRelaService.save(modelRela, user);
        return true;
    }

    @PutMapping
    public boolean update(@RequestBody ModelRela modelRela, User user) {
        modelRelaService.update(modelRela, user);
        return true;
    }

    @RequestMapping("/list")
    public List<ModelRela> getModelRelaList(@RequestParam("domainId") Long domainId) {
        return modelRelaService.getModelRelaList(domainId);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) {
        modelRelaService.delete(id);
    }
}
