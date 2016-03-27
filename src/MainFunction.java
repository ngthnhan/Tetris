/**
 * Created by ThanhNhan on 23/03/2016.
 */

import java.util.Arrays;

public class MainFunction {
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

	// Feature 11 - These are basically all the 20 features mentioned in the
	// Project Handout.

	// Implementation of f1

	// Implementation of f2

	// Implementation of f3

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
		return;
	}

	// Implementation of f5

	// Implementation of f6

	// Implementation of f7: counting hole depth
	public int features7(State s) {
		int finalValue = 0;
		boolean prevHole = false;

		for (int i = 0; i < s.COLS; i++) {
			for (int j = 0; i < s.ROWS; j++) {
				if (prevHole == true) {
					if (s.getField()[j][i] != 0) {
						prevHole = false;
						finalValue++;
					}

				} else {
					if (s.getField()[j][i] == 0) {
						prevHole = true;

					}
				}
			}
		}

		return finalValue;
	}

	// Implementation of f8: row hole count
	public int features8(State s) {
		int finalValue = 0, filled = 0, nonFilled = 0;

		for (int i = 0; i < s.ROWS; i++) {
			for (int j = 0; i < s.COLS; j++) {

				if (s.getField()[i][j] != 0) {
					filled++;
				}else{
					nonFilled++;
				}

			}
			if(filled != 0 && nonFilled != 0){
				finalValue++;
			}
			filled =0;
			nonFilled= 0;
		}

		return finalValue;
	}

	// Implementation of f9

	public void features910(State s) {
		double[] computedValues = new double[23];
		Arrays.fill(computedValues, 0);
		int[] top = s.getTop();
		int[][][] pBottom = State.getpBottom();
		int totalAcc = 0;
		int uniqueAcc = 0;
		boolean uniqueFound;

		// Note that even though we have four for loops, the time
		// complexity is not O(n^4), as all the array lenghts are fixed

		// For each column
		for (int i = 0; i < top.length - 1; i++) {
			// For each possible piece
			for (int j = 0; j < pBottom.length; j++) {
				uniqueFound = false;
				// For each rotation
				for (int k = 0; k < pBottom[j].length; k++) {
					// For each piece column
					for (int l = 0; l < pBottom[j][k].length - 1; l++) {
						// Continue if only two columns are left, and the
						// rotation is three squares wide
						if (top.length - i < pBottom[j][k].length)
							continue;
						// Check if the the stack pattern matches the piece's
						// bottom
						if (top[i] - top[i + 1] == pBottom[j][k][l]
								- pBottom[j][k][l + 1]) {
							uniqueFound = true;
							totalAcc++;
						}
						// Break if part of piece does not fit
						else
							break;
					}
				}
				// Only count unique piece once, regardless of number of fits
				if (uniqueFound)
					uniqueAcc++;
			}
		}

		// Don't know where to put the data
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
