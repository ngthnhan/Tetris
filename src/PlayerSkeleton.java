import java.io.IOException;
import java.util.Arrays;

public class PlayerSkeleton {
	/**
	 * A utility class to help calculating the feature functions and
	 * the state value of a given state.
	 */
	public class FeatureFunction {

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
	public class NextState extends State {
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
	}
	
}
