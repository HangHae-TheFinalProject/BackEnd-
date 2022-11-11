package com.example.finalproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

@EnableJpaAuditing
@SpringBootApplication
@EnableWebSocket
public class FinalprojectApplication {

    public static void main(String[] args) {

        SpringApplication.run(FinalprojectApplication.class, args);
        System.out.println("실행 확인~~~~~~~");
    }

}
