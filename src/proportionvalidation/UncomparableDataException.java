package proportionvalidation;

public class UncomparableDataException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9190193054703120332L;
	
	public UncomparableDataException() {
		System.out.println("Unequal number of data points from data sets being compared");
	}

}
