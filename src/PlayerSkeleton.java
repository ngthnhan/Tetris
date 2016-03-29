import javax.xml.parsers.FactoryConfigurationError;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

public class PlayerSkeleton {

	//implement this function to have a working system
	public NextState nextState ;
	public FeatureFunction ff;
	private double[] weights;
	private final double MIN_VAL = Double.NEGATIVE_INFINITY;
	private final boolean DEBUG = true;

	public PlayerSkeleton(){
		nextState = new NextState();
		ff = new FeatureFunction();
		weights = readWeights("final_weights.txt");
	}

	public double[] readWeights(String fileName) {
		double[] w = new double[FeatureFunction.NUM_OF_FEATURE];
		try (Scanner sc = new Scanner(new FileReader(fileName))) {
			int i = 0;
			while (sc.hasNextDouble()) {
				w[i++] = sc.nextDouble();
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

			if (nextState.hasLost()) continue; // Ignore move if it is a lost move

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
		/*
		Need to implement the learning Techinques. Otherwise, I think PlayerSkeleton is complete.
		*/

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
				if (p.DEBUG) {
					System.in.read();
				} else {
					Thread.sleep(300);
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
