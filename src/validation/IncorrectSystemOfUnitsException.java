package validation;

public class IncorrectSystemOfUnitsException extends RuntimeException {

	private static final long serialVersionUID = 206377358825605176L;

	public IncorrectSystemOfUnitsException() {
		System.out.println("Incorrect system of units for the given units");
	}

}
