package com.tencent.supersonic.auth.authentication.service;

import com.tencent.supersonic.auth.authentication.persistence.mapper.TenantUsageDOMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UsageTrackingServiceImplTest {

    private UsageTrackingServiceImpl service;

    private final AtomicInteger incrementApiCallsCount = new AtomicInteger();
    private final AtomicInteger incrementQueryCount = new AtomicInteger();
    private final AtomicInteger insertCount = new AtomicInteger();
    private final AtomicReference<Integer> incrementApiCallsReturnValue = new AtomicReference<>(1);

    @BeforeEach
    void setUp() {
        incrementApiCallsCount.set(0);
        incrementQueryCount.set(0);
        insertCount.set(0);
        incrementApiCallsReturnValue.set(1);

        TenantUsageDOMapper mapper = (TenantUsageDOMapper) Proxy.newProxyInstance(
                TenantUsageDOMapper.class.getClassLoader(), new Class[] {TenantUsageDOMapper.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "incrementApiCalls":
                            incrementApiCallsCount.incrementAndGet();
                            return incrementApiCallsReturnValue.get();
                        case "incrementQueryCount":
                            incrementQueryCount.incrementAndGet();
                            return 1;
                        case "incrementTokensUsed":
                        case "incrementStorageBytes":
                        case "incrementActiveUsers":
                            return 1;
                        case "insert":
                            insertCount.incrementAndGet();
                            return 1;
                        case "selectApiCallsForDate":
                            return 42;
                        case "sumTokensUsedInRange":
                            return 5000L;
                        default:
                            return null;
                    }
                });

        service = new UsageTrackingServiceImpl(mapper);
    }

    @Test
    void recordApiCallShouldUseAtomicIncrement() {
        service.recordApiCall(1L);

        assertEquals(1, incrementApiCallsCount.get());
        assertEquals(0, insertCount.get());
    }

    @Test
    void recordApiCallShouldCreateRowWhenUpdateAffectsZeroRows() {
        incrementApiCallsReturnValue.set(0);

        service.recordApiCall(1L);

        // First attempt returns 0, then ensure + retry
        assertEquals(2, incrementApiCallsCount.get());
        assertEquals(1, insertCount.get());
    }

    @Test
    void recordQueryShouldUseAtomicIncrement() {
        service.recordQuery(1L);

        assertEquals(1, incrementQueryCount.get());
    }

    @Test
    void getTodayApiCallsShouldQueryDirectly() {
        int result = service.getTodayApiCalls(1L);

        assertEquals(42, result);
    }

    @Test
    void getMonthlyTokenUsageShouldUseSqlAggregation() {
        long result = service.getMonthlyTokenUsage(1L);

        assertEquals(5000L, result);
    }
}
