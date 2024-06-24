package com.tencent.supersonic.headless.server.service.impl;

import static com.google.protobuf.Any.pack;
import static com.google.protobuf.ByteString.copyFrom;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.apache.arrow.adapter.jdbc.JdbcToArrow.sqlToArrowVectorIterator;
import static org.apache.arrow.adapter.jdbc.JdbcToArrowUtils.jdbcToArrowSchema;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.tencent.supersonic.auth.api.authentication.config.AuthenticationConfig;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.request.UserReq;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.headless.api.pojo.Param;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.server.service.FlightService;
import com.tencent.supersonic.headless.server.service.SemanticLayerService;
import com.tencent.supersonic.headless.server.utils.FlightUtils;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import javax.sql.RowSetMetaData;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.RowSetProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.arrow.adapter.jdbc.ArrowVectorIterator;
import org.apache.arrow.adapter.jdbc.JdbcToArrowUtils;
import org.apache.arrow.flight.CallHeaders;
import org.apache.arrow.flight.CallStatus;
import org.apache.arrow.flight.FlightConstants;
import org.apache.arrow.flight.FlightDescriptor;
import org.apache.arrow.flight.FlightEndpoint;
import org.apache.arrow.flight.FlightInfo;
import org.apache.arrow.flight.Location;
import org.apache.arrow.flight.Result;
import org.apache.arrow.flight.Ticket;
import org.apache.arrow.flight.sql.BasicFlightSqlProducer;
import org.apache.arrow.flight.sql.impl.FlightSql.ActionClosePreparedStatementRequest;
import org.apache.arrow.flight.sql.impl.FlightSql.ActionCreatePreparedStatementRequest;
import org.apache.arrow.flight.sql.impl.FlightSql.ActionCreatePreparedStatementResult;
import org.apache.arrow.flight.sql.impl.FlightSql.CommandPreparedStatementQuery;
import org.apache.arrow.flight.sql.impl.FlightSql.CommandStatementQuery;
import org.apache.arrow.flight.sql.impl.FlightSql.TicketStatementQuery;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.VectorLoader;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.VectorUnloader;
import org.apache.arrow.vector.types.pojo.Schema;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * arrow flight FlightSqlProducer
 */
@Slf4j
@Service("FlightService")
public class FlightServiceImpl extends BasicFlightSqlProducer implements FlightService {

    private String host;
    private Integer port;
    private ExecutorService executorService;
    private Cache<ByteString, SemanticQueryReq> preparedStatementCache;
    private final String dataSetIdHeaderKey = "dataSetId";
    private final String nameHeaderKey = "name";
    private final String passwordHeaderKey = "password";
    private final Calendar defaultCalendar = JdbcToArrowUtils.getUtcCalendar();
    private final SemanticLayerService queryService;
    private final AuthenticationConfig authenticationConfig;
    private final UserService userService;

    public FlightServiceImpl(SemanticLayerService queryService,
                             AuthenticationConfig authenticationConfig,
                             UserService userService) {
        this.queryService = queryService;
        this.authenticationConfig = authenticationConfig;

        this.userService = userService;
    }

