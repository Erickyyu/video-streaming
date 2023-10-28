package com.example.videostreaming.controller;

import com.azure.core.http.HttpHeader;
import com.azure.core.util.io.IOUtils;
import com.example.videostreaming.service.VideoStreamingService;
import com.example.videostreaming.utils.IOUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.OutputStream;

@RestController
public class VideoStramingController {

    private final VideoStreamingService vidoeStreamingService;

    public VideoStramingController(VideoStreamingService vidoeStreamingService) {
        this.vidoeStreamingService = vidoeStreamingService;
    }

    @GetMapping(path="/blob/{fileName}",produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadBlob(@PathVariable String fileName, HttpServletResponse response) {
        OutputStream os = null;
        try {
            response.setContentType("application/octet-stream");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + fileName);
            os = response.getOutputStream();
            vidoeStreamingService.streamFromBlob(os, fileName);
            os.flush();
        }catch (Exception ex) {
            ex.printStackTrace();
        }finally {
            if (os != null) {
                IOUtil.closeQuietly(os);
            }

        }
    }

    @GetMapping("/videos/{fileName}")
    public ResponseEntity<Resource> steamVideo(@RequestHeader(value = "Range", required = false) String httpRangeList,
                                               @PathVariable("fileName") String fileName) {

        return vidoeStreamingService.streamFile(fileName);

    }

}
