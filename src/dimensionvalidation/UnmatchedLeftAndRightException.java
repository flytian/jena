package dimensionvalidation;

public class UnmatchedLeftAndRightException extends RuntimeException{
	
	/**
	 * This exception is thrown when the RDF graph of the notebook LaTeX equations contains equations that are not
	 * perfectly split into LHS and RHS (i.e; expressions, etc.) and therefore cannot be checked for dimensional 
	 * consistency.
	 */
	private static final long serialVersionUID = -7939131904942370179L;

	public UnmatchedLeftAndRightException() {		
		System.out.println("The number of left hand sides of relations does not match "
				+ "the number of right hand sides of relations.");
	}
	
}
