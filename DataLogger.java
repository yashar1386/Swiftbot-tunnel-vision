import swiftbot.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

// This class handles saving photos of obstacles and writing the log file for the TunnelVision2 project.
// It keeps track of where the obstacle photo is saved and writes all the journey details to a log file.
public class DataLogger {
    private String obstaclePhotoPath = ""; // Stores the path where the obstacle photo is saved

    // This method saves a photo of an obstacle with a timestamp in the filename.
    // It creates a "photos" directory if it doesn't exist and saves the image as a JPG.
    public void saveObstaclePhoto(BufferedImage img) throws IOException {
        // Create a timestamp for the filename so we know when the photo was taken
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        obstaclePhotoPath = "photos/obstacle_" + timestamp + ".jpg";
        File outputFile = new File(obstaclePhotoPath);
        File parentDir = outputFile.getParentFile();

        // Check if the "photos" directory exists, and if not, create it
        if (!parentDir.exists()) {
            System.out.println("Creating photos directory at: " + parentDir.getAbsolutePath());
            if (!parentDir.mkdirs()) {
                throw new IOException("Failed to create photos directory at " + parentDir.getAbsolutePath());
            }
        }

        // Let the user know we're saving the photo
        System.out.println("Saving obstacle photo to: " + obstaclePhotoPath);

        // Make sure the image isn't null before saving
        if (img == null) {
            throw new IOException("Cannot save null image");
        }

        // Save the image as a JPG file
        if (!ImageIO.write(img, "jpg", outputFile)) {
            throw new IOException("Failed to write image to " + obstaclePhotoPath);
        }
        System.out.println("Obstacle photo saved successfully to: " + obstaclePhotoPath);
    }

    // This method writes all the journey details to a log file.
    // It includes things like the number of tunnels, their lengths, the total distance, and more.
    public void writeLogFile(String path, double totalDistance, long endTime,
                             ArrayList<Double> tunnelLengths, ArrayList<Double> avgIntensities) {
        try (FileWriter writer = new FileWriter(path)) {
            // Write the header and some basic info to the log file
            writer.write("Tunnel Vision Log\n");
            writer.write("-----------------\n");
            writer.write("Total Tunnels: " + tunnelLengths.size() + "\n");

            // Write the lengths of all tunnels in a list
            writer.write("Tunnel Lengths: [");
            for (int i = 0; i < tunnelLengths.size(); i++) {
                writer.write(String.format("%.0f cm", tunnelLengths.get(i)) +
                        (i < tunnelLengths.size() - 1 ? ", " : ""));
            }
            writer.write("]\n");

            // Write the total distance and average light intensity
            writer.write(String.format("Total Distance Covered: %.0f cm\n", totalDistance));
            writer.write(String.format("Average Light Intensity: %.0f%%\n", calculateAverage(avgIntensities) / 255.0 * 100));

            // Check if we detected any obstacles and include the photo path if we did
            writer.write("Obstacles Detected: " + (obstaclePhotoPath.isEmpty() ? "0" : "1\n"));
            if (!obstaclePhotoPath.isEmpty()) writer.write("Obstacle Photo: " + obstaclePhotoPath + "\n");

            // Write the total time the program ran
            writer.write("Execution Time: " + formatTime(endTime) + "\n");
        } catch (IOException e) {
            // If something goes wrong while writing the file, print an error message
            System.out.println("Error writing log file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // This method calculates the average of a list of numbers.
    // I use it to find the average light intensity across all tunnels.
    private double calculateAverage(ArrayList<Double> values) {
        // If the list is empty, return 0 to avoid dividing by zero
        if (values.isEmpty()) return 0;

        // Add up all the values and divide by the number of values
        double sum = 0;
        for (Double val : values) sum += val;
        return sum / values.size();
    }

    // This method turns a time in milliseconds into a nice HH:MM:SS format.
    // I use it to show how long the journey took in the log file.
    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds);
    }

    // This method returns the path where the obstacle photo was saved.
    // It is used when writing the log file to include the photo location.
    public String getObstaclePhotoPath() {
        return obstaclePhotoPath;
    }
}