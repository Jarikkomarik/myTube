package com.jarikkomarik;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class CommandService {
    public static void main(String[] args) {
        SpringApplication.run(CommandService.class, args);
    }

}
