package com.tencent.supersonic.headless.server.task;

import com.tencent.supersonic.headless.server.facade.service.FlightService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.apache.arrow.flight.FlightServer;
import org.apache.arrow.flight.Location;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

/**
 * Initialize flight jdbc server
 */
@Component
@Slf4j
public class FlightServerInitTask implements CommandLineRunner {

    @Value("${s2.flightSql.enable:false}")
    private Boolean enable = false;
    @Value("${s2.flightSql.host:localhost}")
    private String host = "localhost";
    @Value("${s2.flightSql.port:9081}")
    private Integer port = 9081;
    @Value("${s2.flightSql.executor:4}")
    private Integer executor = 4;
    @Value("${s2.flightSql.queue:128}")
    private Integer queue = 128;
    @Value("${s2.flightSql.expireMinute:10}")
    private Integer expireMinute = 10;

    private final FlightService flightService;
    private ExecutorService executorService;
    private FlightServer flightServer;
    private BufferAllocator allocator;
    private Boolean isRunning = false;

    public FlightServerInitTask(FlightService flightService) {
        this.allocator = new RootAllocator();
        this.flightService = flightService;
        this.flightService.setLocation(host, port);
        executorService = Executors.newFixedThreadPool(executor);
        this.flightService.setExecutorService(executorService, queue, expireMinute);
        Location listenLocation = Location.forGrpcInsecure(host, port);
        flightServer = FlightServer.builder(allocator, listenLocation, this.flightService)
                .build();
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public void startServer() {
        try {
            log.info("Arrow Flight JDBC server started on {} {}", host, port);
            flightServer.start();
            isRunning = true;
        } catch (Exception e) {
            log.error("FlightServerInitTask start error {}", e);
        }

    }

    public Boolean isRunning() {
        return isRunning;
    }

    @PreDestroy
    public void onShutdown() {
        try {
            log.info("Arrow Flight JDBC server stop on {} {}", host, port);
            flightServer.close();
            allocator.close();
        } catch (Exception e) {
            log.error("FlightServerInitTask start error {}", e);
        }
    }

    @Override
    public void run(String... args) throws Exception {
        if (enable) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        startServer();
                        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                            try {
                                flightServer.close();
                                allocator.close();
                            } catch (Exception e) {
                                log.error("flightServer close error {}", e);
                            }
                        }));
                        //flightServer.awaitTermination();
                    } catch (Exception e) {
                        log.error("run error {}", e);
                    }
                }
            }.start();
        }
    }
}
