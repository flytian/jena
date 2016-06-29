package proportionvalidation;

import java.util.ArrayList;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.LiteralImpl;
import org.apache.jena.riot.RDFDataMgr;
import dimensionvalidation.*;

public class ProportionValidator {
	
	public static final String NL = System.getProperty("line.separator") ;
	public static final String PREFIXES = "PREFIX owl: <http://www.w3.org/2002/07/owl#>" + NL + 
			"PREFIX : <http://modelmeth.nist.gov/manufacturing#>"  + NL + 
			"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>";		//ontology prefixes
	public static Model manufacturingOntology = RDFDataMgr.loadModel("manufacturing.ttl");
	public static Model tempOntology = RDFDataMgr.loadModel("temp-doc-turning-triples.ttl");
	public static Model tempOntology2 = RDFDataMgr.loadModel("temp-doc-turning-triples.ttl");
	
	//main method
	public static void main(String[] args) {
		Boolean correctProportionality=false;
		
		Identifier proportionality = new Identifier(tempOntology, manufacturingOntology, PREFIXES);	
		TripleList directlyProportional = proportionality.createTriple("isDirectlyProportionalTo");
		TripleList indirectlyProportional = proportionality.createTriple("isIndirectlyProportionalTo");
		ArrayList<String> variables = findVariables(tempOntology);
		System.out.println(printArray(variables));
		ArrayList<String> otherVars = findVariables(tempOntology2);
		System.out.println(printArray(otherVars));
		ArrayList<Integer> numericalValues = findValues(tempOntology);
		System.out.println(printArray(numericalValues));
		ArrayList<Integer> otherNumericalValues = findValues(tempOntology2);
		System.out.println(printArray(otherNumericalValues));
		
		if(numericalValues.size() != otherNumericalValues.size()) {
			throw new UncomparableDataException();
		}
		
		/*
		 * PLAN
		 * The problem: inconsistent naming, which we cannot on rely on etc.
		 * The solution: utilize the ":hasConcept" property and use those names to somehow evaluate proportionality
		 * The difficulty: this involves extracting the useful names from lists of :hasConcept objects (ie; choosing 
		 * CuttingSpeed but ignoring MaximumAcceptableValue), finding their proportionality relationships, then
		 * going back into the temporary ontology to find where the objects appear
		 */
		
		for(int i=0; i<directlyProportional.getSubject().size(); i++) {
			if (numericalValues.get(variables.indexOf(directlyProportional.getSubject().get(i))) >= 
			otherNumericalValues.get(otherVars.indexOf(directlyProportional.getSubject().get(i))) &&
			numericalValues.get(variables.indexOf(directlyProportional.getObject().get(i))) >= 
			otherNumericalValues.get(otherVars.indexOf(directlyProportional.getObject().get(i)))) {
				correctProportionality=true;
			} else if (numericalValues.get(variables.indexOf(directlyProportional.getSubject().get(i))) <= 
			otherNumericalValues.get(otherVars.indexOf(directlyProportional.getSubject().get(i))) &&
			numericalValues.get(variables.indexOf(directlyProportional.getObject().get(i))) <= 
			otherNumericalValues.get(otherVars.indexOf(directlyProportional.getObject().get(i)))) {
				correctProportionality=true;
			}
		}

		for(int j=0; j<indirectlyProportional.getSubject().size(); j++) {
			if (numericalValues.get(variables.indexOf(directlyProportional.getSubject().get(j))) >= 
			otherNumericalValues.get(otherVars.indexOf(directlyProportional.getSubject().get(j))) &&
			numericalValues.get(variables.indexOf(directlyProportional.getObject().get(j))) <= 
			otherNumericalValues.get(otherVars.indexOf(directlyProportional.getObject().get(j)))) {
				correctProportionality=true;
			} else if (numericalValues.get(variables.indexOf(directlyProportional.getSubject().get(j))) <= 
			otherNumericalValues.get(otherVars.indexOf(directlyProportional.getSubject().get(j))) &&
			numericalValues.get(variables.indexOf(directlyProportional.getObject().get(j))) >= 
			otherNumericalValues.get(otherVars.indexOf(directlyProportional.getObject().get(j)))) {
				correctProportionality=true;
			}
		}
		
		System.out.println("It is " + correctProportionality 
				+ " that proportionality in this predictive model is consistent with the ontology");
	}

	//finds assigned values from notebook and puts them in an ArrayList<Integer>
	public static ArrayList<Integer> findValues(Model notebookTriples) {

		ArrayList<Integer> values = new ArrayList<Integer>();

		String queryString =   PREFIXES + NL +
				"SELECT ?value" + NL +
				"WHERE { \n ?subject :hasNumericalValue ?value }";

		Query query = QueryFactory.create(queryString, Syntax.syntaxSPARQL) ; 

		QueryExecution qexec = QueryExecutionFactory.create(query, notebookTriples) ;
		ResultSet rs = qexec.execSelect();
		for ( ; rs.hasNext() ; ){
			QuerySolution rb = rs.nextSolution();
			LiteralImpl x = (LiteralImpl) rb.getLiteral("value");
			values.add(x.getInt());
		}
		qexec.close();

		return values;
	}
	public static ArrayList<String> findVariables(Model notebookTriples) {

		ArrayList<String> variables = new ArrayList<String>();

		String queryString =   PREFIXES + NL +
				"SELECT ?subject" + NL +
				"WHERE { \n ?subject :hasNumericalValue ?value }";

		Query query = QueryFactory.create(queryString, Syntax.syntaxSPARQL) ; 

		QueryExecution qexec = QueryExecutionFactory.create(query, notebookTriples) ;
		ResultSet rs = qexec.execSelect();
		for ( ; rs.hasNext() ; ){
			QuerySolution rb = rs.nextSolution();
			Resource x = (Resource) rb.getResource("subject");
			variables.add(x.getLocalName());
		}
		qexec.close();

		return variables;
	}
	public static String printArray(ArrayList input){
		String results = "";
		for(int i=0; i<input.size(); i++) {
			results += input.get(i).toString() + ", ";
		}
		return results;
	}
}
