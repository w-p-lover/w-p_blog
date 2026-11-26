package com.ican.service.impl;

import com.ican.entity.ActivityRegistration;
import com.ican.exception.ServiceException;
import com.ican.mapper.ActivityMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import java.util.Map;
import static com.ican.entity.ActivityRegistration.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ActivityRegistrationServiceTest {
    private final ActivityMapper mapper = mock(ActivityMapper.class);
    private final ActivityRegistrationDispatchService dispatcher = mock(ActivityRegistrationDispatchService.class);
    private final ActivityService activities = new ActivityService(mapper, dispatcher);
    private final ActivityRegistrationExecutionService execution = new ActivityRegistrationExecutionService(mapper, dispatcher);
    private static final String ID = "12345678-1234-1234-1234-123456789abc";

    @Test
    void acceptedRequestReturnsPendingAndOnlySchedulesWakeAfterPersistence() {
        when(mapper.insertRegistration(ID, 1, 7)).thenReturn(1);
        when(mapper.reserveAdmission(1)).thenReturn(1);
        when(mapper.lockRequest(ID)).thenReturn(row(PENDING));
        assertEquals(PENDING, activities.submit(1, 7, ID).getStatus());
        var order = inOrder(mapper, dispatcher);
        order.verify(mapper).insertRegistration(ID, 1, 7);
        order.verify(mapper).reserveAdmission(1);
        order.verify(dispatcher).wakeAfterCommit(ID);
    }

    @Test
    void duplicateIdentityReturnsExistingResultButCrossUserReuseIsRejected() {
        when(mapper.insertRegistration(ID, 1, 7)).thenThrow(new DuplicateKeyException("synthetic"));
        when(mapper.lockRequest(ID)).thenReturn(row(CONFIRMED));
        assertEquals(CONFIRMED, activities.submit(1, 7, ID).getStatus());
        verify(mapper, never()).reserveAdmission(anyInt());
        verifyNoInteractions(dispatcher);
        String anotherId = "12345678-1234-1234-1234-123456789def";
        when(mapper.insertRegistration(anotherId, 1, 7)).thenThrow(new DuplicateKeyException("same user"));
        when(mapper.lockUserRequest(1, 7)).thenReturn(row(CONFIRMED));
        assertEquals(ID, activities.submit(1, 7, anotherId).getRequestId());
        ActivityRegistration other = row(CONFIRMED);
        other.setUserId(8);
        when(mapper.lockRequest(ID)).thenReturn(other);
        assertThrows(ServiceException.class, () -> activities.submit(1, 7, ID));
    }

    @Test
    void admissionRejectionDoesNotWakeOrConfirm() {
        when(mapper.reserveAdmission(1)).thenReturn(0);
        assertThrows(ServiceException.class, () -> activities.submit(1, 7, ID));
        verifyNoInteractions(dispatcher);
        verify(mapper, never()).consumeCapacity(anyInt());
    }

    @Test
    void duplicateOrOldDeliveryCannotDeductAndSuccessfulDeliveryRequiresTerminalWrite() {
        execution.process(ID, "old");
        verify(mapper, never()).consumeCapacity(anyInt());
        when(mapper.lockDispatched(ID, "current")).thenReturn(row(PENDING));
        when(mapper.consumeCapacity(1)).thenReturn(1);
        when(mapper.changeStatus(ID, PENDING, CONFIRMED)).thenReturn(0);
        assertThrows(ServiceException.class, () -> execution.process(ID, "current"));
        verify(mapper).changeStatus(ID, PENDING, CONFIRMED);
    }

    @Test
    void pendingCancelDoesNotRestoreCapacityAndRepeatedConfirmedCancelRestoresOnce() {
        when(mapper.lockUserRequest(1, 7)).thenReturn(row(PENDING));
        when(mapper.releasePending(1)).thenReturn(1);
        when(mapper.changeStatus(ID, PENDING, CANCELED)).thenReturn(1);
        when(mapper.lockRequest(ID)).thenReturn(row(CANCELED));
        execution.cancel(1, 7);
        verify(mapper, never()).restoreCapacity(anyInt());
        when(mapper.lockUserRequest(1, 7)).thenReturn(row(CONFIRMED), row(CANCELED));
        when(mapper.restoreCapacity(1)).thenReturn(1);
        when(mapper.changeStatus(ID, CONFIRMED, CANCELED)).thenReturn(1);
        execution.cancel(1, 7);
        execution.cancel(1, 7);
        verify(mapper, times(1)).restoreCapacity(1);
    }

    @Test
    void unknownCommitChecksCurrentTokenAndThirdFailureReleasesPendingBudget() {
        when(mapper.lockRequest(ID)).thenReturn(row(CONFIRMED));
        execution.recordFailure(ID, "token");
        verify(mapper, never()).failDispatch(anyString(), anyString(), anyBoolean());
        ActivityRegistration pending = row(PENDING);
        pending.setDispatchToken("new");
        pending.setFailureCount(2);
        when(mapper.lockRequest(ID)).thenReturn(pending);
        execution.recordFailure(ID, "old");
        verify(mapper, never()).releasePending(anyInt());
        when(mapper.releasePending(1)).thenReturn(1);
        when(mapper.failDispatch(ID, "new", true)).thenReturn(1);
        execution.recordFailure(ID, "new");
        verify(mapper).releasePending(1);
        verify(mapper).failDispatch(ID, "new", true);
    }

    @Test
    void mapperParsesAndAdmissionAndCapacitySqlKeepTheirDistinctTimeBoundaries() throws Exception {
        Configuration config = new Configuration();
        try (var input = getClass().getClassLoader().getResourceAsStream("mapper/ActivityMapper.xml")) {
            assertNotNull(input);
            new XMLMapperBuilder(input, config, "mapper/ActivityMapper.xml", config.getSqlFragments()).parse();
        }
        String namespace = ActivityMapper.class.getName() + ".";
        String admission = config.getMappedStatement(namespace + "reserveAdmission").getBoundSql(Map.of("id", 1)).getSql();
        assertTrue(admission.replaceAll("\\s+", " ").contains("ends_at > TIMESTAMPADD(HOUR,8,UTC_TIMESTAMP(6))"));
        assertTrue(admission.contains("pending_count < 1000"));
        String capacity = config.getMappedStatement(namespace + "consumeCapacity").getBoundSql(Map.of("id", 1)).getSql();
        assertTrue(capacity.contains("remaining > 0"));
        assertFalse(capacity.contains("ends_at")); // Accepted requests may finish after the admission window.
        assertNotNull(config.getMappedStatement(namespace + "listRegistrations")
                .getBoundSql(Map.of("activityId", 1, "status", "FAILED", "offset", 0, "size", 20)));
    }

    private ActivityRegistration row(String status) {
        ActivityRegistration row = new ActivityRegistration();
        row.setRequestId(ID);
        row.setActivityId(1);
        row.setUserId(7);
        row.setStatus(status);
        row.setFailureCount(0);
        return row;
    }
}
