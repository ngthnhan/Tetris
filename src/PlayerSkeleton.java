import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * This class is a player that will automatically spawn
 * different players that use a policy and evaluate that
 * policy based on the statistic.
 */
class Player implements Runnable {
	private double[] weights;
	private String reportFileName;
	private File policyFile;

	private final double MIN_VAL = Double.NEGATIVE_INFINITY;
	private FeatureFunction ff = new FeatureFunction();

	private int score;
	private int gameLimit;

	private boolean inGame;

	private double[] getWeights() { return weights; }
	private int getScore() { return score; }

	public static final String REPORT_DIR = "Report";

	Player(String policyFile, int gameLimit) {
		this.reportFileName = "report_" + policyFile;
		this.policyFile = new File(Learner.LEARNER_DIR, policyFile);

		this.score = 0;
		this.weights = new double[FeatureFunction.NUM_OF_FEATURE];
		this.gameLimit = gameLimit;

		this.inGame = false;

		readPolicy();
	}

	/**
	 * Reading the policy (weights vector) from the given file inside
	 * Learner folder
	 *
	 */
	private void readPolicy() {
		try (Scanner sc = new Scanner(new FileReader(policyFile))) {
			for (int i = 0; i < weights.length; i++) {
				weights[i] = Double.parseDouble(sc.nextLine());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			// If anything wrong happens, this player will not play any game
			System.out.println("Something's wrong. Not playing");
			gameLimit = 0;
		}
	}

	/**
	 * Write the report header to include information about the policy being used
	 * and the time the file is first created.
	 *
	 * @param bw The writer that it tries to write to
	 * @param p The player that it trying to write report.
	 * @throws IOException
	 */
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

	/**
	 * Write the result to the report file name given. If the file does not exist yet,
	 * it will create the file, write the header and append the result. Otherwise, it
	 * will just append the result
	 *
	 * @param p The player that is trying to write the result
	 * @param fileName The name of the report file that the result should be written to
	 */
	private static synchronized void writeToReport(Player p, String fileName) {
		File f = new File(fileName);
		boolean exists = f.exists() && f.isFile();

		try (BufferedWriter bw = new BufferedWriter(new FileWriter(f, exists))){
			if (!exists) {
				writeReportHeader(bw, p);
			}

			String s = new Integer(p.getScore()).toString();
			bw.write(s);
			bw.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create a fresh new State object and start playing based on the given policy
	 * until it loses.
	 *
	 * Store the score with the number of rows cleared.
	 */
	private void play() {
		State s = new State();
		inGame = true;
		while(!s.hasLost()) {
			s.makeMove(pickBestMove(s, weights));
		}

		this.score = s.getRowsCleared();
		inGame = false;
	}

	@Override
	public void run() {
		int limit = 0;
		boolean infinite = gameLimit < 0;
		try {
			while (!Thread.currentThread().isInterrupted() && infinite || limit < gameLimit) {
				limit++;
				play();

				writeToReport(this, reportFileName);
			}
		} finally {
			if (!inGame) writeToReport(this, reportFileName);
		}
	}

	private int pickBestMove(State s, double[] w) {
		int bestMove=0, currentMove;
		double bestValue = MIN_VAL, currentValue;
		NextState ns = new NextState();

		for (currentMove = 0; currentMove < s.legalMoves().length; currentMove++)
		{
			ns.copyState(s);
			ns.makeMove(currentMove);
			currentValue = ff.valueOfState(ns, w);
			if (currentValue > bestValue) {
				bestMove = currentMove;
				bestValue = currentValue;
			}
		}

		return bestMove;
	}

	/**
	 * Allow parallel playing the same policy file.
	 *
	 * @param args The string array of arguments passed
	 *             fileName: the name of the policy file. Default is ""
	 *             numOfPlayers: the number of players to play. Default is 4
	 *             limit: the number of game limit that the player should play. Default is infinitely
	 */
	public static void main(String[] args) {
		String fileName = args.length >= 1 && args[0] != null ? args[0] : "";
		int numOfPlayers = args.length >= 2 && args[1] != null ? Integer.parseInt(args[1]) : 4;
		int limit = args.length >= 3 && args[2] != null ? Integer.parseInt(args[2]) : -1;

		final Thread[] threads = new Thread[numOfPlayers];
		for (int i = 0; i < numOfPlayers; i++) {
			threads[i] = new Thread(new Player(fileName, limit/numOfPlayers));
			threads[i].start();
		}

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				System.out.println("Interrupted or Done playing! Shutting down all instances of players.");
				for (Thread t: threads) t.interrupt();
			}
		});

		try {
			for (Thread t: threads) {
				t.join();
			}
		} catch (InterruptedException e) {
			System.out.println("Interrupted or Done playing! Shutting down all instances of players.");
			for (Thread t: threads) t.interrupt();
		}
	}
}

