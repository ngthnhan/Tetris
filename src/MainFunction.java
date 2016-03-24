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

    // Feature 11 - These are basically all the 20 features mentioned in the Project Handout. 


    // Implementation of f1

    // Implementation of f2

    // Implementation of f3

    // Implementation of f4

    /*
	Also implemented features 1-20 mentioned in the handout
	Index Labels: 
	0-Column Transitions
	1-Number of Holes
	2-Well Depths(NotCumalative)
	3 to 11 - Consecutive well depths
	12 to 21 - Column Heights
	22 - Maximum column height

    */
    public void features456(State s){
    	public double[] computedValues = new double[23];
    	Arrays.fill(computedValues,0);
    	int[][] field = s.getField();
    	int[] top = s.top();
    	int maxTop=top[0];
    	int size = field[].length;
    	for (int i=0;i<field.length;i++)
    	{
    		for(int j=0;j<size;j++)
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
    	for (int j=0;j<size;j++)
    	{
    		if (j!=(size-1))
				computedValues[3+j]=top[j]-top[j+1];
			if (maxTop<top[j])
				maxTop=top[j];
			computedValues[12+j]=top[j];
    	} 
    	computedValues[22] = maxTop;
    	return ;
    }

    // Implementation of f5

    // Implementation of f6

    // Implementation of f7

    // Implementation of f8

    // Implementation of f9

    // Implementation of f10


    // Implementation of function to compute the Value of a State
    public double valueOfState()
    {
    	double value=0;
    	for (int i=0;i<computedValues.length;i++)
    	{
    		value += computedValues[i]*weights[i];
    	}
    	return value;
    }

}
