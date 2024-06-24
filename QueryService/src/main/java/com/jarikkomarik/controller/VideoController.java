package com.jarikkomarik.controller;

import com.jarikkomarik.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class VideoController {

    private final VideoService service;
    /**
     * Web server automatically handles partitioning of whole file into smaller byte range based on client request Range header.
     */
    @GetMapping(value = "video/{title}", produces = "video/mp4")
    public Mono<Resource> getVideos(@PathVariable("title") String title) {
        return service.getVideo(title);
    }
}
