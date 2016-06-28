package validation;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;

public interface Dimension {
	public static final Model unitsOntology = RDFDataMgr.loadModel("OVG_units-qudt-(v1.1).ttl");
	public static final Model dimensionsOntology = RDFDataMgr.loadModel("OVG_dimensions-qudt-(v1.1).ttl");
	
	public static final String NL = System.getProperty("line.separator") ;
	public static final String prefixes = "PREFIX owl: <http://www.w3.org/2002/07/owl#>" + NL + 
			"PREFIX qudt:    <http://qudt.org/schema/qudt#> "  + NL
			+ "PREFIX qudt-1.1:  <http://qudt.org/1.1/schema/qudt#> " + NL
			+ "PREFIX qudt-dimension:  <http://qudt.org/vocab/dimension#> " + NL
			+ "PREFIX qudt-dimension-1.1:  <http://qudt.org/1.1/vocab/dimension#> " + NL
			+ "PREFIX qudt-quantity:  <http://qudt.org/vocab/quantity#> " + NL
			+ "PREFIX qudt-unit:  <http://qudt.org/vocab/unit#> " + NL
			+ "PREFIX rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" + NL
			+ "PREFIX rdfs:    <http://www.w3.org/2000/01/rdf-schema#> ";
	
	public String toString();
	public String getDimensionUnits();
	public String getVector();
	public Dimension multiply(Dimension d);
	public boolean equals(Object o);
	public void setDimensionUnits(String s);
	
}
