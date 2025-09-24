import swiftbot.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

// This is the main class for the TunnelVision2 project.
// It controls the SwiftBot, runs the main loop to detect tunnels and obstacles, and updates the UI.
public class TunnelVision2 {
    // This is the main method where the program starts.
    // It sets up all the components and runs the bot until it is stopped.
    public static void main(String[] args) {
        // Create the main objects we need to control the bot, log data, detect tunnels, and show the UI
        SwiftBotController bot = new SwiftBotController();
        DataLogger logger = new DataLogger();
        TunnelDetector detector = new TunnelDetector(bot, logger);
        UIHandler ui = new UIHandler(detector, logger, bot);

        // Show the startup screen and wait for the user to press a button
        ui.displayStartUI();
        bot.setupButtons();
        bot.waitForStart();

        // Start the bot moving and keep track of the start time
        long startTime = System.currentTimeMillis();
        bot.startMove(40, 40); // Move at speed 40 for both wheels
        boolean inTunnel = false; // Tracks if we are inside a tunnel
        int tunnelCount = 0; // Counts how many tunnels we have gone through
        long lastCheck = 0; // To make sure we check every 100 ms
        double totalDistance = 0.0; // Total distance traveled

        // This is the main loop that keeps running until the bot is stopped (e.g., by pressing X)
        while (bot.isRunning()) {
            try {
                long currentTime = System.currentTimeMillis();
                // Only check every 100 ms
                if (currentTime - lastCheck >= 100) {
                    // Take a grayscale picture and calculate the light intensity
                    BufferedImage img = bot.takeGrayscaleStill();
                    double intensity = detector.calculateAverageIntensity(img);
                    double distance = detector.checkUltrasound();
                    totalDistance = detector.calculateTotalDistance();

                    // Update the underlights to show if we are in a tunnel (green) or outside (blue)
                    bot.updateUnderlights(inTunnel);

                    // Check if there is an obstacle within 40 cm
                    if (distance < 40.0 && !bot.isObstacleDetected()) {
                        bot.stopMove(); // Stop the bot
                        ui.handleObstacle(img, distance, totalDistance, bot); // Show the obstacle alert
                    }
                    // Check if we are entering a tunnel (intensity drops below 100)
                    else if (!inTunnel && intensity < 100) {
                        inTunnel = true;
                        tunnelCount++;
                        detector.startTunnel(currentTime, intensity); // Start tracking the tunnel
                        // Update the UI to show we entered a tunnel
                        ui.displayRunningUI(tunnelCount, inTunnel, intensity, totalDistance,
                                "  - Entered Tunnel " + tunnelCount + ", Detecting...");
                    }
                    // Check if we are exiting a tunnel (intensity goes back to 100 or more)
                    else if (inTunnel && intensity >= 100) {
                        inTunnel = false;
                        double length = detector.endTunnel(currentTime); // Calculate the tunnel length
                        detector.logTunnelData(length); // Save the length
                        // Calculate the gap between tunnels if there is a previous tunnel
                        String gap = tunnelCount < detector.getEntryTimes().size()
                                ? String.format("%.0f cm", (detector.getEntryTimes().get(tunnelCount) -
                                detector.getExitTimes().get(tunnelCount - 1)) / 1000.0 * 12.0)
                                : "0 cm";
                        // Update the UI to show we exited the tunnel
                        ui.displayRunningUI(tunnelCount, inTunnel, intensity, totalDistance,
                                "  - Entered Tunnel " + tunnelCount + ", Distance: " + String.format("%.0f cm", length) +
                                        "\n  - Exited Tunnel " + tunnelCount + ", Gap: " + gap);
                    }
                    // If we are inside a tunnel, keep recording the light intensity
                    else if (inTunnel) {
                        detector.addIntensity(intensity);
                    }
                    // If we are outside a tunnel, just update the UI with the current status
                    else {
                        ui.displayRunningUI(tunnelCount, inTunnel, intensity, totalDistance, null);
                    }

                    lastCheck = currentTime;
                }
                Thread.sleep(100); // Wait 100 ms
            } catch (InterruptedException | IOException e) {
                // If something goes wrong, print the error so we can debug it
                e.printStackTrace();
            }
        }

        // When the loop ends, stop the bot and show the final UI
        bot.terminate(totalDistance, ui);
    }
}