package com.example.EcoGo.service.churn;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ChurnFeatureVector, Thresholds, and ChurnRiskLevel.
 */
class ChurnCoverageTest {

    // ==================== ChurnFeatureVector ====================
    @Test
    void featureVector_withFeatures() {
        float[] features = new float[]{1.0f, 2.0f, 3.0f};
        ChurnFeatureVector fv = new ChurnFeatureVector(features);

        assertArrayEquals(features, fv.getFeatures());
        assertArrayEquals(features, fv.toArray());
        assertFalse(fv.isInsufficient());
    }

    @Test
    void featureVector_nullFeatures() {
        ChurnFeatureVector fv = new ChurnFeatureVector(null);
        assertNotNull(fv.getFeatures());
        assertEquals(0, fv.getFeatures().length);
        assertTrue(fv.isInsufficient());
    }

    @Test
    void featureVector_emptyFeatures() {
        ChurnFeatureVector fv = new ChurnFeatureVector(new float[0]);
        assertEquals(0, fv.getFeatures().length);
        assertTrue(fv.isInsufficient());
    }

    @Test
    void featureVector_onlyOneNonZero_isInsufficient() {
        // Only one non-zero -> less than 2 non-zero -> insufficient
        ChurnFeatureVector fv = new ChurnFeatureVector(new float[]{0f, 0f, 5.0f, 0f});
        assertTrue(fv.isInsufficient());
    }

    @Test
    void featureVector_twoNonZero_notInsufficient() {
        ChurnFeatureVector fv = new ChurnFeatureVector(new float[]{1.0f, 0f, 3.0f, 0f});
        assertFalse(fv.isInsufficient());
    }

    @Test
    void featureVector_allZeros_isInsufficient() {
        ChurnFeatureVector fv = new ChurnFeatureVector(new float[]{0f, 0f, 0f});
        assertTrue(fv.isInsufficient());
    }

    @Test
    void featureVector_toString() {
        ChurnFeatureVector fv = new ChurnFeatureVector(new float[]{1.0f, 2.0f});
        String str = fv.toString();
        assertTrue(str.startsWith("ChurnFeatureVector"));
        assertTrue(str.contains("1.0"));
        assertTrue(str.contains("2.0"));
    }

    // ==================== Thresholds ====================
    @Test
    void thresholds_low() {
        Thresholds t = new Thresholds();
        assertEquals(ChurnRiskLevel.LOW, t.toLevel(0.0));
        assertEquals(ChurnRiskLevel.LOW, t.toLevel(0.1));
        assertEquals(ChurnRiskLevel.LOW, t.toLevel(0.32));
    }

    @Test
    void thresholds_medium() {
        Thresholds t = new Thresholds();
        assertEquals(ChurnRiskLevel.MEDIUM, t.toLevel(0.33));
        assertEquals(ChurnRiskLevel.MEDIUM, t.toLevel(0.5));
        assertEquals(ChurnRiskLevel.MEDIUM, t.toLevel(0.65));
    }

    @Test
    void thresholds_high() {
        Thresholds t = new Thresholds();
        assertEquals(ChurnRiskLevel.HIGH, t.toLevel(0.66));
        assertEquals(ChurnRiskLevel.HIGH, t.toLevel(0.8));
        assertEquals(ChurnRiskLevel.HIGH, t.toLevel(1.0));
    }

    @Test
    void thresholds_nan() {
        Thresholds t = new Thresholds();
        assertEquals(ChurnRiskLevel.INSUFFICIENT_DATA, t.toLevel(Double.NaN));
    }

    // ==================== ChurnRiskLevel ====================
    @Test
    void churnRiskLevel_allValues() {
        ChurnRiskLevel[] values = ChurnRiskLevel.values();
        assertEquals(4, values.length);
        assertNotNull(ChurnRiskLevel.valueOf("LOW"));
        assertNotNull(ChurnRiskLevel.valueOf("MEDIUM"));
        assertNotNull(ChurnRiskLevel.valueOf("HIGH"));
        assertNotNull(ChurnRiskLevel.valueOf("INSUFFICIENT_DATA"));
    }
}
