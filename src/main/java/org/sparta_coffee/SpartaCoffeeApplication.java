package org.sparta_coffee;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableRetry
@SpringBootApplication
@EnableJpaAuditing
public class SpartaCoffeeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpartaCoffeeApplication.class, args);
    }

}
