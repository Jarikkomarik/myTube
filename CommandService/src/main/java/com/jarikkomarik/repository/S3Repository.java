package com.jarikkomarik.repository;

import com.jarikkomarik.configuration.S3ClientConfigurationProperties;
import com.jarikkomarik.dto.UploadState;
import com.jarikkomarik.exceptions.UploadFailedException;
import com.jarikkomarik.interfaces.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePartEvent;
import org.springframework.http.codec.multipart.FormPartEvent;
import org.springframework.http.codec.multipart.PartEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import java.nio.ByteBuffer;


@Slf4j
@Service
@RequiredArgsConstructor
public class S3Repository implements VideoRepository {

    private final S3AsyncClient s3Client;
    private final S3ClientConfigurationProperties s3ClientConfigurationProperties;

    @Override
    public Mono<String> registerNewFile(FormPartEvent formPartEvent, UploadState uploadState) {
        if (!formPartEvent.name().equals("fileName"))
            return Mono.error(new IllegalStateException("First header is not fileName"));

        return Mono.fromFuture(
                s3Client.createMultipartUpload(
                        CreateMultipartUploadRequest
                                .builder()
                                .contentType(MediaType.APPLICATION_OCTET_STREAM.getType())
                                .key(uploadState.getFileKey())
                                .bucket(s3ClientConfigurationProperties.getBucket())
                                .build())
        ).map(createMultipartUploadResponse -> {
            checkResult(createMultipartUploadResponse);
            log.info("Successfully registered new file with id: {}", createMultipartUploadResponse.uploadId());
            uploadState.setUploadId(createMultipartUploadResponse.uploadId());
            return createMultipartUploadResponse.uploadId();
        });
    }

    @Override
    public Mono<String> saveFilePart(Flux<PartEvent> windowFlux, FilePartEvent filePartEvent, UploadState uploadState) {
        if (uploadState.getFileKey() == null) return Mono.error(new IllegalStateException("missing fileName header"));
        if (filePartEvent.filename().isEmpty()) return Mono.error(new IllegalStateException("missing file"));

        return byteBufferFromPartEventFlux(windowFlux).flatMap(byteBuffer ->
                Mono.fromFuture(s3Client.uploadPart(
                                UploadPartRequest.builder()
                                        .bucket(s3ClientConfigurationProperties.getBucket())
                                        .key(uploadState.getFileKey())
                                        .partNumber(uploadState.getPartCounter())
                                        .uploadId(uploadState.getUploadId())
                                        .contentLength((long) byteBuffer.capacity())
                                        .build(),
                                AsyncRequestBody.fromByteBuffer(byteBuffer)
                        ))
                        .map(uploadPartResponse -> {
                            uploadState.getCompletedParts().add(
                                    CompletedPart.builder()
                                            .eTag(uploadPartResponse.eTag())
                                            .partNumber(uploadState.getPartCounter())
                                            .build()
                            );
                            uploadState.setPartCounter(uploadState.getPartCounter() + 1);
                            checkResult(uploadPartResponse);
                            log.info("Successfully saved file part with id: {}", uploadPartResponse.eTag());
                            return uploadPartResponse.eTag();
                        })
        );
    }

    @Override
    public Mono<String> completeFileSave(UploadState uploadState) {
        return Mono.fromFuture(s3Client.completeMultipartUpload(
                CompleteMultipartUploadRequest.builder()
                        .bucket(s3ClientConfigurationProperties.getBucket())
                        .key(uploadState.getFileKey())
                        .multipartUpload(
                                CompletedMultipartUpload.builder()
                                        .parts(uploadState.getCompletedParts())
                                        .build()
                        )
                        .uploadId(uploadState.getUploadId())
                        .build())
        ).map(completeMultipartUploadResponse -> {
            log.info("Successfully uploaded file with key: {}", completeMultipartUploadResponse.key());
            return completeMultipartUploadResponse.key();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public void cleanupCreatedFiles(UploadState uploadState) {
        s3Client.abortMultipartUpload(
                AbortMultipartUploadRequest.builder()
                        .bucket(s3ClientConfigurationProperties.getBucket())
                        .key(uploadState.getFileKey())
                        .uploadId(uploadState.getUploadId())
                        .build());
    }

    private Mono<ByteBuffer> byteBufferFromPartEventFlux(Flux<PartEvent> partEventFlux) {
        return DataBufferUtils.join(partEventFlux.map(PartEvent::content)).flatMap(dataBuffer -> {
            try {
                ByteBuffer byteBuffer = ByteBuffer.allocate(dataBuffer.readableByteCount());
                dataBuffer.toByteBuffer(byteBuffer);
                return Mono.just(byteBuffer.rewind());
            } finally {
                DataBufferUtils.release(dataBuffer);
            }
        });
    }

    private static void checkResult(SdkResponse result) {
        if (result.sdkHttpResponse() == null || !result.sdkHttpResponse().isSuccessful()) {
            throw new UploadFailedException(result);
        }
    }
}
