import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

public class PlayerSkeleton {

	//implement this function to have a working system
	public NextState nextState ;
	public FeatureFunction ff;
	private double[] weights;
	double GAMMA = 0.9f;
	private final static double MIN_VAL = Double.NEGATIVE_INFINITY;
	private final boolean DEBUG = true;
	private final boolean LEARNING = false;
	double[][] A = new double[FeatureFunction.NUM_OF_FEATURE][FeatureFunction.NUM_OF_FEATURE];
	double[][] b = new double[FeatureFunction.NUM_OF_FEATURE][1]; 

	public PlayerSkeleton(){
		nextState = new NextState();
		ff = new FeatureFunction();
		weights = readWeights("final_weights.txt");
	}

	public double[] readWeights(String fileName) {
		double[] w = new double[FeatureFunction.NUM_OF_FEATURE];
		try (Scanner sc = new Scanner(new FileReader(fileName))) {
			int i;
			for (i = 0; i < FeatureFunction.NUM_OF_FEATURE; i++) {
				w[i] = Double.parseDouble(sc.nextLine());
				//System.out.println(w[i]);
			}	

			if (i != FeatureFunction.NUM_OF_FEATURE) {
				System.out.println("There are fewer weights than needed");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("There are more weights in files than needed");
		}
		return w;
	}

	private void printFeatures(double[] features) {
		System.out.println("Landing height: 			" + features[0]);
		System.out.println("Rows removed: 				" + features[1]);
		System.out.println("Row transition: 			" + features[2]);
		System.out.println("Col transition: 			" + features[3]);
		System.out.println("Num of holes: 				" + features[4]);
		System.out.println("Well sum: 					" + features[5]);
		System.out.println("Hole depth: 				" + features[6]);
		System.out.println("Rows with holes: 			" + features[7]);
		System.out.println("No of pieces accommodated: 	" + features[8]);
		System.out.println("No of move accommodated: 	" + features[9]);
		System.out.println("Covered Gaps: 				" + features[10]);
		System.out.println("Average Difference Height: 	" + features[11]);
		System.out.println("Total Difference Height:	" + features[12]);
		System.out.println("Max Height:					" + features[13]);
		System.out.println("Diff Max Min Height: 		" + features[14]);
		System.out.println("Column Standard Deviation: 	" + features[15]);
	}


	/**
	 * This function takes in a state and policy (weights vector) and
	 * evaluate the best move
	 * @param s
	 * @param w
     * @return
     */
	public static int pickBestMove(State s, double[] w) {
		int bestMove=0, currentMove;
		double bestValue = MIN_VAL, currentValue;
		NextState ns = new NextState();
		FeatureFunction ff = new FeatureFunction(); // May want to use singleton to optimize

		for (currentMove = 0; currentMove < s.legalMoves().length; currentMove++)
		{
			ns.copyState(s);
			ns.makeMove(currentMove);

			if (ns.hasLost()) continue; // Ignore move if it is a lost move

			currentValue = ff.valueOfState(ns, w);
			if (currentValue > bestValue) {
				bestMove = currentMove;
				bestValue = currentValue;
			}
		}

		return bestMove;
	}



	public int pickMove(State s, int[][] legalMoves) {
		
		int bestMove=0, currentMove;
		double bestValue = MIN_VAL, currentValue, num=0;
		double[] bestFeatures = new double[FeatureFunction.NUM_OF_FEATURE], currentFeatures;

		// Copy the state before trying the moves
		for (currentMove = 0; currentMove < legalMoves.length; currentMove++)
		{
			// Try out the currentMove and compute the value
			nextState.copyState(s);
			nextState.makeMove(currentMove);

			if (nextState.hasLost()) continue; // Ignore move if it is a lost move

			// Use this method for debugging and checking only. Otherwise use
			// the compacted method valueOfState(NextState, weights);
			currentFeatures = ff.computeFeaturesVector(nextState);
			currentValue = ff.valueOfState(currentFeatures, weights);
			if (currentValue > bestValue) {
				bestMove = currentMove;
				bestValue = currentValue;
				num = nextState.getRowsCleared() - s.getRowsCleared();
				bestFeatures = currentFeatures;
			}
		}
/*		nextState.copyState(s);
		currentFeatures = ff.computeFeaturesVector(nextState);
		learning(currentFeatures,bestFeatures,num);
*/		if (false) {
			printFeatures(bestFeatures);
		}
		/*
		Need to implement the learning Techinques. Otherwise, I think PlayerSkeleton is complete.
		*/

		return bestMove;
	}

	public void learning(double[] features, double[] nextStateFeatures, double num){
		// Just using the matrix operations to implement 2 formulas mentioned in LSPI handout
		double[][] tempA;
		try{
			double[][] rowNextStateFeatures = matrix.convertToRowVector(nextStateFeatures);
			rowNextStateFeatures = matrix.multiplyByConstant(rowNextStateFeatures,GAMMA);
			double[][] rowFeatures = matrix.convertToRowVector(features);
			rowNextStateFeatures = matrix.matrixSub(rowFeatures, rowNextStateFeatures);
			tempA = matrix.matrixMultplx(matrix.convertToColumnVector(features), rowNextStateFeatures);
			A = matrix.matrixAdd(A, tempA);
			b = matrix.matrixAdd(b, matrix.multiplyByConstant(matrix.convertToColumnVector(features),num));
			tempA = matrix.matrixMultplx(matrix.matrixInverse(A),b);
		}
		catch(Exception e){
			return;
		}
		weights = matrix.convertToArray(tempA);
	}
	
	public static void main(String[] args) {
		State s = new State();
		new TFrame(s);
		PlayerSkeleton p = new PlayerSkeleton();
		while(!s.hasLost()) {
			s.makeMove(p.pickMove(s,s.legalMoves()));
			/*s.draw();
			s.drawNext(0,0);
			try {
				if (false) {
					System.in.read();
				} else {
					Thread.sleep(0);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}*/
		}
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}
	
}
