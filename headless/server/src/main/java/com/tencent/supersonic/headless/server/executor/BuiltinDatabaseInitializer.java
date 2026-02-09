package com.tencent.supersonic.headless.server.executor;

import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.util.AESEncryptionUtil;
import com.tencent.supersonic.headless.api.pojo.enums.DataType;
import com.tencent.supersonic.headless.api.pojo.request.DatabaseReq;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.server.service.DatabaseService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Component
@Order(0)
@Slf4j
@RequiredArgsConstructor
public class BuiltinDatabaseInitializer implements CommandLineRunner {

    private final DatabaseService databaseService;
    private final UserService userService;
    private final DataSourceProperties dataSourceProperties;

    @Value("${spring.datasource.driver-class-name:}")
    private String driverClassName;

    @Getter
    private DatabaseResp demoDatabase;

    @Override
    public void run(String... args) {
        try {
            demoDatabase = initDatabaseIfNotExist();
        } catch (Exception e) {
            log.error("Failed to initialize builtin database", e);
        }
    }

    private DatabaseResp initDatabaseIfNotExist() {
        User user = userService.getDefaultUser();
        List<DatabaseResp> databaseList = databaseService.getDatabaseList(user);
        if (!CollectionUtils.isEmpty(databaseList)) {
            return databaseList.getFirst();
        }
        String url = dataSourceProperties.getUrl();
        DatabaseReq databaseReq = new DatabaseReq();
        databaseReq.setName("S2数据库DEMO");
        databaseReq.setDescription("样例数据库实例仅用于体验");
        databaseReq.setType(DataType.H2.toString());
        if ("org.postgresql.Driver".equals(driverClassName)) {
            databaseReq.setType(DataType.POSTGRESQL.toString());
        } else if ("com.mysql.cj.jdbc.Driver".equals(driverClassName)
                || "com.mysql.jdbc.Driver".equals(driverClassName)) {
            databaseReq.setType(DataType.MYSQL.toString());
            databaseReq.setVersion("5.7");
        }
        databaseReq.setUrl(url);
        databaseReq.setUsername(dataSourceProperties.getUsername());
        databaseReq
                .setPassword(AESEncryptionUtil.aesEncryptECB(dataSourceProperties.getPassword()));
        log.info("Creating builtin database configuration");
        return databaseService.createOrUpdateDatabase(databaseReq, user);
    }
}
