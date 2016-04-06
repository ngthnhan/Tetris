import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.Scanner;

/**
 * Created by nhan on 6/4/16.
 * This class is a player that will automatically spawn
 * different players that use a policy and evaluate that
 * policy based on the statistic
 *
 * Mean
 * Variance
 */
class Player implements Runnable {
    private double[] weights;
    private String policyFileName;
    private String reportFileName;
    private int score;
    private int gameLimit;

    private double[] getWeights() { return weights; }
    private int getScore() { return score; }

    Player(String policyFile, int gameLimit) {
        this.policyFileName = policyFile;
        this.reportFileName = "report_" + policyFile;
        this.score = 0;
        this.weights = new double[FeatureFunction.NUM_OF_FEATURE];
        this.gameLimit = gameLimit;

        readPolicy();
    }

    private void readPolicy() {
        try (Scanner sc = new Scanner(new FileReader(policyFileName))) {
            for (int i = 0; i < weights.length; i++) {
                weights[i] = sc.nextDouble();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            // If anything wrong happens, this player will not play any game
            gameLimit = 0;
        }
    }

    private static void writeReportHeader(BufferedWriter bw, Player p) throws IOException {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        StringBuilder sb = new StringBuilder();

        sb.append("Result for playing of policy:\n");
        StringBuilder helper = new StringBuilder();
        for (double w: p.getWeights()) {
            helper.append(w).append(" ");
        }

        sb.append(helper.toString().trim()).append('\n');
        sb.append("Created at: ").append(dateFormat.format(new Date())).append('\n');
        sb.append("-----------------------------------\n");

        bw.write(sb.toString());
    }

    private static synchronized void writeToReport(Player p, String fileName) {
        File f = new File(fileName);
        boolean exists = f.exists() && f.isFile();

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f, exists))){
            if (!exists) {
                writeReportHeader(bw, p);
            }

            bw.write(p.getScore());
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void play() {
        State s = new State();
        while(!s.hasLost()) {
            s.makeMove(PlayerSkeleton.pickBestMove(s, weights));
        }

        this.score = s.getRowsCleared();
    }

    @Override
    public void run() {
        int limit = 0;
        boolean infinite = gameLimit < 0;
        while (infinite || limit < gameLimit) {
            limit++;
            play();
            writeToReport(this, reportFileName);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int numOfPlayers = args.length >= 1 && args[0] != null ? Integer.parseInt(args[0]) : 4;
        int limit = args.length >= 2 && args[1] != null ? Integer.parseInt(args[1]) : -1;
        String fileName = args.length >= 3 && args[2] != null ? args[2] : "";

        Thread[] threads = new Thread[numOfPlayers];
        for (int i = 0; i < numOfPlayers; i++) {
            threads[i] = new Thread(new Player(fileName, limit));
            threads[i].start();
        }

        for (Thread t: threads) {
            t.join();
        }
    }
}
