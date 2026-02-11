package com.example.EcoGo.controller;

import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.interfacemethods.CarbonRecordInterface;
import com.example.EcoGo.model.CarbonRecord;
import com.example.EcoGo.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for CarbonRecordController.
 */
class CarbonRecordControllerTest {

    private CarbonRecordInterface carbonRecordService;
    private JwtUtils jwtUtils;
    private CarbonRecordController controller;

    @BeforeEach
    void setUp() throws Exception {
        carbonRecordService = mock(CarbonRecordInterface.class);
        jwtUtils = new JwtUtils();

        controller = new CarbonRecordController();
        setField(controller, "carbonRecordService", carbonRecordService);
        setField(controller, "jwtUtils", jwtUtils);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void getAllRecords_success() {
        CarbonRecord r1 = new CarbonRecord("u1", "EARN", 100, "ACTIVITY", "Test");
        when(carbonRecordService.getAllRecords()).thenReturn(List.of(r1));

        ResponseMessage<List<CarbonRecord>> resp = controller.getAllRecords();
        assertEquals(200, resp.getCode());
        assertEquals(1, resp.getData().size());
    }

    @Test
    void getRecordsByUserId_success() {
        CarbonRecord r1 = new CarbonRecord("u1", "EARN", 50, "DAILY", "Test");
        when(carbonRecordService.getRecordsByUserId("u1")).thenReturn(List.of(r1));

        ResponseMessage<List<CarbonRecord>> resp = controller.getRecordsByUserId("u1");
        assertEquals(200, resp.getCode());
        assertEquals(1, resp.getData().size());
    }

    @Test
    void earnCredits_success() {
        CarbonRecord record = new CarbonRecord("u1", "EARN", 100, "ACTIVITY", "Beach cleanup");
        when(carbonRecordService.earnCredits("u1", 100, "ACTIVITY", "Beach cleanup")).thenReturn(record);

        ResponseMessage<CarbonRecord> resp = controller.earnCredits("u1", 100, "ACTIVITY", "Beach cleanup");
        assertEquals(200, resp.getCode());
        assertEquals("EARN", resp.getData().getType());
    }

    @Test
    void spendCredits_success() {
        CarbonRecord record = new CarbonRecord("u1", "SPEND", 50, "EXCHANGE", "Gift card");
        when(carbonRecordService.spendCredits("u1", 50, "EXCHANGE", "Gift card")).thenReturn(record);

        ResponseMessage<CarbonRecord> resp = controller.spendCredits("u1", 50, "EXCHANGE", "Gift card");
        assertEquals(200, resp.getCode());
        assertEquals("SPEND", resp.getData().getType());
    }

    @Test
    void getTotalCredits_success() {
        when(carbonRecordService.getTotalCreditsByUserId("u1")).thenReturn(250);

        ResponseMessage<Integer> resp = controller.getTotalCredits("u1");
        assertEquals(200, resp.getCode());
        assertEquals(250, resp.getData());
    }

    @Test
    void getFacultyTotalCarbon_success() {
        String token = jwtUtils.generateToken("user1", false);
        String authHeader = "Bearer " + token;

        com.example.EcoGo.dto.FacultyStatsDto.CarbonResponse carbonResp =
                new com.example.EcoGo.dto.FacultyStatsDto.CarbonResponse("Engineering", 150.0);
        when(carbonRecordService.getFacultyTotalCarbon("user1")).thenReturn(carbonResp);

        ResponseMessage<com.example.EcoGo.dto.FacultyStatsDto.CarbonResponse> resp =
                controller.getFacultyTotalCarbon(authHeader);
        assertEquals(200, resp.getCode());
        assertEquals("Engineering", resp.getData().faculty);
    }

    @Test
    void getFacultyTotalCarbonAdmin_success() {
        String token = jwtUtils.generateToken("admin", true);
        String authHeader = "Bearer " + token;

        com.example.EcoGo.dto.FacultyStatsDto.CarbonResponse carbonResp =
                new com.example.EcoGo.dto.FacultyStatsDto.CarbonResponse("Science", 200.0);
        when(carbonRecordService.getFacultyTotalCarbon("admin")).thenReturn(carbonResp);

        ResponseMessage<com.example.EcoGo.dto.FacultyStatsDto.CarbonResponse> resp =
                controller.getFacultyTotalCarbonAdmin(authHeader);
        assertEquals(200, resp.getCode());
        assertEquals("Science", resp.getData().faculty);
    }
}
