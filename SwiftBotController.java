import swiftbot.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

// This class controls the SwiftBot hardware, like moving the bot, using sensors, and setting the underlights.
public class SwiftBotController {
    private final SwiftBotAPI api = new SwiftBotAPI(); // The API to interact with the SwiftBot
    private volatile boolean running = false; // Tracks if the program is running
    private volatile boolean obstacleDetected = false; // Tracks if an obstacle is detected
    private volatile boolean movementActive = false; // Tracks if the bot is moving
    private long movementStartTime; // Time when the bot started moving
    private double accumulatedMoveTime = 0.0; // Total time the bot has been moving
    private static final int[] GREEN_UNDERLIGHTS = {0, 255, 0}; // RGB for green underlights (inside tunnel)
    private static final int[] BLUE_UNDERLIGHTS = {0, 0, 255}; // RGB for blue underlights (outside tunnel)

    // This method sets up the buttons to start or stop the program.
    public void setupButtons() {
        // Press Y to start the program
        api.enableButton(Button.Y, () -> running = true);
        // Press X to stop the program and clear any obstacle detection
        api.enableButton(Button.X, () -> {
            running = false;
            obstacleDetected = false;
            if (movementActive) {
                accumulatedMoveTime += (System.currentTimeMillis() - movementStartTime);
                movementActive = false;
            }
        });
    }

    // This method waits for the user to press Y to start the program.
    public void waitForStart() {
        while (!running) {
            try {
                Thread.sleep(100); // Wait a bit before checking again
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // This method starts the bot moving with the given wheel speeds.
    public void startMove(int leftVelocity, int rightVelocity) {
        api.startMove(leftVelocity, rightVelocity); // Start moving
        movementActive = true; // Mark that the bot is moving
        movementStartTime = System.currentTimeMillis(); // Record the start time
    }

    // This method stops the bot and updates the total movement time.
    public void stopMove() {
        if (movementActive) {
            // Add the time since the last start to the total movement time
            accumulatedMoveTime += (System.currentTimeMillis() - movementStartTime);
            movementActive = false; // Mark that the bot is no longer moving
        }
        api.stopMove(); // Stop the bot
    }

    // This method returns the total time the bot has been moving.
    public double getAccumulatedMoveTime() {
        if (movementActive) {
            // If the bot is still moving, add the current time difference
            return accumulatedMoveTime + (System.currentTimeMillis() - movementStartTime);
        }
        return accumulatedMoveTime;
    }

    // This method takes a grayscale image using the bot's camera.
    public BufferedImage takeGrayscaleStill() throws IOException {
        return api.takeGrayscaleStill(ImageSize.SQUARE_720x720); // Take a 720x720 grayscale image
    }

    // This method uses the ultrasound sensor to measure the distance to the nearest obstacle.
    public double useUltrasound() throws IOException, InterruptedException {
        double distance = api.useUltrasound();
        // If the distance is too far (over 1219 cm), return a really big number
        return distance >= 1219 ? Double.MAX_VALUE : distance;
    }

    // This method sets the underlights to a specific color.
    public void fillUnderlights(int[] rgb) {
        api.fillUnderlights(rgb); // Set the underlights to the given RGB color
    }

    // This method turns off the underlights.
    public void disableUnderlights() {
        api.disableUnderlights(); // Turn off the underlights
    }

    // This method checks if the program is still running.
    public boolean isRunning() {
        return running;
    }

    // This method checks if an obstacle is currently detected.
    public boolean isObstacleDetected() {
        return obstacleDetected;
    }

    // This method sets whether an obstacle is detected or not.
    public void setObstacleDetected(boolean state) {
        obstacleDetected = state;
    }

    // This method checks if the bot is currently moving.
    public boolean isMovementActive() {
        return movementActive;
    }

    // This method updates the underlights based on whether we're in a tunnel.
    public void updateUnderlights(boolean inTunnel) {
        // Set the underlights to green if we're in a tunnel, blue if we're outside
        fillUnderlights(inTunnel ? GREEN_UNDERLIGHTS : BLUE_UNDERLIGHTS);
    }

    // This method stops the bot and shows the final UI when the program ends.
    public void terminate(double totalDistance, UIHandler ui) {
        stopMove(); // Stop the bot
        disableUnderlights(); // Turn off the underlights
        ui.terminate(totalDistance); // Show the termination screen
    }

    // This method returns the SwiftBot API so other classes can use it.
    public SwiftBotAPI getApi() {
        return api;
    }
}