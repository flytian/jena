/*
 * This interface defines all the methods required for dimension classes. 
 * It also contains the Models and prefixes required for queries used when constructing 
 * a dimension object, which are the same across all implementing classes.
 */
package dimensionvalidation;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;

public interface Dimension {
	public static final Model unitsOntology = RDFDataMgr.loadModel("OVG_units-qudt-(v1.1).ttl"); //used to find quantity kind of units
	public static final Model dimensionsOntology = RDFDataMgr.loadModel("OVG_dimensions-qudt-(v1.1).ttl"); //used to find dimension vector of quantity kind
	
	public static final String NL = System.getProperty("line.separator") ;
	public static final String prefixes = "PREFIX owl: <http://www.w3.org/2002/07/owl#>" + NL + 
			"PREFIX qudt:    <http://qudt.org/schema/qudt#> "  + NL
			+ "PREFIX qudt-1.1:  <http://qudt.org/1.1/schema/qudt#> " + NL
			+ "PREFIX qudt-dimension:  <http://qudt.org/vocab/dimension#> " + NL
			+ "PREFIX qudt-dimension-1.1:  <http://qudt.org/1.1/vocab/dimension#> " + NL
			+ "PREFIX qudt-quantity:  <http://qudt.org/vocab/quantity#> " + NL
			+ "PREFIX qudt-unit:  <http://qudt.org/vocab/unit#> " + NL
			+ "PREFIX rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" + NL
			+ "PREFIX rdfs:    <http://www.w3.org/2000/01/rdf-schema#>" + NL
			+ "PREFIX unit: <http://qudt.org/vocab/unit#>";
	
	public String toString();
	public String getDimensionUnits();
	public void setDimensionUnits(String s);
	public String getVector();
	
	public Dimension multiply(Dimension d);
	public Dimension divide(Dimension d);
	public boolean equals(Object o);
	
}
