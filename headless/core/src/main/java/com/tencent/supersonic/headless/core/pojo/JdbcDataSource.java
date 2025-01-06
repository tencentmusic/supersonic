package com.tencent.supersonic.headless.core.pojo;

import com.alibaba.druid.pool.DruidDataSource;
import com.tencent.supersonic.headless.api.pojo.enums.DataType;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.core.utils.JdbcDataSourceUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
public class JdbcDataSource {

    private static final Object lockLock = new Object();
    private static volatile Map<String, DruidDataSource> dataSourceMap = new ConcurrentHashMap<>();
    private static volatile Map<String, Lock> dataSourceLockMap = new ConcurrentHashMap<>();

    @Value("${source.lock-time:30}")
    @Getter
    protected Long lockTime;

    @Value("${source.max-active:2}")
    @Getter
    protected int maxActive;

    @Value("${source.initial-size:0}")
    @Getter
    protected int initialSize;

    @Value("${source.min-idle:1}")
    @Getter
    protected int minIdle;

    @Value("${source.max-wait:60000}")
    @Getter
    protected long maxWait;

    @Value("${source.time-between-eviction-runs-millis:2000}")
    @Getter
    protected long timeBetweenEvictionRunsMillis;

    @Value("${source.min-evictable-idle-time-millis:600000}")
    @Getter
    protected long minEvictableIdleTimeMillis;

    @Value("${source.max-evictable-idle-time-millis:900000}")
    @Getter
    protected long maxEvictableIdleTimeMillis;

    @Value("${source.time-between-connect-error-millis:60000}")
    @Getter
    protected long timeBetweenConnectErrorMillis;

    @Value("${source.test-while-idle:true}")
    @Getter
    protected boolean testWhileIdle;

    @Value("${source.test-on-borrow:false}")
    @Getter
    protected boolean testOnBorrow;

    @Value("${source.test-on-return:false}")
    @Getter
    protected boolean testOnReturn;

    @Value("${source.break-after-acquire-failure:true}")
    @Getter
    protected boolean breakAfterAcquireFailure;

    @Value("${source.connection-error-retry-attempts:1}")
    @Getter
    protected int connectionErrorRetryAttempts;

    @Value("${source.keep-alive:false}")
    @Getter
    protected boolean keepAlive;

    @Value("${source.validation-query-timeout:5}")
    @Getter
    protected int validationQueryTimeout;

    @Value("${source.validation-query:select 1}")
    @Getter
    protected String validationQuery;

    private Lock getDataSourceLock(String key) {
        if (dataSourceLockMap.containsKey(key)) {
            return dataSourceLockMap.get(key);
        }

        synchronized (lockLock) {
            if (dataSourceLockMap.containsKey(key)) {
                return dataSourceLockMap.get(key);
            }
            Lock lock = new ReentrantLock();
            dataSourceLockMap.put(key, lock);
            return lock;
        }
    }

    public void removeDatasource(DatabaseResp database) {

        String key = getDataSourceKey(database);

        Lock lock = getDataSourceLock(key);

        if (!lock.tryLock()) {
            return;
        }

        try {
            DruidDataSource druidDataSource = dataSourceMap.remove(key);
            if (druidDataSource != null) {
                druidDataSource.close();
            }

            dataSourceLockMap.remove(key);
        } finally {
            lock.unlock();
        }
    }

    public DruidDataSource getDataSource(DatabaseResp database) throws RuntimeException {

        String name = database.getName();
        String jdbcUrl = database.getUrl();
        String username = database.getUsername();
        String password = database.passwordDecrypt();

        String key = getDataSourceKey(database);

        DruidDataSource druidDataSource = dataSourceMap.get(key);
        if (druidDataSource != null && !druidDataSource.isClosed()) {
            return druidDataSource;
        }

        Lock lock = getDataSourceLock(key);

        try {
            if (!lock.tryLock(lockTime, TimeUnit.SECONDS)) {
                druidDataSource = dataSourceMap.get(key);
                if (druidDataSource != null && !druidDataSource.isClosed()) {
                    return druidDataSource;
                }
                throw new RuntimeException("Unable to get datasource for jdbcUrl: " + jdbcUrl);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Unable to get datasource for jdbcUrl: " + jdbcUrl);
        }

        druidDataSource = dataSourceMap.get(key);
        if (druidDataSource != null && !druidDataSource.isClosed()) {
            lock.unlock();
            return druidDataSource;
        }

        druidDataSource = new DruidDataSource();

        try {
            String className = JdbcDataSourceUtils.getDriverClassName(jdbcUrl);
            try {
                Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Unable to get driver instance for jdbcUrl: " + jdbcUrl);
            }

            druidDataSource.setDriverClassName(className);

            druidDataSource.setName(name);
            druidDataSource.setUrl(jdbcUrl);
            druidDataSource.setUsername(username);

            if (!jdbcUrl.toLowerCase().contains(DataType.PRESTO.getFeature())) {
                druidDataSource.setPassword(password);
            }

            druidDataSource.setInitialSize(initialSize);
            druidDataSource.setMinIdle(minIdle);
            druidDataSource.setMaxActive(maxActive);
            druidDataSource.setMaxWait(maxWait);
            druidDataSource.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
            druidDataSource.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
            druidDataSource.setMaxEvictableIdleTimeMillis(maxEvictableIdleTimeMillis);
            druidDataSource.setTimeBetweenConnectErrorMillis(timeBetweenConnectErrorMillis);
            druidDataSource.setTestWhileIdle(testWhileIdle);
            druidDataSource.setTestOnBorrow(testOnBorrow);
            druidDataSource.setTestOnReturn(testOnReturn);
            druidDataSource.setConnectionErrorRetryAttempts(connectionErrorRetryAttempts);
            druidDataSource.setBreakAfterAcquireFailure(breakAfterAcquireFailure);
            druidDataSource.setKeepAlive(keepAlive);
            druidDataSource.setValidationQueryTimeout(validationQueryTimeout);
            druidDataSource.setRemoveAbandoned(true);
            druidDataSource.setRemoveAbandonedTimeout(3600 + 5 * 60);
            druidDataSource.setLogAbandoned(true);

            // default validation query
            String driverName = druidDataSource.getDriverClassName();
            if (driverName.indexOf("sqlserver") != -1 || driverName.indexOf("mysql") != -1
                    || driverName.indexOf("h2") != -1 || driverName.indexOf("moonbox") != -1) {
                druidDataSource.setValidationQuery("select 1");
            }

            if (driverName.indexOf("oracle") != -1) {
                druidDataSource.setValidationQuery("select 1 from dual");
            }

            if (driverName.indexOf("elasticsearch") != -1) {
                druidDataSource.setValidationQuery(null);
            }

            Properties properties = new Properties();
            if (driverName.indexOf("mysql") != -1) {
                properties.setProperty("druid.mysql.usePingMethod", "false");
            }

            druidDataSource.setConnectProperties(properties);

            try {
                druidDataSource.init();
            } catch (Exception e) {
                log.error("Exception during pool initialization", e);
                throw new RuntimeException(e.getMessage());
            }

            dataSourceMap.put(key, druidDataSource);

        } finally {
            lock.unlock();
        }

        return druidDataSource;
    }

    private String getDataSourceKey(DatabaseResp database) {
        return JdbcDataSourceUtils.getKey(database.getName(), database.getUrl(),
                database.getUsername(), database.passwordDecrypt(), "", false);
    }
}
