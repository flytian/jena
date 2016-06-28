package validation;

import java.util.*;

import org.apache.jena.datatypes.xsd.*;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.LiteralImpl;
//import org.apache.jena.atlas.io.IndentedWriter; //----> Needed to format the printed SPARQL query


public class Identifier {
	public static final String NL = System.getProperty("line.separator") ;	//makes notation easier
	public Model ontologyModel;
	private Model myModel;	//the model that is being parsed for triples
	private String prefixes; //list of any and all prefixes used in ontology ----> write a method to parse this?
	
	//constructor
	public Identifier() {
		myModel = ModelFactory.createDefaultModel();
		prefixes = null;
		ontologyModel = ModelFactory.createDefaultModel();
	}
	
	//constructor
	public Identifier(Model equationModel, Model ontologyModelIn, String prefixesIn){
		myModel = equationModel;
		prefixes = prefixesIn;
		ontologyModel = ontologyModelIn;
	}
	
	//get prefix String
	public String getPrefixes(){
		return prefixes;
	}
	
	//get Model myModel
	public Model getModel(){
		Model copyModel = ModelFactory.createModelForGraph(myModel.getGraph());
		return copyModel;
	}
	
	//set prefixes for the Identifier
	public void setPrefixes(String prefixesIn) {
		prefixes = prefixesIn;
	}
	
	//find the dimensions of one side of an equation given the side and the equation URI
	public Dimension findDimensionality(String equationURI, String equationSide) {
		return findDimensionality(equationURI, equationSide, "SI");
	}
	
