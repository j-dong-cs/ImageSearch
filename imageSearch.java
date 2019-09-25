
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

/**
 * This class provides a GUI for user to browse image database and select the
 * query image. Then, user is able to choose from intensity, color code, and
 * intensity plus color code method to retrieve similar images. The retrieved
 * results are display as most relevant to least relevant from left to right
 * and then top to bottom. After selecting combination of intensity and color
 * code, user is able to add relevant feedback on retrieved images to improve
 * retrieve results. The relevant feedback process can continue as several
 * iteration as user wants.
 *
 * @author Jingjing Dong
 * @date 07/23/2014
 */
public class imageSearch extends JFrame {
    // constants
    private static final int BORDER = 12;  // Window border in pixels.
    private static final int GAP    = 5;   // Default gap btwn components.
    private static final int NUMBER_OF_IMAGES = 100;
    private static final int INTENSITY_BIN = 25;
    private static final int COLOR_CODE_BIN = 64;
    private static final int RESULT_IMAGE_HEIGHT = 60;
    private static final int RESULT_IMAGE_WIDTH = 200;
    
    // GUI elements
    JPanel mainPanel;
    JPanel bottomPanel;
    JPanel upperPanel;
    JPanel imagePanel;
    JPanel buttonsPanel;
    
    JLabel imageLabel;
    JButton intensity;
    JButton colorCode;
    JButton both;
    JCheckBox relevance;
    JButton previous;
    JButton next;
    JButton clear;
    JButton [] imageButton;
    JCheckBox [] relevant;
    
    // 2-D array to store intensity, colorCode and intensity plus colorCode featuers
    private double [][] intensityMatrix;
    private double [][] colorCodeMatrix;
    private double [][] intensityColorCodeMatrix;
    private int [] buttonOrder; //creates an array to keep up with the image order
    
    int picNo;
    int imageCount; //keeps up with the number of images displayed since the first page.
    int pageNo;
    int relevantCount;
    
    /**
     * Constructs imageSearch object which contains GUI elements.
     */
    public imageSearch() {
        // initialize GUI elements
        mainPanel = new JPanel(new GridLayout(2,1));
        bottomPanel = new JPanel(new GridBagLayout());
        upperPanel = new JPanel(new GridLayout(1,2));
        imagePanel = new JPanel(new GridLayout(1,1));
        buttonsPanel = new JPanel(new GridLayout(4,2));
        
        imageLabel = new JLabel("image", JLabel.CENTER);
        intensity = new JButton("Intensity");
        colorCode = new JButton("Color Code");
        both = new JButton("Intensity + ColorCode");
        relevance = new JCheckBox("Relevance Feedback", false);
        previous = new JButton("Prev");
        next = new JButton("Next");
        clear = new JButton("Clear");
        imageButton = new JButton[NUMBER_OF_IMAGES];
        relevant = new JCheckBox[NUMBER_OF_IMAGES];
        
        // initialize variables
        intensityMatrix = new double [NUMBER_OF_IMAGES + 1][INTENSITY_BIN];
        colorCodeMatrix = new double [NUMBER_OF_IMAGES + 1][COLOR_CODE_BIN];
        intensityColorCodeMatrix = new double [NUMBER_OF_IMAGES + 1][INTENSITY_BIN + COLOR_CODE_BIN];
        buttonOrder = new int [NUMBER_OF_IMAGES];
        picNo = 0;
        imageCount = 0;
        pageNo = 0;
        relevantCount = 0;
        
        // initialize image buttons, relevant check boxes, and button order.
        for (int i = 0; i < NUMBER_OF_IMAGES; i++) {
            ImageIcon icon = new ImageIcon(getClass().getResource((i+1) + ".jpg"));
            ImageIcon newIcon = new ImageIcon(icon.getImage().getScaledInstance(
                                                                                RESULT_IMAGE_WIDTH, RESULT_IMAGE_HEIGHT, java.awt.Image.SCALE_SMOOTH));
            imageButton[i] = new JButton(newIcon);
            imageButton[i].addActionListener(new IconButtonHandler(i, icon));
            relevant[i] = new JCheckBox("relevant");
            relevant[i].setPreferredSize(new Dimension(RESULT_IMAGE_WIDTH, 15));
            relevant[i].addItemListener(new relevantHandler());
            buttonOrder[i] = i;
        }
        
        // add Listners for buttons and check boxes
        intensity.addActionListener(new intensityHandler());
        colorCode.addActionListener(new colorCodeHandler());
        both.addActionListener(new bothHandler());
        relevance.addItemListener(new relevanceHandler());
        previous.addActionListener(new previousHandler());
        next.addActionListener(new nextHandler());
        clear.addActionListener(new clearHandler());
        
        // set main content pane and display all components on screen
        super.setContentPane(createContentPane());
        super.pack();
        super.setTitle("imageSearch");
        super.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        super.setLocationRelativeTo(null);  // center window
        
        // read in intensity and color code data
        readIntensityFile();
        readColorCodeFile();
    }
    
