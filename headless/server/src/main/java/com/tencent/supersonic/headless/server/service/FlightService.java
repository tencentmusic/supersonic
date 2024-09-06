package com.tencent.supersonic.headless.server.service;

import org.apache.arrow.flight.sql.FlightSqlProducer;

import java.util.concurrent.ExecutorService;

public interface FlightService extends FlightSqlProducer {

    void setLocation(String host, Integer port);

    void setExecutorService(ExecutorService executorService, Integer queue, Integer expireMinute);
}
