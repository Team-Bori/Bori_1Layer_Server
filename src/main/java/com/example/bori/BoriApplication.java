package com.example.bori;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class BoriApplication {

    public static void main(String[] args) {
        // DB는 TIMESTAMP(타임존 없음)에 한국 시간을 저장한다. 서버 기본 타임존이 UTC여도 같게 맞춘다.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        SpringApplication.run(BoriApplication.class, args);
    }

}