    /**
     * Constructs GUI components and adds all to mainPanel
     * @return JPanel mainPanel the main panel after being built
     */
    private JPanel createContentPane() {
        // build upper panel
        imagePanel.setPreferredSize(new Dimension(600, 200));
        imageLabel.setAlignmentX(SwingConstants.CENTER);
        imageLabel.setAlignmentY(SwingConstants.CENTER);
        imagePanel.add(imageLabel);
        buttonsPanel.setPreferredSize(new Dimension(600, 200));
        buttonsPanel.add(intensity);
        buttonsPanel.add(colorCode);
        buttonsPanel.add(both);
        buttonsPanel.add(relevance);
        buttonsPanel.add(previous);
        buttonsPanel.add(next);
        buttonsPanel.add(clear);
        upperPanel.add(imagePanel);
        upperPanel.add(buttonsPanel);
        
        // build bottem panel
        bottomPanel.setPreferredSize(new Dimension(1200, 500));
        addImagesOnly(0); // add images for first page when opens the application
        
        // build main panel
        mainPanel.add(upperPanel);
        mainPanel.add(bottomPanel);
        
        return mainPanel;
    }
    
    /**
     * This class implements an ItemListner for relevance check box.
     * When relevance check box is selected, the relevant check box for each
     * image appears on the bottom of each image. When the box is deselected,
     * all the relevant check boxes are gone. Image order remains the same.
     */
    private class relevanceHandler implements ItemListener{
        @Override
        public void itemStateChanged(ItemEvent e) {
            int state = e.getStateChange();
            if (state == ItemEvent.SELECTED) {
                relevance.setSelected(true);
                addRelevant(pageNo);
            } else {
                relevance.setSelected(false);
                addImagesOnly(pageNo);
            }
            imageCount-=20; // reduce image count after rebulid the same page
            bottomPanel.revalidate();
            bottomPanel.repaint();
        }
    }
    
    /**
     * This class implements an ItemListener for relevant check box. When relevant
     * check box under a image is selected, the corresponding image is added as
     * an relevant image to query image. When it is deselected, the corresponding
     * image is not a relevant image to query image.
     */
    private class relevantHandler implements ItemListener{
        @Override
        public void itemStateChanged(ItemEvent e) {
            int state = e.getStateChange();
            JCheckBox source = (JCheckBox) e.getItemSelectable();
            if (state == ItemEvent.SELECTED) { // add as a relevant result
                source.setSelected(true);
            } else {
                source.setSelected(false);
            }
        }
    }
    
    /**
     * This class implements an ActionListener for each iconButton.
     * When an icon button is clicked, the image on the
     * the button is added to the photographLabel and the picNo is set to the
     * image number selected and being displayed.
     */
    private class IconButtonHandler implements ActionListener{
        int pNo = 1;
        ImageIcon iconUsed;
        
        IconButtonHandler(int i, ImageIcon j){
            pNo = i;
            iconUsed = j;  //sets the icon to the one used in the button
        }
        
        public void actionPerformed(ActionEvent e){
            imageLabel.setPreferredSize(new Dimension(600,300));
            imageLabel.setIcon(iconUsed);
            imageLabel.setText("Image " + (pNo+1));
            picNo = pNo;
        }
    }
    
