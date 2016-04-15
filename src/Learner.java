import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by nhan on 28/3/16.
 */
public class Learner implements Runnable {
    private double[] weights;
    private int id;
    private String weightFileName;
    private File weightFile;

    private NextState ns;
    private NextState nns;

    private FeatureFunction ff;
    private final int LOST_REWARD = -1000000;
    private final int INFINITE = -1;
    private final double GAMMA = 0.9;
    private final double EPSILON = 0.0005;

    private static Random rand;
    private static ArrayList<String> samplesSource;

    public static final String LEARNER_DIR = "Learner";

    private static final int K = FeatureFunction.NUM_OF_FEATURE;

    private Learner(int id) {
        this.id = id;
        this.weightFileName = String.format("weight%d.txt", id);

        if (samplesSource == null) samplesSource = new ArrayList<String>();
        if (rand == null) {
            rand = new Random();
            rand.setSeed(System.currentTimeMillis());
        }


        weightFile = new File(LEARNER_DIR, weightFileName);
        weights = new double[FeatureFunction.NUM_OF_FEATURE];
        readWeightsVector();

        this.ns = new NextState();
        this.nns = new NextState();

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


                if (!weightFile.exists()) {
                    weightFile.createNewFile();
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
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static synchronized  void readSampleSource() {
        if (samplesSource != null && samplesSource.size() > 0) return;
        try (BufferedReader br = new BufferedReader(new FileReader("states.txt"))) {
            Scanner sc = new Scanner(br);
            int i = 0;
            while(sc.hasNext()) {
                samplesSource.add(sc.nextLine());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Done reading in states");
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
        return ns.hasLost() ? LOST_REWARD : (ns.getRowsCleared() - s.getRowsCleared());
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
        double[][] tempA;

        int nextAction;

        for (int action = 0; action < s.legalMoves().length; action++) {
            // A = A + phi(s,a)*transpose(phi(s,a) - GAMMA * sum(P(s,a,s')*phi(s', pi(s')))
            double[][] summation = new double[1][FeatureFunction.NUM_OF_FEATURE];
            double computeSumForB=0;
            double[][] temp;
            ns.copyState(s);
            ns.makeMove(action);
            double[][] currentFeatures = matrix.convertToRowVector(ff.computeFeaturesVector(ns));
            for (int nextStatePiece = 0; nextStatePiece < 7; nextStatePiece++)
            {
                ns.setNextPiece(nextStatePiece);
                nextAction = PlayerSkeleton.pickBestMove(ns, weights);
                nns.copyState(ns);
                nns.makeMove(nextAction);
                temp = matrix.convertToRowVector(ff.computeFeaturesVector(nns));
                summation = matrix.matrixAdd(summation, matrix.multiplyByConstant(temp, GAMMA*P(ns, action, nns)));
                computeSumForB += P(ns, action, nns)*R(ns, action, nns);
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
     * This is to learn for every sample (s, a, s'). Adjust the weight vector
     *
     * phi(s,a) : features of state. It is a col vector size K x 1
     * P(s,a,s') : transition model from s, a to s'
     * phi(s', pi(s')) : features of the state after action is taken and pick the best based
     *                   on the current policy (1 step look ahead)
     * @return the adjusted weight vector given the sample
     */
    private double[] LSTDQ_OPT(int limit) {
        double[][] B = new double[K][K];
        for (int i = 0; i < K; i++) {
            B[i][i] = 0.00001;
        }

        double[][] b = new double[K][1];

        Generator gen = new Generator();
        NextState s;
        for (int l = 0; l < limit; l++) {
            do {
                s = Generator.decodeState(gen.generateUniqueState());
            } while (s == null);

            for (int action = 0; action < s.legalMoves().length; action++) {
                double[][] phi, phi_;
                // B = B - B*phi(s,a)*transpose(phi(s, a) - gamma* SUM(P(s,a,s')*phi(s', pi(s'))*B * (1/
                ns.copyState(s);
                ns.makeMove(action);

                if (ns.hasLost()) continue;
                phi = matrix.convertToColumnVector(ff.computeFeaturesVector(ns));

                // Compute summation of P(s,a,s') * phi(s', pi(s'))
                double[][] sumPhi = new double[K][1];
                double sumReward = 0;

                for (int piece = 0; piece < State.N_PIECES; piece++) {
                    ns.setNextPiece(piece);
                    nns.copyState(ns);
                    nns.makeMove(PlayerSkeleton.pickBestMove(nns, weights));

                    phi_ = matrix.convertToColumnVector(ff.computeFeaturesVector(nns));
                    sumPhi = matrix.matrixAdd(sumPhi, phi_);
                    sumReward += P(ns, action, nns) * R(ns, action, nns);
                }
                // Multiply summation with gamma and P(s,a,s')
                // We can do this because P(s,a,s') is a constant
                double[][] tempSum = matrix.multiplyByConstant(sumPhi, GAMMA * P(ns, action, nns));
                double[][] transposed = matrix.transpose(matrix.matrixSub(phi, tempSum));

                double[][] numerator = matrix.matrixMultplx(B, phi);
                numerator = matrix.matrixMultplx(numerator, transposed);
                numerator = matrix.matrixMultplx(numerator, B);

                double[][] temp = matrix.matrixMultplx(transposed, B);
                temp = matrix.matrixMultplx(temp, phi);
                double denominator = 1.0 + temp[0][0];

                B = matrix.matrixSub(B, matrix.multiplyByConstant(numerator, 1.0 / denominator));
                b = matrix.matrixAdd(b, matrix.multiplyByConstant(phi, sumReward));
            }
        }
        weights = matrix.convertToArray(matrix.matrixMultplx(B, b));

        return weights;
    }

    /**
     * This is mainly the wrapper to continuously iterate through samples and give it
     * to LSTDQ to adjust the weight.
     * The way we do it is by randomly pick a move for a given state to make new sample.
     * Learning until the difference between 2 consecutive weights is very small (convergence)
     *
     * EPSILON is small
     *
     * @return the adjusted weight after the whole learning process
     */
    private double[] LSPI() {
        double[] prevWeights;
//        readSampleSource();
//        NextState s;

        int count = 20;

        do {
            prevWeights = Arrays.copyOf(weights, weights.length);
            // Making random move to generate sample
            weights = LSTDQ_OPT(50000);

        } while (difference(prevWeights, weights) >= EPSILON && count-- > 0);

        System.out.println(count);
        return weights;
    }

    /**
     * Find absolute difference between magnitudes of vectors
     * @param a
     * @param b
     * @return the absolute difference
     */
    private double difference(double[] a, double[] b) {
        if (a.length != b.length) throw new IllegalArgumentException("Arrays not same size");
        int length = a.length;
        double[] diff = new double[length];
        for (int i = 0; i < length; i++) {
            diff[i] = a[i] - b[i];
        }

        return magnitude(diff);
    }

    /**
     * Finding magnitude of a vector
     * @param a
     * @return the magnitude
     */
    private double magnitude(double[] a) {
        double sqrSum = 0;
        for (double d: a) {
            sqrSum += d*d;
        }

        return Math.sqrt(sqrSum);
    }


    @Override
    public void run() {
        try {
            LSPI();
            System.out.println("Learner#" + this.id + " is done");
        } finally {
            // Interrupted or finish learning. Writing back weights
            System.out.println("Learner#" + this.id + " is writing back weights");
            writeWeightsVector();
            System.out.println("Learner#" + this.id + " 's done writing back weights");
        }
    }

    /**
     * Using stochastic gradient descent to find the final weights.
     * The value for each policy is determined by the average score of
     * 1000 games played based on the policy
     *
     * First step:
     *      Generate all the average score for each policy by iterating through
     *      each weight file and evaluate.
     * Second step:
     *      Perform SGD with momentum.
     *       dw = alpha * gradient(Q(w'), Q(w)) + beta * dw
     *       w = w + dw
     *
     *      In which alpha is the learning rate, beta is the constant affect of the momentum
     *      Both are user defined.
     */
    public static void consolidateLearning_SGD() throws FileNotFoundException, InterruptedException {
        // First step
        ExecutorService threadPool = Executors.newFixedThreadPool(10);
        int gamesNo = 1000;
        double alpha = 0.01;
        double beta = 0.5;

        FilenameFilter weightFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.matches("^weight\\d+\\.txt$");
            }
        };

        FilenameFilter reportFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.matches("^report_weight\\d+\\.txt$");
            }
        };

        for (File w: new File(LEARNER_DIR).listFiles(weightFilter)) {
            for (int i = 0; i < gamesNo; i++) {
                threadPool.execute(new Player(w.getName(), 1));
            }
        }

        threadPool.shutdown();
        threadPool.awaitTermination(1L, TimeUnit.DAYS);

        ArrayList<PolicyResult> samples = new ArrayList<PolicyResult>();

        for (File w: new File(Player.REPORT_DIR).listFiles(reportFilter)) {
            samples.add(Player.getResult(w.getName()));
        }
        // After the results are obtained. Proceed to Second step
        double[][] w = new double[K][1];
        double[][] dw = new double[K][1]; // Delta W
        double[][] gradient;

        w = samples.get(0).weightsV;
        for (int i = 1; i < samples.size(); i++) {
            gradient = matrix.matrixSub(samples.get(i).weightsV, samples.get(i - 1).weightsV);
            dw = matrix.matrixAdd(matrix.multiplyByConstant(gradient, alpha), matrix.multiplyByConstant(dw, beta));
            w = matrix.matrixAdd(w, dw);
        }

        File targetFile = new File("final_weights.txt");

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(targetFile))){
            StringBuilder sb = new StringBuilder();
            for (double[] d: w) {
                sb.append(d[0]).append('\n');
            }

            bw.write(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
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
        File targetFile = new File("final_weights.txt");

        double[] finalWeights = new double[K];
        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(targetFile))){
            Scanner sc = new Scanner(br);
            count = sc.nextInt();
            for (int i = 0; i < K; i++) {
                finalWeights[i] = sc.nextDouble();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FilenameFilter fileNameFilter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.matches("^weight\\d+\\.txt$");
                }
            };

            for (File w: dir.listFiles(fileNameFilter)) {
                Scanner sc = new Scanner(w);
                count++;
                for (int i = 0; i < K; i++) {
                    finalWeights[i] += sc.nextDouble();
                }
            }
        } catch (FileNotFoundException|NullPointerException e) {
            e.printStackTrace();
        }

        BufferedWriter bw = null;

        try {
            bw = new BufferedWriter(new FileWriter(targetFile));
            StringBuilder sb = new StringBuilder();
            sb.append(count).append('\n');
            for (double w: finalWeights) {
                sb.append(w).append('\n');
            }

            // Write current average
            sb.append("---Average---\n");

            for (double w: finalWeights) {
                sb.append(w/count).append('\n');
            }

            bw.write(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bw != null) try {
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        int numOfLearners = args.length >= 1 && args[0] != null ? Integer.parseInt(args[0]) : 1;
        int startingId = args.length >= 2 && args[1] != null ? Integer.parseInt(args[1]) : 0;

        Thread[] threads = new Thread[numOfLearners];
        for (int i = 0; i < numOfLearners; i++) {
            threads[i] = new Thread(new Learner(i + startingId));
            threads[i].start();
        }

        try {
            for (Thread t: threads) {
                t.join();
            }
        } catch (InterruptedException e) {
            System.out.println("Interrupted! Shutting down all instances of learners.");
            for (Thread t: threads) t.interrupt();
        }

        System.out.println("Done learning or interrupted. Beginning to consolidate learning.");
        consolidateLearning();
//        try {
//            consolidateLearning_SGD();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        System.out.println("Done consolidate learning!");
    }
}
