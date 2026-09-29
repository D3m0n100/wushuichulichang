(function () {
  "use strict";

  const SESSION_URL = "/api/auth/session";
  const CHECK_TIMEOUT_MS = 10000;
  const CHECK_INTERVAL_MS = 60000;
  const REQUIRED_INVALID_CHECKS = 10;
  let checkTimer = null;
  let checkInFlight = false;
  let monitoredToken = null;
  let consecutiveInvalidChecks = 0;

  async function readSessionState(token) {
    const controller = new AbortController();
    const timeout = window.setTimeout(function () {
      controller.abort();
    }, CHECK_TIMEOUT_MS);

    try {
      const response = await fetch(SESSION_URL, {
        headers: { Authorization: "Bearer " + token },
        cache: "no-store",
        signal: controller.signal
      });
      if (response.status === 401 || response.status === 403) return "invalid";
      if (!response.ok) return "unavailable";
      const payload = await response.json();
      return payload && payload.success ? "valid" : "invalid";
    } catch (error) {
      return "unavailable";
    } finally {
      window.clearTimeout(timeout);
    }
  }

  function scheduleCheck() {
    if (checkTimer) window.clearTimeout(checkTimer);
    checkTimer = window.setTimeout(checkSession, CHECK_INTERVAL_MS);
  }

  async function checkSession() {
    checkTimer = null;
    if (checkInFlight) {
      scheduleCheck();
      return;
    }

    const token = localStorage.getItem("token");
    if (!token) {
      monitoredToken = null;
      consecutiveInvalidChecks = 0;
      scheduleCheck();
      return;
    }
    if (monitoredToken !== token) {
      monitoredToken = token;
      consecutiveInvalidChecks = 0;
    }

    checkInFlight = true;
    const state = await readSessionState(token);
    checkInFlight = false;
    if (localStorage.getItem("token") !== token) {
      scheduleCheck();
      return;
    }

    if (state === "invalid") consecutiveInvalidChecks += 1;
    else consecutiveInvalidChecks = 0;

    if (consecutiveInvalidChecks >= REQUIRED_INVALID_CHECKS) {
      localStorage.removeItem("token");
      localStorage.removeItem("username");
      window.location.href = "/#/login";
      return;
    }
    scheduleCheck();
  }

  window.DebugAuth = {
    confirmInvalid: async function () {
      // Business 401 responses never accelerate the 60-second session monitor.
      return false;
    }
  };

  scheduleCheck();
})();
