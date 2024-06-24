package com.jarikkomarik.controller;

import com.jarikkomarik.interfaces.FileVideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.multipart.PartEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
@RestController
@RequestMapping("api/v1/video")
@RequiredArgsConstructor
@Slf4j
public class VideoController {

    private final FileVideoService videoService;


    @PostMapping("upload")
    public Mono<String> uploadFile(@RequestBody Flux<PartEvent> partEvent) {
        return videoService.processFileUpload(partEvent);
    }
}
