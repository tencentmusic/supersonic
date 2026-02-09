package com.tencent.supersonic.headless.server.service.impl;

import javax.sql.DataSource;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.core.adaptor.db.DbAdaptor;
import com.tencent.supersonic.headless.core.adaptor.db.DbAdaptorFactory;
import com.tencent.supersonic.headless.core.pojo.ConnectInfo;
import com.tencent.supersonic.headless.core.pojo.JdbcDataSource;
import com.tencent.supersonic.headless.server.service.DatabaseService;
import com.tencent.supersonic.headless.server.service.RowCountEstimator;
import com.tencent.supersonic.headless.server.utils.DatabaseConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RowCountEstimatorImpl implements RowCountEstimator {

    private static final long UNKNOWN_ESTIMATE = -1L;
    private static final String POOL_TYPE_INTERACTIVE = "INTERACTIVE";

    private final DatabaseService databaseService;
    private final JdbcDataSource jdbcDataSource;

    private final Cache<String, Long> estimateCache =
            Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).maximumSize(1000).build();

    public RowCountEstimatorImpl(DatabaseService databaseService, JdbcDataSource jdbcDataSource) {
        this.databaseService = databaseService;
        this.jdbcDataSource = jdbcDataSource;
    }

    @Override
    public long estimate(Long databaseId, String sql) {
        if (databaseId == null || sql == null || sql.isBlank()) {
            return UNKNOWN_ESTIMATE;
        }

        // Check cache first
        String cacheKey = databaseId + ":" + sql.hashCode();
        Long cached = estimateCache.getIfPresent(cacheKey);
        if (cached != null) {
            log.debug("Row count estimate cache hit: databaseId={}, estimate={}", databaseId,
                    cached);
            return cached;
        }

        try {
            DatabaseResp database = databaseService.getDatabase(databaseId);
            if (database == null) {
                log.warn("Database not found: {}", databaseId);
                return UNKNOWN_ESTIMATE;
            }

            DbAdaptor dbAdaptor = DbAdaptorFactory.getEngineAdaptor(database.getType());
            ConnectInfo connectInfo = DatabaseConverter.getConnectInfo(database);

            List<String> explainResult = runExplain(connectInfo, sql);
            long estimate = dbAdaptor.parseExplainRowCount(explainResult);

            if (estimate > 0) {
                estimateCache.put(cacheKey, estimate);
                log.info("Row count estimate: databaseId={}, sql_hash={}, estimate={}", databaseId,
                        sql.hashCode(), estimate);
            }

            return estimate;
        } catch (Exception e) {
            log.warn("Failed to estimate row count for databaseId={}: {}", databaseId,
                    e.getMessage());
            return UNKNOWN_ESTIMATE;
        }
    }

    private List<String> runExplain(ConnectInfo connectInfo, String sql) throws Exception {
        List<String> results = new ArrayList<>();
        String explainSql = "EXPLAIN " + sql;

        // Use pooled connection from INTERACTIVE pool for EXPLAIN queries
        DatabaseResp dbResp = DatabaseResp.builder().name("explain").url(connectInfo.getUrl())
                .username(connectInfo.getUserName()).password(connectInfo.getPassword()).build();

        DataSource dataSource =
                jdbcDataSource.getDataSource(dbResp, POOL_TYPE_INTERACTIVE, null, null);

        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(explainSql)) {

            int colCount = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                StringBuilder row = new StringBuilder();
                for (int i = 1; i <= colCount; i++) {
                    if (i > 1) {
                        row.append(" ");
                    }
                    String colName = rs.getMetaData().getColumnName(i);
                    Object value = rs.getObject(i);
                    row.append(colName).append("=").append(value);
                }
                results.add(row.toString());
            }
        }

        return results;
    }
}
