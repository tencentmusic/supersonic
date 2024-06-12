package com.tencent.supersonic.headless.server.service;

import java.util.concurrent.ExecutorService;
import org.apache.arrow.flight.sql.FlightSqlProducer;

public interface FlightService extends FlightSqlProducer {

    void setLocation(String host, Integer port);

    void setExecutorService(ExecutorService executorService, Integer queue, Integer expireMinute);
}
