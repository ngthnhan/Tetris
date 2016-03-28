import java.io.*;
import java.util.Scanner;

/**
 * Created by nhan on 28/3/16.
 */
public class Learner implements Runnable {
    private double[] weights;
    private int id;
    private int turnLimit;
    private String weightFile;


    public Learner(int id, int turnLimit) {
        this.id = id;
        this.turnLimit = turnLimit;
        this.weightFile = String.format("weight%d.txt", id);
        weights = new double[FeatureFunction.NUM_OF_FEATURE];
        readWeightsVector();
    }

    private void readWeightsVector() {
        try (BufferedReader br = new BufferedReader(new FileReader(weightFile))) {
            Scanner sc = new Scanner(br);
            int i = 0;
            while(sc.hasNextDouble()) {
                weights[i++] = sc.nextDouble();
            }
        } catch (IOException e) {

        }
    }

    private void writeWeightsVector() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(weightFile))) {
            for (Double b: weights) {
                bw.write(b.toString());
                bw.write('\n');
            }
        } catch (IOException e) {

        }
    }

    @Override
    public void run() {
        // TODO: Learning process
        try {
            int turn = 0;
            while (turn < turnLimit) {

                turn++;
            }

        } finally {
            // Interrupted or finish learning. Writing back weights
            writeWeightsVector();
        }
    }
}
