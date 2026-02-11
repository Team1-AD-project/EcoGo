package com.example.EcoGo.service.churn.impl;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

/**
 * Tests for OnnxModelRunner.
 * Uses reflection to test private static helper methods.
 */
class OnnxModelRunnerTest {

    private static OnnxModelRunner runner;
    private static boolean modelAvailable;

    @BeforeAll
    static void setup() {
        try {
            runner = new OnnxModelRunner();
            modelAvailable = true;
        } catch (Exception e) {
            modelAvailable = false;
        }
    }

    // ==================== predictProbability ====================
    @Test
    void predictProbability_nullFeatures() {
        OrtEnvironment env = mock(OrtEnvironment.class);
        OrtSession session = mock(OrtSession.class);
        OnnxModelRunner r = new OnnxModelRunner(env, session, "in", "out", (e, f, s) -> mock(OnnxTensor.class));
        double result = r.predictProbability(null);
        assertTrue(Double.isNaN(result));
    }

    @Test
    void predictProbability_emptyFeatures() {
        OrtEnvironment env = mock(OrtEnvironment.class);
        OrtSession session = mock(OrtSession.class);
        OnnxModelRunner r = new OnnxModelRunner(env, session, "in", "out", (e, f, s) -> mock(OnnxTensor.class));
        double result = r.predictProbability(new float[0]);
        assertTrue(Double.isNaN(result));
    }

    @Test
    void predictProbability_parsesFromPrimaryOutput() throws Exception {
        OrtEnvironment env = mock(OrtEnvironment.class);
        OrtSession session = mock(OrtSession.class);

        OnnxTensor tensor = mock(OnnxTensor.class);
        OnnxModelRunner.TensorFactory tensorFactory = (e, f, s) -> tensor;

        OrtSession.Result result = mock(OrtSession.Result.class);
        OnnxValue outVal = mock(OnnxValue.class);
        when(outVal.getValue()).thenReturn(new float[][]{{0.2f, 0.8f}});
        when(result.get("out")).thenReturn(java.util.Optional.of(outVal));
        when(result.iterator()).thenReturn(List.<Map.Entry<String, OnnxValue>>of().iterator());
        when(session.run(anyMap())).thenReturn(result);

        OnnxModelRunner r = new OnnxModelRunner(env, session, "in", "out", tensorFactory);
        double p = r.predictProbability(new float[]{1f, 2f, 3f});

        assertEquals(0.8, p, 1e-6);
        verify(result).close();
        verify(tensor).close();
    }

    @Test
    void predictProbability_primaryOutputNaN_fallsBackToOtherOutput() throws Exception {
        OrtEnvironment env = mock(OrtEnvironment.class);
        OrtSession session = mock(OrtSession.class);

        OnnxTensor tensor = mock(OnnxTensor.class);
        OnnxModelRunner.TensorFactory tensorFactory = (e, f, s) -> tensor;

        OrtSession.Result result = mock(OrtSession.Result.class);

        OnnxValue primary = mock(OnnxValue.class);
        when(primary.getValue()).thenReturn("unknown_string");
        when(result.get("out")).thenReturn(java.util.Optional.of(primary));

        OnnxValue fallback = mock(OnnxValue.class);
        when(fallback.getValue()).thenReturn(new float[]{0.1f, 0.9f});
        Map.Entry<String, OnnxValue> fallbackEntry = Map.entry("fallback", fallback);
        Iterator<Map.Entry<String, OnnxValue>> it = List.of(fallbackEntry).iterator();
        when(result.iterator()).thenReturn(it);

        when(session.run(anyMap())).thenReturn(result);

        OnnxModelRunner r = new OnnxModelRunner(env, session, "in", "out", tensorFactory);
        double p = r.predictProbability(new float[]{1f, 2f, 3f});

        assertEquals(0.9, p, 1e-6);
        verify(result).close();
        verify(tensor).close();
    }

