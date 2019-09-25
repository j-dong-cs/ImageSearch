
/**
 * readImage class
 * @author Jingjing Dong
 * @date
 */
import java.awt.Color;
import java.awt.image.*;
import java.lang.Object.*;
import javax.swing.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import javax.imageio.*;

/**
 * This class reads in images and calculates intensity and color code values for
 * each pixel in each image. Then, it outputs to a text file which containing
 * all values for each image.
 */
public class readImage {
    public final int NUMBER_OF_IMAGES = 100;
    public final int INTENSITY_BINS = 25;
    public final int COLORCODE_BINS = 64;
    
    int imageCount = 1; // keep track of images being read in
    int[] imageSize = new int[NUMBER_OF_IMAGES + 1]; // 1-100
    int intensityMatrix [][] = new int[NUMBER_OF_IMAGES + 1][INTENSITY_BINS];
    int colorCodeMatrix [][] = new int[NUMBER_OF_IMAGES + 1][COLORCODE_BINS];
    
    /**
     * Each image is retrieved from the file. The height and width are found for
     * the image and the getIntensity and getColorCode methods are called.
     * @throws java.io.IOException
     */
    public readImage() throws URISyntaxException, IOException {
        while(imageCount <= NUMBER_OF_IMAGES){
            try {
                // read in image and find the height and width
                URI imageurl = getClass().getResource(imageCount + ".jpg").toURI();
                BufferedImage image = ImageIO.read(new File(imageurl));
                int height = image.getHeight();
                int width = image.getWidth();
                
                getIntensity(image, height, width);
                getColorCode(image, height, width);
                imageSize[imageCount] = height * width;
                imageCount++;
            }
            catch (IOException e) {
                System.out.println("Error occurred when reading the file.");
            }
        }
        writeIntensity();
        writeColorCode();
    }
    
    /**
     * Calculates the intensity of each pixel in an image and count each value in
     * corresponding bin.
     */
    public void getIntensity(BufferedImage image, int height, int width){
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int rgb = image.getRGB(j, i);
                Color c = new Color(rgb);
                int rValue = c.getRed();
                int gValue = c.getGreen();
                int bValue = c.getBlue();
                // I = 0.299R + 0.587G + 0.114B
                double intensity = 0.299*rValue + 0.587*gValue + 0.114*bValue;
                
                // add count in corresponding bin
                int insertIndex = (int) intensity / 10;
                if (intensity >= 250) {
                    intensityMatrix[imageCount][INTENSITY_BINS - 1]++;
                } else {
                    intensityMatrix[imageCount][insertIndex]++;
                }
            }
        }
    }
    
    /**
     * Calculates the color code for each pixel in an image and count each value in
     * corresponding bin.
     */
    public void getColorCode(BufferedImage image, int height, int width){
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int rgb = image.getRGB(j, i);
                Color c = new Color(rgb);
                int rValue = c.getRed();
                int gValue = c.getGreen();
                int bValue = c.getBlue();
                
                // extract the most signigicant first 2 bits of each of the color value
                rValue = ((rValue & 0xFF) >>> 6) << 4;
                gValue = ((gValue & 0xFF) >>> 6) << 2;
                bValue = (bValue & 0xFF) >>> 6;
                
                int colorCode = rValue + gValue + bValue;
                colorCodeMatrix[imageCount][colorCode]++;
            }
        }
    }
    
    /**
     * This method writes the contents of the intensity matrix to a file called
     * colorCode.txt
     */
    public void writeColorCode() throws IOException{
        try {
            FileWriter writer = new FileWriter(new File("colorCode.txt"));
            BufferedWriter ostream = new BufferedWriter(writer);
            for (int i = 1; i < NUMBER_OF_IMAGES + 1; i++) {
                ostream.append(i + "");
                ostream.append(" " + imageSize[i]);
                for (int j = 0; j < COLORCODE_BINS; j++) {
                    ostream.append(" ");
                    ostream.append(colorCodeMatrix[i][j] + "");
                }
                ostream.newLine();
            }
            ostream.close();
        }
        catch (IOException e) {
            System.out.println("Failed to write to a text file.");
        }
    }
    
    /**
     * This method writes the contents of the intensity matrix to a file called
     * intensity.txt
     */
    public void writeIntensity() {
        try {
            FileWriter writer = new FileWriter(new File("intensity.txt"));
            BufferedWriter ostream = new BufferedWriter(writer);
            for (int i = 1; i < NUMBER_OF_IMAGES + 1; i++) {
                ostream.append(i + "");
                ostream.append(" " + imageSize[i]);
                for (int j = 0; j < INTENSITY_BINS; j++) {
                    ostream.append(" ");
                    ostream.append(intensityMatrix[i][j] + "");
                }
                ostream.newLine();
            }
            ostream.close();
        }
        catch (IOException e) {
            System.out.println("Failed to write to a text file.");
        }
    }
}

