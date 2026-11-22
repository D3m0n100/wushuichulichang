/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  javax.annotation.PreDestroy
 *  org.apache.plc4x.java.PlcDriverManager
 *  org.apache.plc4x.java.api.PlcConnection
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.example.waterfactoryceshi.service;

import com.example.waterfactoryceshi.service.PlcConnectionProvider;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PreDestroy;
import org.apache.plc4x.java.PlcDriverManager;
import org.apache.plc4x.java.api.PlcConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractS7ConnectionPool
implements PlcConnectionProvider {
    private static final Logger log = LoggerFactory.getLogger(AbstractS7ConnectionPool.class);
    private final List<String> connectionUrls;
    private int activeUrlIndex = 0;

    protected AbstractS7ConnectionPool(String ip, int rack, int slot) {
        this.connectionUrls = AbstractS7ConnectionPool.buildCandidateUrls(ip, rack, slot);
    }

    private synchronized String getActiveUrl() {
        return this.connectionUrls.get(this.activeUrlIndex);
    }

    private synchronized void rotateActiveUrl(String failedUrl) {
        int failedIndex = this.connectionUrls.indexOf(failedUrl);
        if (failedIndex >= 0 && failedIndex == this.activeUrlIndex) {
            this.activeUrlIndex = (this.activeUrlIndex + 1) % this.connectionUrls.size();
            log.warn("Switching S7-200 SMART connection URL after failure: {}", (Object)this.getActiveUrl());
        }
    }

    private static List<String> buildCandidateUrls(String ip, int rack, int slot) {
        ArrayList<String> urls = new ArrayList<String>();
        urls.add(String.format("s7://%s:102?local-tsap=512&remote-tsap=513&pdu-size=240&max-amq-caller=1&max-amq-callee=1", ip));
        urls.add(String.format("s7://%s:102?local-tsap=768&remote-tsap=769&pdu-size=240&max-amq-caller=1&max-amq-callee=1", ip));
        urls.add(String.format("s7://%s:102?local-tsap=512&remote-tsap=512&pdu-size=240&max-amq-caller=1&max-amq-callee=1", ip));
        urls.add(String.format("s7://%s:102?local-tsap=768&remote-tsap=768&pdu-size=240&max-amq-caller=1&max-amq-callee=1", ip));
        urls.add(String.format("s7://%s:102?local-tsap=512&remote-tsap=513&pdu-size=240&max-amq-caller=1&max-amq-callee=1&controller-type=LOGO", ip));
        urls.add(String.format("s7://%s:102?local-tsap=768&remote-tsap=769&pdu-size=240&max-amq-caller=1&max-amq-callee=1&controller-type=LOGO", ip));
        urls.add(String.format("s7://%s:102?remote-rack=0&remote-slot=0&pdu-size=240&max-amq-caller=1&max-amq-callee=1", ip));
        urls.add(String.format("s7://%s:102?remote-rack=0&remote-slot=0&pdu-size=240&max-amq-caller=1&max-amq-callee=1&controller-type=LOGO", ip));
        urls.add(String.format("s7://%s:102?remote-rack=0&remote-slot=0", ip));
        urls.add(String.format("s7://%s:102?local-tsap=256&remote-tsap=256&pdu-size=240&max-amq-caller=1&max-amq-callee=1", ip));
        urls.add(String.format("s7://%s:102?local-tsap=256&remote-tsap=257&pdu-size=240&max-amq-caller=1&max-amq-callee=1", ip));
        urls.add(String.format("s7://%s:102?local-tsap=256&remote-tsap=512&pdu-size=240&max-amq-caller=1&max-amq-callee=1", ip));
        urls.add(String.format("s7://%s:102?remote-rack=%d&remote-slot=%d&pdu-size=240&max-amq-caller=1&max-amq-callee=1", ip, rack, slot));
        urls.add(String.format("s7://%s:102?remote-rack=%d&remote-slot=%d", ip, rack, slot));
        return urls;
    }

    @Override
    public PlcConnection borrowConnection() throws Exception {
        Exception lastException = null;
        int maxAttempts = Math.min(3, this.connectionUrls.size());
        for (int i = 0; i < maxAttempts; ++i) {
            String url = this.getActiveUrl();
            try {
                log.info("Opening S7-200 SMART connection: {}", (Object)url);
                return new PlcDriverManager().getConnection(url);
            }
            catch (Exception ex) {
                lastException = ex;
                log.warn("S7 connection candidate failed: {}, reason: {}", (Object)url, (Object)ex.getMessage());
                this.rotateActiveUrl(url);
                continue;
            }
        }
        throw lastException;
    }

    @Override
    public void returnConnection(PlcConnection connection) {
        if (connection != null) {
            try {
                connection.close();
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    @Override
    public void invalidateConnection(PlcConnection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.rotateActiveUrl(this.getActiveUrl());
    }

    @Override
    public int maxReadAttempts() {
        return this.connectionUrls.size();
    }

    @PreDestroy
    public void close() {
    }
}