	//find the dimension vector of one side of an equation
	public Dimension findDimensionality(String equationURI, String equationSide, String systemOfUnits) {
		Dimension finalDimension;	
		switch (systemOfUnits) {
		case "US" :
			finalDimension = new USDimension();
			break;
		case "Planck":
			finalDimension = new PlanckDimension();
			break;
		case "CGS" :
			finalDimension=new CGSDimension();
			break;
		default :
			finalDimension = new SIDimension();	
		}
		
		Dimension dimension; //temporary variable
		
		ArrayList<Dimension> factorsToMultiply = new ArrayList<Dimension>(); //list of dimensions that go into finalDimension
		ArrayList<String> factors = new ArrayList<String>(); //list of factors of each side of the equation
		
		String nameQuery = ":" +equationURI+" :"+equationSide + " _:d . \n "
				+ " _:d :hasTerm _:a . \n" + 
				" _:a :hasFactor _:b . \n " + 
				" _:b :hasVar _:c . \n "
				+ "_:c :hasName ?name . \n";
		String subQuery = "_:c :hasSubscript _:e . \n"
				+ "_:e :hasExpression ?subscript . \n";
		String supQuery =  "_:c :hasSuperscript _:f . \n "
				+ "_:f :hasExpression ?superscript . \n ";
		
		//query to create list of names of factors in equation
		String queryString03 =   prefixes + NL +
				"SELECT ?name " + NL +
				"WHERE { \n " + nameQuery + "\n }";
		Query query03 = QueryFactory.create(queryString03, Syntax.syntaxSPARQL);
		QueryExecution qexec03 = QueryExecutionFactory.create(query03, myModel);
		ResultSet names = qexec03.execSelect();
		for (; names.hasNext() ; ) {
			QuerySolution rb = names.nextSolution();
			LiteralImpl x = (LiteralImpl) rb.getLiteral("name");
			factors.add(x.getString());
		}
		qexec03.close();
		
		String nameStatement; //customized query w name of each factor
		String checkSubscript; //query to check for presence of subscript to factor
		String checkSuperscript; //query to check for presence of superscript to factor
		
		for (int index=0;index<factors.size(); index++) {
			
			nameStatement = ":" +equationURI+" :"+equationSide + " _:d . \n "
					+ " _:d :hasTerm _:a . \n" + 
					" _:a :hasFactor _:b . \n " + 
					" _:b :hasVar _:c . \n "
					+ "_:c :hasName \""+ factors.get(index) +"\"^^xsd:string . \n";

			checkSubscript =  prefixes + NL +
					"ASK {"+nameStatement + subQuery +"}"; 
			Query query01 = QueryFactory.create(checkSubscript) ;
			QueryExecution qexec01 = QueryExecutionFactory.create(query01, myModel) ;
			Boolean subscriptPresent = qexec01.execAsk();
			qexec01.close();

			checkSuperscript =  prefixes + NL +
					"ASK {" + nameStatement + supQuery + "}";
			Query query02 = QueryFactory.create(checkSuperscript) ;
			QueryExecution qexec02 = QueryExecutionFactory.create(query02, myModel) ;
			Boolean superscriptPresent = qexec02.execAsk();
			qexec02.close();
			
			if(subscriptPresent && superscriptPresent) {

				String queryString =   prefixes + NL +
						"SELECT ?superscript ?subscript" + NL +
						"WHERE { \n "+ nameStatement + subQuery + supQuery + " }"
						+ "GROUP BY ?superscript ?subscript";

				Query query = QueryFactory.create(queryString, Syntax.syntaxSPARQL) ; 

				QueryExecution qexec = QueryExecutionFactory.create(query, myModel) ;
				ResultSet rs = qexec.execSelect();

				for ( ; rs.hasNext() ; ){
					QuerySolution rb = rs.nextSolution() ;
					LiteralImpl y = (LiteralImpl) rb.getLiteral("superscript");
					LiteralImpl z = (LiteralImpl) rb.getLiteral("subscript");
					System.out.println("(" + factors.get(index) + "_" + z.getString() + ")^" + y.getString());
					if(!(y.getDatatype()).equals(XSDDatatype.XSDinteger)) {
						throw new UndeterminedDimensionException();
					}
					String queryString04 = prefixes + NL  + "SELECT ?dimension" + NL  + 
							"WHERE {:" + factors.get(index) + "_" + z.getString() + " :hasUnits" + " ?dimension}";
					Query query04 = QueryFactory.create(queryString04);
					QueryExecution qexec04 = QueryExecutionFactory.create(query04, ontologyModel);
					ResultSet factorDimension = qexec04.execSelect();
					for (; factorDimension.hasNext() ;) {
						QuerySolution rb3 = factorDimension.nextSolution();
						Resource x3 = (Resource) rb3.getResource("dimension");
						dimension = new SIDimension(x3.getLocalName());
						for(int k=1; k < y.getInt(); k++) {
							dimension = dimension.multiply(dimension);
						}
						factorsToMultiply.add(dimension);
					}					
					qexec04.close();
				}
				qexec.close() ;


			} else if(!subscriptPresent && superscriptPresent) {

				String queryString =   prefixes + NL +
						"SELECT ?superscript" + NL +
						"WHERE { \n"+ nameStatement + supQuery + "}"
						+ "GROUP BY ?superscript";

				Query query = QueryFactory.create(queryString, Syntax.syntaxSPARQL) ;

				QueryExecution qexec = QueryExecutionFactory.create(query, myModel) ;
				ResultSet rs = qexec.execSelect();

				for ( ; rs.hasNext() ; ){
					QuerySolution rb = rs.nextSolution() ;
					LiteralImpl y = (LiteralImpl) rb.getLiteral("superscript");
					System.out.println(factors.get(index) +"^" + y.getString());
					if(!(y.getDatatype()).equals(XSDDatatype.XSDinteger)) {
						throw new UndeterminedDimensionException();
					}
					String queryString04 = prefixes + NL  + "SELECT ?dimension" + NL  + 
							"WHERE {:" + factors.get(index) + " :hasUnits" + " ?dimension}";
					Query query04 = QueryFactory.create(queryString04);
					QueryExecution qexec04 = QueryExecutionFactory.create(query04, ontologyModel);
					ResultSet factorDimension = qexec04.execSelect();
					for (; factorDimension.hasNext() ;) {
						QuerySolution rb3 = factorDimension.nextSolution();
						Resource x3 = (Resource) rb3.getResource("dimension");
						dimension = new SIDimension(x3.getLocalName());
						for(int k=1; k < y.getInt(); k++) {
							dimension = dimension.multiply(dimension);
						}
						factorsToMultiply.add(dimension);
					}
					qexec04.close();
				}

				qexec.close() ;

			}else if(subscriptPresent && !superscriptPresent) {

				String queryString =   prefixes + NL +
						"SELECT ?subscript" + NL +
						"WHERE { \n "+ nameStatement + subQuery + "}"
						+ "GROUP BY ?subscript";

				Query query = QueryFactory.create(queryString, Syntax.syntaxSPARQL) ;

				QueryExecution qexec = QueryExecutionFactory.create(query, myModel) ;
				ResultSet rs = qexec.execSelect();

				for ( ; rs.hasNext() ; ){
					QuerySolution rb = rs.nextSolution() ;
					LiteralImpl z = (LiteralImpl) rb.getLiteral("subscript");
					String queryString04 = prefixes + NL  + "SELECT ?dimension" + NL  + 
							"WHERE { \n :" + factors.get(index) + "_" + z.getString() + " :hasUnits" + " ?dimension \n}";
					Query query04 = QueryFactory.create(queryString04, Syntax.syntaxSPARQL);
					QueryExecution qexec04 = QueryExecutionFactory.create(query04, ontologyModel);
					ResultSet factorDimension = qexec04.execSelect();
					for (; factorDimension.hasNext() ;) {
						QuerySolution rb3 = factorDimension.nextSolution();
						Resource x3 = (Resource) rb3.getResource("dimension");
						dimension = new SIDimension(x3.getLocalName());
						factorsToMultiply.add(dimension);
					}
					
					System.out.println(factors.get(index) + "_" + z.getString());
					qexec04.close();
				}

				qexec.close() ;
			} else {
				String queryString04 = prefixes + NL  + "SELECT ?dimension" + NL  + 
						"WHERE {:" + factors.get(index) + " :hasUnits" + " ?dimension}";
				Query query04 = QueryFactory.create(queryString04);
				QueryExecution qexec04 = QueryExecutionFactory.create(query04, ontologyModel);
				ResultSet factorDimension = qexec04.execSelect();
				for (; factorDimension.hasNext() ;) {
					QuerySolution rb3 = factorDimension.nextSolution();
					Resource x3 = (Resource) rb3.getResource("dimension");
					dimension = new SIDimension(x3.getLocalName());
					factorsToMultiply.add(dimension);
				}
				System.out.println(factors.get(index));
				qexec04.close();
			}
		}


		if(factorsToMultiply.size()>0) {
			finalDimension = factorsToMultiply.get(0);
			for(int i=1; i<factorsToMultiply.size(); i++) {
				finalDimension = factorsToMultiply.get(i).multiply(finalDimension);
			}
		} 
		
		return finalDimension;	
	}

