package bg.softuni.split_video_to_parts.controller;

import bg.softuni.split_video_to_parts.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Controller
public class VideoController {

    @Autowired
    private VideoService videoService;

    @PostMapping("/split-video")
    public StreamingResponseBody splitVideo(@RequestParam("file") MultipartFile file,
                                            @RequestParam("ytLink") String ytLink,
                                            @RequestParam("parts") int parts,
                                            @RequestParam("partName") String partName,
                                            HttpServletResponse response) {
        try {
            String outputDir = videoService.processVideo(file, ytLink, parts, partName);

            File dir = new File(outputDir);
            File[] files = dir.listFiles();
            if (files == null || files.length == 0) {
                throw new IOException("No files found in the output directory");
            }

            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=\"split_videos.zip\"");

            return outputStream -> {
                try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
                    for (File filePart : files) {
                        try (FileInputStream fis = new FileInputStream(filePart)) {
                            ZipEntry zipEntry = new ZipEntry(filePart.getName());
                            zipOut.putNextEntry(zipEntry);
                            byte[] bytes = new byte[1024];
                            int length;
                            while ((length = fis.read(bytes)) >= 0) {
                                zipOut.write(bytes, 0, length);
                            }
                            zipOut.closeEntry();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    throw e;
                }
            };
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return outputStream -> {
                try (PrintWriter writer = new PrintWriter(outputStream)) {
                    writer.println("An error occurred while processing the video.");
                }
            };
        }
    }
}
