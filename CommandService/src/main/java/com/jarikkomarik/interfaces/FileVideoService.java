package com.jarikkomarik.interfaces;

import org.springframework.http.codec.multipart.PartEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FileVideoService {

    Mono<String> processFileUpload(Flux<PartEvent> partEvent);
}
