package com.jarikkomarik.service;

import com.jarikkomarik.dto.UploadState;
import com.jarikkomarik.interfaces.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.codec.multipart.FilePartEvent;
import org.springframework.http.codec.multipart.FormPartEvent;
import org.springframework.http.codec.multipart.PartEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileVideoService {

    @Qualifier("localFileRepository")
    private final VideoRepository videoRepository;


    public Mono<String> processFileUpload(Flux<PartEvent> partEvent) {
        AtomicReference<UploadState> uploadStateAtomicReference = new AtomicReference<>();

        return partEvent.windowUntil(PartEvent::isLast)
                .concatMap(window -> window.switchOnFirst((signal, windowFlux) -> {
                    if (!signal.hasValue()) return Mono.error(new IllegalStateException("Received empty PartEvent"));

                    if (signal.get() instanceof FormPartEvent formPartEvent) {
                        String fileName = formPartEvent.value() + ":" + UUID.randomUUID() + ".mp4";
                        uploadStateAtomicReference.set(new UploadState(fileName));

                        return videoRepository.registerNewFile(formPartEvent, uploadStateAtomicReference.get());

                    } else if (signal.get() instanceof FilePartEvent filePartEvent) {
                        return videoRepository.saveFilePart(windowFlux, filePartEvent, uploadStateAtomicReference.get());
                    }

                    return Mono.error(new IllegalStateException("Unsupported partEvent type"));
                }))
                .doOnError(exception -> videoRepository.cleanupCreatedFiles(uploadStateAtomicReference.get()))
                .reduce((first, second) -> first)
                .flatMap(string -> videoRepository.completeFileSave(uploadStateAtomicReference.get()));
    }

}