    /**
     * Goes to the previous page when previous page button is clicked.
     * Image display order remains the same.
     */
    private class previousHandler implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int startImage = imageCount - 40;
            if(startImage >= 0){
                pageNo--;
                if (!relevance.isSelected()) {
                    addImagesOnly(pageNo);
                } else {
                    addRelevant(pageNo);
                }
                bottomPanel.revalidate();
                bottomPanel.repaint();
                imageCount-=40;
            }
        }
    }
    
    /**
     * Goes to the next page when next page button is clicked.
     * Image display order remains the same.
     */
    private class nextHandler implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int endImage = imageCount + 20;
            if(endImage <= NUMBER_OF_IMAGES){
                pageNo++;
                if (!relevance.isSelected()) {
                    addImagesOnly(pageNo);
                } else {
                    addRelevant(pageNo);
                }
                bottomPanel.revalidate();
                bottomPanel.repaint();
            }
        }
    }
    
    /**
     * This class implements an ActionListener when the user selects the
     * intensityHandler button.  The image number that the user would like to
     * find similar images for is stored in the variable pic.  pic takes the
     * image number associated with the image selected.
     * The selected image's intensity bin values are compared to all the other image's
     * intensity bin values and a score is determined for how well the images compare.
     * The images are then arranged from most similar to the least.
     */
    private class intensityHandler implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            double [] distance = new double [NUMBER_OF_IMAGES];
            double d = 0.0;
            int pic = picNo;
            
            // calculate distance
            for (int i = 0; i < NUMBER_OF_IMAGES; i++) {
                for (int j = 0; j < INTENSITY_BIN; j++) {
                    d += Math.abs(intensityMatrix[pic+1][j] -
                                  intensityMatrix[i+1][j]);
                }
                distance[i] = d;
                d = 0.0;
            }
            
            // re-order buttons
            double[] sortedD = Arrays.copyOf(distance, NUMBER_OF_IMAGES);
            Arrays.sort(sortedD);
            for (int i = 0; i < NUMBER_OF_IMAGES; i++) {
                for (int j = 0; j < NUMBER_OF_IMAGES; j++) {
                    if (sortedD[i] == distance[j]) {
                        buttonOrder[i] = j;
                    }
                }
            }
            imageCount = 0;
            pageNo = 0;
            if (!relevance.isSelected()) {
                addImagesOnly(pageNo);
            } else {
                addRelevant(pageNo);
            }
            bottomPanel.revalidate();
            bottomPanel.repaint();
        }
    }
    
    /**
     * This class implements an ActionListener when the user selects the colorCode button.
     * The image number that the user would like to find similar images for is
     * stored in the variable pic. The selected image's intensity bin values are
     * compared to all the other image's intensity bin values and a score is
     * determined for how well the images compare. The images are then arranged
     * from most similar to the least.
     */
    private class colorCodeHandler implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            double [] distance = new double [NUMBER_OF_IMAGES];
            double d = 0.0;
            int pic = picNo;
            
            // calculate distance
            for (int i = 0; i < NUMBER_OF_IMAGES; i++) {
                for (int j = 0; j < COLOR_CODE_BIN; j++) {
                    d += Math.abs(colorCodeMatrix[pic+1][j] - colorCodeMatrix[i+1][j]);
                }
                distance[i] = d;
                d = 0.0;
            }
            
            // re-order buttons
            double[] sortedD = Arrays.copyOf(distance, NUMBER_OF_IMAGES);
            Arrays.sort(sortedD);
            for (int i = 0; i < NUMBER_OF_IMAGES; i++) {
                for (int j = 0; j < NUMBER_OF_IMAGES; j++) {
                    if (sortedD[i] == distance[j]) {
                        buttonOrder[i] = j;
                    }
                }
            }
            imageCount = 0;
            pageNo = 0;
            if (!relevance.isSelected()) {
                addImagesOnly(pageNo);
            } else {
                addRelevant(pageNo);
            }
            bottomPanel.revalidate();
            bottomPanel.repaint();
        }
    }
    
    /**
     * This class implements an AcitonListener when user selects the intensity+colorCode
     * button. The image number that the user would like to find similar images for is
     * stored in the variable pic. pic takes the image number associated with the
     * image selected. The selected image's bin values are compared to all the
     * other images' bin values and a score is determined for how well the images
     * compare. The images are then arranged from most similar to the least.
     *
     */
    private class bothHandler implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            normalizeFeatures();
            
            double [] distance = new double [NUMBER_OF_IMAGES];
            double d = 0.0;
            int pic = picNo;
            
            if (relevantCount == 0) { // use equal weight
                // calculates distance matrix
                double initialWeight = 1.0/(COLOR_CODE_BIN + INTENSITY_BIN);
                for (int i = 0; i < NUMBER_OF_IMAGES; i++) {
                    for (int j = 0; j < COLOR_CODE_BIN + INTENSITY_BIN; j++) {
                        d += initialWeight*Math.abs(intensityColorCodeMatrix[pic+1][j]
                                                    - intensityColorCodeMatrix[i+1][j]);
                    }
                    distance[i] = d;
                    d = 0.0;
                }
                relevantCount++;
            } else {
                // update relevant images weight
                Map<Integer, LinkedList<Double>> map = new TreeMap<Integer, LinkedList<Double>>();
                double[] sum = new double[COLOR_CODE_BIN + INTENSITY_BIN];
                int relCount = 0;
                // map col num with feature vector for relevant images
                for (int i = 0; i < NUMBER_OF_IMAGES; i++) {
                    if (relevant[i].isSelected()) {
                        relCount++;
                        for (int col = 0; col < COLOR_CODE_BIN + INTENSITY_BIN; col++) {
                            if (map.get(col) == null) {
                                map.put(col, new LinkedList<Double>());
                            }
                            map.get(col).add(intensityColorCodeMatrix[i+1][col]);
                            sum[col] += intensityColorCodeMatrix[i+1][col];
                        }
                    }
                }
                
                // compute standard deviation for each feature vector between relevant images
                double[] std = new double[COLOR_CODE_BIN + INTENSITY_BIN];
                for (int key: map.keySet()) {
                    for (int i = 0; i < map.get(key).size(); i++) {
                        std[key] += Math.pow(map.get(key).get(i) - sum[key]/relCount, 2);
                    }
                    std[key] = Math.sqrt(std[key]/(relCount-1));
                }
                
                // find the min non-zero std value
                double minSTD = Double.MAX_VALUE;
                for (double value: std) {
                    minSTD = (value == 0) ? minSTD : Math.min(minSTD, value);
                }
                
                double[] updatedWeight = new double[COLOR_CODE_BIN + INTENSITY_BIN];
                double sumOfWeight = 0.0;
                for (int i = 0; i < COLOR_CODE_BIN + INTENSITY_BIN; i++) {
                    if (std[i] == 0) {
                        if (sum[i] / relCount == 0) {
                            updatedWeight[i] = 0.0;
                        } else {
                            std[i] = 0.5 * minSTD;
                            updatedWeight[i] = 1/std[i];
                        }
                    } else {
                        updatedWeight[i] = 1/std[i];
                    }
                    sumOfWeight+=updatedWeight[i];
                }
                
                for (int i = 0; i < COLOR_CODE_BIN + INTENSITY_BIN; i++) {
                    updatedWeight[i] = updatedWeight[i]/sumOfWeight;
                }
                
                for (int i = 0; i < NUMBER_OF_IMAGES; i++) {
                    for (int j = 0; j < COLOR_CODE_BIN + INTENSITY_BIN; j++) {
                        d += updatedWeight[j]*Math.abs(intensityColorCodeMatrix[pic+1][j]
                                                       - intensityColorCodeMatrix[i+1][j]);
                    }
                    distance[i] = d;
                    d = 0.0;
                }
            }
            
            // rank results
            double[] sortedD = Arrays.copyOf(distance, NUMBER_OF_IMAGES);
            Arrays.sort(sortedD);
            for (int i = 0; i < NUMBER_OF_IMAGES; i++) {
                for (int j = 0; j < NUMBER_OF_IMAGES; j++) {
                    if (sortedD[i] == distance[j]) {
                        buttonOrder[i] = j;
                    }
                }
            }
            imageCount = 0;
            pageNo = 0;
            if (!relevance.isSelected()) {
                addImagesOnly(pageNo);
            } else {
                addRelevant(pageNo);
            }
            bottomPanel.revalidate();
            bottomPanel.repaint();
        }
    }
    
    /**
     * Normalize intensity+colorCode feature values using Gaussian Normalization.
     */
    private void normalizeFeatures() {
        // calculate mean & std of features
        double[] meanOfFeatures = new double[INTENSITY_BIN+COLOR_CODE_BIN];
        double[] stdOfFeatures = new double[INTENSITY_BIN+COLOR_CODE_BIN];
        for (int col = 0; col < INTENSITY_BIN+COLOR_CODE_BIN; col++) {
            for (int row = 1; row <= NUMBER_OF_IMAGES; row++) {
                meanOfFeatures[col] += intensityColorCodeMatrix[row][col];
            }
            meanOfFeatures[col] = meanOfFeatures[col]/NUMBER_OF_IMAGES;
            for (int row = 1; row <= NUMBER_OF_IMAGES; row++) {
                stdOfFeatures[col] += Math.pow(intensityColorCodeMatrix[row][col] - meanOfFeatures[col], 2);
            }
            stdOfFeatures[col] = Math.sqrt(stdOfFeatures[col] / (NUMBER_OF_IMAGES - 1));
        }
        
        // calculate Gaussian normalized features
        for (int row = 1; row <= NUMBER_OF_IMAGES; row++) {
            for (int col = 0; col < INTENSITY_BIN+COLOR_CODE_BIN; col++) {
                if (stdOfFeatures[col] != 0) {
                    intensityColorCodeMatrix[row][col] =
                    (intensityColorCodeMatrix[row][col] -
                     meanOfFeatures[col])/stdOfFeatures[col];
                }
            }
        }
    }
    
    /**
     * This class implements ActionListener which the clear button is selected
     * all the relevant check box are deselected.
     */
    private class clearHandler implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            for (int i = 0; i < NUMBER_OF_IMAGES; i++) {
                relevant[i].setSelected(false);
            }
            relevantCount = 0;
        }
    }
    
    /**
     * Adds image buttons to bottom part of panel
     * @param startPage the starting number of image to be added
     */
    public void addImagesOnly(int startPage) {
        bottomPanel.removeAll();
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10,5,0,0);
        int startImage = startPage*20;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 5; j++) {
                c.gridx = j;
                c.gridy = i;
                imageButton[buttonOrder[startImage]].setPreferredSize(new
                                                                      Dimension(RESULT_IMAGE_WIDTH,RESULT_IMAGE_HEIGHT));
                bottomPanel.add(imageButton[buttonOrder[startImage]], c);
                startImage++;
            }
        }
        imageCount+=20;
    }
    
    /**
     * Adds image buttons and relevant check boxes to bottom part of panel
     * @param startPage the starting number of image to be added
     */
    public void addRelevant(int startPage) {
        bottomPanel.removeAll();
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5,5,0,0);
        int startImage = startPage*20;
        for (int dy = 0; dy < 8; dy+=2) {
            c.gridy = dy;
            for (int i = 0; i < 5; i++) {
                c.gridx = i;
                imageButton[buttonOrder[startImage]].setPreferredSize(new
                                                                      Dimension(RESULT_IMAGE_WIDTH,RESULT_IMAGE_HEIGHT));
                bottomPanel.add(imageButton[buttonOrder[startImage]], c);
                startImage++;
            }
            startImage-=5;
            c.gridy = dy + 1;
            for (int i = 0; i < 5; i++) {
                c.gridx = i;
                bottomPanel.add(relevant[buttonOrder[startImage]], c);
                startImage++;
            }
        }
        imageCount+=20;
    }
    
    /**
     * This method opens the intensity text file containing the intensity matrix
     * with the histogram bin values for each image. The contents of the matrix
     * are processed and stored in a two dimensional array called intensityMatrix.
     */
    public void readIntensityFile() {
        StringTokenizer token;
        Scanner read;
        String line = "";
        
        try{
            read = new Scanner(new File ("intensity.txt"));
            while (read.hasNext()) {
                line = read.nextLine();
                token = new StringTokenizer(line);
                int imageNo = Integer.parseInt(token.nextToken());
                int imageSize = Integer.parseInt(token.nextToken());
                
                int index = 0;
                while (token.hasMoreTokens()) {
                    int count = Integer.parseInt(token.nextToken());
                    double temp = (double) count / imageSize;
                    intensityMatrix[imageNo][index] = temp;
                    intensityColorCodeMatrix[imageNo][index] = temp;
                    index++;
                }
                index = 0;
            }
        } catch(FileNotFoundException EE){
            System.out.println("The file intensity.txt does not exist");
        }
    }
    
    /**
     * This method opens the color code text file containing the color code
     * matrix with the histogram bin values for each image. The contents of the
     * matrix are processed and stored in a two dimensional array called
     * colorCodeMatrix.
     */
    private void readColorCodeFile(){
        StringTokenizer token;
        Scanner read;
        String line = "";
        
        try{
            read = new Scanner(new File ("colorCode.txt"));
            while (read.hasNext()) {
                line = read.nextLine();
                token = new StringTokenizer(line);
                int imageNo = Integer.parseInt(token.nextToken());
                int imageSize = Integer.parseInt(token.nextToken());
                
                int index = 0;
                while (token.hasMoreTokens()) {
                    int count = Integer.parseInt(token.nextToken());
                    double temp = (double) count / imageSize;
                    colorCodeMatrix[imageNo][index] = temp;
                    intensityColorCodeMatrix[imageNo][index + INTENSITY_BIN] = temp;
                    index++;
                }
                index = 0;
            }
        }
        catch(FileNotFoundException EE){
            System.out.println("The file intensity.txt does not exist");
        }
    }
    
    /**
     * Run GUI
     * @param args
     */
    public static void main(String[] args) {
        // start GUI
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    readImage a = new readImage();
                } catch (URISyntaxException ex) {
                    Logger.getLogger(imageSearch.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(imageSearch.class.getName()).log(Level.SEVERE, null, ex);
                }
                imageSearch app = new imageSearch();
                app.setVisible(true);
            }
        });
    }
}

