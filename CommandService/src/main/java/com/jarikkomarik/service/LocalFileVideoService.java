package com.jarikkomarik.service;

import com.jarikkomarik.interfaces.FileVideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePartEvent;
import org.springframework.http.codec.multipart.FormPartEvent;
import org.springframework.http.codec.multipart.PartEvent;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocalFileVideoService implements FileVideoService {


    public Mono<String> processFileUpload(Flux<PartEvent> partEvent) {
        AtomicReference<String> directoryName = new AtomicReference<>();

        return partEvent.windowUntil(PartEvent::isLast)
                .concatMap(window -> window.switchOnFirst((signal, windowFlux) -> {
                    if (signal.hasValue()) {

                        if (signal.get() instanceof FormPartEvent formPartEvent) {
                            String fileName = formPartEvent.value() + ":" + UUID.randomUUID();
                            directoryName.set(fileName);
                            return registerNewFile(formPartEvent, fileName).contextWrite(Context.of("fileName", fileName));

                        } else if (signal.get() instanceof FilePartEvent filePartEvent) {
                            return saveFilePart(windowFlux, filePartEvent, directoryName);

                        } else {

                            return Mono.error(new IllegalStateException("Unsupported partEvent type"));
                        }

                    } else {
                        return Mono.error(new IllegalStateException("Received empty PartEvent"));
                    }
                }))
                .doOnError(exception -> cleanupCreatedFiles(directoryName.get()))
                .reduce((first, second) -> first);
    }

    private Mono<String> registerNewFile(FormPartEvent formPartEvent, String fileName) {
        if (!formPartEvent.name().equals("fileName")) return Mono.error(new IllegalStateException("First header is not fileName"));

        return Mono.fromCallable(() -> {
            Path directoryPath = Path.of(fileName);
            Files.createDirectories(directoryPath);
            return fileName;
        }).subscribeOn(Schedulers.boundedElastic());


    }

    private Mono<String> saveFilePart(Flux<PartEvent> windowFlux, FilePartEvent filePartEvent, AtomicReference<String> directoryName) {
        if (directoryName.get() == null) return Mono.error(new IllegalStateException("missing fileName header"));
        if (filePartEvent.filename().isEmpty()) return Mono.error(new IllegalStateException("missing file"));

        Path filePath = Path.of(directoryName.get(), filePartEvent.filename());
        return DataBufferUtils.write(windowFlux.map(PartEvent::content), filePath).onErrorStop()
                .then(Mono.empty());
    }

    private void cleanupCreatedFiles(String dirName) {
        if (dirName == null) throw new IllegalStateException("missing fileName header");
        FileSystemUtils.deleteRecursively(new File(dirName));
    }

}
