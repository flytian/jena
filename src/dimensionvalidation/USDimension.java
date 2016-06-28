package dimensionvalidation;

import java.util.ArrayList;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Resource;

public class USDimension implements Dimension{
	private String dimensionUnits; //name of units
	
	private int L; //length
	private int M; //mass
	private int T; //time
	private int theta; //thermodynamic temperature
	private int U; //dimension-less 
	
	public USDimension() {
		dimensionUnits = null;
	}
	
	public USDimension(String dimensionIn) {
		dimensionUnits = dimensionIn;
		
		dimensionUnits = dimensionIn;
		String quantityString=null;
		String quantityKind=null;
		
		String checkSystem = prefixes + NL + 
				"ASK { Unit:" + dimensionUnits + "rdf:type qudt:NotUsedWithSIUnits } ";
		Query systemQuery = QueryFactory.create(checkSystem, Syntax.syntaxSPARQL);
		QueryExecution systemqexec = QueryExecutionFactory.create(systemQuery, unitsOntology);
		Boolean nonSIsystem = systemqexec.execAsk();
		systemqexec.close();
		
		if(nonSIsystem == false) {
			throw new IncorrectSystemOfUnitsException();
		}
		
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
			quantityString = quantity.getLocalName();
		}
		queryExec.close();
		quantityString = quantityString.substring(5);	
		quantityKind = quantityString.replaceAll("Unit", "");
		
		switch (quantityKind)
		{
		case "Length" :
			L=1;
			break;
		case "Mass" : 
			M=1;
			break;
		case "Time" :
			T=1;
			break;
		case "Temperature" :
			theta=1;
			break;
		case "Dimensionless" :
			U=1;
			break;
		default :
		
		ArrayList<String> results = new ArrayList<String>(); 
		
		String queryString = prefixes + NL +
				"SELECT ?vector " + NL +
				"WHERE { ?dimension qudt:referenceQuantity qudt-quantity:" + quantityKind + " ; \n "
				+ "rdfs:label ?label ; \n"
				+ "qudt:dimensionVector ?vector ; \n"
				+ "qudt:symbol ?symbol . \n"
				+ " FILTER regex(?label, \"US\") \n"
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
			} else {
				if(results.get(i).indexOf('-')!= -1) {
					theta = - Character.getNumericValue(results.get(i).charAt(9));
				} else {
					theta = Character.getNumericValue(results.get(i).charAt(8));
				}
			}
		}
		}
		
		
		
	}
	
	public String toString() {
		return dimensionUnits + " (M^" + M + " L^" + L + " T^" + T 
				+ " theta^" + theta + " U^" + U+ ")";
	}
	
	public String getDimensionUnits() {
		return dimensionUnits;
	}
	public String getVector() {
		return "M^" + M + " L^" + L + " T^" + T + " theta^" + theta  + " U^" + U;
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
	public int getTheta() {
		return theta;
	}
	public int getU() {
		return U;
	}
	
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
	public void setTheta(int dimensionIn) {
		theta = dimensionIn;
	}
	public void setU(int dimensionIn) {
		U = dimensionIn;
	}
	
	public boolean equals(Object otherDimension) {
		if(!(otherDimension instanceof USDimension)) {
			return false;
		}
		USDimension otherDimensionCast = (USDimension) otherDimension;
		
		if(dimensionUnits.equals(otherDimensionCast.getDimensionUnits())) {
			return true;
		} else if(M!=otherDimensionCast.getM()) {
			return false;			
		}else if(L!=otherDimensionCast.getL()) {
			return false;			
		}else if(T!=otherDimensionCast.getT()) {
			return false;			
		}else if(theta!=otherDimensionCast.getTheta()) {
			return false;			
		}else if(U!=otherDimensionCast.getU()) {
			return false;			
		}else {
			return true;
		}
	}
	
	public Dimension multiply(Dimension otherDimension) {
		USDimension otherCopy = (USDimension) otherDimension;
		
		USDimension newDimension = new USDimension();
		
		newDimension.setDimensionUnits(dimensionUnits + "*" + otherDimension.getDimensionUnits());		
		newDimension.setM(M + otherCopy.getM());
		newDimension.setL(L + otherCopy.getL());
		newDimension.setT(T + otherCopy.getT());
		newDimension.setTheta(theta + otherCopy.getTheta());
		newDimension.setU(U + otherCopy.getU());
		
		return newDimension;
	}
}
