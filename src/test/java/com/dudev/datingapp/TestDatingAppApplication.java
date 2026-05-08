package com.dudev.datingapp;

import org.springframework.boot.SpringApplication;

public class TestDatingAppApplication {

    public static void main(String[] args) {
        SpringApplication.from(DatingAppApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
