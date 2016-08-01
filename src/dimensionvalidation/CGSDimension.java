package dimensionvalidation;

import java.util.ArrayList;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Resource;

/*
 * Used to store dimension vectors of equations when using the Centimeter-Gram-Second system of units. 
 * There are five instance variables: one that stores the name of the units creating the dimension
 * vector and four to hold each component of the vector. The four vector components are length, mass,
 * time, and a dimension-less component.
 * 
 * There are methods in place to set and get each instance variable, as well as a toString() method.
 * Additionally, there are methods to multiply, divide, and determine equality between CGSDimension 
 * objects by looking at the values of the instance variables.
 */

public class CGSDimension implements Dimension{
	private String dimensionUnits; //name of units
	
	private int L; //length
	private int M; //mass
	private int T; //time
	private int U; //dimension-less 
	
	//CONSTRUCTORS
	public CGSDimension() {
		dimensionUnits = null;
	}
	
	/*
	 * Uses SPARQL queries to determine the dimension vector of a unit. 
	 * The parameter dimensionIn should be the name of a unit found in 
	 * the CGS system of units.
	 */
	public CGSDimension(String dimensionIn) {
		dimensionUnits = dimensionIn;
		String quantityString=null;
		String quantityKind=null;
		
		//check that the system of units matches the parameter units
		String checkSystem = prefixes + NL + 
				"ASK { Unit:" + dimensionUnits + "rdf:type qudt:NotUsedWithSIUnits } ";
		Query systemQuery = QueryFactory.create(checkSystem, Syntax.syntaxSPARQL);
		QueryExecution systemqexec = QueryExecutionFactory.create(systemQuery, unitsOntology);
		Boolean nonSIsystem = systemqexec.execAsk();
		systemqexec.close();
		if(nonSIsystem == true) {
			throw new IncorrectSystemOfUnitsException();
		}
		
		//find the quantity kind (i.e; Area) corresponding to the dimensionUnits
		String findQuantityKind = prefixes + NL +
				"SELECT ?quantity " + NL +
				"WHERE { \n"
				+ "unit:" + dimensionUnits + " rdf:type ?quantity \n } \n "
				+ "MINUS { \n"
				+ "unit:" + dimensionUnits + " rdf:type qudt:NotUsedWithSIUnit; qudt:DerivedUnit; qudt:SIBaseUnit; "
				+ "qudt:NonSIUnit; qudt:SIDerivedUnit ."
				+ "\n }";
		Query quantityQuery = QueryFactory.create(findQuantityKind, Syntax.syntaxSPARQL);
		QueryExecution queryExec = QueryExecutionFactory.create(quantityQuery, dimensionsOntology);
		ResultSet rs = queryExec.execSelect();
		for (; rs.hasNext() ; ) {				
			QuerySolution rb = rs.nextSolution();
			Resource quantity = (Resource) rb.getResource("quantity"); 
			quantityString = quantity.getLocalName(); //output of form BlahBlahUnit
		}
		queryExec.close();
		
		//format the quantity kind String to allow for querying 	
		quantityKind = quantityString.replaceAll("Unit", "");
		
		//determine the dimension vector
		switch (quantityKind)
		{
		//if it's a base unit
		case "Length" :
			L=1;
			break;
		case "Mass" : 
			M=1;
			break;
		case "Time" :
			T=1;
			break;
		case "Dimensionless" :
			U=1;
			break;
			
		//not a base unit
		default :

			ArrayList<String> results = new ArrayList<String>(); //holds list of base unit vectors
			
			//querying for qudt:dimensionVector results
			String queryString = prefixes + NL +
					"SELECT ?vector " + NL +
					"WHERE { ?dimension qudt:referenceQuantity qudt-quantity:" + quantityKind + " ; \n "
					+ "rdfs:label ?label ; \n"
					+ "qudt:dimensionVector ?vector ; \n"
					+ "qudt:symbol ?symbol . \n"
					+ " FILTER regex(?label, \"CGS\") \n"
					+ "}";
			Query query = QueryFactory.create(queryString, Syntax.syntaxSPARQL);
			QueryExecution qexec = QueryExecutionFactory.create(query, dimensionsOntology);
			ResultSet length = qexec.execSelect();
			for (; length.hasNext() ; ) {				
				QuerySolution rb = length.nextSolution();
				Resource vector = (Resource) rb.getResource("vector"); 	
				results.add(vector.getLocalName());			
			}
			qexec.close();
			
			//iterate through the results ArrayList and assign each element to its corresponding
			//instance variable
			for (int i=0; i<results.size(); i++) {
				if(results.get(i).indexOf('L')!= -1) {
					if(results.get(i).indexOf('-')!= -1) {
						L = - Character.getNumericValue(results.get(i).charAt(9));
					} else {
						L = Character.getNumericValue(results.get(i).charAt(8));
					}
				} else if (results.get(i).indexOf('M')!= -1) {
					if(results.get(i).indexOf('-')!= -1) {
						M = - Character.getNumericValue(results.get(i).charAt(9));
					} else {
						M = Character.getNumericValue(results.get(i).charAt(8));
					}
				}else if (results.get(i).indexOf('T')!= -1) {
					if(results.get(i).indexOf('-')!= -1) {
						T = - Character.getNumericValue(results.get(i).charAt(9));
					} else {
						T = Character.getNumericValue(results.get(i).charAt(8));
					}
				}else if (results.get(i).indexOf('U')!= -1) {
					if(results.get(i).indexOf('-')!= -1) {
						U = - Character.getNumericValue(results.get(i).charAt(9));
					} else {
						U = Character.getNumericValue(results.get(i).charAt(8));
					}
				}

			}
		}

	}
	
