package com.tencent.supersonic;

import com.tencent.supersonic.headless.server.service.DomainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
@Slf4j
@Order(1)
public class HeadlessDemoLoader implements CommandLineRunner {

    private static boolean isLoad = false;

    @Autowired
    private DomainService domainService;

    @Autowired
    private ModelDemoDataLoader modelDataDemoLoader;

    @Autowired
    private BenchMarkDemoDataLoader benchMarkDemoLoader;

    @Value("${demo.enabled:false}")
    private boolean demoEnabled;

    @Override
    public void run(String... args) {
        if (!checkLoadDemo()) {
            log.info("skip load demo");
            return;
        }
        modelDataDemoLoader.doRun();
        benchMarkDemoLoader.doRun();
        isLoad = true;
    }

    private boolean checkLoadDemo() {
        if (!demoEnabled) {
            return false;
        }
        return CollectionUtils.isEmpty(domainService.getDomainList());
    }

    public static boolean isLoad() {
        return isLoad;
    }

}