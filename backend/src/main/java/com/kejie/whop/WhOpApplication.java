package com.kejie.whop;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.kejie.whop.mapper")
public class WhOpApplication {

    public static void main(String[] args) {
        SpringApplication.run(WhOpApplication.class, args);
    }
}