    public void setLocation(String host, Integer port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void setExecutorService(ExecutorService executorService, Integer queue, Integer expireMinute) {
        this.executorService = executorService;
        this.preparedStatementCache =
                CacheBuilder.newBuilder()
                        .maximumSize(queue)
                        .expireAfterWrite(expireMinute, TimeUnit.MINUTES)
                        .build();
    }

    @Override
    public FlightInfo getFlightInfo(CallContext callContext, FlightDescriptor flightDescriptor) {
        return super.getFlightInfo(callContext, flightDescriptor);
    }

    @Override
    public void getStreamStatement(final TicketStatementQuery ticketStatementQuery, final CallContext context,
            final ServerStreamListener listener) {
        final ByteString handle = ticketStatementQuery.getStatementHandle();
        log.info("getStreamStatement {} ", handle);
        executeQuery(handle, listener);
    }

    @Override
    public FlightInfo getFlightInfoStatement(final CommandStatementQuery request, final CallContext context,
            final FlightDescriptor descriptor) {
        try {
            ByteString preparedStatementHandle = addPrepared(context, request.getQuery());
            TicketStatementQuery ticket = TicketStatementQuery.newBuilder()
                    .setStatementHandle(preparedStatementHandle)
                    .build();
            return getFlightInfoForSchema(ticket, descriptor, null);
        } catch (Exception e) {
            log.error("getFlightInfoStatement error {}", e);
        }
        return null;
    }

    @Override
    public void getStreamPreparedStatement(final CommandPreparedStatementQuery command, final CallContext context,
            final ServerStreamListener listener) {
        log.info("getStreamPreparedStatement {}", command.getPreparedStatementHandle());
        executeQuery(command.getPreparedStatementHandle(), listener);
    }

    private void executeQuery(ByteString hander, final ServerStreamListener listener) {
        SemanticQueryReq semanticQueryReq = preparedStatementCache.getIfPresent(hander);
        if (Objects.isNull(semanticQueryReq)) {
            listener.error(CallStatus.INTERNAL
                    .withDescription("Failed to get prepared statement: empty")
                    .toRuntimeException());
            log.error("getStreamPreparedStatement error {}", hander);
            listener.completed();
            return;
        }
        executorService.submit(() -> {
            BufferAllocator rootAllocator = new RootAllocator();
            try {
                Optional<Param> authOpt = semanticQueryReq.getParams().stream()
                        .filter(p -> p.getName().equals(authenticationConfig.getTokenHttpHeaderKey())).findFirst();
                if (authOpt.isPresent()) {
                    User user = UserHolder.findUser(authOpt.get().getValue(),
                            authenticationConfig.getTokenHttpHeaderAppKey());
                    SemanticQueryResp resp = queryService.queryByReq(semanticQueryReq, user);
                    ResultSet resultSet = semanticQueryRespToResultSet(resp, semanticQueryReq.getDataSetId());
                    final Schema schema = jdbcToArrowSchema(resultSet.getMetaData(), defaultCalendar);
                    try (final VectorSchemaRoot vectorSchemaRoot = VectorSchemaRoot.create(schema, rootAllocator)) {
                        final VectorLoader loader = new VectorLoader(vectorSchemaRoot);
                        listener.start(vectorSchemaRoot);
                        final ArrowVectorIterator iterator = sqlToArrowVectorIterator(resultSet, rootAllocator);
                        while (iterator.hasNext()) {
                            final VectorSchemaRoot batch = iterator.next();
                            if (batch.getRowCount() == 0) {
                                break;
                            }
                            final VectorUnloader unloader = new VectorUnloader(batch);
                            loader.load(unloader.getRecordBatch());
                            listener.putNext();
                            vectorSchemaRoot.clear();
                        }

                        listener.putNext();
                    }

                }
            } catch (Exception e) {
                listener.error(CallStatus.INTERNAL
                        .withDescription(String.format("Failed to get exec statement %s", e.getMessage()))
                        .toRuntimeException());
                log.error("getStreamPreparedStatement error {}", hander);
            } finally {
                preparedStatementCache.invalidate(hander);
                listener.completed();
                rootAllocator.close();
            }
        });
    }

    @Override
    public void closePreparedStatement(final ActionClosePreparedStatementRequest request, final CallContext context,
            final StreamListener<Result> listener) {
        log.info("closePreparedStatement {}", request.getPreparedStatementHandle());
        listener.onCompleted();
    }

    @Override
    public FlightInfo getFlightInfoPreparedStatement(final CommandPreparedStatementQuery command,
            final CallContext context,
            final FlightDescriptor descriptor) {
        return getFlightInfoForSchema(command, descriptor, null);
    }

    @Override
    public void createPreparedStatement(final ActionCreatePreparedStatementRequest request, final CallContext context,
            final StreamListener<Result> listener) {
        prepared(request, context, listener);
    }

    private ByteString addPrepared(final CallContext context, String query) throws Exception {
        if (Arrays.asList(dataSetIdHeaderKey, nameHeaderKey, passwordHeaderKey).stream()
                .anyMatch(h -> !context.getMiddleware(FlightConstants.HEADER_KEY).headers().containsKey(h))) {
            throw new Exception(String.format("Failed to create prepared statement: HeaderCallOption miss %s %s %s",
                    dataSetIdHeaderKey, nameHeaderKey, passwordHeaderKey));
        }
        Long dataSetId = Long.valueOf(
                context.getMiddleware(FlightConstants.HEADER_KEY).headers().get(dataSetIdHeaderKey));
        if (StringUtils.isBlank(query)) {
            throw new Exception("Failed to create prepared statement: query is empty");
        }
        try {
            String auth = getUserAuth(context.getMiddleware(FlightConstants.HEADER_KEY).headers());
            if (StringUtils.isBlank(auth)) {
                throw new Exception("auth empty");
            }
            final ByteString preparedStatementHandle = copyFrom(
                    randomUUID().toString().getBytes(StandardCharsets.UTF_8));
            QuerySqlReq querySqlReq = new QuerySqlReq();
            querySqlReq.setDataSetId(dataSetId);
            querySqlReq.setSql(query);
            querySqlReq.setParams(Arrays.asList(new Param(authenticationConfig.getTokenHttpHeaderKey(), auth)));
            preparedStatementCache.put(preparedStatementHandle, querySqlReq);
            log.info("createPreparedStatement {} {} {} ", preparedStatementHandle, dataSetId, query);
            return preparedStatementHandle;
        } catch (Exception e) {
            throw e;
        }
    }

    private void prepared(final ActionCreatePreparedStatementRequest request, final CallContext context,
            final StreamListener<Result> listener) {
        try {
            ByteString preparedStatementHandle = addPrepared(context, request.getQuery());
            final ActionCreatePreparedStatementResult result = ActionCreatePreparedStatementResult.newBuilder()
                    .setDatasetSchema(ByteString.EMPTY)
                    .setParameterSchema(ByteString.empty())
                    .setPreparedStatementHandle(preparedStatementHandle)
                    .build();
            listener.onNext(new Result(pack(result).toByteArray()));
        } catch (Exception e) {
            listener.onError(CallStatus.INTERNAL
                    .withDescription(String.format("Failed to create prepared statement: %s", e.getMessage()))
                    .toRuntimeException());
        } finally {
            listener.onCompleted();
        }
    }

    @Override
    protected <T extends Message> List<FlightEndpoint> determineEndpoints(T t, FlightDescriptor flightDescriptor,
            Schema schema) {
        throw CallStatus.UNIMPLEMENTED.withDescription("Not implemented.").toRuntimeException();
    }

    private <T extends Message> FlightInfo getFlightInfoForSchema(final T request, final FlightDescriptor descriptor,
            final Schema schema) {
        final Ticket ticket = new Ticket(pack(request).toByteArray());
        Location listenLocation = Location.forGrpcInsecure(host, port);
        final List<FlightEndpoint> endpoints = singletonList(new FlightEndpoint(ticket, listenLocation));

        return new FlightInfo(schema, descriptor, endpoints, -1, -1);
    }

    private String getUserAuth(CallHeaders callHeaders) throws Exception {

        UserReq userReq = new UserReq();
        userReq.setName(callHeaders.get(nameHeaderKey));
        userReq.setPassword(callHeaders.get(passwordHeaderKey));
        if (StringUtils.isBlank(userReq.getName()) || StringUtils.isBlank(userReq.getPassword())) {
            throw new Exception("name or password is empty");
        }
        String auth = userService.login(userReq, authenticationConfig.getTokenDefaultAppKey());
        return auth;
    }

    private ResultSet semanticQueryRespToResultSet(SemanticQueryResp resp, Long dataSetId) throws SQLException {
        RowSetFactory factory = RowSetProvider.newFactory();
        CachedRowSet rowset = factory.createCachedRowSet();
        RowSetMetaData rowSetMetaData = new RowSetMetaDataImpl();
        int columnNum = resp.getColumns().size();
        rowSetMetaData.setColumnCount(columnNum);
        for (int i = 1; i <= columnNum; i++) {
            String columnName = resp.getColumns().get(i - 1).getNameEn();
            rowSetMetaData.setColumnName(i, columnName);
            Optional<Map<String, Object>> valOpt = resp.getResultList().stream()
                    .filter(r -> r.containsKey(columnName) && Objects.nonNull(r.get(columnName))).findFirst();
            if (valOpt.isPresent()) {
                int type = FlightUtils.resolveType(valOpt.get());
                rowSetMetaData.setColumnType(i, type);
                rowSetMetaData.setNullable(i, FlightUtils.isNullable(type));
            } else {
                rowSetMetaData.setNullable(i, ResultSetMetaData.columnNullable);
                rowSetMetaData.setColumnType(i, Types.VARCHAR);
            }
            rowSetMetaData.setCatalogName(i, String.valueOf(dataSetId));
            rowSetMetaData.setSchemaName(i, dataSetIdHeaderKey);
        }
        rowset.setMetaData(rowSetMetaData);
        for (Map<String, Object> row : resp.getResultList()) {
            rowset.moveToInsertRow();
            for (int i = 1; i <= columnNum; i++) {
                String columnName = resp.getColumns().get(i - 1).getNameEn();
                if (row.containsKey(columnName)) {
                    rowset.updateObject(i, row.get(columnName));
                } else {
                    rowset.updateObject(i, null);
                }
            }
            rowset.insertRow();
            rowset.moveToCurrentRow();
        }
        return rowset;
    }
}
