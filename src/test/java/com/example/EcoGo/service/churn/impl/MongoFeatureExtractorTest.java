package com.example.EcoGo.service.churn.impl;

import com.example.EcoGo.service.churn.ChurnFeatureVector;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for MongoFeatureExtractor.
 */
class MongoFeatureExtractorTest {

    private MongoTemplate mongoTemplate;
    private MongoFeatureExtractor extractor;

    @BeforeEach
    void setUp() {
        mongoTemplate = mock(MongoTemplate.class);
        extractor = new MongoFeatureExtractor(mongoTemplate);
    }

    @Test
    void extract_userNotFound_returnsEmptyFeatures() {
        when(mongoTemplate.findOne(any(), eq(Document.class), eq("users"))).thenReturn(null);

        ChurnFeatureVector fv = extractor.extract("nonexistent");

        assertNotNull(fv);
        assertEquals(0, fv.getFeatures().length);
        assertTrue(fv.isInsufficient());
    }

    @Test
    void extract_userFoundByUserid_fullFeatures() {
        Document stats = new Document()
                .append("totalTrips", 10)
                .append("activeDays", 5)
                .append("completedTasks", 3);

        Document vip = new Document()
                .append("level", 2);

        Document userDoc = new Document()
                .append("userid", "u1")
                .append("stats", stats)
                .append("vip", vip)
                .append("totalCarbon", 50.0)
                .append("totalPoints", 1000)
                .append("currentPoints", 500);

        // First findOne returns the user
        when(mongoTemplate.findOne(any(), eq(Document.class), eq("users")))
                .thenReturn(userDoc);

        ChurnFeatureVector fv = extractor.extract("u1");

        assertNotNull(fv);
        float[] features = fv.getFeatures();
        assertEquals(7, features.length);
        assertEquals(10f, features[0]); // totalTrips
        assertEquals(5f, features[1]);  // activeDays
        assertEquals(3f, features[2]);  // completedTasks
        assertEquals(50f, features[3]); // totalCarbon
        assertEquals(1000f, features[4]); // totalPoints
        assertEquals(500f, features[5]); // currentPoints
        assertEquals(2f, features[6]); // vipValue (level)
    }

    @Test
    void extract_userWithAlternativeFieldNames() {
        Document stats = new Document()
                .append("total_trips", 8)
                .append("active_days", 4)
                .append("completed_tasks", 2);

        Document vip = new Document()
                .append("isActive", true); // no level, use isActive

        Document userDoc = new Document()
                .append("userid", "u2")
                .append("stats", stats)
                .append("vip", vip)
                .append("total_carbon", 30.0)
                .append("total_points", 800)
                .append("current_points", 300);

        when(mongoTemplate.findOne(any(), eq(Document.class), eq("users")))
                .thenReturn(userDoc);

        ChurnFeatureVector fv = extractor.extract("u2");

        assertNotNull(fv);
        float[] features = fv.getFeatures();
        assertEquals(7, features.length);
        assertEquals(8f, features[0]);  // total_trips
        assertEquals(4f, features[1]);  // active_days
        assertEquals(2f, features[2]);  // completed_tasks
        assertEquals(30f, features[3]); // total_carbon
        assertEquals(800f, features[4]); // total_points
        assertEquals(300f, features[5]); // current_points
        assertEquals(1f, features[6]); // vip isActive = true -> 1f
    }

    @Test
    void extract_userWithNoStatsNoVip() {
        Document userDoc = new Document()
                .append("userid", "u3");

        when(mongoTemplate.findOne(any(), eq(Document.class), eq("users")))
                .thenReturn(userDoc);

        ChurnFeatureVector fv = extractor.extract("u3");

        assertNotNull(fv);
        float[] features = fv.getFeatures();
        assertEquals(7, features.length);
        // All should be 0
        for (float f : features) {
            assertEquals(0f, f);
        }
    }

    @Test
    void extract_userFoundBySecondQuery() {
        // First query (by userid) returns null, second query (by _id string) returns user
        when(mongoTemplate.findOne(any(), eq(Document.class), eq("users")))
                .thenReturn(null)
                .thenReturn(new Document().append("_id", "u4"));

        ChurnFeatureVector fv = extractor.extract("u4");

        assertNotNull(fv);
    }

    @Test
    void extract_userFoundByObjectId() {
        // First two queries return null, third (by ObjectId) returns user
        // Using a valid 24-char hex string for ObjectId
        String oid = "507f1f77bcf86cd799439011";
        when(mongoTemplate.findOne(any(), eq(Document.class), eq("users")))
                .thenReturn(null)
                .thenReturn(null)
                .thenReturn(new Document().append("_id", oid));

        ChurnFeatureVector fv = extractor.extract(oid);

        assertNotNull(fv);
    }

    @Test
    void extract_invalidObjectId_returnsEmpty() {
        // userId is not a valid ObjectId and not found by any query
        when(mongoTemplate.findOne(any(), eq(Document.class), eq("users")))
                .thenReturn(null);

        ChurnFeatureVector fv = extractor.extract("not-an-objectid");

        assertNotNull(fv);
        assertEquals(0, fv.getFeatures().length);
    }

    @Test
    void extract_vipWithIsActiveFalse() {
        Document vip = new Document()
                .append("is_active", false);

        Document userDoc = new Document()
                .append("userid", "u5")
                .append("vip", vip);

        when(mongoTemplate.findOne(any(), eq(Document.class), eq("users")))
                .thenReturn(userDoc);

        ChurnFeatureVector fv = extractor.extract("u5");

        float[] features = fv.getFeatures();
        assertEquals(0f, features[6]); // vip isActive false -> 0f
    }
}
