package com.xzc.buyipicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * @author xuzhichao
 */
@SpringBootApplication
@MapperScan("com.xzc.buyipicturebackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class BuyiPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BuyiPictureBackendApplication.class, args);
    }

}
