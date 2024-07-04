package com.jarikkomarik.service;

import com.jarikkomarik.dto.UploadState;
import com.jarikkomarik.interfaces.VideoRepository;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePartEvent;
import org.springframework.http.codec.multipart.FormPartEvent;
import org.springframework.http.codec.multipart.PartEvent;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;

@Service
public class LocalFileRepository implements VideoRepository {


    @Override
    public Mono<String> registerNewFile(FormPartEvent formPartEvent, UploadState uploadState) {
        if (!formPartEvent.name().equals("fileName")) return Mono.error(new IllegalStateException("First header is not fileName"));

        return Mono.fromCallable(() -> {
            Path directoryPath = Path.of(uploadState.getFileKey());
            Files.createDirectories(directoryPath);
            return uploadState.getFileKey();
        }).subscribeOn(Schedulers.boundedElastic());


    }

    @Override
    public Mono<String> saveFilePart(Flux<PartEvent> windowFlux, FilePartEvent filePartEvent, UploadState uploadState) {
        if (uploadState.getFileKey() == null) return Mono.error(new IllegalStateException("missing fileName header"));
        if (filePartEvent.filename().isEmpty()) return Mono.error(new IllegalStateException("missing file"));

        Path filePath = Path.of(uploadState.getFileKey(), filePartEvent.filename());
        return DataBufferUtils.write(windowFlux.map(PartEvent::content), filePath).onErrorStop()
                .then(Mono.empty());
    }

    @Override
    public Mono<String> completeFileSave(UploadState uploadState) {
        return Mono.fromCallable(() -> {
            Path directoryPath = Path.of(uploadState.getFileKey());
            Path renamedDirectoryPath = directoryPath.resolveSibling(directoryPath.getFileName() + "_temp");
            Path mergedFilePath = directoryPath.resolveSibling(directoryPath.getFileName());

            try {
                // Rename original directory
                Files.move(directoryPath, renamedDirectoryPath);

                // Create the merged file
                Files.createFile(mergedFilePath);

                // Read and write all files in the renamed directory to the merged file
                try (Stream<Path> files = Files.list(renamedDirectoryPath).sorted()) {
                    files.forEach(file -> {
                        try {
                            Files.write(mergedFilePath, Files.readAllBytes(file), StandardOpenOption.APPEND);
                        } catch (Exception e) {
                            throw new RuntimeException("Error writing to merged file", e);
                        }
                    });
                }

                // Delete the renamed(original) directory
                FileSystemUtils.deleteRecursively(renamedDirectoryPath);

                return mergedFilePath.toString();
            } catch (Exception e) {
                throw new RuntimeException("Error processing directory", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public void cleanupCreatedFiles(UploadState uploadState) {
        if (uploadState.getFileKey() == null) throw new IllegalStateException("missing fileName header");
        FileSystemUtils.deleteRecursively(new File(uploadState.getFileKey()));
    }
}
