/**
 * Created by ThanhNhan on 23/03/2016.
 */

import java.util.Arrays;

public class FeatureFunction {
    // Write your functions here

    // Feature 1

    // Feature 2

    // Feature 3

    // Feature 4

    // Feature 5

    // Feature 6

    // Feature 7

    // Feature 8

    // Feature 9

    // Feature 10

	public static final int NUM_OF_FEATURE = 8;
	public static final int F1 	= 0; // Landing height
	public static final int F2 	= 1; // Rows clear
	public static final int F3 	= 2; // Row transition
	public static final int F4 	= 3; // Col transition
	public static final int F5 	= 4; // Num of holes
	public static final int F6 	= 5; // Well sum
	public static final int F7 	= 10; // Hole depth
	public static final int F8 	= 16; // Row hole
	public static final int F9 	= 11; // Number of different pieces accommodated
	public static final int F10 = 9; // Total number of pieces + rotations accommodated
	public static final int F11	= 6;
	public static final int F12	= 7; 
	public static final int F13	= 12; 
	public static final int F14	= 13; 
	public static final int F15 = 14; 
	public static final int F16 = 15;
	public static final int F17 = 8;


	private double[] featuresVector = new double[NUM_OF_FEATURE];

	public double[] getFeaturesVector() { return featuresVector; }

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
	 * Checking if 2 given cells are filled
	 * @param cellA
	 * @param cellB
     * @return true if both filled. False otherwise.
     */
	private boolean isBothFilled(int cellA, int cellB) {
		return cellA != 0 && cellB != 0;
	}

	/**
	 * Checking if 2 given cells are not filled
	 * @param cellA
	 * @param cellB
     * @return true if both are not filled. False otherwise.
     */
	private boolean isBothNotFilled(int cellA, int cellB) {
		return cellA == 0 && cellB == 0;
	}

	/**
	 * This function will take in a state and action and compute the features
	 * vector of the next state after taking the action
	 * @param s The current state of tetris
	 * @param action The index of action taken
	 *
	 * This function promise to compute all the features and update
	 * featuresVector correctly
	 *
	 * @deprecated use {@link #computeFeaturesVector(NextState)} instead.
	 */

	@Deprecated
	private void computeFeatures(State s, int action) {
		State nextStage = new State(s);
		nextStage.makeMove(action);

		featuresVector[F1] = getLandingHeight(s, action);
		featuresVector[F2] = getErodedPieces(s, action);
		featuresVector[F3] = getRowTransition(nextStage);

		int[] features45Return = features45(nextStage);
		featuresVector[F4] = features45Return[0];
		featuresVector[F5] = features45Return[1];

		//featuresVector[F6] = feature6(nextStage);
		featuresVector[F7] = features7(nextStage);
		featuresVector[F8] = features8(nextStage);

		int[] features910Return = features910(nextStage);
		featuresVector[F9] = features910Return[0];
		featuresVector[F10] = features910Return[1];
	}

	/**
	 * A more compact function to compute and return the features vector using only a NextState object
	 * @param ns
	 * @return feature vectors in an array of double
     */
	public double[] computeFeaturesVector(NextState ns) {
		double[] features = new double[NUM_OF_FEATURE];
		features[F1] = getLandingHeight(ns.getOriginalState(), ns.getAction());
		features[F2] = getErodedPieces(ns);
		features[F3] = getRowTransition(ns);

		int[] features45Return = features45(ns);
		features[F4] = features45Return[0];
		features[F5] = features45Return[1];
		features[F11] = features45Return[2];
		//features[F17] = features45Return[3];
		double[] features6Return = feature6(ns);
		features[F6] = features6Return[0];
		features[F12] = features6Return[1];
		//features[F13] = features6Return[2];
		//features[F14] = features6Return[3];
//		features[F15] = features6Return[4];
//		features[F16] = features6Return[5];
//		features[F7] = features7(ns);
//		features[F8] = features8(ns);
//
//		int[] features910Return = features910(ns);
//		features[F9] = features910Return[0];
//		features[F10] = features910Return[1];

		return features;
	}

