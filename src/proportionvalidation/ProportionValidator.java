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
		
		Identifier proportionality = new Identifier(manufacturingOntology, PREFIXES);	
		TripleList directlyProportional = proportionality.createTriple("isDirectlyProportionalTo");
		TripleList indirectlyProportional = proportionality.createTriple("isIndirectlyProportionalTo");
		//ArrayList<String> variables = findVariables(tempOntology);
		//ArrayList<String> otherVars = findVariables(tempOntology2);
		//ArrayList<Double> numericalValues = findValues(tempOntology);
		//ArrayList<Double> otherNumericalValues = findValues(tempOntology2);
		
		ArrayList<Double> proportionSub1 = new ArrayList<Double>();
		ArrayList<Double> proportionSub2 = new ArrayList<Double>();
		ArrayList<Double> proportionOb1 = new ArrayList<Double>();
		ArrayList<Double> proportionOb2 = new ArrayList<Double>();
		
		String subject = "";
		String object = "";
		
		for(int i=0; i<directlyProportional.getSubject().size(); i++) {
			String queryString =  PREFIXES + NL +
					"SELECT ?characteristic" + NL +
					"WHERE {\n "
					+ "?characteristic :hasDescription :" + directlyProportional.getSubject().get(i) + " \n} ";
			Query query = QueryFactory.create(queryString) ;
			QueryExecution qexec = QueryExecutionFactory.create(query, manufacturingOntology) ;
			ResultSet result = qexec.execSelect();
			for ( ; result.hasNext() ;) {
				QuerySolution rb = result.nextSolution() ;
				Resource x = (Resource) rb.getResource("characteristic");
				subject = x.getLocalName();
			}		
			object = directlyProportional.getObject().get(i);
			System.out.println(subject);
			System.out.println(object);
			String queryString02 =  PREFIXES + NL +
					"SELECT ?variable1 ?variable2" + NL +
					"WHERE {\n "
					+ "?variable1 :hasConcept :"+ subject +" . \n "
					+ " ?variable2 :hasConcept :" + object +" . \n } ";
			Query query02 = QueryFactory.create(queryString02) ;
			QueryExecution qexec02 = QueryExecutionFactory.create(query02, tempOntology) ;
			ResultSet result02 = qexec02.execSelect();
			for ( ; result02.hasNext() ; ){
				QuerySolution rb = result02.nextSolution() ;
				Resource x = (Resource) rb.getResource("variable1");
				String var1 = x.getLocalName();
				Resource y = (Resource) rb.getResource("variable2");
				String var2 = y.getLocalName();
				
				//get values of variables for first ontology
				String queryString03 =  PREFIXES + NL +
						"SELECT ?value1 ?value2" + NL +
						"WHERE {\n :" + var1 +" :hasNumericalValue ?value1 .\n "
								+ " :" + var2 +" :hasNumericalValue ?value2 .\n } ";
				Query query03 = QueryFactory.create(queryString03) ;
				QueryExecution qexec03 = QueryExecutionFactory.create(query03, tempOntology) ;
				ResultSet result03 = qexec03.execSelect();
				for ( ; result03.hasNext() ; ){
					QuerySolution rb03 = result03.nextSolution() ;
					LiteralImpl z = (LiteralImpl) rb03.getLiteral("value1");
					LiteralImpl w = (LiteralImpl) rb03.getLiteral("value2");
					proportionSub1.add(z.getDouble());
					proportionOb1.add(w.getDouble());			
				}
				qexec03.close();
				
				//get values of variables for second ontology
				String queryString04 =  PREFIXES + NL +
						"SELECT ?value1 ?value2" + NL +
						"WHERE {\n :" + var1 +" :hasNumericalValue ?value1 .\n "
								+ " :" + var2 +" :hasNumericalValue ?value2 .\n } ";
				Query query04 = QueryFactory.create(queryString04) ;
				QueryExecution qexec04 = QueryExecutionFactory.create(query04, tempOntology2) ;
				ResultSet result04 = qexec04.execSelect();
				for ( ; result04.hasNext() ; ){
					QuerySolution rb04 = result.nextSolution() ;
					LiteralImpl z = (LiteralImpl) rb04.getLiteral("value1");
					LiteralImpl w = (LiteralImpl) rb04.getLiteral("value2");
					proportionSub2.add(z.getDouble());
					proportionOb2.add(w.getDouble());
				}
				qexec04.close();
			}
			qexec02.close();			
			
			if(proportionSub1.size() != proportionSub2.size() || proportionSub1.size() != proportionOb1.size() || 
					proportionSub2.size() != proportionOb2.size()) {
				throw new UncomparableDataException();
			}
			
			System.out.println(printArray(proportionSub1));
			System.out.println(printArray(proportionOb1));
			System.out.println(printArray(proportionSub2));
			System.out.println(printArray(proportionOb2));
			
			if (proportionSub1.get(i) >= proportionSub2.get(i) && proportionOb1.get(i) >= proportionOb2.get(i)) {				
				correctProportionality=true;				
			} else if (proportionSub1.get(i) <= proportionSub2.get(i) && proportionOb1.get(i) <= proportionOb2.get(i)) {				
				correctProportionality=true;				
			}
			
		}
		/*
		for(int j=0; j<indirectlyProportional.getSubject().size(); j++) {
			if (proportionSub1.get(j) >= proportionSub2.get(j) && proportionOb1.get(j) <= proportionOb2.get(j)) {
				correctProportionality=true;
			} else if (proportionSub1.get(j) <= proportionSub2.get(j) && proportionOb1.get(j) >= proportionOb2.get(j)) {
				correctProportionality=true;
			}
		}
		*/
		
		System.out.println("It is " + correctProportionality 
				+ " that proportionality in this predictive model is consistent with the ontology");
	}

	//finds assigned values from notebook and puts them in an ArrayList<Integer>
	public static ArrayList<Double> findValues(Model notebookTriples) {

		ArrayList<Double> values = new ArrayList<Double>();

		String queryString =   PREFIXES + NL +
				"SELECT ?value" + NL +
				"WHERE { \n ?subject :hasNumericalValue ?value }";

		Query query = QueryFactory.create(queryString, Syntax.syntaxSPARQL) ; 

		QueryExecution qexec = QueryExecutionFactory.create(query, notebookTriples) ;
		ResultSet rs = qexec.execSelect();
		for ( ; rs.hasNext() ; ){
			QuerySolution rb = rs.nextSolution();
			LiteralImpl x = (LiteralImpl) rb.getLiteral("value");
			values.add(x.getDouble());
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