	//prints out a table of subject and objects for a given predicate
    public void showPair(String predicate) {
        
    	String queryString =   prefixes + NL +
                               "SELECT ?subject ?object" + NL +
                               "WHERE {?subject :"+predicate+" ?object}" + NL + 
                               "GROUP  BY ?subject ?object";

        Query query = QueryFactory.create(queryString) ;
        
        //query.serialize(new IndentedWriter(System.out,true)) ; ---> Prints out the SPARQL query
        //System.out.println() ;

        QueryExecution qexec = QueryExecutionFactory.create(query, myModel) ;
        ResultSet rs = qexec.execSelect();
        ResultSetFormatter.out(System.out, rs, query);
        qexec.close() ;

	}
	
    //creates a Triple (object that stores subject and object) for a given predicate
	public TripleList createTriple(String predicate) {
	        ArrayList<String> subject = new ArrayList<String>();
	        ArrayList<String> object = new ArrayList<String>();
	    
	    	String queryString =   prefixes + NL +
	                               "SELECT ?subject ?object" + NL +
	                               "WHERE {?subject :"+predicate+" ?object}" + NL + 
	                               "GROUP  BY ?subject ?object";

	        Query query = QueryFactory.create(queryString) ;

	        QueryExecution qexec = QueryExecutionFactory.create(query, myModel) ;
	        ResultSet rs = qexec.execSelect();
	        
	        for ( ; rs.hasNext() ; ){
	        	QuerySolution rb = rs.nextSolution() ;
	        	Resource z = (Resource) rb.getResource("subject");
	        	Resource y = (Resource) rb.getResource("object");
	        	object.add(y.getLocalName());
	        	subject.add(z.getLocalName());
	        }
	        qexec.close() ;
	        
	        TripleList myTriple = new TripleList(subject, object, predicate);
	        
	        return myTriple;
	}
	
