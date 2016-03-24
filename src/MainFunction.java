/**
 * Created by ThanhNhan on 23/03/2016.
 */
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


    // Implementation of f1

    // Implementation of f2

    // Implementation of f3

    // Implementation of f4
    public double[] features456(State s){
    	public double[] computedValues = new double[]{0,0,0};
    	int[][] field = s.getField();
    	int[] top = s.top();
    	for (int i=0;i<field.length;i++)
    	{
    		for(int j=0;j<field[].length;j++)
    		{
    			if((field[i][j]==1)&&((field[i][j+1]==0)||(field[i][j-1]==0)))
    				computedValues[0]++;
    			if((field[i][j]==0)&&(field[i][j+1]==1))
    				computedValues[1]++;
    			if ((j==0||top[j-1]>top[j]) && (j==9||top[j+1]>top[j]))
    			{
    				if (j==0)
    					computedValues[2] += top[1]-top[0];
    				else if (j==9)
    					computedValues[2] += top[8]-top[9];
    				else
    					computedValues[2] += Math.min(top[j-1],top[j+1])-top[j];
    			}
    		}
    	} 
    	return computedValues;



    }

    // Implementation of f5

    // Implementation of f6

    // Implementation of f7

    // Implementation of f8

    // Implementation of f9

    // Implementation of f10

}
