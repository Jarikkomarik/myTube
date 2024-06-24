package com.jarikkomarik.Util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileChunker {

    public static void main(String[] args) {
        String filePath = "CommandService/src/test/resources/chiki.mp4";
        splitFileIntoChunks(filePath);
    }

    private static void splitFileIntoChunks(String filePath) {
        try {
            Path file = Paths.get(filePath);
            long fileSize = Files.size(file);
            long chunkSize = 5 * 1024 * 1024; // 5 MB in bytes
            long totalChunks = (fileSize + chunkSize - 1) / chunkSize;

            try (InputStream inputStream = new BufferedInputStream(new FileInputStream(filePath))) {
                byte[] buffer = new byte[(int) chunkSize];
                int chunkIndex = 1;

                while (true) {
                    int bytesRead = inputStream.read(buffer);
                    if (bytesRead == -1) {
                        break;
                    }

                    String chunkFileName = getChunkFileName(filePath, chunkIndex, totalChunks);
                    try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(chunkFileName))) {
                        outputStream.write(buffer, 0, bytesRead);
                    }

                    chunkIndex++;
                }
            }

            System.out.println("File split into " + totalChunks + " chunks successfully.");
        } catch (IOException e) {
            System.err.println("Error while splitting file: " + e.getMessage());
        }
    }

    private static String getChunkFileName(String originalFilePath, int chunkIndex, long totalChunks) {
        String extension = "";
        int lastDotIndex = originalFilePath.lastIndexOf(".");
        if (lastDotIndex != -1) {
            extension = originalFilePath.substring(lastDotIndex);
        }
        return originalFilePath.replace(extension, "") + "_part" + chunkIndex + "of" + totalChunks + extension;
    }


}
