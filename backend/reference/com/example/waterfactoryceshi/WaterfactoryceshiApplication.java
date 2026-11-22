/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration
 *  org.springframework.boot.SpringApplication
 *  org.springframework.boot.autoconfigure.SpringBootApplication
 *  org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
 *  org.springframework.scheduling.annotation.EnableAsync
 *  org.springframework.scheduling.annotation.EnableScheduling
 */
package com.example.waterfactoryceshi;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication(exclude={DataSourceAutoConfiguration.class, MybatisPlusAutoConfiguration.class})
public class WaterfactoryceshiApplication {
    public static void main(String[] args) {
        SpringApplication.run(WaterfactoryceshiApplication.class, (String[])args);
    }
}