	//METHODS
	
	/*
	 * returns a String representation of the dimension vector
	 */
	public String toString() {
		return dimensionUnits + " (M^" + M + " L^" + L + " T^" + T + " U^" + U + ")";
	}
	
	//getters
	public String getDimensionUnits() {
		return dimensionUnits;
	}
	public String getVector() {
		return "M^" + M + " L^" + L + " T^" + T + " U^" + U;
	}
	public int getM() {
		return M;
	}
	public int getL() {
		return L;
	}
	public int getT() {
		return T;
	}
	public int getU() {
		return U;
	}
	
	//setters
	public void setDimensionUnits(String nameIn) {
		dimensionUnits = nameIn;
	}
	public void setM(int dimensionIn) {
		M = dimensionIn;
	}
	public void setL(int dimensionIn) {
		L = dimensionIn;
	}
	public void setT(int dimensionIn) {
		T = dimensionIn;
	}
	public void setU(int dimensionIn) {
		U = dimensionIn;
	}
	
	/*
	 * This method compares the current object to another object to determine
	 * if the two have equivalent dimension vectors. It does this by first
	 * comparing the dimensionUnits string, and then comparing each vector
	 * component individually. It returns a boolean value of either true or false.
	 */
	public boolean equals(Object otherDimension) {
		if(!(otherDimension instanceof CGSDimension)) {
			return false;
		}
		CGSDimension otherDimensionCast = (CGSDimension) otherDimension;
		
		if(dimensionUnits.equals(otherDimensionCast.getDimensionUnits())) {
			return true;
		} else if(M!=otherDimensionCast.getM()) {
			return false;
			
		}else if(L!=otherDimensionCast.getL()) {
			return false;
			
		}else if(T!=otherDimensionCast.getT()) {
			return false;
			
		}else if(U!=otherDimensionCast.getU()) {
			return false;
			
		}else {
			return true;
		}
	}
	
	/*
	 * This method multiplies two CGSDimension objects by combining the dimensionUnits
	 * strings and adding the vector component values of the two objects together to create
	 * new vector component values. For example, it will add the values of the length
	 * components together. It returns a new CGSDimension object.
	 */
	public Dimension multiply(Dimension otherDimension) {
		CGSDimension otherCopy = (CGSDimension) otherDimension;

		CGSDimension newDimension = new CGSDimension();
		
		newDimension.setDimensionUnits(dimensionUnits + "*" + otherDimension.getDimensionUnits());
		newDimension.setM(M + otherCopy.getM());
		newDimension.setL(L + otherCopy.getL());
		newDimension.setT(T + otherCopy.getT());
		newDimension.setU(U + otherCopy.getU());
		
		return newDimension;
	}
	
	/*
	 * This method divides two CGSDimension objects by combining the dimensionUnits
	 * strings and subtracting the vector component values of the two objects to create
	 * new vector component values. The current object is divided by the parameter.
	 * For example, it will subtract the value of the parameter object's length component
	 * from the value of the current object's length component. It returns a new CGSDimension object.
	 */
	public Dimension divide(Dimension otherDimension) {
		CGSDimension otherCopy = (CGSDimension) otherDimension;

		CGSDimension newDimension = new CGSDimension();
		
		newDimension.setDimensionUnits(dimensionUnits + "/" + otherDimension.getDimensionUnits());
		newDimension.setM(M - otherCopy.getM());
		newDimension.setL(L - otherCopy.getL());
		newDimension.setT(T - otherCopy.getT());
		newDimension.setU(U - otherCopy.getU());
		
		return newDimension;
	}
}