/**
 * This class will generate a random but valid state.
 * The generated state consist of the board configuration and the next piece
 * The state generated is compressed into an array of numbers which
 * is light on memory
 */
class Generator {
	private HashSet<String> explored;
	private static final int  NUM_OF_ENCODED = 7;
	private Random rand;

	public Generator() {
		explored = new HashSet<String>();
		rand = new Random(System.currentTimeMillis());
	}

	public String convertToStr(int[] nums) {
		StringBuilder sb = new StringBuilder();
		for (int n: nums) {
			sb.append(n);
			sb.append(',');
		}

		return sb.substring(0, sb.length()-1);
	}

	public static NextState decodeState(String encoded) {
		String[] strs = encoded.split(",");
		int[] nums = new int[NUM_OF_ENCODED];
		int[][] fields = new int[NextState.ROWS][NextState.COLS];
		for (int i = 0; i < strs.length; i++) {
			nums[i] = Integer.parseInt(strs[i]);
		}

		// Decode the nums by shifting bits
		int bits = 0;
		int[] tops = new int[NextState.COLS];
		int t;
		for (int i = 0; i < NextState.ROWS - 1; i++) {
			t = 0;
			for (int j = 0; j < NextState.COLS; j++) {
				int num = bits / 32;
				fields[i][j] = nums[num] & 1;
				nums[bits / 32] >>= 1;
				bits++;
				if (fields[i][j] == 1) {
					tops[j] = i + 1;
				}
			}
		}

		int nextPiece = nums[NUM_OF_ENCODED-1] & ((1 << 3) - 1);

		// Checking validity of the state
		int maxHeight = 0;
		for (int j = 0; j < NextState.COLS; j++) {
			if (tops[j] > maxHeight) maxHeight = tops[j];
		}

		// Checking if there is a row with all empty or all non-empty
		boolean valid;
		for (int i = 0; i < maxHeight; i++) {
			valid = false;
			for (int j = 0; j < NextState.COLS - 1; j++) {
				if (fields[i][j] != fields[i][j+1]) valid = true;
			}

			if (!valid) return null;
		}

		// Check if nextPiece is valid
		if (nextPiece >= NextState.N_PIECES) return null;

		NextState s = new NextState();
		s.setNextPiece(nextPiece);
		s.setFieldDeep(fields);
		s.setTopDeep(tops);

		return s;
	}

	/**
	 * The state is encoded into a string. The string will contains integer (32-bit)
	 * separated by commas. There will be 7 integers (224 bits) to represent a complete
	 * state. The first 200 LSB bits will represent the status of the cells. The next 3 LSB
	 * represent the next piece.
	 * @return the encoded string of a complete state
	 */
	public String generateUniqueState() {
		String encodedStr = "";
		int[] encodedNums = new int[NUM_OF_ENCODED];

		do {
			for (int i = 0; i < NUM_OF_ENCODED; i++) {
				encodedNums[i] = rand.nextInt();
			}

			encodedStr = convertToStr(encodedNums);
		} while (explored.contains(encodedStr) || !isValid(encodedStr));

		return encodedStr;
	}

	public boolean isValid(String str) {
		return decodeState(str) != null;
	}
}

/**
 * The learner that uses Least-Squared Policy Iteration to learn from
 * a pool of randomly generated states.
 */
class Learner implements Runnable {
	private double[] weights;
	private int id;
	private int sampleSize;
	private String weightFileName;
	private File weightFile;

	private NextState ns;
	private NextState nns;

	private FeatureFunction ff;
	private final int LOST_REWARD = -1000000;
	private final int INFINITE = -1;
	private final double GAMMA = 0.9;
	private final double EPSILON = 0.0005;
	private final double MIN_VAL = Double.NEGATIVE_INFINITY;

	private static Random rand;
	private static ArrayList<String> samplesSource;

	public static final String LEARNER_DIR = "Learner";

	private static final int K = FeatureFunction.NUM_OF_FEATURE;

