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

	public static final int NUM_OF_FEATURE = 10;
	public static final int F1 	= 0; // Landing height
	public static final int F2 	= 1; // Rows clear
	public static final int F3 	= 2; // Row transition
	public static final int F4 	= 3; // Col transition
	public static final int F5 	= 4; // Num of holes
	public static final int F6 	= 5; // Well sum
	public static final int F7 	= 6; // Hole depth
	public static final int F8 	= 7; // Row hole
	public static final int F9 	= 8; // Number of different pieces accommodated
	public static final int F10 = 9; // Total number of pieces + rotations accommodated

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

		// Calling feature 4 5 6 and assign correct values
		//features456(nextStage);

		featuresVector[F7] = features7(nextStage);
		featuresVector[F8] = features8(nextStage);

		// Calling feature 9 10 and assign correct values
		double[] features910Return = features910(nextStage);
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

		// TODO: Implement such that this function returns 3 values into features.
		// double[] results456 = features456(ns)
		// features[F4] = results456[0];
		// features[F5] = results456[1];
		// featuers[F6] = results456[2]
		// or param passing
		// features456(ns, features);
		//features456(ns);

		features456(ns, features);

		features[F7] = features7(ns);
		features[F8] = features8(ns);

		// Calling feature 9 10 and assign correct values
		double[] features910Return = features910(ns);
		features[F9] = features910Return[0];
		features[F10] = features910Return[1];

		return features;
	}

    // Implementation of f1
    double getLandingHeight(State s, int action) {
        int orient = s.legalMoves()[action][State.ORIENT];
        int slot = s.legalMoves()[action][State.SLOT];
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
		return ns.getRowsCleared() - ns.getOriginalState().getRowsCleared();
	}

    // Implementation of f3
    int getRowTransition(State s) {
        int transCount = 0;
		int[][] field = s.getField();
        for (int i = 0; i < State.ROWS - 1; i++) {
            if(s.getField()[i][0] == 0) transCount++;
            if(s.getField()[i][State.COLS-1] == 0) transCount++;
            for(int j=1;j<State.COLS;j++) {
                if (isDifferent(field[i][j], field[i][j-1])) {
                    transCount++;
                }
            }
        }
        return transCount;
    }

    // Implementation of f4

	/*
	 * Also implemented features 1-20 mentioned in the handout Index Labels:
	 * 0-Column Transitions 1-Number of Holes 2-Well Depths(NotCumalative) 3 to
	 * 11 - Consecutive well depths 12 to 21 - Column Heights 22 - Maximum
	 * column height
	 */
	public void features456(State s, double[] featuresVector) {
		double[] computedValues = new double[3];
		Arrays.fill(computedValues, 0);
		int[][] field = s.getField();
		int[] top = s.getTop();
		int maxTop = top[0];
		int cumulativeWell;
		for (int i = 0; i < State.ROWS; i++) {
			for (int j = 0; j < State.COLS; j++) {
				// FIXME: Boundary checking when j = 0
				try{
				if ((field[i][j] != 0)
						&& ((field[i+1][j] == 0) || (field[i-1][j] == 0)))
					computedValues[0]++;
				}
			catch(Exception e){}
				// If a filled cell is adjacent to an empty cell in the same column, we add 1 to the column transitions
				try{
				if ((field[i][j] == 0) && (field[i+1][j] != 0))
					computedValues[1]++;
				}
				catch(Exception e){}
				// If a hole is right below an filled cell, we add 1 to number of holes. This is confusing but remember that this is not hole depths but number of holes
				try{
				if ((j == 0 || top[j - 1] > top[j])
						&& (j == 9 || top[j + 1] > top[j])) {
					// We check if the adjacent columns have height greater than the current column
					if (j == 0) {
						cumulativeWell = top[1] - top[0];
						// For the 1st column, the well depth is the difference between its height and column 2's height
					} else if (j == 9) {
						// Same as the previous 1, column9 - column8
						cumulativeWell = top[8] - top[9];
					} else {
						// For any intermediate column, the well depth is the minimum difference between columns height and its neighbours height
						cumulativeWell = Math.min(top[j - 1], top[j + 1]) - top[j];
					}
					computedValues[2] += cumulativeWell
								* (cumulativeWell + 1) / 2;
					// Using the formula n*(n+1)/2 to calculate cumulative well depths
				}
			}catch(Exception e){}
			}
		}
		/*
		for (int j = 0; j < State.ROWS; j++) {
			if (j != (State.ROWS - 1))
				computedValues[3 + j] = top[j] - top[j + 1];
			if (maxTop < top[j])
				maxTop = top[j];
			computedValues[12 + j] = top[j];
		}
		computedValues[22] = maxTop;*/

		// TODO: Change the computedValues to use featuresVector
		featuresVector[F4] = computedValues[0];
		featuresVector[F5] = computedValues[1];
		featuresVector[F6] = computedValues[2];
		return;
	}

	// Implementation of f5

	// Implementation of f6

	// Implementation of f7: counting hole depth
	public int features7(State s) {
		int finalValue = 0;
		boolean holeDetected;
		int[] top = s.getTop();

		for (int i = 0; i < s.COLS; i++) {
			holeDetected = false;
			// Only go as high as the column height
			for (int j = 0; j < top[i]; j++) {
				// Flag if a hole is detected in the column
				if(s.getField()[j][i] == 0)
					holeDetected = true;
				// Count every filled square above the hole
				else if(holeDetected)
					finalValue++;
			}
		}

		return finalValue;
	}

	// Implementation of f8: row hole count
	public int features8(State s) {
		int finalValue = 0;

		int[] top = s.getTop();
		for (int i = 0; i < s.ROWS; i++) {
			for (int j = 0; j < s.COLS; j++) {
				// Skip if above top of column
				if(i > top[j]) continue;
				// If a hole is found, count row and jump to next row
				if (s.getField()[i][j] == 0) {
					finalValue++;
					break;
				}
			}
		}

		return finalValue;
	}

	// Implementation of f9

	public double[] features910(State s) {
		double[] computedValues = new double[23];
		Arrays.fill(computedValues, 0);
		int[] top = s.getTop();
		int[][][] pBottom = State.getpBottom();
		int totalAcc = 0;
		int uniqueAcc = 0;
		boolean pieceFitsFlag;

		// Note that even though we have four for loops, the time
		// complexity is not O(n^4), as all the array lenghts are fixed

		// For each column
		for (int i = 0; i < State.COLS - 1; i++) {
			// For each different piece
			for (int j = 0; j < pBottom.length; j++) {
				pieceFitsFlag = false;
				// For each rotation
				for (int k = 0; k < pBottom[j].length; k++) {
					boolean rotationFitsFlag = true;
					// For each piece column.
					for (int l = 0; l < pBottom[j][k].length - 1; l++) {
						// Skip if only two columns are left, and
						// the rotation is three squares wide
						if (top.length - i < pBottom[j][k].length)
							continue;
						// Check if the the stack pattern matches the piece's
						// bottom
						if (top[i] - top[i + 1] != pBottom[j][k][l]
								- pBottom[j][k][l + 1]) {
							// Break if part of piece does not fit
							rotationFitsFlag = false;
							break;
						}
					}
					if(rotationFitsFlag){
						totalAcc++;
						pieceFitsFlag = true;
					}
				}
				// Only count unique piece once, regardless of number of fits
				if (pieceFitsFlag)
					uniqueAcc++;
			}
		}

		double[] ret = {uniqueAcc, totalAcc};
		return ret;
	}

	// Implementation of f10

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
