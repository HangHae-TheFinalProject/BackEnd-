package com.example.finalproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

import javax.annotation.PostConstruct;
import java.util.TimeZone;

@EnableJpaAuditing
@SpringBootApplication
@EnableWebSocket
public class FinalprojectApplication {


    //@PostConstruct는 Bean이 완전히 초기화 된 후,단 한번만 호출되는 메서드 이다.
    //애플리케이션이 처음 구동될때 한번 실행된다
    @PostConstruct
    public void started() {
        // timezone 세팅
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

    public static void main(String[] args) {

        SpringApplication.run(FinalprojectApplication.class, args);
        System.out.println("실행 확인~~~~~~~");
    }

}
