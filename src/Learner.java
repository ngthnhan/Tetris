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
    private State s;
    private State ns;
    private PlayerSkeleton p;
    private final int LOST_REWARD = -1000000;

    private final int K = FeatureFunction.NUM_OF_FEATURE;

    public Learner(int id, int turnLimit) {
        this.id = id;
        this.turnLimit = turnLimit;
        this.weightFile = String.format("weight%d.txt", id);
        weights = new double[FeatureFunction.NUM_OF_FEATURE];
        readWeightsVector();

        this.s = new State();
        this.ns = new NextState(s);
    }

    private void readWeightsVector() {
        try (BufferedReader br = new BufferedReader(new FileReader(weightFile))) {
            Scanner sc = new Scanner(br);
            int i = 0;
            while(sc.hasNextDouble()) {
                weights[i++] = sc.nextDouble();
            }
        } catch (IOException e) {
            e.printStackTrace();
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
            double[][] temp = new double[1][FeatureFunction.NUM_OF_FEATURE];
            nextState.copyState(s);
            nextState.makeMove(action);
            double[][] currentFeatures = matrix.convertToRowVector(FeatureFunction.computeFeaturesVector(nextState));
            for (int nextStatePiece = 0; nextStatePiece < 7; nextStatePiece++)
            {
                nextState.setPiece(nextStatePiece);
                nextAction = pickBestMove(nextState, weights);
                nextNextState.copyState(nextState);
                nextNextState.makeMove(nextAction);
                temp = matrix.convertToRowVector(FeatureFunction.computeFeaturesVector(nextNextState));
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
        return null;
    }

    /**
     * This is mainly the wrapper to continuously iterate through samples and give it
     * to LSTDQ to adjust the weight.
     * The way we do it is by randomly pick a move for a given state to make new sample.
     *
     * @return the adjusted weight after the whole learning process
     */
    private double[] LSPI(int limit) {
        State s = new State();
        for (int i=0;((limit<0)||((i<=limit)))&&(!(s.hasLost()));i++){
            int nextAction = (int)((Math.random())*s.legalMoves().length);
            s.makeMove(nextAction);
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
}