    @Test
    void predictProbability_allOutputsUnparseable_returnsNaN() throws Exception {
        OrtEnvironment env = mock(OrtEnvironment.class);
        OrtSession session = mock(OrtSession.class);

        OnnxTensor tensor = mock(OnnxTensor.class);
        OnnxModelRunner.TensorFactory tensorFactory = (e, f, s) -> tensor;

        OrtSession.Result result = mock(OrtSession.Result.class);

        OnnxValue primary = mock(OnnxValue.class);
        when(primary.getValue()).thenReturn("unknown_string");
        when(result.get("out")).thenReturn(java.util.Optional.of(primary));

        OnnxValue other = mock(OnnxValue.class);
        when(other.getValue()).thenReturn(null);
        when(result.iterator()).thenReturn(List.of(Map.entry("x", other)).iterator());

        when(session.run(anyMap())).thenReturn(result);

        OnnxModelRunner r = new OnnxModelRunner(env, session, "in", "out", tensorFactory);
        double p = r.predictProbability(new float[]{1f, 2f, 3f});

        assertTrue(Double.isNaN(p));
        verify(result).close();
        verify(tensor).close();
    }

    @Test
    void predictProbability_whenSessionThrows_returnsNaN() throws Exception {
        OrtEnvironment env = mock(OrtEnvironment.class);
        OrtSession session = mock(OrtSession.class);

        OnnxTensor tensor = mock(OnnxTensor.class);
        OnnxModelRunner.TensorFactory tensorFactory = (e, f, s) -> tensor;

        when(session.run(anyMap())).thenThrow(new RuntimeException("boom"));

        OnnxModelRunner r = new OnnxModelRunner(env, session, "in", "out", tensorFactory);
        double p = r.predictProbability(new float[]{1f, 2f, 3f});

        assertTrue(Double.isNaN(p));
        // tensor still should be closed because it was created before session.run()
        verify(tensor).close();
    }

    @Test
    void predictProbability_validFeatures() {
        if (!modelAvailable) return;
        // Use 7 features (matching the model's expected input)
        float[] features = new float[]{10f, 5f, 3f, 50f, 1000f, 500f, 1f};
        double result = runner.predictProbability(features);
        // Result should be a valid probability or NaN
        assertTrue(Double.isNaN(result) || (result >= 0 && result <= 1));
    }

    // ==================== tryParseProbability (private static) ====================
    @Test
    void tryParseProbability_null() throws Exception {
        Method method = OnnxModelRunner.class.getDeclaredMethod("tryParseProbability", Object.class);
        method.setAccessible(true);

        double result = (double) method.invoke(null, (Object) null);
        assertTrue(Double.isNaN(result));
    }

    @Test
    void tryParseProbability_float2dArray_twoColumns() throws Exception {
        Method method = OnnxModelRunner.class.getDeclaredMethod("tryParseProbability", Object.class);
        method.setAccessible(true);

        float[][] probs = new float[][]{{0.3f, 0.7f}};
        double result = (double) method.invoke(null, (Object) probs);
        assertEquals(0.7, result, 0.01);
    }

    @Test
    void tryParseProbability_float2dArray_singleColumn() throws Exception {
        Method method = OnnxModelRunner.class.getDeclaredMethod("tryParseProbability", Object.class);
        method.setAccessible(true);

        float[][] probs = new float[][]{{0.85f}};
        double result = (double) method.invoke(null, (Object) probs);
        assertEquals(0.85, result, 0.01);
    }

    @Test
    void tryParseProbability_float1dArray_twoElements() throws Exception {
        Method method = OnnxModelRunner.class.getDeclaredMethod("tryParseProbability", Object.class);
        method.setAccessible(true);

        float[] arr = new float[]{0.4f, 0.6f};
        double result = (double) method.invoke(null, (Object) arr);
        assertEquals(0.6, result, 0.01);
    }

    @Test
    void tryParseProbability_float1dArray_singleElement() throws Exception {
        Method method = OnnxModelRunner.class.getDeclaredMethod("tryParseProbability", Object.class);
        method.setAccessible(true);

        float[] arr = new float[]{0.9f};
        double result = (double) method.invoke(null, (Object) arr);
        assertEquals(0.9, result, 0.01);
    }

    @Test
    void tryParseProbability_double2dArray_twoColumns() throws Exception {
        Method method = OnnxModelRunner.class.getDeclaredMethod("tryParseProbability", Object.class);
        method.setAccessible(true);

        double[][] probs = new double[][]{{0.2, 0.8}};
        double result = (double) method.invoke(null, (Object) probs);
        assertEquals(0.8, result, 0.01);
    }

    @Test
    void tryParseProbability_double2dArray_singleColumn() throws Exception {
        Method method = OnnxModelRunner.class.getDeclaredMethod("tryParseProbability", Object.class);
        method.setAccessible(true);

        double[][] probs = new double[][]{{0.75}};
        double result = (double) method.invoke(null, (Object) probs);
        assertEquals(0.75, result, 0.01);
    }

