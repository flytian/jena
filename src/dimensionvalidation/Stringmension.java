package dimensionvalidation;

/*
 * This class was created to hold both a String (a factor name) and the factor's corresponding dimension.
 * It contains only setter methods because the instance variables are public (Strings and Dimensions are 
 * immutable) so they can be easily accessed by methods in other classes that need them. 
 */
public class Stringmension {
	
	public String factorName;
	public Dimension dimension;
	
	//CONSTRUCTORS 
	public Stringmension(String nameIn, Dimension dimensionIn) {
		factorName = nameIn;
		dimension = dimensionIn;
	}
	public Stringmension() {
		factorName = null;
		dimension = new SIDimension();
	}
	
	//METHODS
	
	//sets the factorName of the object
	public void setName(String nameIn) {
		factorName = nameIn;
	}	
	
	//sets the dimension of the object
	public void setDimn(Dimension dimIn) {
		dimension = dimIn;
	}
	
	//returns a String representation of the object
	public String toString() {
		return factorName;
	}
	
}