	//prints out a table with variable names, subscripts, and superscripts as applicable
	//only works properly if all factors on a side have the same combo of super and subscripts
	public void printObject(String subject, String predicate) {
		
		String nameQuery = ":" +subject+" :"+predicate + " _:d . \n "
					+ " _:d :hasTerm _:a . \n" + 
					" _:a :hasFactor _:b . \n " + 
					" _:b :hasVar ?c . \n "
					+ "?c :hasName ?name . \n";
		String subQuery = "?c :hasSubscript ?e . \n"
				+ "?e :hasExpression ?subscript . \n";
		String supQuery =  "?c :hasSuperscript ?f . \n "
				+ "?f :hasExpression ?superscript . \n ";
		
		String queryString =   prefixes + NL +
				"SELECT ?name ?superscript ?subscript" + NL +
				"WHERE { \n "+ nameQuery + "OPTIONAL {" +subQuery +"} \n OPTIONAL {" + supQuery + "} }"
				+ "GROUP BY ?name ?superscript ?subscript";

		Query query = QueryFactory.create(queryString, Syntax.syntaxSPARQL) ;

		QueryExecution qexec = QueryExecutionFactory.create(query, myModel) ;
		ResultSet rs = qexec.execSelect();

		ResultSetFormatter.out(System.out, rs, query);
		
		qexec.close() ;
	}
	
	//returns an ArrayList<String> containing the subjects associated with a given predicate and object
	public ArrayList<String> findSubject(String predicate, String object) {
        ArrayList<String> subject = new ArrayList<String>();
    
    	String queryString =   prefixes + NL +
                               "SELECT ?subject" + NL +
                               "WHERE {?subject "+predicate+" :" + object + "}";

        Query query = QueryFactory.create(queryString) ;

        QueryExecution qexec = QueryExecutionFactory.create(query, myModel) ;
        ResultSet rs = qexec.execSelect();
        
        //ResultSetFormatter.out(System.out, rs, query);
        
        for ( ; rs.hasNext() ; ){
        	QuerySolution rb = rs.nextSolution() ;
        	Resource y = (Resource) rb.getResource("subject");
        	subject.add(y.getLocalName());
        }
        qexec.close() ;
        
        return subject;
	}
	
	//returns an ArrayList<String> containing the objects associated with a given subject and predicate
	public ArrayList<String> findObject(String property, String subject){
		ArrayList<String> objects = new ArrayList<String>();
		String object = null;

		String queryString02 =  prefixes + NL +
				"SELECT ?object" + NL +
				"WHERE {:" + subject + " :" + property + " ?object}";

		Query query02 = QueryFactory.create(queryString02) ;
		QueryExecution qexec02 = QueryExecutionFactory.create(query02, myModel) ;
		ResultSet result = qexec02.execSelect();
		for ( ; result.hasNext() ; ){
			QuerySolution rb = result.nextSolution() ;
			Resource x = (Resource) rb.getResource("object");
			object = x.getLocalName();
			objects.add(object);
		}
		qexec02.close();

		return objects;
	}
}
	

