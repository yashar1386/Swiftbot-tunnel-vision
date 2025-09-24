import swiftbot.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

// This class handles all the user interface stuff for the TunnelVision2 project.
// It shows the status updates, handles obstacles, and lets the user view logs or a map at the end.
public class UIHandler {
    private final TunnelDetector detector; // To get tunnel data
    private final DataLogger logger; // To save logs and photos
    private final SwiftBotController bot; // To control the bot
    private final long startTime; // To track how long the program has been running
    private boolean viewLog; // Tracks if the user wants to see the log
    private boolean mapRequested; // Tracks if the user wants to see the tunnel map
    private final AtomicBoolean choiceMade = new AtomicBoolean(false); // To handle button presses

    // Constructor to set up the UIHandler with the components it needs.
    public UIHandler(TunnelDetector detector, DataLogger logger, SwiftBotController bot) {
        this.detector = detector;
        this.logger = logger;
        this.bot = bot;
        this.startTime = System.currentTimeMillis(); // Record the start time
        this.viewLog = false;
        this.mapRequested = false;
    }

    // This method shows the startup screen with instructions and the initial status.
    public void displayStartUI() {
        clearScreen(); // Clear the screen for a fresh display
        System.out.println("*********************************************************");
        System.out.println("**********   SWIFTBOT TUNNEL VISION        **************");
        System.out.println("*********************************************************");
        System.out.println("** Task Overview:");
        System.out.println("Swift Bot will travel through tunnels, measure distances, detect obstacles, and log its journey.");
        System.out.println("***********************************************************");
        System.out.println("*               STATUS PANEL                              *");
        System.out.println("***********************************************************");
        System.out.println("*  Current Position: Outside Tunnel                       *");
        System.out.println("*  Light Intensity: 85%                                   *");
        System.out.println("*  Obstacle Detected: No                                  *");
        System.out.println("*  Time Elapsed: 00:00:00                                 *");
        System.out.println("***********************************************************");
        System.out.println("**Objective**:");
        System.out.println("The Swift Bot will:");
        System.out.println("  - Detect tunnels using light intensity changes.");
        System.out.println("  - Measure distances and navigate obstacles.");
        System.out.println("  - Log all data before completion.");
        System.out.println("**Controls**:");
        System.out.println("  - [Y] Start the journey");
        System.out.println("  - [X] Terminate the program");
        System.out.println("***************************************************");
        System.out.println("Initializing...");
        System.out.println("Press Button 'Y' to start.");
    }

    // This method updates the UI while the bot is running.
    // It shows the current status like position, intensity, and distance, plus any action logs.
    public void displayRunningUI(int tunnelCount, boolean inTunnel, double intensity, double totalDistance, String actionLog) {
        clearScreen(); // Clear the screen for a fresh display
        System.out.println("*********************************************************");
        System.out.println("**********   SWIFTBOT TUNNEL VISION        **************");
        System.out.println("*********************************************************");
        System.out.println("Task Progress:");
        System.out.println("Swift Bot is navigating through the tunnels...");
        System.out.println();
        System.out.println("*****************************************************************");
        System.out.println("Action Log:");
        if (actionLog != null) {
            System.out.println(actionLog); // Show any specific actions like entering a tunnel
        }
        System.out.println("**No obstacles detected. Continuing forward...**");
        System.out.println("Press Button 'X' to terminate early.");
        System.out.println();
        System.out.println("**********************************************************");
        System.out.println("*                STATUS PANEL                            *");
        System.out.println("**********************************************************");
        // Show the current position (inside or outside a tunnel)
        System.out.printf("*  Current Position: %s                     *\n",
                inTunnel ? "Inside Tunnel " + tunnelCount : "Outside Tunnel");
        // Show the light intensity as a percentage
        System.out.printf("*  Light Intensity: %.0f%%                                  *\n",
                intensity / 255.0 * 100);
        System.out.printf("*  Distance Covered: %.0f cm                               *\n", totalDistance);
        System.out.printf("*  Obstacle Detected: No                                 *\n");
        // Show how long the program has been running
        System.out.printf("*  Time Elapsed: %s                                *\n",
                formatTime(System.currentTimeMillis()));
        System.out.println("**********************************************************");
    }

