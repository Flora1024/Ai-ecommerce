package com.flora.ai.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class FloraAiEcommerceSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(FloraAiEcommerceSpringApplication.class, args);
    }

}
