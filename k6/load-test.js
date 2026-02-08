/**
 * EcoGo k6 Performance Test
 *
 * Stages:
 *   1. Ramp-up   — 0 → 50 VUs over 30s
 *   2. Sustained — hold 50 VUs for 1 minute
 *   3. Spike     — burst to 100 VUs for 30s
 *   4. Ramp-down — 100 → 0 VUs over 30s
 *
 * Thresholds mirror JMeter test expectations.
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// ── Custom Metrics ──────────────────────────────────────────
const errorRate = new Rate('errors');
const healthCheckDuration = new Trend('health_check_duration', true);
const metricsEndpointDuration = new Trend('metrics_endpoint_duration', true);

// ── Configuration ───────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8090';

export const options = {
  stages: [
    { duration: '30s', target: 50 },   // ramp-up
    { duration: '1m',  target: 50 },   // sustained load
    { duration: '30s', target: 100 },  // spike
    { duration: '30s', target: 0 },    // ramp-down
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'],  // 95% of requests under 2s
    http_req_failed:   ['rate<0.1'],    // error rate < 10%
    errors:            ['rate<0.1'],
    health_check_duration: ['p(99)<1000'],
  },
};

// ── Scenarios ───────────────────────────────────────────────

export default function () {
  // 1. Health Check (most critical)
  const healthRes = http.get(`${BASE_URL}/actuator/health`);
  healthCheckDuration.add(healthRes.timings.duration);

  const healthOk = check(healthRes, {
    'health: status 200':       (r) => r.status === 200,
    'health: body contains UP': (r) => r.body && r.body.includes('UP'),
    'health: response < 500ms': (r) => r.timings.duration < 500,
  });
  errorRate.add(!healthOk);

  sleep(0.5);

  // 2. Info Endpoint
  const infoRes = http.get(`${BASE_URL}/actuator/info`);
  check(infoRes, {
    'info: status 200': (r) => r.status === 200,
  });

  sleep(0.5);

  // 3. Metrics Endpoint
  const metricsRes = http.get(`${BASE_URL}/actuator/metrics`);
  metricsEndpointDuration.add(metricsRes.timings.duration);

  check(metricsRes, {
    'metrics: status 200':       (r) => r.status === 200,
    'metrics: response < 1000ms': (r) => r.timings.duration < 1000,
  });

  sleep(0.5);

  // 4. Login Page (static content)
  const loginRes = http.get(`${BASE_URL}/login`);
  check(loginRes, {
    'login: status 200 or redirect': (r) => r.status === 200 || r.status === 302,
  });

  sleep(0.5);

  // 5. Prometheus Metrics (if enabled)
  const promRes = http.get(`${BASE_URL}/actuator/prometheus`);
  check(promRes, {
    'prometheus: status 200': (r) => r.status === 200,
  });

  sleep(1);
}

// ── Lifecycle Hooks ─────────────────────────────────────────

export function setup() {
  // Verify the application is running before the test
  const res = http.get(`${BASE_URL}/actuator/health`);
  if (res.status !== 200) {
    throw new Error(`Application not ready. Health check returned ${res.status}`);
  }
  console.log(`Application is healthy. Starting k6 performance test against ${BASE_URL}`);
  return { baseUrl: BASE_URL };
}

export function teardown(data) {
  console.log(`k6 performance test completed against ${data.baseUrl}`);
}