    // This method handles what happens when an obstacle is detected.
    // It stops the bot, saves a photo, and waits for the user to press X to continue.
    public void handleObstacle(BufferedImage img, double distance, double totalDistance, SwiftBotController bot) {
        try {
            // Save a photo of the obstacle
            logger.saveObstaclePhoto(img);

            clearScreen(); // Clear the screen for the alert
            System.out.println("\n*********************************************************");
            System.out.println("**********   SWIFTBOT TUNNEL VISION        **************");
            System.out.println("*********************************************************");
            System.out.println("**ALERT**: Obstacle detected!");
            System.out.println("**********************************************************");
            System.out.println("*                STATUS PANEL                            *");
            System.out.println("**********************************************************");
            // Show the current position (inside or outside a tunnel)
            System.out.printf("*  Current Position: %s                     *\n",
                    detector.getEntryTimes().size() > detector.getExitTimes().size() ?
                            "Inside Tunnel " + detector.getEntryTimes().size() : "Outside Tunnel");
            // Show the light intensity and other status details
            System.out.printf("*  Light Intensity: %.0f%%                                  *\n",
                    calculateAverageIntensity(img) / 255.0 * 100);
            System.out.printf("*  Distance Covered: %.0f cm                               *\n", totalDistance);
            System.out.printf("*  Obstacle Detected: Yes (%.0f cm ahead)                  *\n", distance);
            System.out.printf("*  Time Elapsed: %s                                *\n",
                    formatTime(System.currentTimeMillis()));
            System.out.println("**********************************************************");
            System.out.println("Taking action...");
            System.out.println("  - SwiftBot has stopped.");
            System.out.println("Press Button 'X' to acknowledge and proceed.");

            // Mark that an obstacle was detected and wait for the user to press X
            bot.setObstacleDetected(true);
            while (bot.isObstacleDetected()) {
                try {
                    Thread.sleep(100); // Wait a bit before checking again
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            // If we can't save the photo, print an error message
            System.out.println("Error saving obstacle photo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // This method handles the end of the program.
    // It saves the log and asks the user if they want to view the log or a tunnel map.
    public void terminate(double totalDistance) {
        clearScreen(); // Clear the screen for the termination message
        System.out.println("\n*********************************************************");
        System.out.println("**********   SWIFTBOT TUNNEL VISION        **************");
        System.out.println("*********************************************************");
        System.out.println("**TASK COMPLETED**");
        System.out.println("*********************************************************");
        System.out.println("**ACTION REQUIRED**");
        System.out.println("*********************************************************");
        System.out.println("* Please select an option:                              *");
        System.out.println("*  - Press [Y] to view detailed log and map             *");
        System.out.println("*  - Press [X] to exit without viewing log              *");
        System.out.println("*  - Invalid buttons (A/B) will prompt an error         *");
        System.out.println("*********************************************************");

        // Turn off all buttons before setting up new ones
        bot.getApi().disableAllButtons();

        // Set up buttons for the user to choose what to do
        bot.getApi().enableButton(Button.Y, () -> {
            synchronized (choiceMade) {
                viewLog = true; // User wants to see the log
                choiceMade.set(true);
            }
        });
        bot.getApi().enableButton(Button.X, () -> {
            synchronized (choiceMade) {
                viewLog = false; // User wants to exit without seeing the log
                choiceMade.set(true);
            }
        });
        bot.getApi().enableButton(Button.A, () -> {
            displayButtonError(); // Show an error if they press A
            synchronized (choiceMade) {
                choiceMade.set(false);
            }
        });
        bot.getApi().enableButton(Button.B, () -> {
            displayButtonError(); // Show an error if they press B
            synchronized (choiceMade) {
                choiceMade.set(false);
            }
        });

        // Wait for the user to make a choice
        while (!choiceMade.get()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        bot.getApi().disableAllButtons(); // Turn off the buttons after the choice

        // Save the log file with all the journey details
        logger.writeLogFile("tunnel_vision_log.txt", totalDistance, System.currentTimeMillis(),
                detector.getTunnelLengths(), detector.getAvgIntensities());

        // If the user chose to view the log, show the detailed log and ask if they want to see the map
        if (viewLog) {
            clearScreen();
            System.out.println("\n*********************************************************");
            System.out.println("*           TASK COMPLETED SUCCESSFULLY                 *");
            System.out.println("*********************************************************");
            System.out.println("**Session Summary**:");
            System.out.printf("  - Total Tunnels: %d\n", detector.getEntryTimes().size());
            System.out.print("  - Tunnel Lengths: [");
            for (int i = 0; i < detector.getTunnelLengths().size(); i++) {
                System.out.printf("%.0f cm%s", detector.getTunnelLengths().get(i),
                        i < detector.getTunnelLengths().size() - 1 ? ", " : "");
            }
            System.out.println("]");
            System.out.printf("  - Total Distance Covered: %.0f cm\n", totalDistance);
            double avgIntensity = calculateAverage(detector.getAvgIntensities());
            System.out.println("Debug - Avg Intensities: " + detector.getAvgIntensities());
            System.out.printf("  - Average Light Intensity: %.0f%%\n", avgIntensity / 255.0 * 100);
            System.out.printf("  - Obstacles Detected: %d\n", logger.getObstaclePhotoPath().isEmpty() ? 0 : 1);
            System.out.printf("  - Execution Time: %s\n", formatTime(System.currentTimeMillis()));
            System.out.println("**Log Information**:");
            System.out.println("  - Log saved at: tunnel_vision_log.txt");
            System.out.println("  - Photos saved at: photos/");
            System.out.println("Press Button 'A' to see a map of the tunnels, or any other button to proceed to exit.");

            // Wait for the user to decide if they want to see the map
            choiceMade.set(false);
            bot.getApi().enableButton(Button.A, () -> {
                synchronized (choiceMade) {
                    mapRequested = true; // User wants to see the map
                    System.out.println("Button A pressed, setting mapRequested to true");
                    choiceMade.set(true);
                }
            });
            bot.getApi().enableButton(Button.Y, () -> {
                synchronized (choiceMade) {
                    System.out.println("Button Y pressed, proceeding to exit");
                    choiceMade.set(true);
                }
            });
            bot.getApi().enableButton(Button.X, () -> {
                synchronized (choiceMade) {
                    System.out.println("Button X pressed, proceeding to exit");
                    choiceMade.set(true);
                }
            });
            bot.getApi().enableButton(Button.B, () -> {
                synchronized (choiceMade) {
                    System.out.println("Button B pressed, proceeding to exit");
                    choiceMade.set(true);
                }
            });

            while (!choiceMade.get()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            bot.getApi().disableAllButtons();

            // If the user wants to see the map, show it and wait for them to press a button to exit
            if (mapRequested) {
                System.out.println("Displaying tunnel mapping...");
                displayTunnelMapping();
                System.out.println("Press any button to proceed to exit.");
                choiceMade.set(false);
                bot.getApi().enableButton(Button.X, () -> {
                    synchronized (choiceMade) {
                        choiceMade.set(true);
                    }
                });
                bot.getApi().enableButton(Button.Y, () -> {
                    synchronized (choiceMade) {
                        choiceMade.set(true);
                    }
                });
                bot.getApi().enableButton(Button.A, () -> {
                    synchronized (choiceMade) {
                        choiceMade.set(true);
                    }
                });
                bot.getApi().enableButton(Button.B, () -> {
                    synchronized (choiceMade) {
                        choiceMade.set(true);
                    }
                });

                while (!choiceMade.get()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                bot.getApi().disableAllButtons();
            }
        } else {
            // If the user didn't want to see the log, just show where the log and photos were saved
            clearScreen();
            System.out.println("\n*********************************************************");
            System.out.println("*           TASK COMPLETED SUCCESSFULLY                 *");
            System.out.println("*********************************************************");
            System.out.println("**Log Information**:");
            System.out.println("  - Log saved at: tunnel_vision_log.txt");
            System.out.println("  - Photos saved at: photos/");
        }

        // Show the final exit screen and wait for the user to press any button to exit
        clearScreen();
        System.out.println("\n*********************************************************");
        System.out.println("**********   SWIFTBOT TUNNEL VISION        **************");
        System.out.println("*********************************************************");
        System.out.println("**PROGRAM COMPLETE**");
        System.out.println("*********************************************************");
        System.out.println("**Log Information**:");
        System.out.println("  - Log saved at: tunnel_vision_log.txt");
        System.out.println("  - Photos saved at: photos/");
        System.out.println("*********************************************************");
        System.out.println("Press any button to exit the program.");
        System.out.println("*********************************************************");

        choiceMade.set(false);
        bot.getApi().enableButton(Button.X, () -> {
            synchronized (choiceMade) {
                System.out.println("Exiting program...");
                choiceMade.set(true);
            }
        });
        bot.getApi().enableButton(Button.Y, () -> {
            synchronized (choiceMade) {
                System.out.println("Exiting program...");
                choiceMade.set(true);
            }
        });
        bot.getApi().enableButton(Button.A, () -> {
            synchronized (choiceMade) {
                System.out.println("Exiting program...");
                choiceMade.set(true);
            }
        });
        bot.getApi().enableButton(Button.B, () -> {
            synchronized (choiceMade) {
                System.out.println("Exiting program...");
                choiceMade.set(true);
            }
        });

        while (!choiceMade.get()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.exit(0); // Exit the program
    }

    // This method shows a map of the tunnels with details like length and intensity.
    private void displayTunnelMapping() {
        clearScreen();
        System.out.println("**************************************************************");
        System.out.println("*                TUNNEL MAPPING                             *");
        System.out.println("**************************************************************");
        System.out.println();
        System.out.println(" **Tunnel Layout**:");
        System.out.print("   [Start]");
        if (detector.getEntryTimes().isEmpty()) {
            // If no tunnels were detected, show a simple layout
            System.out.print(" --- [End]");
            System.out.println();
            System.out.println("No tunnels detected during the journey.");
        } else {
            // Show a layout with all the tunnels
            for (int i = 1; i <= detector.getEntryTimes().size(); i++) {
                System.out.print(" --- [ Tunnel " + i + " ]");
            }
            System.out.println(" --- [End]");
            System.out.println();
            System.out.println(" **Tunnel Details**:");
            for (int i = 0; i < detector.getTunnelLengths().size(); i++) {
                System.out.println("***********************************************************");
                System.out.println("* Tunnel " + (i + 1) + "                                                *");
                System.out.println("***********************************************************");
                System.out.printf("*  Length: %.0f cm                                          *\n", detector.getTunnelLengths().get(i));
                System.out.printf("*  Avg Light Intensity: %.0f%% (%s)                      *\n",
                        detector.getAvgIntensities().get(i) / 255.0 * 100,
                        (detector.getAvgIntensities().get(i) / 255.0 * 100) > 70 ? "Bright" : "Moderate");
                // Check if an obstacle was detected in this tunnel (assuming it is the first tunnel for simplicity)
                System.out.println("*  Obstacles: " + (i == 0 && !logger.getObstaclePhotoPath().isEmpty() ? "Yes" : "None") + "                                        *");
                long travelTimeMs = detector.getExitTimes().get(i) - detector.getEntryTimes().get(i);
                System.out.printf("*  Travel Time: %s                                     *\n", formatTime(travelTimeMs + startTime));
                System.out.println("***********************************************************");
            }
        }
    }

    // This method shows an error message if the user presses an invalid button.
    public void displayButtonError() {
        clearScreen();
        System.out.println("\n*********************************************************");
        System.out.println("**********   SWIFTBOT TUNNEL VISION        **************");
        System.out.println("*********************************************************");
        System.out.println("**Error: incorrect button pressed");
        System.out.println("Valid Buttons are 'X', 'Y', and 'A'");
        System.out.println("**Re-enter a valid Button and continue");
    }

    // This method calculates the average light intensity of a grayscale image.
    // I use it to show the intensity in the UI.
    private double calculateAverageIntensity(BufferedImage img) {
        if (img == null) return 0; // Return 0 if the image is null
        int width = img.getWidth();
        int height = img.getHeight();
        long sum = 0;
        // Add up the intensity of each pixel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = img.getRGB(x, y) & 0xFF;
                sum += pixel;
            }
        }
        return sum / (double) (width * height); // Return the average
    }

    // This method calculates the average of a list of numbers.
    // I use it to find the average light intensity across all tunnels.
    private double calculateAverage(ArrayList<Double> values) {
        if (values.isEmpty()) {
            System.out.println("Average intensities list is empty in UIHandler");
            return 0; // Return 0 if the list is empty
        }
        double sum = 0;
        for (Double val : values) {
            sum += val;
        }
        return sum / values.size();
    }

    // This method formats the time since the program started into MM:SS format.
    // I use it to show the elapsed time in the UI.
    private String formatTime(long milliseconds) {
        long elapsed = milliseconds - startTime;
        long seconds = elapsed / 1000;
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }

    // This method clears the console screen so the UI looks clean.
    private void clearScreen() {
        try {
            System.out.println("Attempting to clear screen...");
            // Check if we're on Windows or another OS to use the right command
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                new ProcessBuilder("clear").inheritIO().start().waitFor();
            }
            System.out.println("Screen cleared.");
        } catch (IOException | InterruptedException e) {
            // If clearing the screen fails, just print some blank lines as a fallback
            System.out.println("Clear screen failed: " + e.getMessage());
            for (int i = 0; i < 50; i++) System.out.println();
        }
    }
}