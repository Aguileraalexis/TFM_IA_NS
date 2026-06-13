package com.tesis.nsdemo;

import com.tesis.nsdemo.config.TravelDemoProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@EnableConfigurationProperties(TravelDemoProperties.class)
public class NsFrameworkDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(NsFrameworkDemoApplication.class, args);
    }
}
