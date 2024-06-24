package com.jarikkomarik.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePartEvent;
import org.springframework.http.codec.multipart.FormPartEvent;
import org.springframework.http.codec.multipart.PartEvent;
import org.springframework.util.FileSystemUtils;
import reactor.core.publisher.Flux;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocalFileVideoServiceTest {

    private final LocalFileVideoService localFileVideoService = new LocalFileVideoService();

    AtomicReference<String> directoryPath = new AtomicReference<>();

    @AfterEach
    void cleanUp() {
        if (directoryPath.get() != null) {
            FileSystemUtils.deleteRecursively(new File(directoryPath.get()));
        }
    }

    @Test
    void testProcessFileUpload_Valid() {

        Flux<PartEvent> partEventFlux = Flux.concat(
                FormPartEvent.create("fileName", "chiki"),
                FilePartEvent.create("1:3", new ClassPathResource("chiki_part1of3.mp4")),
                FilePartEvent.create("2:3", new ClassPathResource("chiki_part2of3.mp4")),
                FilePartEvent.create("3:3", new ClassPathResource("chiki_part3of3.mp4"))
        );

        assertThat(localFileVideoService.processFileUpload(partEventFlux).block())
                .satisfies(generatedFileName -> {
                    directoryPath.set(generatedFileName);
                    assertThat(generatedFileName).startsWith("chiki");
                    assertThat(Files.exists(Path.of(generatedFileName + File.separator + "chiki_part1of3.mp4"))).isTrue();
                    assertThat(Files.exists(Path.of(generatedFileName + File.separator + "chiki_part2of3.mp4"))).isTrue();
                    assertThat(Files.exists(Path.of(generatedFileName + File.separator + "chiki_part3of3.mp4"))).isTrue();
                });
    }

    @Test
    void testProcessFileUpload_MissingFileName() {

        Flux<PartEvent> partEventFlux = Flux.concat(
                FilePartEvent.create("1:3", new ClassPathResource("chiki_part1of3.mp4"))
        );

        assertThrows(IllegalStateException.class, () -> localFileVideoService.processFileUpload(partEventFlux).block(), "First header is not fileName");
    }

    @Test
    void testProcessFileUpload_EmptyFilePart() {

        FilePartEvent emptyFilePartEvent = mock(FilePartEvent.class);
        when(emptyFilePartEvent.filename()).thenReturn("");


        Flux<PartEvent> partEventFlux = Flux.concat(
                FormPartEvent.create("fileName", "chiki"),
                Flux.just(emptyFilePartEvent),
                FilePartEvent.create("2:3", new ClassPathResource("chiki_part2of3.mp4")),
                FilePartEvent.create("3:3", new ClassPathResource("chiki_part3of3.mp4"))
        );

        assertThrows(IllegalStateException.class, () -> localFileVideoService.processFileUpload(partEventFlux).block(), "missing file");
    }
}
