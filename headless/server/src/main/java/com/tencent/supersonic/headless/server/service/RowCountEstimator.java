package com.tencent.supersonic.headless.server.service;

/**
 * Estimates row counts for SQL queries using database EXPLAIN functionality. Used to decide whether
 * to execute queries synchronously or asynchronously based on estimated result size.
 */
public interface RowCountEstimator {

    /**
     * Estimate the number of rows that a SQL query will return.
     *
     * @param databaseId the database ID to run the estimation against
     * @param sql the SQL query to estimate
     * @return estimated row count, or -1 if estimation is not possible
     */
    long estimate(Long databaseId, String sql);
}
