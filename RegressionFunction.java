import org.opensourcephysics.numerics.Function;

public class RegressionFunction implements Function {
        private double[] parameters;
        private int n;
        private double T;

        public RegressionFunction(double[] parameters, int n, double T) {
            this.parameters = parameters;
            this.n = n;
            this.T = T;
        }

        @Override
        public double evaluate(double x) {
            double tempG = 0;

		    if (x == 0.0) {
		        tempG = 8.314 * T * (1 - x) * Math.log(1 - x);
		    }
		    else if(x == 1.0) {
		    	tempG = 8.314 * T * x * Math.log(x);
		    }
		    else{
		    	tempG = 8.314 * T * (x * Math.log(x) + (1 - x) * Math.log(1 - x));
		    }

            double sum = 0;

            switch(n) {
                // subsubsuboptimal
                case 3:
                    sum += (parameters[6] + parameters[7] * T) * Math.pow(2 * x - 1, 3);

                // subsuboptimal
                case 2:
                    sum += (parameters[4] + parameters[5] * T) * Math.pow(2 * x - 1, 2);

                // suboptimal
                case 1:
                    sum += (parameters[2] + parameters[3] * T) * (2 * x - 1);

                // optimal
                case 0:
                    sum += (parameters[0] + parameters[1] * T);
            }
            
           	tempG += x * (1 - x) * (sum);

            return tempG;
        }
    }