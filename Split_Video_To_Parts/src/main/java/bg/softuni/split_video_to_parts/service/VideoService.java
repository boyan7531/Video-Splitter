package bg.softuni.split_video_to_parts.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class VideoService {

    public String processVideo(MultipartFile file, String ytLink, int parts, String partName) throws IOException, InterruptedException {
        String videoPath = "";
        String outputDir = Files.createTempDirectory("video_split_").toString(); // Temporary unique directory for each job

        if (!file.isEmpty()) {
            // Handle file upload
            videoPath = saveUploadedFile(file);
        } else if (!ytLink.isEmpty()) {
            // Handle YouTube link
            videoPath = downloadYouTubeVideo(ytLink, outputDir);
        }

        if (!videoPath.isEmpty()) {
            // Process video and split into parts
            splitVideoIntoParts(videoPath, parts, outputDir, partName);
        }

        return outputDir;
    }

    private String saveUploadedFile(MultipartFile file) throws IOException {
        Path uploadDir = Files.createTempDirectory("uploads_");  // Save uploads in a temporary directory
        Path filePath = uploadDir.resolve(file.getOriginalFilename());
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return filePath.toString();
    }

    private String downloadYouTubeVideo(String ytLink, String outputDir) throws IOException, InterruptedException {
        String command = String.format("youtube-dl -o \"%s\\video.mp4\" %s", outputDir, ytLink);
        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", command);
        Process process = pb.start();
        process.waitFor();

        return outputDir + "\\video.mp4";
    }
    public void splitVideoIntoParts(String videoPath, int parts, String partName, String outputDirectory) throws IOException, InterruptedException {
        // Use the outputDirectory parameter instead of hardcoding the output path

        // Ensure the output directory exists
        File outputDir = new File(outputDirectory);
        if (!outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                throw new IOException("Failed to create output directory: " + outputDirectory);
            }
        }

        double videoDuration = getVideoDuration(videoPath); // Assuming you have a method to get video duration
        double partDuration = videoDuration / parts;

        for (int i = 0; i < parts; i++) {
            double startTime = i * partDuration;
            String startTimeFormatted = String.format("%.2f", startTime);
            String partDurationFormatted = String.format("%.2f", partDuration);

            // Sanitize part name to avoid issues
            String sanitizedPartName = partName.replaceAll("[^a-zA-Z0-9_-]", "_");

            // Construct the output file path using the outputDirectory
            String outputFilePath = Paths.get(outputDirectory, String.format("%s_Part%d.mp4", sanitizedPartName, i + 1)).toString();

            // Escape any quotes or backslashes in the path (especially important for Windows)
            String escapedVideoPath = videoPath.replace("\\", "\\\\").replace("\"", "\\\"");
            String escapedOutputFilePath = outputFilePath.replace("\\", "\\\\").replace("\"", "\\\"");

            String command = String.format(
                    "\"C:\\Users\\bobch\\Downloads\\ffmpeg-master-latest-win64-gpl\\ffmpeg-master-latest-win64-gpl\\bin\\ffmpeg.exe\" -i \"%s\" -ss %s -t %s -c copy \"%s\"",
                    escapedVideoPath, startTimeFormatted, partDurationFormatted, escapedOutputFilePath
            );

            System.out.println("Running split command: " + command);

            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Capture FFmpeg output for detailed analysis
            String ffmpegOutput = new String(process.getInputStream().readAllBytes());
            System.out.println("FFmpeg output: " + ffmpegOutput);

            int exitCode = process.waitFor();
            System.out.println("FFmpeg process exited with code: " + exitCode);

            File outputFile = new File(outputFilePath);
            if (outputFile.exists() && outputFile.isFile()) {
                System.out.println("Successfully created: " + outputFilePath);
            } else {
                throw new IOException("Failed to create file: " + outputFilePath);
            }
        }
    }


    private double getVideoDuration(String videoPath) throws IOException, InterruptedException {
        // Ensure the video file exists
        File videoFile = new File(videoPath);
        if (!videoFile.exists()) {
            throw new IOException("Video file does not exist: " + videoPath);
        }

        // Full path to the ffmpeg executable
        String ffmpegPath = "C:\\Users\\bobch\\Downloads\\ffmpeg-master-latest-win64-gpl\\ffmpeg-master-latest-win64-gpl\\bin\\ffmpeg.exe";

        // Ensure the ffmpeg executable exists
        File ffmpegFile = new File(ffmpegPath);
        if (!ffmpegFile.exists()) {
            throw new IOException("FFmpeg executable not found at: " + ffmpegPath);
        }

        // Command to get video information using FFmpeg
        String command = String.format("\"%s\" -i \"%s\" 2>&1", ffmpegPath, videoPath);
        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", command);
        pb.redirectErrorStream(true); // Redirects the error stream to the input stream (FFmpeg outputs to stderr)

        Process process = pb.start();
        String result = new String(process.getInputStream().readAllBytes()).trim();

        // Log the command and its output for debugging purposes
        System.out.println("Running command: " + command);
        System.out.println("FFmpeg output: " + result);

        // Check if the output contains the "Duration" line
        if (!result.contains("Duration")) {
            throw new IOException("Failed to retrieve video duration. Output: " + result);
        }

        // Extract the duration from the output
        String durationLine = result.split("Duration: ")[1].split(",")[0].trim();
        System.out.println("Extracted duration: " + durationLine); // Debugging log

        String[] timeComponents = durationLine.split(":");

        // Check if we have exactly 3 components (hours, minutes, seconds)
        if (timeComponents.length != 3) {
            throw new IOException("Failed to parse duration into hours, minutes, and seconds. Duration string: " + durationLine);
        }

        double hours = Double.parseDouble(timeComponents[0]);
        double minutes = Double.parseDouble(timeComponents[1]);
        double seconds = Double.parseDouble(timeComponents[2]);

        return (hours * 3600) + (minutes * 60) + seconds;
    }
}
