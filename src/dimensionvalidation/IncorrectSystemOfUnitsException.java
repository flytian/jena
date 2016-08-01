package dimensionvalidation;

public class IncorrectSystemOfUnitsException extends RuntimeException {

	/**
	 * This exception is thrown when the units of a variable don't match the 
	 * asserted system of units (i.e; when a variable is given in feet but the
	 * system of units is SI).
	 */
	
	private static final long serialVersionUID = 206377358825605176L;

	public IncorrectSystemOfUnitsException() {
		System.out.println("Incorrect system of units for the given units");
	}

}
