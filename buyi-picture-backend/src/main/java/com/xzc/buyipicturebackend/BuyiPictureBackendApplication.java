package com.xzc.buyipicturebackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * @author xuzhichao
 */
@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableAsync
public class BuyiPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BuyiPictureBackendApplication.class, args);
    }

}
