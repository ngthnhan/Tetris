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
	 */
	private void computeFeatures(State s, int action) {
		State nextStage = new State(s);
		nextStage.makeMove(action);

		featuresVector[F1] = getLandingHeight(s, action);
		featuresVector[F2] = getErodedPieces(s, action);
		featuresVector[F3] = getRowTransition(nextStage);

		// Calling feature 4 5 6 and assign correct values
		features456(nextStage);

		featuresVector[F7] = features7(nextStage);
		featuresVector[F8] = features8(nextStage);

		// Calling feature 9 10 and assign correct values
		double[] features910Return = features910(nextStage);
		featuresVector[F9] = features910Return[0];
		featuresVector[F10] = features910Return[1];
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
        return height + + s.getpHeight()[piece][orient] / 2.0;
    }
    // Implementation of f2
    int getErodedPieces(State s, int action) {
       	NextState ns = new NextState(s);
		ns.makeMove(action);

		return ns.getRowsCleared() - s.getRowsCleared();
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
	public void features456(State s) {
		double[] computedValues = new double[23];
		Arrays.fill(computedValues, 0);
		int[][] field = s.getField();
		int[] top = s.getTop();
		int maxTop = top[0];
		int cumalativeWell;
		for (int i = 0; i < field.length; i++) {
			for (int j = 0; j < field[0].length; j++) {
				if ((field[i][j] == 1)
						&& ((field[i][j + 1] == 0) || (field[i][j - 1] == 0)))
					computedValues[0]++;
				if ((field[i][j] == 0) && (field[i][j + 1] == 1))
					computedValues[1]++;
				if ((j == 0 || top[j - 1] > top[j])
						&& (j == 9 || top[j + 1] > top[j])) {
					if (j == 0) {
						cumalativeWell = top[1] - top[0];
						computedValues[2] += cumalativeWell
								* (cumalativeWell + 1) / 2;
					} else if (j == 9) {
						cumalativeWell = top[8] - top[9];
						computedValues[2] += cumalativeWell
								* (cumalativeWell + 1) / 2;
					} else {
						cumalativeWell = Math.min(top[j - 1], top[j + 1])
								- top[j];
						computedValues[2] += cumalativeWell
								* (cumalativeWell + 1) / 2;
					}
				}
			}
		}
		for (int j = 0; j < field[0].length; j++) {
			if (j != (field[0].length - 1))
				computedValues[3 + j] = top[j] - top[j + 1];
			if (maxTop < top[j])
				maxTop = top[j];
			computedValues[12 + j] = top[j];
		}
		computedValues[22] = maxTop;

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
			for (int j = 0; i < s.COLS; j++) {
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
		for (int i = 0; i < top.length - 1; i++) {
			// For each possible piece
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

}