    @Test
    void tryParseProbability_double1dArray_twoElements() throws Exception {
        Method method = OnnxModelRunner.class.getDeclaredMethod("tryParseProbability", Object.class);
        method.setAccessible(true);

        double[] arr = new double[]{0.1, 0.9};
        double result = (double) method.invoke(null, (Object) arr);
        assertEquals(0.9, result, 0.01);
    }

    @Test
    void tryParseProbability_double1dArray_singleElement() throws Exception {
        Method method = OnnxModelRunner.class.getDeclaredMethod("tryParseProbability", Object.class);
        method.setAccessible(true);

        double[] arr = new double[]{0.65};
        double result = (double) method.invoke(null, (Object) arr);
        assertEquals(0.65, result, 0.01);
    }

    @Test
    void tryParseProbability_map() throws Exception {
        Method method = OnnxModelRunner.class.getDeclaredMethod("tryParseProbability", Object.class);
        method.setAccessible(true);

        Map<String, Number> map = new HashMap<>();
        map.put("1", 0.78);
        double result = (double) method.invoke(null, (Object) map);
        assertEquals(0.78, result, 0.01);
    }

    @Test
    void tryParseProbability_unknownType() throws Exception {
        Method method = OnnxModelRunner.class.getDeclaredMethod("tryParseProbability", Object.class);
        method.setAccessible(true);

        double result = (double) method.invoke(null, "unknown_string");
        assertTrue(Double.isNaN(result));
    }

    // ==================== clamp01 (private static) ====================
    @Test
    void clamp01_withinRange() throws Exception {
        Method method = OnnxModelRunner.class.getDeclaredMethod("clamp01", double.class);
        method.setAccessible(true);

        assertEquals(0.5, (double) method.invoke(null, 0.5));
        assertEquals(0.0, (double) method.invoke(null, 0.0));
        assertEquals(1.0, (double) method.invoke(null, 1.0));
    }

    @Test
    void clamp01_belowZero() throws Exception {
        Method method = OnnxModelRunner.class.getDeclaredMethod("clamp01", double.class);
        method.setAccessible(true);

        assertEquals(0.0, (double) method.invoke(null, -0.5));
        assertEquals(0.0, (double) method.invoke(null, -100.0));
    }

    @Test
    void clamp01_aboveOne() throws Exception {
        Method method = OnnxModelRunner.class.getDeclaredMethod("clamp01", double.class);
        method.setAccessible(true);

        assertEquals(1.0, (double) method.invoke(null, 1.5));
        assertEquals(1.0, (double) method.invoke(null, 100.0));
    }

    // ==================== pickProbabilityOutputName (private static) ====================
    @Test
    void pickProbabilityOutputName_withProbOutput() throws Exception {
        Method method = OnnxModelRunner.class.getDeclaredMethod("pickProbabilityOutputName", Set.class);
        method.setAccessible(true);

        Set<String> outputs = new LinkedHashSet<>();
        outputs.add("label");
        outputs.add("probabilities");
        String result = (String) method.invoke(null, outputs);
        assertEquals("probabilities", result);
    }

    @Test
    void pickProbabilityOutputName_withProba() throws Exception {
        Method method = OnnxModelRunner.class.getDeclaredMethod("pickProbabilityOutputName", Set.class);
        method.setAccessible(true);

        Set<String> outputs = new LinkedHashSet<>();
        outputs.add("output_label");
        outputs.add("output_proba");
        String result = (String) method.invoke(null, outputs);
        assertEquals("output_proba", result);
    }

    @Test
    void pickProbabilityOutputName_noProbOutput_returnsLast() throws Exception {
        Method method = OnnxModelRunner.class.getDeclaredMethod("pickProbabilityOutputName", Set.class);
        method.setAccessible(true);

        Set<String> outputs = new LinkedHashSet<>();
        outputs.add("label");
        outputs.add("score");
        String result = (String) method.invoke(null, outputs);
        assertEquals("score", result); // last element
    }

    // ==================== readAllBytesFromResource (private static) ====================
    @Test
    void readAllBytesFromResource_notFound() throws Exception {
        Method method = OnnxModelRunner.class.getDeclaredMethod("readAllBytesFromResource", String.class);
        method.setAccessible(true);

        try {
            method.invoke(null, "/nonexistent/model.onnx");
            fail("Expected exception");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
        }
    }
}
