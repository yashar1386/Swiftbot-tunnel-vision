import swiftbot.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;

// This class handles detecting tunnels and calculating their lengths.
// It uses the bot's camera to measure light intensity and tracks when we enter and exit tunnels.
public class TunnelDetector {
    private static final double BOT_SPEED = 12.0; // Bot speed in cm/s, used to calculate tunnel length
    private final ArrayList<Long> entryTimes = new ArrayList<>(); // Times when we enter tunnels
    private final ArrayList<Long> exitTimes = new ArrayList<>(); // Times when we exit tunnels
    private final ArrayList<Double> tunnelLengths = new ArrayList<>(); // Lengths of each tunnel
    private final ArrayList<Double> avgIntensities = new ArrayList<>(); // Average intensity for each tunnel
    private final ArrayList<Double> intensitiesInTunnel = new ArrayList<>(); // Intensities while in a tunnel
    private final SwiftBotController bot; // To control the bot
    private final DataLogger logger; // To log data

    // Constructor to set up the TunnelDetector with the bot and logger.
    public TunnelDetector(SwiftBotController bot, DataLogger logger) {
        this.bot = bot;
        this.logger = logger;
    }

    // This method calculates the average light intensity of a grayscale image.
    // I use it to decide if we're entering or exiting a tunnel.
    public double calculateAverageIntensity(BufferedImage img) {
        if (img == null) {
            System.out.println("Image is null in calculateAverageIntensity");
            return 0; // Return 0 if the image is null
        }
        int width = img.getWidth();
        int height = img.getHeight();
        long sum = 0;
        // Add up the intensity of each pixel in the image
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = img.getRGB(x, y) & 0xFF; // Get the grayscale value
                sum += pixel;
            }
        }
        double average = sum / (double) (width * height);
        System.out.println("Calculated intensity: " + average);
        return average;
    }

    // This method uses the ultrasound sensor to measure the distance to the nearest obstacle.
    public double checkUltrasound() throws IOException, InterruptedException {
        return bot.useUltrasound(); // Return the distance in cm
    }

    // This method calculates the total distance the bot has traveled.
    // It uses the time the bot has been moving and the speed (12 cm/s).
    public double calculateTotalDistance() {
        double totalMoveTimeMs = bot.getAccumulatedMoveTime();
        return (totalMoveTimeMs / 1000.0) * BOT_SPEED;
    }

    // This method records when we enter a tunnel.
    // It saves the entry time and the initial light intensity.
    public void startTunnel(long time, double intensity) {
        entryTimes.add(time); // Save the time we entered
        intensitiesInTunnel.clear(); // Clear any old intensity readings
        intensitiesInTunnel.add(intensity); // Add the first intensity reading
        System.out.println("Started tunnel, initial intensity: " + intensity);
    }

    // This method records when we exit a tunnel and calculates its length.
    // It uses the time difference and the bot's speed to find the length.
    public double endTunnel(long time) {
        exitTimes.add(time); // Save the time we exited
        // Calculate the length using the time difference and speed
        double length = (time - entryTimes.get(entryTimes.size() - 1)) / 1000.0 * BOT_SPEED;
        // Calculate the average intensity for this tunnel
        double avgIntensity = calculateAverage(intensitiesInTunnel);
        avgIntensities.add(avgIntensity);
        System.out.println("Ended tunnel, average intensity: " + avgIntensity + ", intensities recorded: " + intensitiesInTunnel);
        return length;
    }

    // This method adds a light intensity reading while we're inside a tunnel.
    public void addIntensity(double intensity) {
        intensitiesInTunnel.add(intensity); // Add the intensity to the list
        System.out.println("Added intensity in tunnel: " + intensity);
    }

    // This method saves the length of a tunnel after we exit it.
    public void logTunnelData(double length) {
        tunnelLengths.add(length); // Add the length to the list
    }

    // This method returns the list of times we entered tunnels.
    public ArrayList<Long> getEntryTimes() {
        return entryTimes;
    }

    // This method returns the list of times we exited tunnels.
    public ArrayList<Long> getExitTimes() {
        return exitTimes;
    }

    // This method returns the list of intensity readings while in the current tunnel.
    public ArrayList<Double> getIntensities() {
        return intensitiesInTunnel;
    }

    // This method returns the list of tunnel lengths.
    public ArrayList<Double> getTunnelLengths() {
        return tunnelLengths;
    }

    // This method returns the list of average intensities for each tunnel.
    public ArrayList<Double> getAvgIntensities() {
        return avgIntensities;
    }

    // This method calculates the average of a list of numbers.
    // I use it to find the average light intensity for a tunnel.
    private double calculateAverage(ArrayList<Double> values) {
        if (values.isEmpty()) {
            System.out.println("Intensity list is empty in calculateAverage");
            return 0; // Return 0 if the list is empty
        }
        double sum = 0;
        for (Double val : values) {
            sum += val;
        }
        return sum / values.size();
    }
}