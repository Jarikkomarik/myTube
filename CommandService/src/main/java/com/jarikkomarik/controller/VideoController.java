package com.jarikkomarik.controller;

import com.jarikkomarik.service.FileVideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.multipart.PartEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
@RestController
@RequestMapping("api/v1/video")
@RequiredArgsConstructor
@Slf4j
public class VideoController {

    private final FileVideoService fileVideoService;


    @PostMapping("upload")
    public Mono<String> uploadFile(@RequestBody Flux<PartEvent> partEvent) {
        return fileVideoService.processFileUpload(partEvent);
    }
}
