import com.sun.org.apache.xalan.internal.utils.FeatureManager;

import java.io.*;
import java.util.Random;
import java.util.Scanner;

/**
 * Created by nhan on 28/3/16.
 */
public class Learner implements Runnable {
    private double[] weights;
    private int id;
    private int turnLimit;
    private String weightFile;
    private State s;
    private State ns;
    private PlayerSkeleton p;
    private FeatureFunction ff;
    private final int LOST_REWARD = -1000000;
    private final int INFINITE = -1;
    private final double GAMMA = 0.9;

    private final String LEARNER_DIR = "Learner";

    private final int K = FeatureFunction.NUM_OF_FEATURE;

    private Learner(int id, int turnLimit) {
        this.id = id;
        this.turnLimit = turnLimit;
        this.weightFile = String.format("weight%d.txt", id);
        weights = new double[FeatureFunction.NUM_OF_FEATURE];
        readWeightsVector();

        this.s = new State();
        this.ns = new NextState(s);

        this.p = new PlayerSkeleton();
        this.ff = new FeatureFunction();
    }

    private void readWeightsVector() {
        try (BufferedReader br = new BufferedReader(new FileReader(weightFile))) {
            Scanner sc = new Scanner(br);
            int i = 0;
            while(sc.hasNextDouble()) {
                weights[i++] = sc.nextDouble();
            }
        } catch (IOException e) {
            try {
                // Randomise weights
                Random rdm = new Random();
                for (int i = 0; i < FeatureFunction.NUM_OF_FEATURE; i++) {
                    double w = rdm.nextDouble();
                    weights[i] = rdm.nextBoolean() ? w : -w;
                }

                // Create file if not exists
                File file = new File(weightFile);

                if (!file.exists()) {
                    file.createNewFile();
                }
            } catch (IOException f) {
                f.printStackTrace();
            }
        }
    }

    private void writeWeightsVector() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(weightFile))) {
            for (Double b: weights) {
                bw.write(b.toString());
                bw.write('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Mainly a pseudo function so that it resembles the algorithm given in handout
     * @param s
     * @param action
     * @param ns
     * @return mostly 1.0 / 7 if we assume that the randomness to non-biased
     */
    private double P(State s, int action, State ns) {
        return 0.14285714285; // 1.0 / 7
    }

    /**
     * Reward function.
     * @param s
     * @param action
     * @param ns
     * @return Number of lines cleared. -100000 if the action is a lost move
     */
    private double R(State s, int action, State ns) {
        return ns.hasLost() ? LOST_REWARD : ns.getRowsCleared() - s.getRowsCleared();
    }

    /**
     * This is to learn for every sample (s, a, s'). Adjust the weight vector
     *
     * phi(s,a) : features of state. It is a col vector size K x 1
     * P(s,a,s') : transition model from s, a to s'
     * phi(s', pi(s')) : features of the state after action is taken and pick the best based
     *                   on the current policy (1 step look ahead)
     * @return the adjusted weight vector given the sample
     */
    private double[] LSTDQ(NextState s) {
        double[][] A = new double[K][K]; // Refer to algo on page 19
        double[][] b = new double[K][1];
        double[][] phi = new double[K][1];
        double[][] tempA;
        NextState nextState = new NextState();
        NextState nextNextState = new NextState();
        int nextAction;

        for (int action = 0; action < s.legalMoves().length; action++) {
            // A = A + phi(s,a)*transpose(phi(s,a) - GAMMA * sum(P(s,a,s')*phi(s', pi(s')))
            double[][] summation = new double[1][FeatureFunction.NUM_OF_FEATURE];
            double computeSumForB=0;
            double[][] temp;
            nextState.copyState(s);
            nextState.makeMove(action);
            double[][] currentFeatures = matrix.convertToRowVector(ff.computeFeaturesVector(nextState));
            for (int nextStatePiece = 0; nextStatePiece < 7; nextStatePiece++)
            {
                nextState.setNextPiece(nextStatePiece);
                nextAction = PlayerSkeleton.pickBestMove(nextState, weights);
                nextNextState.copyState(nextState);
                nextNextState.makeMove(nextAction);
                temp = matrix.convertToRowVector(ff.computeFeaturesVector(nextNextState));
                summation = matrix.matrixAdd(summation, matrix.multiplyByConstant(temp, GAMMA*P(s, action, nextState)));
                computeSumForB += P(s, action, nextState)*R(s, action, nextState);
            }
            summation = matrix.matrixSub(currentFeatures, summation);
            tempA = matrix.matrixMultplx(matrix.transpose(currentFeatures), summation);
            A = matrix.matrixAdd(A, tempA);
            b = matrix.matrixAdd(b, matrix.multiplyByConstant(matrix.transpose(currentFeatures), computeSumForB));
        }
        tempA = matrix.matrixMultplx(matrix.matrixInverse(A),b);
        weights = matrix.convertToArray(tempA);
        return weights;
    }

    /**
     * This is mainly the wrapper to continuously iterate through samples and give it
     * to LSTDQ to adjust the weight.
     * The way we do it is by randomly pick a move for a given state to make new sample.
     *
     * @return the adjusted weight after the whole learning process
     */
    private double[] LSPI(int limit) {
        NextState s = new NextState(new State());
        for (int i=0;((limit<0)||((i<=limit)))&&(!(s.hasLost()));i++){
            int nextAction = (int)((Math.random())*s.legalMoves().length);
            s.makeMoveWithRandomNext(nextAction);
            weights = LSTDQ(s);
        }
        return weights;
    }


    @Override
    public void run() {
        // TODO: Learning process
        try {
            int turn = 0;
            boolean infinite = turnLimit < 0;
            while (infinite || turn < turnLimit) {

                turn++;
            }

        } finally {
            // Interrupted or finish learning. Writing back weights
            writeWeightsVector();
        }
    }

    /**
     * Map reducer function to consolidate learning from the learners.
     * For now it just finds the mean of all the weights.
     *
     * Future change: Make it a stochastic gradient ascent algorithm
     * Get value of each state using player to critic.
     */
    public static void consolidateLearning() {
        File dir = new File(LEARNER_DIR);
        double[] finalWeights = new double[K];

        int count = 0;
        try {
            for (File w: dir.listFiles()) {
                Scanner sc = new Scanner(w);
                count++;
                for (int i = 0; i < K; i++) {
                    finalWeights[i] += sc.nextDouble();
                }
            }
        } catch (FileNotFoundException|NullPointerException e) {
            e.printStackTrace();
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dir.getAbsolutePath() + "final_weights.txt"))){
            StringBuilder sb = new StringBuilder();
            for (double w: finalWeights) {
                sb.append(w).append('\n');
            }

            bw.write(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        int numOfLearners = args.length >= 1 && args[0] != null ? Integer.parseInt(args[0]) : 4;
        int limit = args.length >= 2 && args[1] != null ? Integer.parseInt(args[1]) : -1;

        for (int i = 0; i < numOfLearners; i++) {
            new Thread(new Learner(i, limit)).start();
        }
    }
}
