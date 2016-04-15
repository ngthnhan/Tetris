/**
 * Created by nhan on 15/4/16.
 */
public class PolicyResult {
    public double[] weights;
    public double[][] weightsV;
    public double value;

    public PolicyResult(double[] weights, double value) {
        this.weights = weights;
        this.weightsV = matrix.convertToColumnVector(weights);
        this.value = value;
    }
}