	private Learner(int id, int sampleSize) {
		this.id = id;
		this.sampleSize = sampleSize;
		this.weightFileName = String.format("weight%d.txt", id);

		if (samplesSource == null) samplesSource = new ArrayList<String>();
		if (rand == null) {
			rand = new Random();
			rand.setSeed(System.currentTimeMillis());
		}

		File dir = new File(Learner.LEARNER_DIR);
		if (!dir.exists() || !dir.isDirectory()) {
			dir.mkdir();
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
			while(sc.hasNext()) {
				weights[i++] = Double.parseDouble(sc.nextLine());
			}
		} catch (IOException e) {
			try {
				// Randomise weights
				Random rdm = new Random(System.currentTimeMillis());
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
					nns.makeMove(pickBestMove(nns, weights));

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

	private int pickBestMove(State s, double[] w) {
		int bestMove=0, currentMove;
		double bestValue = MIN_VAL, currentValue;
		NextState ns = new NextState();

		for (currentMove = 0; currentMove < s.legalMoves().length; currentMove++)
		{
			ns.copyState(s);
			ns.makeMove(currentMove);
			currentValue = ff.valueOfState(ns, w);
			if (currentValue > bestValue) {
				bestMove = currentMove;
				bestValue = currentValue;
			}
		}

		return bestMove;
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
			System.out.println(count);
			prevWeights = Arrays.copyOf(weights, weights.length);
			// Making random move to generate sample
			weights = LSTDQ_OPT(sampleSize);

		} while (difference(prevWeights, weights) >= EPSILON && count-- > 0);

		System.out.println(count);
		for (int i = 0; i < K; i++) {
			weights[i] = weights[i] < 0 ? weights[i] : -weights[i];
		}
		weights[FeatureFunction.F2] = - weights[FeatureFunction.F2];

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
	 * Map reducer function to consolidate learning from the learners.
	 * For now it just finds the mean of all the weights.
	 *
	 * Future change: Make it a stochastic gradient ascent algorithm
	 * Get value of each state using player to critic.
	 */
	public static void consolidateLearning(final int numOfLearners, final int startingId) {
		File dir = new File(LEARNER_DIR);
		String fileName = String.format("weight_%d_%d.txt", numOfLearners, startingId);
		File targetFile = new File(dir, fileName);

		double[] finalWeights = new double[K];
		int count = 0;

		FilenameFilter fileNameFilter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				boolean valid = false;
				for (int i = 0; i < numOfLearners; i++) {
					int id = startingId + i;
					valid = valid || name.matches(String.format("^weight%d\\.txt$", id));
				}
				return valid;
			}
		};

		try {
			for (File w: dir.listFiles(fileNameFilter)) {
				Scanner sc = new Scanner(w);
				count++;
				for (int i = 0; i < K; i++) {
					finalWeights[i] += Double.parseDouble(sc.nextLine());
				}
			}
		} catch (FileNotFoundException|NullPointerException e) {
			System.out.println("File not found.");
		}

		BufferedWriter bw = null;

		try {
			bw = new BufferedWriter(new FileWriter(targetFile));
			StringBuilder sb = new StringBuilder();
			for (double w: finalWeights) {
				sb.append(w/count).append(System.lineSeparator());
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

		System.out.println("PolicyFileName: " + fileName);
	}

	public static void main(String[] args) {
		int numOfLearners = args.length >= 1 && args[0] != null ? Integer.parseInt(args[0]) : 1;
		int startingId = args.length >= 2 && args[1] != null ? Integer.parseInt(args[1]) : 0;
		int sampleSize = args.length >= 3 && args[2] != null ? Integer.parseInt(args[2]) : 50000;

		Thread[] threads = new Thread[numOfLearners];
		for (int i = 0; i < numOfLearners; i++) {
			threads[i] = new Thread(new Learner(i + startingId, sampleSize / numOfLearners));
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
		consolidateLearning(numOfLearners, startingId);
		System.out.println("Done consolidate learning!");
	}
}

/**
 * A utility class to help calculating the feature functions and
 * the state value of a given state.
 */
class FeatureFunction {

	public static final int NUM_OF_FEATURE = 8;
	public static final int F1 	= 0; // Landing height
	public static final int F2 	= 1; // Rows clear
	public static final int F3 	= 2; // Row transition
	public static final int F4 	= 3; // Col transition
	public static final int F5 	= 4; // Num of holes
	public static final int F6 	= 5; // Well sum
	public static final int F7	= 6; // Empty cells below some filled cell in the same column
	public static final int F8	= 7; // Average height of columns

	/**
	 * Checking if the 2 given cells are different. Different are defined in term of
	 * either filled or not filled. Filled is when a cell contains a non-0 number. Not filled otherwise
	 * @param cellA
	 * @param cellB
	 * @return true if cellA is different from cellB. False otherwise
	 */
	private boolean isDifferent(int cellA, int cellB) {
		boolean cellAFilled = cellA != 0;
		boolean cellBFilled = cellB != 0;

		return cellAFilled != cellBFilled;
	}

	/**
	 * This function promise to compute all the features and update
	 * featuresVector correctly
	 * A more compact function to compute and return the features vector using only a NextState object
	 * @param ns
	 * @return feature vectors in an array of double
	 */
	public double[] computeFeaturesVector(NextState ns) {
		double[] features = new double[NUM_OF_FEATURE];
		features[F1] = feature1(ns.getOriginalState(), ns.getAction());
		features[F2] = feature2(ns);
		features[F3] = feature3(ns);

		int[] features45Return = features457(ns);
		features[F4] = features45Return[0];
		features[F5] = features45Return[1];
		features[F7] = features45Return[2];
		double[] features6Return = features68(ns);
		features[F6] = features6Return[0];
		features[F8] = features6Return[1];

		return features;
	}

	/**
	 * F1: Landing height
	 *
	 * The height at where the piece is put = height of column + (height of piece / 2)
	 * @param s To get the board position and how it will change according to the action
	 * @param action The action index of the piece.
	 * @return The heuristic score for the given state and action taken.
	 */
	double feature1(State s, int action) {
		int[][] legalMoves = s.legalMoves();

		int orient = legalMoves[action][State.ORIENT];
		int slot = legalMoves[action][State.SLOT];

		return getLandingHeight(s, orient, slot);
	}

	/**
	 * The height at where the piece is put = height of column + (height of piece / 2)
	 * @param s To get the board position and how it will change according to the action
	 * @param orient To orientation of the piece
	 * @param slot The slot index of the left most column
	 * @return The heuristic score for the given state and action taken.
	 */
	double getLandingHeight(State s, int orient, int slot) {
		int piece = s.getNextPiece();

		double height = -1;
		for (int i = 0, col = slot; i < s.getpWidth()[piece][orient]; i++, col++) {
			height = Math.max(height,  s.getTop()[col] - s.getpBottom()[piece][orient][i]);
		}
		return height + s.getpHeight()[piece][orient] / 2.0;
	}
	/**
	 * F2: Row cleared
	 *
	 * Compute the number of rows removed if a move is taken within the NextState object
	 * @param ns the state where the move is already taken place
	 * @return number of rows removed.
	 */
	int feature2(NextState ns) {
		// Return difference between rows cleared before and after move
		return ns.getRowsCleared() - ns.getOriginalState().getRowsCleared() + 1;
	}


	/**
	 * F3: Row transition
	 *
	 * Number of filled cells adjacent to empty cells, summed over all rows. Implemented by traversing each row,
	 * checking whether each filled cell is different from the cell next to it, or the border is next to an empty
	 * cell on that row, and counting up if it is
	 * @param s The state to check
	 * @return the number of row transition
	 */
	int feature3(State s) {
		int transCount = 0;
		int[][] field = s.getField();

		// Traverse all rows
		for (int i = 0; i < State.ROWS - 1; i++) {
			// Count empty edge as row transition, if not higher than highest edge
			if (field[i][0] == 0) transCount++;
			if (field[i][State.COLS - 1] == 0) transCount++;
			// Count all row transitions
			for(int j=1;j<State.COLS;j++) {
				if (isDifferent(field[i][j], field[i][j-1])) {
					transCount++;
				}
			}
		}
		return transCount;
	}

	/**
	 * F4: Column transition
	 *
	 * Number of filled cells adjacent to empty cells, summed over all columns.
	 * Implemented by traversing each column, checking whether each filled cell is different
	 * from the cell above it, and counting up if it is.
	 *
	 * F5: Number of holes
	 *
	 * Number of empty cells with at least one filled cell above.
	 * Implemented by traversing each column, checking if we have an empty cell directly under a
	 * filled cell, and counting up if it is.
	 *
	 * F7: Covered empty cells
	 *
	 * Number of empty cells below one or more filled cells in the same column,
	 * summed over all columns. Implemented by traversing each column, counting any empty cell
	 * lower than its column top.
	 *
	 * @param s The state to check
	 * @return an array of results of column transition, number of holes and covered empty cells
	 */
	public int[] features457(State s) {
		int[][] field = s.getField();
		int[] top = s.getTop();
		// Feature 4 result:
		int columnTransitions = 0;
		// Feature 5 result:
		int holes = 0;
		int gaps = 0;
		boolean columnDone = false;
		// Traverse each column
		for (int i = 0; i < State.COLS; i++) {
			// Traverse each row until the second highest
			for (int j = 0; j < State.ROWS - 1; j++) {
				// Feature 4: Count any differences in adjacent rows
				if (isDifferent(field[j][i], field[j+1][i]))
					columnTransitions++;
				// Feature 5: Count any empty cells directly under a filled cell
				if ((field[j][i] == 0) && (field[j+1][i] > 0))
					holes++;
				if ((field[j][i] == 0) && j<top[i])
					gaps++;
				// Break if rest of column is empty
				if(j >= top[i])
					columnDone = true;
			}
			if(columnDone)
				continue;
		}
		int[] results = {columnTransitions, holes, gaps};
		return results;
	}

	/**
	 * F6: Cumulative well
	 *
	 * Sum of the accumulated depths of the wells. Implemented by traversing through each column,
	 * comparing the height to the height of the lowest of the two neighbors. Boundary columns are
	 * always compared to the neighbor towards the middle, as the edge is considered a column of maximum height.
	 *
	 * F8: Average column height
	 *
	 * The average height of all the columns. Implemented by adding the top of each column,
	 * dividing by the number of columns.
	 *
	 * @param s The state to check
	 * @return An array of results of cumulative well and average column height
	 */
	public double[] features68(State s)
	{
		int[] top = s.getTop();
		double cumulativeWells = 0, total=0;

		for (int i = 0; i < State.COLS; i++){
			total += top[i];
			// Feature 6:
			// Make sure array doesn't go out of bounds
			int prevCol = i == 0 ? State.ROWS : top[i - 1];
			int nextCol = i == State.COLS - 1 ? State.ROWS : top[i + 1];
			// Find depth of well
			int wellDepth = Math.min(prevCol, nextCol) - top[i];
			// If number is positive, there is a well. Calculate cumulative well depth
			if(wellDepth > 0)
				cumulativeWells += wellDepth * (wellDepth + 1) / 2;
		}
		total = ((double)total)/State.COLS;
		double[] results = {cumulativeWells, total};
		return results;
	}
	// Implementation of function to compute the Value of a State
	public double valueOfState(double[] computedValues, double[] weights) {
		double value = 0;
		for (int i = 0; i < computedValues.length; i++) {
			value += computedValues[i] * weights[i];
		}
		return value;
	}

	/**
	 * A compact method to compute the value of a state with the given weights vector
	 * @param ns The state after the move is taken
	 * @param weights The weights vector given
	 * @return the value of the state based on the features and weights
	 */
	public double valueOfState(NextState ns, double[] weights) {
		if (weights.length != NUM_OF_FEATURE) throw new IllegalArgumentException("Weights vector not matching size");
		double result = 0;
		double[] features = computeFeaturesVector(ns);
		for (int i = 0; i < NUM_OF_FEATURE; i++) {
			result += features[i] * weights[i];
		}

		return result;
	}

}

/**
 * An extended class of State to add more functionality.
 * Mainly, it is so that the main State does not get modified
 * when simulating the move.
 */
class NextState extends State {
	private State originalState;
	private int turn = 0;
	private int cleared = 0;

	private int action = 0;

	//each square in the grid - int means empty - other values mean the turn it was placed
	private int[][] field = new int[ROWS][COLS];
	private int[] top = new int[COLS];

	private int[][][] 	pBottom;
	private int[][] 	pHeight;
	private int[][][]	pTop;

	NextState(State s) {
		this.pBottom = State.getpBottom();
		this.pHeight = State.getpHeight();
		this.pTop = State.getpTop();

		copyState(s);
	}

	NextState() {
		this.pBottom = State.getpBottom();
		this.pHeight = State.getpHeight();
		this.pTop = State.getpTop();

		this.turn = 0;
		this.cleared = 0;

		this.lost = false;

		this.field = new int[ROWS][COLS];
		this.top = new int[COLS];
	}

	//random integer, returns 0-6
	private int randomPiece() {
		return (int)(Math.random()*N_PIECES);
	}


	public void copyState(State s) {
		originalState = s;
		this.nextPiece = s.getNextPiece();
		this.lost = s.lost;
		for (int i = 0; i < originalState.getField().length; i++) {
			field[i] = Arrays.copyOf(originalState.getField()[i], originalState.getField()[i].length);
		}

		top = Arrays.copyOf(originalState.getTop(), originalState.getTop().length);
		turn = originalState.getTurnNumber();
		cleared = originalState.getRowsCleared();
		action = -1;
	}

	public State getOriginalState() { return originalState; }

	public int getRowsCleared() { return cleared; }

	public int[][] getField() { return field; }

	public void setFieldDeep(int[][] newField) {
		for (int i = 0; i < newField.length; i++) {
			this.field[i] = Arrays.copyOf(newField[i], newField[i].length);
		}
	}

	public int[] getTop() { return top; }

	public void setTopDeep(int[] newTop) {
		this.top = Arrays.copyOf(newTop, newTop.length);
	}

	public int getAction() { return action; }

	public int getTurnNumber() { return turn; }

	public int getNextPiece() { return this.nextPiece; }

	public void setNextPiece(int next) { this.nextPiece = next; }

	public void makeMove(int move) {
		action = move;
		makeMove(legalMoves[nextPiece][move]);
	}

	public boolean makeMove(int orient, int slot) {
		turn++;
		//height if the first column makes contact
		int height = top[slot]-pBottom[nextPiece][orient][0];
		//for each column beyond the first in the piece
		for(int c = 1; c < pWidth[nextPiece][orient];c++) {
			height = Math.max(height,top[slot+c]-pBottom[nextPiece][orient][c]);
		}

		//check if game ended
		if(height+pHeight[nextPiece][orient] >= ROWS) {
			lost = true;
			return false;
		}

		//for each column in the piece - fill in the appropriate blocks
		for(int i = 0; i < pWidth[nextPiece][orient]; i++) {

			//from bottom to top of brick
			for(int h = height+pBottom[nextPiece][orient][i]; h < height+pTop[nextPiece][orient][i]; h++) {
				field[h][i+slot] = turn;
			}
		}

		//adjust top
		for(int c = 0; c < pWidth[nextPiece][orient]; c++) {
			top[slot+c]=height+pTop[nextPiece][orient][c];
		}

		//check for full rows - starting at the top
		for(int r = height+pHeight[nextPiece][orient]-1; r >= height; r--) {
			//check all columns in the row
			boolean full = true;
			for(int c = 0; c < COLS; c++) {
				if(field[r][c] == 0) {
					full = false;
					break;
				}
			}
			//if the row was full - remove it and slide above stuff down
			if(full) {
				cleared++;
				//for each column
				for(int c = 0; c < COLS; c++) {

					//slide down all bricks
					for(int i = r; i < top[c]; i++) {
						field[i][c] = field[i+1][c];
					}
					//lower the top
					top[c]--;
					while(top[c]>=1 && field[top[c]-1][c]==0)	top[c]--;
				}
			}
		}

		return true;
	}
}

/**
 * This is a utility class to handle matrix arithmetics.
 */
class matrix {

	/**
	 * Performs matrix multiplication.
	 * @param A input matrix
	 * @param B input matrix
	 * @return the new matrix resultMatrix
	 */
	public static double [][] matrixMultplx(double [][] A, double [][]B){
		int aRows = A.length;
		int aCols = A[0].length;
		int bRows = B.length;
		int bCols = B[0].length;
		if(aCols != bRows){
			throw new IllegalArgumentException("The first matrix's rows is not equal to the second matrix's columns, cannot perform matrix multiplication");
		}
		else{
			double [][] resultMatrix = new double [aRows][bCols];
			for (int i = 0; i < aRows; i++) {
				for (int j = 0; j < bCols; j++) {
					resultMatrix[i][j] = 0.00000;
				}
			}
			for (int i = 0; i < aRows; i++) {
				for (int j = 0; j < bCols; j++) {
					for (int k = 0; k < aCols; k++) {
						resultMatrix[i][j] += A[i][k] * B[k][j];
					}
				}
			}
			return resultMatrix;
		}
	}

	/**
	 * returns the transpose of the input matrix M
	 */
	public static double [][] transpose(double [][] M){
		int mRows = M.length;
		int mCols = M[0].length;
		double [][] resultMatrix = new double [mCols][mRows];
		for(int i = 0; i < mRows; i++){
			for(int j = 0; j < mCols; j++){
				resultMatrix[j][i] = M[i][j];
			}
		}
		return resultMatrix;
	}

	/**
	 * creates positiv or negativ matrix addition of matrix A relative matrix B based of character c
	 * @param A input matrix
	 * @param B additon matrix
	 * @param c either '-' or '+'
	 * @return output matrix of this addition
	 */
	public static double[][] matrixAddition(double[][] A, double[][] B, char c) {
		int aRows = A.length;
		int aCols = A[0].length;
		int bRows = B.length;
		int bCols = B[0].length;
		if (aRows != bRows || aCols !=bCols ) {
			throw new IllegalArgumentException("both input matrix needs to be in the same format");
		}
		double [][] resultmatrix = new double [aRows][aCols];
		for ( int i = 0 ; i < aRows ; i++ ) {
			for (int j = 0; j < aCols; j++) {
				if(c== '+'){
					resultmatrix[i][j] = A[i][j] +  B[i][j];
				}
				else if (c=='-'){
					resultmatrix[i][j] = A[i][j] -  B[i][j];
				}
				else{
					throw new IllegalArgumentException("character input can only be '-' or '+'");
				}
			}
		}
		return resultmatrix;
	}

	//Matrix addition. A add B
	public static double[][] matrixAdd(double[][] A, double[][] B) {
		return matrixAddition(A,B,'+');
	}

	//Matrix substitution. A minus B
	public static double[][] matrixSub(double[][] A, double[][] B) {
		return matrixAddition(A,B,'-');
	}


	/**
	 * Creates the submatrix of a given position of the input matrix M
	 * @param M input matrix
	 * @param exclude_row excluding row
	 * @param exclude_col excluding column
	 * @return the new matrix resultMatrix
	 */
	public static double [][] createSubMatrix(double [][] M, int exclude_row, int exclude_col) {
		int mRows = M.length;
		int mCols = M[0].length;
		double[][] resultMatrix = new double[mRows - 1][mCols - 1];
		int resultMatrixRow = 0;

		for (int i = 0; i < mRows; i++) {
			//excludes the aaa row
			if (i == exclude_row) {
				continue;
			}
			int resultMatrixCol = 0;
			for (int j = 0; j < mCols; j++) {
				//excludes the aaa column
				if (j == exclude_col){
					continue;
				}
				resultMatrix[resultMatrixRow][resultMatrixCol] = M[i][j];
				resultMatrixCol+=1;
			}
			resultMatrixRow+=1;
		}
		return resultMatrix;
	}

	/**
	 * Calculate the determinant of the input matrix
	 * @param M input matrix
	 * @return the determinant
	 * @throws IllegalArgumentException
	 */
	public static double determinant(double [][] M) throws IllegalArgumentException {
		int aRows = M.length;
		int aCols = M[0].length;
		double sum = 0.0;

		if (aRows!=aCols) {
			throw new IllegalArgumentException("matrix need to be square.");
		}
		else if(aRows ==1){
			return M[0][0];
		}
		if (aRows==2) {
			return (M[0][0] * M[1][1]) - ( M[0][1] * M[1][0]);
		}
		// breaks down larger matrix into smaller Submatrix
		// calculates their determinant by recursion
		for (int j=0; j<aCols; j++) {
			sum += placeSign(0,j) * M[0][j] * determinant(createSubMatrix(M, 0, j));
		}
		return sum;
	}

	/**
	 * Checks if the place sign is positive or negative
	 */
	private static double placeSign(int i, int j) {
		if((i+j)%2 ==0 ){
			return 1.0;
		}
		return -1.0;
	}

	/**
	 * function creating the Adjugate of a matrix
	 * @param M input matrix
	 * @return the Adjugate matrix called resultMatrix
	 * @throws IllegalArgumentException
	 */
	public static double [][] matrixAdjugate(double[][] M) throws IllegalArgumentException{
		int mRows = M.length;
		int mCols = M[0].length;
		double [][] resultMatrix = new double [mRows][mCols];

		for (int i=0;i<mRows;i++) {
			for (int j=0; j<mCols;j++) {
				// i j is reversed to get the transpose of the cofactor matrix
				resultMatrix[j][i] = placeSign(i,j)* determinant(createSubMatrix(M, i, j));
			}
		}
		return resultMatrix;
	}


	/**
	 * Add constant c to every element in the matrix M
	 */
	public static double[][] multiplyByConstant(double[][] M, double c) {
		int mRows = M.length;
		int mCols = M[0].length;
		double [][] resultMatrix = new double [mRows][mCols];

		for(int i = 0; i < mRows; i++){
			for(int j = 0; j < mCols; j++){
				resultMatrix[i][j] = c*M[i][j];
			}
		}
		return resultMatrix;
	}

	/**
	 * Return the Inverse of the matrix
	 */
	public static double [][] matrixInverse(double [][] M) throws IllegalArgumentException {
		double det = determinant(M);
		if(det==0){
			throw new IllegalArgumentException("The determinant is Zero, the matrix doesn't have an inverse");
		}
		return (multiplyByConstant(matrixAdjugate(M), 1.0/det));
	}

	public static double [][] convertToRowVector(double[] singleArray){
		double[][] rowVector = new double[1][singleArray.length];
		for(int i=0;i<singleArray.length;i++) {
			rowVector[0][i]=singleArray[i];
		}
		return rowVector;
	}

	public static double [][] convertToColumnVector(double[] singleArray){
		double[][] columnVector = new double[singleArray.length][1];
		for(int i=0;i<singleArray.length;i++) {
			columnVector[i][0]=singleArray[i];
		}
		return columnVector;
	}

	public static double[] convertToArray(double[][] myMatrix){
		double[] myArray = new double[myMatrix.length];
		for(int i=0;i<myMatrix.length;i++) {
			myArray[i]=myMatrix[i][0];
		}
		return myArray;
	}

}

public class PlayerSkeleton {
	//implement this function to have a working system
	public NextState nextState ;
	public FeatureFunction ff;
	private double[] weights;
	private final static double MIN_VAL = Double.NEGATIVE_INFINITY;
	private static final boolean DEBUG = false;

	public PlayerSkeleton(){
		nextState = new NextState();
		ff = new FeatureFunction();
		weights = new double[] {
				-18632.774652174616,
				6448.762504425676,
				-29076.013395444257,
				-36689.271441668505,
				-16894.091937650956,
				-8720.173920864327,
				-49926.16836221889,
				-47198.39106032252
		};
	}

	private void printFeatures(double[] features) {
		System.out.println("Landing height: 			" + features[0]);
		System.out.println("Rows removed: 				" + features[1]);
		System.out.println("Row transition: 			" + features[2]);
		System.out.println("Col transition: 			" + features[3]);
		System.out.println("Num of holes: 				" + features[4]);
		System.out.println("Well sum: 					" + features[5]);
		System.out.println("Covered empty cells: 		" + features[6]);
		System.out.println("Average column height: 		" + features[7]);
	}

	public int pickMove(State s, int[][] legalMoves) {
		
		int bestMove=0, currentMove;
		double bestValue = MIN_VAL, currentValue;
		double[] bestFeatures = new double[FeatureFunction.NUM_OF_FEATURE], currentFeatures;

		// Copy the state before trying the moves
		for (currentMove = 0; currentMove < legalMoves.length; currentMove++)
		{
			// Try out the currentMove and compute the value
			nextState.copyState(s);
			nextState.makeMove(currentMove);
			// Use this method for debugging and checking only. Otherwise use
			// the compacted method valueOfState(NextState, weights);
			currentFeatures = ff.computeFeaturesVector(nextState);
			currentValue = ff.valueOfState(currentFeatures, weights);
			if (currentValue > bestValue) {
				bestMove = currentMove;
				bestValue = currentValue;
				bestFeatures = currentFeatures;
			}
		}
	if (DEBUG) {
			printFeatures(bestFeatures);
		}

		return bestMove;
	}

	
	public static void main(String[] args) {
		int choice;
		String params;
		Scanner sc = new Scanner(System.in);

		System.out.println("Hi, what would you like to do?");
		System.out.println("1. Let the PlayerSkeleton play the game with our final weights.");
		System.out.println("2. Use our Learner to learn.");
		System.out.println("3. Use our Player to play based on a given policy.");
		System.out.println("4. Quit.");
		System.out.println("Your choice? (1-4):");
		choice = Integer.parseInt(sc.nextLine());

		switch (choice) {
			case 1:
				State s = new State();
				new TFrame(s);
				PlayerSkeleton p = new PlayerSkeleton();
				while(!s.hasLost()) {
					s.makeMove(p.pickMove(s,s.legalMoves()));
					s.draw();
					s.drawNext(0,0);
					try {
						if (DEBUG) {
							System.in.read();
						} else {
							Thread.sleep(0);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				System.out.println("You have completed "+s.getRowsCleared()+" rows.");
				break;
			case 2:
				System.out.println("To use our learner. Please provide NumOfLearners, StartingId, SampleSize as arguments");
				System.out.println("NumOfLearners: The number of learner to learn in parallel. Default 4.");
				System.out.println("StartingId: The starting id of this learners' batch. Default 0.");
				System.out.println("SampleSize: The total number of states that this batch will learn. Default 50000");
				System.out.println("The format of argument is: ");
				System.out.println("NumOfLearner StartingId SampleSize");
				System.out.println("e.g.: 4 0 100000");
				System.out.println("Please note that you will have to create a folder called Learner in this folder.");
				System.out.println("The result will be written there.");
				params = sc.nextLine();

				Learner.main(params.split(" "));
				break;
			case 3:
				System.out.println("To use our player. Please provide PolicyFileName, NumOfPlayers, NumOfGames as arguments");
				System.out.println("PolicyFileName: The filename contains the weights in Learner folder.");
				System.out.println("NumOfPlayers: The number of concurrent players to play. Default 4.");
				System.out.println("NumOfGames: The number of games to play. Default -1 (infinity)");
				System.out.println("The format of argument is: ");
				System.out.println("PolicyFileName NumOfPlayers NumOfGames");
				System.out.println("e.g. weight_4_0.txt 4 2000");
				System.out.println("This will create a report file with format report_PolicyFileName");
				params = sc.nextLine();

				Player.main(params.split(" "));
				break;
			case 4:
				System.out.println("Bye!");
		}
	}
}

