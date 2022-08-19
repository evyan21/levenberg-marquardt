import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.HashMap;
import java.awt.Color;

import org.opensourcephysics.numerics.LevenbergMarquardt;
import org.opensourcephysics.display.Dataset;
import org.opensourcephysics.display.DrawingFrame;
import org.opensourcephysics.display.FunctionDrawer;
import org.opensourcephysics.display.PlottingPanel;

public class Optimizer {
    public static double[] convertDouble(List<Double> list) {
        return list.stream().mapToDouble(i -> i).toArray();
    }

    public static Color generateColor() {
        Random random = new Random();
        final float hue = random.nextFloat();
        final float saturation = 0.9f;//1.0 for brilliant, 0.0 for dull
        final float luminance = 1.0f; //1.0 for brighter, 0.0 for black
        return Color.getHSBColor(hue, saturation, luminance);
    }

    public static int getUserResponse() {
        Scanner userInput = new Scanner(System.in);
        System.out.println("Enter number of parameters (0-3): ");
        int n = 0;
        try {
            n = userInput.nextInt();

            while (n < 0 || n > 3) {
                System.out.println("Invalid number. Retry: ");
                n = userInput.nextInt();
            }

        } catch (Exception e) {
            System.out.println("Integers only.");
        }
        userInput.close();

        return n;
    }

    public static void printData(double[] parameters, int n) {
        System.out.println("L0:\t" + parameters[0]);
        System.out.println("L0T:\t" + parameters[1]);

        if (n >= 1) {
            System.out.println("L1:\t" + parameters[2]);
            System.out.println("L1T:\t" + parameters[3]);
        }

        if (n >= 2) {
            System.out.println("L2:\t" + parameters[4]);
            System.out.println("L2T:\t" + parameters[5]);
        }

        if (n == 3) {
            System.out.println("L3:\t" + parameters[6]);
            System.out.println("L3T:\t" + parameters[7]);
        }
    }

    public static void main(String[] args) throws FileNotFoundException {
        ArrayList<Double> T = new ArrayList<Double>();
        ArrayList<Double> x = new ArrayList<Double>();
        ArrayList<Double> G = new ArrayList<Double>();

        HashMap<Double, Dataset> map = new HashMap<Double, Dataset>();

        // read csv file
        // add data to arraylists
        try (Scanner sc = new Scanner(new File("cusiLIq.csv"))) {

            // ignore first line
            sc.nextLine();

            while (sc.hasNextLine()) {
                String[] values = sc.nextLine().split(",");

                if (values.length == 4) {
                    T.add(Double.parseDouble(values[0]) + 273.15);
                    x.add(Double.parseDouble(values[2]) / 100.0);
                    G.add(Double.parseDouble(values[3]));
                }
            }

            // type of solution
            // 0 <= n <= 3
            int n = getUserResponse();

            // begin optimization
            LevenbergMarquardt optimizer = new LevenbergMarquardt();
            ObjectiveFunction function = new ObjectiveFunction(convertDouble(x), convertDouble(T), convertDouble(G), n);
            
            double[] parameters = new double[2 * (n + 1)];
            optimizer.minimize(function, parameters, 10000, 1e-20);

            System.out.println(optimizer.getIterations());
            printData(parameters, n);

            // initialize hashmap
            for (int i = 0; i < x.size(); i++) {
                Dataset dataset = map.get(T.get(i));
                if (dataset == null) {
                    dataset = new Dataset();
                    dataset.setName(T.get(i).toString());
                    dataset.setMarkerColor(generateColor());
                    dataset.setSorted(true);
                    dataset.setMarkerSize(1);
                    dataset.setMarkerShape(2);

                    map.put(T.get(i), dataset);
                }

                dataset.append(x.get(i), G.get(i));
            }

            PlottingPanel panel = new PlottingPanel("x","y","Test");
            DrawingFrame frame = new DrawingFrame(panel);

            frame.setSize(1000,1000);
            frame.setTitle("Test");

            map.forEach((temp, dataset) -> {
                panel.addDrawable(dataset);
                panel.addDrawable(new FunctionDrawer(new RegressionFunction(parameters, n, temp)));
            });

            panel.repaint();
            panel.render();
            frame.render();
            frame.setVisible(true);
        }
    }
}
