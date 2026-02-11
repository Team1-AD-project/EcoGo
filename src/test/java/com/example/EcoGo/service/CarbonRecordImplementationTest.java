package com.example.EcoGo.service;

import com.example.EcoGo.exception.BusinessException;
import com.example.EcoGo.model.CarbonRecord;
import com.example.EcoGo.model.User;
import com.example.EcoGo.repository.CarbonRecordRepository;
import com.example.EcoGo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for CarbonRecordImplementation.
 */
class CarbonRecordImplementationTest {

    private CarbonRecordRepository carbonRecordRepository;
    private UserRepository userRepository;
    private MongoTemplate mongoTemplate;
    private CarbonRecordImplementation service;

    @BeforeEach
    void setUp() throws Exception {
        carbonRecordRepository = mock(CarbonRecordRepository.class);
        userRepository = mock(UserRepository.class);
        mongoTemplate = mock(MongoTemplate.class);

        service = new CarbonRecordImplementation();

        setField(service, "carbonRecordRepository", carbonRecordRepository);
        setField(service, "userRepository", userRepository);
        setField(service, "mongoTemplate", mongoTemplate);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void getAllRecords() {
        CarbonRecord r1 = new CarbonRecord("u1", "EARN", 100, "ACTIVITY", "Beach cleanup");
        CarbonRecord r2 = new CarbonRecord("u2", "SPEND", 50, "EXCHANGE", "Gift card");
        when(carbonRecordRepository.findAll()).thenReturn(List.of(r1, r2));

        List<CarbonRecord> result = service.getAllRecords();
        assertEquals(2, result.size());
    }

    @Test
    void getRecordsByUserId() {
        CarbonRecord r = new CarbonRecord("u1", "EARN", 100, "ACTIVITY", "Test");
        when(carbonRecordRepository.findByUserId("u1")).thenReturn(List.of(r));

        List<CarbonRecord> result = service.getRecordsByUserId("u1");
        assertEquals(1, result.size());
    }

    @Test
    void createRecord() {
        CarbonRecord r = new CarbonRecord("u1", "EARN", 100, "ACTIVITY", "Test");
        when(carbonRecordRepository.save(any())).thenReturn(r);

        CarbonRecord result = service.createRecord(r);
        assertEquals("u1", result.getUserId());
    }

    @Test
    void earnCredits() {
        when(carbonRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CarbonRecord result = service.earnCredits("u1", 100, "ACTIVITY", "Beach cleanup");
        assertEquals("u1", result.getUserId());
        assertEquals("EARN", result.getType());
        assertEquals(100, result.getCredits());
    }

    @Test
    void spendCredits_sufficient() {
        // Set up records: user has earned 200 and spent 50, so balance = 150
        CarbonRecord earn = new CarbonRecord("u1", "EARN", 200, "ACTIVITY", "Test");
        CarbonRecord spent = new CarbonRecord("u1", "SPEND", 50, "EXCHANGE", "Test");
        when(carbonRecordRepository.findByUserId("u1")).thenReturn(List.of(earn, spent));
        when(carbonRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CarbonRecord result = service.spendCredits("u1", 100, "EXCHANGE", "Gift card");
        assertEquals("u1", result.getUserId());
        assertEquals("SPEND", result.getType());
        assertEquals(100, result.getCredits());
    }

    @Test
    void spendCredits_insufficient_throwsException() {
        CarbonRecord earn = new CarbonRecord("u1", "EARN", 50, "ACTIVITY", "Test");
        when(carbonRecordRepository.findByUserId("u1")).thenReturn(List.of(earn));

        assertThrows(BusinessException.class,
                () -> service.spendCredits("u1", 100, "EXCHANGE", "Gift card"));
    }

    @Test
    void getTotalCreditsByUserId() {
        CarbonRecord earn1 = new CarbonRecord("u1", "EARN", 200, "ACTIVITY", "Test1");
        CarbonRecord earn2 = new CarbonRecord("u1", "EARN", 100, "DAILY_CHECK", "Test2");
        CarbonRecord spend = new CarbonRecord("u1", "SPEND", 50, "EXCHANGE", "Test3");
        when(carbonRecordRepository.findByUserId("u1")).thenReturn(List.of(earn1, earn2, spend));

        Integer total = service.getTotalCreditsByUserId("u1");
        assertEquals(250, total); // 200 + 100 - 50
    }

    @Test
    void getTotalCreditsByUserId_empty() {
        when(carbonRecordRepository.findByUserId("u1")).thenReturn(List.of());

        Integer total = service.getTotalCreditsByUserId("u1");
        assertEquals(0, total);
    }

    @SuppressWarnings("unchecked")
    @Test
    void getFacultyTotalCarbon_success() {
        User user = new User();
        user.setUserid("u1");
        user.setFaculty("Engineering");
        when(userRepository.findByUserid("u1")).thenReturn(Optional.of(user));

        User facultyUser1 = new User();
        facultyUser1.setUserid("u1");
        User facultyUser2 = new User();
        facultyUser2.setUserid("u2");
        when(userRepository.findByFaculty("Engineering")).thenReturn(List.of(facultyUser1, facultyUser2));

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("totalCarbonSaved", 150.567);
        AggregationResults<Map> aggResults = mock(AggregationResults.class);
        when(aggResults.getUniqueMappedResult()).thenReturn(resultMap);
        when(mongoTemplate.aggregate(any(Aggregation.class), anyString(), any(Class.class))).thenReturn(aggResults);

        var result = service.getFacultyTotalCarbon("u1");
        assertNotNull(result);
        assertEquals("Engineering", result.faculty);
        assertEquals(150.57, result.totalCarbon, 0.01);
    }

    @Test
    void getFacultyTotalCarbon_userNotFound() {
        when(userRepository.findByUserid("u1")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.getFacultyTotalCarbon("u1"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void getFacultyTotalCarbon_nullFaculty() {
        User user = new User();
        user.setUserid("u1");
        user.setFaculty(null);
        when(userRepository.findByUserid("u1")).thenReturn(Optional.of(user));

        var result = service.getFacultyTotalCarbon("u1");
        assertEquals("", result.faculty);
        assertEquals(0.0, result.totalCarbon);
    }

    @SuppressWarnings("unchecked")
    @Test
    void getFacultyTotalCarbon_emptyFaculty() {
        User user = new User();
        user.setUserid("u1");
        user.setFaculty("");
        when(userRepository.findByUserid("u1")).thenReturn(Optional.of(user));

        var result = service.getFacultyTotalCarbon("u1");
        assertEquals("", result.faculty);
        assertEquals(0.0, result.totalCarbon);
    }

    @SuppressWarnings("unchecked")
    @Test
    void getFacultyTotalCarbon_noFacultyUsers() {
        User user = new User();
        user.setUserid("u1");
        user.setFaculty("EmptyFaculty");
        when(userRepository.findByUserid("u1")).thenReturn(Optional.of(user));
        when(userRepository.findByFaculty("EmptyFaculty")).thenReturn(List.of());

        var result = service.getFacultyTotalCarbon("u1");
        assertEquals("EmptyFaculty", result.faculty);
        assertEquals(0.0, result.totalCarbon);
    }

    @SuppressWarnings("unchecked")
    @Test
    void getFacultyTotalCarbon_nullAggResult() {
        User user = new User();
        user.setUserid("u1");
        user.setFaculty("Science");
        when(userRepository.findByUserid("u1")).thenReturn(Optional.of(user));

        User fu = new User();
        fu.setUserid("u1");
        when(userRepository.findByFaculty("Science")).thenReturn(List.of(fu));

        AggregationResults<Map> aggResults = mock(AggregationResults.class);
        when(aggResults.getUniqueMappedResult()).thenReturn(null);
        when(mongoTemplate.aggregate(any(Aggregation.class), anyString(), any(Class.class))).thenReturn(aggResults);

        var result = service.getFacultyTotalCarbon("u1");
        assertEquals("Science", result.faculty);
        assertEquals(0.0, result.totalCarbon);
    }
}
