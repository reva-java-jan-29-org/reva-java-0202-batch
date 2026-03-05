package com.training.category;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo")
public class DemoController {

    record DemoResponse(String message) {}

    @GetMapping
    public DemoResponse sayHello() {
        return new DemoResponse("hello");
    }
}
