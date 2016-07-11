package dimensionvalidation;

public class Stringmension {
	
	public String factorName;
	public Dimension dimension;
	
	public Stringmension(String nameIn, Dimension dimensionIn) {
		factorName = nameIn;
		dimension = dimensionIn;
	}
	public Stringmension() {
		factorName = null;
		dimension = new SIDimension();
	}
	
	public void setName(String nameIn) {
		factorName = nameIn;
	}	
	public void setDimn(Dimension dimIn) {
		dimension = dimIn;
	}
	
	public String toString() {
		return factorName;
	}
	
}
