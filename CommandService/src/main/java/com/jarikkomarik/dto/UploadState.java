package com.jarikkomarik.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.services.s3.model.CompletedPart;

import java.util.LinkedHashSet;
import java.util.Set;

@RequiredArgsConstructor
@Getter
@Setter
public class UploadState {
    private final String fileKey;

    private String uploadId;
    private int partCounter = 1;
    private Set<CompletedPart> completedParts = new LinkedHashSet<>();
}
