/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  javax.annotation.PreDestroy
 *  org.apache.commons.pool2.BasePooledObjectFactory
 *  org.apache.commons.pool2.PooledObject
 *  org.apache.commons.pool2.PooledObjectFactory
 *  org.apache.commons.pool2.impl.DefaultPooledObject
 *  org.apache.commons.pool2.impl.GenericObjectPool
 *  org.apache.commons.pool2.impl.GenericObjectPoolConfig
 *  org.apache.plc4x.java.PlcDriverManager
 *  org.apache.plc4x.java.api.PlcConnection
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.stereotype.Component
 */
package com.example.waterfactoryceshi.service;

import javax.annotation.PreDestroy;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.plc4x.java.PlcDriverManager;
import org.apache.plc4x.java.api.PlcConnection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class H1PlcConnectionPool {
    private final GenericObjectPool<PlcConnection> pool;

    public H1PlcConnectionPool(final @Value(value="${plc.h1.ip}") String ip, final @Value(value="${plc.h1.rack:0}") int rack, final @Value(value="${plc.h1.slot:1}") int slot) {
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(5);
        config.setMaxIdle(3);
        config.setMinIdle(1);
        config.setTestOnBorrow(false);
        config.setTestWhileIdle(true);
        config.setMinEvictableIdleTimeMillis(180000L);
        config.setTimeBetweenEvictionRunsMillis(60000L);
        config.setMaxWaitMillis(5000L);
        BasePooledObjectFactory<PlcConnection> factory = new BasePooledObjectFactory<PlcConnection>(){

            public PlcConnection create() throws Exception {
                String url = String.format("s7://%s:102?remote-rack=%d&remote-slot=%d", ip, rack, slot);
                return new PlcDriverManager().getConnection(url);
            }

            public PooledObject<PlcConnection> wrap(PlcConnection conn) {
                return new DefaultPooledObject((Object)conn);
            }

            public void destroyObject(PooledObject<PlcConnection> p) throws Exception {
                ((PlcConnection)p.getObject()).close();
            }

            public boolean validateObject(PooledObject<PlcConnection> p) {
                try {
                    PlcConnection connection = (PlcConnection)p.getObject();
                    return connection != null && connection.isConnected();
                }
                catch (Exception ex) {
                    return false;
                }
            }
        };
        this.pool = new GenericObjectPool((PooledObjectFactory)factory, config);
    }

    public PlcConnection borrowConnection() throws Exception {
        return (PlcConnection)this.pool.borrowObject();
    }

    public void returnConnection(PlcConnection connection) {
        if (connection != null) {
            this.pool.returnObject((Object)connection);
        }
    }

    public void invalidateConnection(PlcConnection connection) {
        if (connection != null) {
            try {
                this.pool.invalidateObject((Object)connection);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    @PreDestroy
    public void close() {
        this.pool.close();
    }
}
