package validation;

public class UnmatchedLeftAndRightException extends RuntimeException{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7939131904942370179L;

	public UnmatchedLeftAndRightException() {		
		System.out.println("The number of left hand sides of relations does not match "
				+ "the number of right hand sides of relations.");
	}
	
}
