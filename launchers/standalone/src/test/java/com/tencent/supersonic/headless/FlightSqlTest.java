package com.tencent.supersonic.headless;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.auth.authentication.strategy.FakeUserStrategy;
import com.tencent.supersonic.headless.server.task.FlightServerInitTask;
import lombok.extern.slf4j.Slf4j;
import org.apache.arrow.flight.CallHeaders;
import org.apache.arrow.flight.FlightCallHeaders;
import org.apache.arrow.flight.FlightClient;
import org.apache.arrow.flight.FlightInfo;
import org.apache.arrow.flight.FlightStream;
import org.apache.arrow.flight.HeaderCallOption;
import org.apache.arrow.flight.Location;
import org.apache.arrow.flight.sql.FlightSqlClient;
import org.apache.arrow.memory.RootAllocator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class FlightSqlTest extends BaseTest {


    @Autowired
    private FlightServerInitTask flightSqlListener;
    @Autowired
    private FakeUserStrategy fakeUserStrategy;

    @Test
    void test01() throws Exception {
        startServer();
        String host = flightSqlListener.getHost();
        Integer port = flightSqlListener.getPort();
        FlightSqlClient sqlClient = new FlightSqlClient(
                FlightClient.builder(new RootAllocator(Integer.MAX_VALUE), Location.forGrpcInsecure(host, port))
                        .build());

        CallHeaders headers = new FlightCallHeaders();
        headers.insert("dataSetId", "1");
        headers.insert("name", "admin");
        headers.insert("password", "admin");
        HeaderCallOption headerOption = new HeaderCallOption(headers);
        try (final FlightSqlClient.PreparedStatement preparedStatement = sqlClient.prepare(
                "SELECT 部门, SUM(访问次数) AS 访问次数 FROM 超音数PVUV统计  GROUP BY 部门",
                headerOption)) {
            final FlightInfo info = preparedStatement.execute();
            FlightStream stream = sqlClient.getStream(info
                    .getEndpoints()
                    .get(0).getTicket());
            int rowCnt = 0;
            int colCnt = 0;
            while (stream.next()) {
                if (stream.getRoot().getRowCount() > 0) {
                    colCnt = stream.getRoot().getFieldVectors().size();
                    rowCnt += stream.getRoot().getRowCount();
                }
            }
            assertEquals(2, colCnt);
            assertTrue(rowCnt > 0);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Test
    void test02() throws Exception {
        startServer();
        String host = flightSqlListener.getHost();
        Integer port = flightSqlListener.getPort();
        FlightSqlClient sqlClient = new FlightSqlClient(
                FlightClient.builder(new RootAllocator(Integer.MAX_VALUE), Location.forGrpcInsecure(host, port))
                        .build());

        CallHeaders headers = new FlightCallHeaders();
        headers.insert("dataSetId", "1");
        headers.insert("name", "admin");
        headers.insert("password", "admin");
        HeaderCallOption headerOption = new HeaderCallOption(headers);
        try {
            FlightInfo flightInfo = sqlClient.execute(
                    "SELECT 部门, SUM(访问次数) AS 访问次数 FROM 超音数PVUV统计  GROUP BY 部门",
                    headerOption);
            FlightStream stream = sqlClient.getStream(flightInfo
                    .getEndpoints()
                    .get(0).getTicket());
            int rowCnt = 0;
            int colCnt = 0;
            while (stream.next()) {
                if (stream.getRoot().getRowCount() > 0) {
                    colCnt = stream.getRoot().getFieldVectors().size();
                    rowCnt += stream.getRoot().getRowCount();
                }
            }
            assertEquals(2, colCnt);
            assertTrue(rowCnt > 0);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    private void startServer() {
        if (!flightSqlListener.isRunning()) {
            UserHolder.setStrategy(fakeUserStrategy);
            flightSqlListener.startServer();
        }
    }
}
