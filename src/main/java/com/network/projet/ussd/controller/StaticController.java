package com.network.projet.ussd.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
public class StaticController {

    @GetMapping("/")
    public Mono<Resource> index() {
        return Mono.just(new ClassPathResource("static/index.html"));
    }

    @GetMapping("/index.html")
    public Mono<Resource> indexHtml() {
        return Mono.just(new ClassPathResource("static/index.html"));
    }
}
