package org.example.web_lab_3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WebLab3Application {

    public static void main(String[] args) {
        System.out.println(System.getProperty("java.home"));
        SpringApplication.run(WebLab3Application.class, args);
    }

}
