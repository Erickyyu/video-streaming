package com.example.videostreaming.service;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.DownloadRetryOptions;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Service
public class VideoStreamingService {


    public void streamFromBlob(OutputStream os, String blobName) {
        BlobServiceClient blobServiceClient = getBlobServiceClient();
        BlobContainerClient blobContainerClient = blobServiceClient.getBlobContainerClient("test");
        BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
        blobClient.downloadStreamWithResponse(os,null, new DownloadRetryOptions().setMaxRetryRequests(5), null, false,null, Context.NONE);
    }

    public BlobServiceClient getBlobServiceClient(){
         return new BlobServiceClientBuilder()
                .endpoint("https://test0426.blob.core.windows.net/")
                .sasToken("?sv=2022-11-02&ss=bfqt&srt=sco&sp=rwdlacupiytfx&se=2023-10-30T14:44:38Z&st=2023-10-28T06:44:38Z&spr=https&sig=qeB%2B3R1ujbtqa6GfAIy9m45%2BK2OLjMMTr7nEqnqd9oI%3D")
                .buildClient();
    }


    public ResponseEntity<Resource> streamFile(String fileName) {

        BlobServiceClient blobServiceClient = getBlobServiceClient();
        BlobContainerClient blobContainerClient = blobServiceClient.getBlobContainerClient("test");
        BlobClient blobClient = blobContainerClient.getBlobClient(fileName);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        blobClient.downloadStream(outputStream);
        final byte[] bytes = outputStream.toByteArray();

        ByteArrayResource resource = new ByteArrayResource(bytes);

        return ResponseEntity.status(HttpStatus.OK)
                .header("Content-Type", "video/mp4")
                .header("Accept-Ranges", "bytes")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }


    //local files
    public ResponseEntity<StreamingResponseBody> streamVideo(String httpRangeList, String filePath) {

        try {
            long rangeStart = 0;
            long rangeEnd = 314700;
            final long fileSize = getFileSize(filePath);

            if (httpRangeList == null) {
                return createResponseEntity(rangeStart, rangeEnd, filePath, fileSize);
            }

            String[] ranges = httpRangeList.split("-");
            rangeStart = Long.parseLong(ranges[0].substring(6));
            if (ranges.length > 1) {
                rangeEnd = Long.parseLong(ranges[1]);
            } else {
                rangeEnd = rangeStart + 314700;
            }

            rangeEnd = Math.min(rangeEnd, fileSize - 1);
            String contentLength = String.valueOf((rangeEnd - rangeStart) + 1);
            return createResponseEntity(rangeStart, rangeEnd, filePath, fileSize);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }

    public ResponseEntity<StreamingResponseBody> createResponseEntity(long rangeStart, long rangeEnd, String filePath, long fileSize) {

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header("Content-Type", "video/mp4")
                .header("Accept-Ranges", "bytes")
                .header("Content-Range", "bytes" + " " + rangeStart + "-" + rangeEnd + "/" + fileSize)
                .header("Content-Length", String.valueOf(fileSize))
                .body(streamVideoFile(filePath));
    }
    public Long getFileSize(String filePath) {
        return Optional.ofNullable(filePath)
                .map(file -> Paths.get(filePath))
                .map(this::sizeFromFile)
                .orElse(0L);
    }

    private Long sizeFromFile(Path path) {
        try {
            return Files.size(path);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        return 0L;
    }

    public StreamingResponseBody streamVideoFile(String filePath) {
        try (InputStream inputStream = new FileInputStream(filePath)) {
            return outputStream -> {
                try (outputStream) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();
                }
            };
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