    // Implementation of f1
    double getLandingHeight(State s, int action) {
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
    // Implementation of f2

	/**
	 * @deprecated use {@link #getErodedPieces(NextState)} instead
	 * @param s
	 * @param action
     * @return
     */
	@Deprecated
    int getErodedPieces(State s, int action) {
       	NextState ns = new NextState(s);
		ns.makeMove(action);

		return ns.getRowsCleared() - s.getRowsCleared();
    }

	/**
	 * Compute the number of rows removed if a move is taken within the NextState object
	 * @param ns the state where the move is already taken place
	 * @return number of rows removed.
     */
	int getErodedPieces(NextState ns) {
		// Return difference between rows cleared before and after move
		return ns.getRowsCleared() - ns.getOriginalState().getRowsCleared() + 1;
	}

    // Implementation of f3
    int getRowTransition(State s) {
        int transCount = 0;
		int[][] field = s.getField();
		int[] top = s.getTop();

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

    // Implementation of f4 and f5
	public int[] features45(State s) {
		int[][] field = s.getField();
		int[] top = s.getTop();
		// Feature 4 result:
		int columnTransitions = 0;
		// Feature 5 result:
		int holes = 0;
		int gaps = 0, totalBlocks=0;
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
				if ((field[j][i] != 0))
					totalBlocks++;
				// Break if rest of column is empty
				if(j >= top[i])
					columnDone = true;
			}
			if(columnDone)
				continue;
		}

		int[] results = {columnTransitions, holes, gaps, totalBlocks};
		return results;
	}

	// Implementation of f6
	public double[] feature6(State s)
	{
		int[] top = s.getTop();
		double cumulativeWells = 0,
		maxHeight=0,
		minHeight = Integer.MAX_VALUE,
		total=0,
		totalHeightSquared = 0,
		diffTotal = 0,
		squaredDiffTotal=0,colStdDev=0;

		for (int i = 0; i < State.COLS; i++){
			total += top[i];
			totalHeightSquared += Math.pow(top[i], 2);	
			diffTotal += (i>0)?Math.abs(top[i-1]-top[i]):0;
			squaredDiffTotal += (i>0)?Math.abs(Math.pow(top[i-1],2)-Math.pow(top[i],2)):0;
			maxHeight = Math.max(maxHeight,top[i]);
			minHeight = Math.min(minHeight,top[i]); 
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
		minHeight = maxHeight-minHeight;
		colStdDev = (totalHeightSquared - total*((double)total)/State.COLS)/(double)(State.COLS-1);
		double[] results = {cumulativeWells, total, diffTotal, maxHeight, minHeight, colStdDev};
		return results;
	}

	// Implementation of f7: counting hole depth
	public int features7(State s) {
		int[][] field = s.getField();
		int[] top = s.getTop();
		boolean holeDetected;
		int holeDepth = 0;

		for (int i = 0; i < s.COLS; i++) {
			holeDetected = false;
			// Only go as high as the column height
			for (int j = 0; j < top[i]; j++) {
				// Flag if a hole is detected in the column
				if(field[j][i] == 0)
					holeDetected = true;
				// Count every filled square above the hole
				else if(holeDetected)
					holeDepth++;
			}
		}

		return holeDepth;
	}

	// Implementation of f8: row hole count
	public int features8(State s) {
		int finalValue = 0;
		int[][] field = s.getField();
		int[] top = s.getTop();

		for (int i = 0; i < s.ROWS; i++) {
			for (int j = 0; j < s.COLS; j++) {
				// Skip if above top of column
				if(i >= top[j]) continue;
				// If a hole is found, count row and jump to next row
				if (field[i][j] == 0) {
					finalValue++;
					break;
				}
			}
		}

		return finalValue;
	}

	// Implementation of f9 and f10

	public int[] features910(State s) {
		int[] top = s.getTop();
		int[][][] pBottom = State.getpBottom();
		// Feature 9 result:
		int totalAcc = 0;
		// Feature 10 result:
		int uniqueAcc = 0;
		boolean[] pieceFitsFlags = new boolean[State.N_PIECES];

		// Note that even though we have four for loops, the time
		// complexity is not O(n^4), as all the array lengths are fixed

		// For each column
		for (int i = 0; i < State.COLS - 1; i++) {
			// For each different piece
			for (int j = 0; j < State.N_PIECES; j++) {
				// For each rotation
				for (int k = 0; k < pBottom[j].length; k++) {
					boolean rotationFitsFlag = false;
					// For each piece column.
					for (int l = 0; l < pBottom[j][k].length - 1; l++) {
						// Skip if only two columns are left, and
						// the rotation is three squares wide
						if (top.length - i < pBottom[j][k].length)
							continue;
						rotationFitsFlag = true;
						// Check if the the stack pattern matches the piece's
						// bottom
						if (top[i + l] - top[i + l + 1] != pBottom[j][k][l]
								- pBottom[j][k][l + 1]) {
							// Break if part of piece does not fit
							rotationFitsFlag = false;
							break;
						}
					}
					if(rotationFitsFlag){
						totalAcc++;
						pieceFitsFlags[j] = true;
					}
				}
			}
		}
		// Only count unique piece once, regardless of number of fits
		for (boolean b: pieceFitsFlags)
			if(b) uniqueAcc++;

		int[] ret = {uniqueAcc, totalAcc};
		return ret;
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
