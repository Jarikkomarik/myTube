package com.jarikkomarik.Util;
import java.io.*;
import java.util.Arrays;
import java.util.Comparator;

public class FileJoiner {

    public static void main(String[] args) {
        String chunkDirectory = "hello:2cc97684-efd7-4534-8db4-257490edcaa3";
        String outputFile = "longVideo_reconstructed.mp4";
        joinChunks(chunkDirectory, outputFile);
    }

    private static void joinChunks(String chunkDirectory, String outputFile) {
        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile))) {
            Arrays.stream(new File(chunkDirectory).listFiles()).sorted(Comparator.comparing(File::lastModified)).forEach(file -> {
                try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            System.out.println("Chunks merged successfully into: " + outputFile);
        } catch (IOException e) {
            System.err.println("Error while joining chunks: " + e.getMessage());
        }
    }
}
