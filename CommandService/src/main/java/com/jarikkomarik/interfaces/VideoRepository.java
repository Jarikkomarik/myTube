package com.jarikkomarik.interfaces;

import com.jarikkomarik.dto.UploadState;
import org.springframework.http.codec.multipart.FilePartEvent;
import org.springframework.http.codec.multipart.FormPartEvent;
import org.springframework.http.codec.multipart.PartEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
public interface VideoRepository {



    Mono<String> registerNewFile(FormPartEvent formPartEvent, UploadState uploadState);

    public Mono<String> saveFilePart(Flux<PartEvent> windowFlux, FilePartEvent filePartEvent, UploadState uploadState);

    public Mono<String> completeFileSave(UploadState uploadState);

    public void cleanupCreatedFiles(UploadState uploadState);
}
