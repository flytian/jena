package dimensionvalidation;

public class UndeterminedDimensionException extends RuntimeException {
	
	/**
	 * This exception is thrown when the dimension of an element cannot be determined
	 * due to a variable exponent. When there is a variable exponent in an equation, the 
	 * dimension vector is undetermined (i.e; if there is a factor x^alpha, dimensionality
	 * is different when alpha=2 than if alpha=3)
	 */
	private static final long serialVersionUID = 8881369696156308450L;

	public UndeterminedDimensionException() {
		System.out.println("UndeterminedDimensionException");
	}
}
