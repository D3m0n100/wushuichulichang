/*
 * Decompiled with CFR 0.152.
 */
package com.example.waterfactoryceshi.service;

class OverviewCache {
    private volatile Object value;
    private volatile long timestamp;
    private volatile long lastSuccessAt;
    private volatile long lastFailureAt;
    private volatile String lastFailureMessage;

    OverviewCache() {
    }

    public synchronized void set(Object freshValue) {
        long now = System.currentTimeMillis();
        this.value = freshValue;
        this.timestamp = now;
        this.lastSuccessAt = now;
        this.lastFailureAt = 0L;
        this.lastFailureMessage = null;
    }

    public synchronized void markFailure(String message) {
        this.lastFailureAt = System.currentTimeMillis();
        this.lastFailureMessage = message;
    }

    public CacheResult getCached() {
        long now = System.currentTimeMillis();
        if (this.value == null) {
            return null;
        }
        boolean stale = now - this.timestamp > 5000L;
        return new CacheResult(this.value, !stale, stale, this.cacheAgeMs(now), this.lastSuccessAt, this.lastFailureAt, this.lastFailureMessage);
    }

    public synchronized Object get(long ttlMs, Loader loader) {
        Object freshValue;
        long now = System.currentTimeMillis();
        if (this.value != null && now - this.timestamp <= ttlMs) {
            return this.value;
        }
        this.value = freshValue = loader.load();
        this.timestamp = now;
        this.lastSuccessAt = now;
        this.lastFailureAt = 0L;
        this.lastFailureMessage = null;
        return freshValue;
    }

    public synchronized CacheResult getResilient(long ttlMs, Loader loader) {
        long now = System.currentTimeMillis();
        if (this.value != null && now - this.timestamp <= ttlMs) {
            return new CacheResult(this.value, true, false, this.cacheAgeMs(now), this.lastSuccessAt, this.lastFailureAt, this.lastFailureMessage);
        }
        try {
            Object freshValue;
            this.value = freshValue = loader.load();
            this.timestamp = now;
            this.lastSuccessAt = now;
            this.lastFailureAt = 0L;
            this.lastFailureMessage = null;
            return new CacheResult(freshValue, false, false, 0L, this.lastSuccessAt, this.lastFailureAt, this.lastFailureMessage);
        }
        catch (RuntimeException ex) {
            this.lastFailureAt = now;
            this.lastFailureMessage = ex.getMessage();
            if (this.value != null) {
                return new CacheResult(this.value, false, true, this.cacheAgeMs(now), this.lastSuccessAt, this.lastFailureAt, this.lastFailureMessage);
            }
            throw ex;
        }
    }

    public synchronized void invalidate() {
        this.value = null;
        this.timestamp = 0L;
    }

    private long cacheAgeMs(long now) {
        if (this.timestamp <= 0L) {
            return 0L;
        }
        return Math.max(0L, now - this.timestamp);
    }

    static class CacheResult {
        private final Object value;
        private final boolean cacheHit;
        private final boolean stale;
        private final long cacheAgeMs;
        private final long lastSuccessAt;
        private final long lastFailureAt;
        private final String lastFailureMessage;

        CacheResult(Object value, boolean cacheHit, boolean stale, long cacheAgeMs, long lastSuccessAt, long lastFailureAt, String lastFailureMessage) {
            this.value = value;
            this.cacheHit = cacheHit;
            this.stale = stale;
            this.cacheAgeMs = cacheAgeMs;
            this.lastSuccessAt = lastSuccessAt;
            this.lastFailureAt = lastFailureAt;
            this.lastFailureMessage = lastFailureMessage;
        }

        public Object getValue() {
            return this.value;
        }

        public boolean isCacheHit() {
            return this.cacheHit;
        }

        public boolean isStale() {
            return this.stale;
        }

        public long getCacheAgeMs() {
            return this.cacheAgeMs;
        }

        public long getLastSuccessAt() {
            return this.lastSuccessAt;
        }

        public long getLastFailureAt() {
            return this.lastFailureAt;
        }

        public String getLastFailureMessage() {
            return this.lastFailureMessage;
        }
    }

    static interface Loader {
        public Object load();
    }
}
