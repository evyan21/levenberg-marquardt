import org.opensourcephysics.numerics.MultiVarFunction;

public class ObjectiveFunction implements MultiVarFunction {
    private double[] x1;
    private double[] T;
    private double[] G;

    private int length;
    private int n;

    public ObjectiveFunction(double[] x1, double[] T, double[] G, int n) {
        if(x1.length != T.length && x1.length != G.length)
            throw new IllegalArgumentException("ObjectiveFunction requires array of equal length.", null);

        this.x1 = x1;
        this.T = T;
        this.G = G;
        
        length = x1.length;
        this.n = n;
    }
    
    public double evaluate(double[] parameters) {
        // return sum_i difference of squares
        double result = 0;

        for (int i = 0; i < length; i++) {
            // function goes here
           	double tempG = new RegressionFunction(parameters, n, T[i]).evaluate(x1[i]);

            // get residual squared and add to sum
            result += Math.pow(G[i] - tempG, 2);
        }

        return result;
    }
}