
public class PlayerSkeleton {

	//implement this function to have a working system
	public State nextState ;
	public FeatureFunction func = new FeatureFunction();
	double[] computedValues;
	double[] weights;
	double minval;

	public PlayerSkeleton(){
		nextState = new State();
		minval = Double.NEGATIVE_INFINITY;
		computedValues = new double[31];
		Arrays.fill(computedValues,0);
		weights = new double[31];
	}

	public int pickMove(State s, int[][] legalMoves) {
		
		int bestMove=0, currentMove;
		double currentValue,maxValue=minval;

		for (currentMove = 0; currentMove != legalMoves.length; currentMove++)
		{
			int[][] field = s.getField();
			Arrays.fill(nextState.top,0);
			for(int i=field.length-1;i>=0;i--) 
			{
				System.arraycopy(field[i], 0, nextState.field[i], 0, field[i].length);
				for(int j=0;j<top.length;j++) 
				{
					if(top[j]==0 && field[i][j]>0)
					{ 
						top[j]=i+1;
					}
				}
			}
			nextState.nextPiece = s.getNextPiece();
			nextState.lost = s.hasLost();
			nextState.cleared = s.getRowsCleared();
			nextState.turn = s.getTurnNumber();
			// In the Current state now

			nextState.makeMove(currentMove);
			if (!nextState.hasLost())
			{
				computeAllFeatures(nextState);
				currentValue = valueOfState(nextState);
				if (currentValue>=maxValue)
				{
					maxValue = currentValue;
					bestMove = currentMove;
				}
			}

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
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}
	
}
