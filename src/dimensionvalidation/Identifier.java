package dimensionvalidation;

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

	//CONSTRUCTORS

	public Identifier(Model equationModel, Model ontologyModelIn, String prefixesIn){
		myModel = equationModel;
		prefixes = prefixesIn;
		ontologyModel = ontologyModelIn;
	}

	public Identifier(Model equationModel, String prefixesIn){
		this(equationModel, ModelFactory.createDefaultModel(), prefixesIn);
	}

	public Identifier() {
		this( ModelFactory.createDefaultModel(), null);
	}

	//METHODS
	
	//return prefix String
	public String getPrefixes(){
		return prefixes;
	}

	//return a copy of Model myModel
	public Model getModel(){
		Model copyModel = ModelFactory.createModelForGraph(myModel.getGraph());
		return copyModel;
	}

	//set prefixes for the Identifier
	public void setPrefixes(String prefixesIn) {
		prefixes = prefixesIn;
	}

	//returns a TripleList for a given predicate
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

	//returns an ArrayList<String> containing the subjects associated with a given predicate and object
	public ArrayList<String> findSubject(String predicate, String object) {
		ArrayList<String> subject = new ArrayList<String>();

		String queryString =   prefixes + NL +
				"SELECT ?subject" + NL +
				"WHERE {?subject "+predicate+" " + object + "}";

		Query query = QueryFactory.create(queryString) ;
		//query.serialize(new IndentedWriter(System.out,true)) ;
		QueryExecution qexec = QueryExecutionFactory.create(query, myModel) ;
		ResultSet rs = qexec.execSelect();
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

	/*
	 * Prints out a table with the answers to the given query string. 
	 * The query must be in correct SPARQL format with prefixes and new line tokens. 
	 * The table is printed in the console.
	 */
	public void printTable(String queryString) {	
		Query query = QueryFactory.create(queryString, Syntax.syntaxSPARQL) ;
		QueryExecution qexec = QueryExecutionFactory.create(query, myModel) ;
		ResultSet rs = qexec.execSelect();
		ResultSetFormatter.out(System.out, rs, query);		
		qexec.close() ;
	}

	/*
	 * Performs a SPARQL ASK query for the given String. The askQuery string
	 * must only contain the actual query itself. For example, ask("mfi:equationURI :hasLHS ?node").
	 * The rest of the ASK query syntax is included in the method and should be omitted
	 * from the parameter to avoid redundancy and errors. Returns a boolean value.
	 */
	public Boolean ask(String askQuery) {
		String queryString = prefixes + NL + "ASK { " + askQuery + "}";
		Query query = QueryFactory.create(queryString, Syntax.syntaxSPARQL) ;
		QueryExecution qexec = QueryExecutionFactory.create(query, myModel) ;
		Boolean answer = qexec.execAsk();
		qexec.close();

		return answer;
	}
	
	//totalDimension method with default SI units
	public Stringmension totalDimension(String beginning, int index) {
		return totalDimension(beginning, index, "SI");
	}
	
	/*
	 * Uses an initial SPARQL query segment, an index that is incremented each time the method
	 * recursively calls itself, and a system of units to determine the dimensionality of a given
	 * segment of an equation (generally one side of the equation). The segment is determined by 
	 * the beginning String, which should have a format similar to "mfi:equationURI001 mfi:hasRelation _:X . \n "
	 * + "_:X mfi:hasLHS _:Y .\n _:Y ". No other components of a SPARQL query besides the actual query 
	 * should be included. The index should start at 0 for the first method call. The system of units
	 * can be chosen from:
	 * "SI" = International System
	 * "US" = US Customary System
	 * "CGS" = Gauss/Centimetre-gram-second System
	 * "Planck" = Planck System
	 * 
	 * The method recursively calls itself as it queries down the levels of the Model, and each time returns a 
	 * Stringmension containing the current dimension vector and dimension name. When it is called for the last
	 * time, the final values of the variables are returned to the original method call. 
	 */
	public Stringmension totalDimension(String beginning, int index, String systemOfUnits) {		 
		//method variables 
		String factorName=null; 
		Dimension dimension;
		Stringmension temp;
		Dimension finalDimension = new SIDimension();
		String property=null;
		ArrayList<Dimension> factorDim = new ArrayList<Dimension>();
		ArrayList<Dimension> termDim = new ArrayList<Dimension>();
		String[] alphabet = {"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","z","y"};
		String node = " _:"+alphabet[index]+" . \n _:"+alphabet[index]+" ";
		
		//SPARQL query for current predicate
		String queryString = prefixes + NL 
				+ "SELECT DISTINCT ?predicate \n"
				+ "WHERE {"+beginning+" ?predicate ?node \n"
						+ " FILTER (?predicate != rdf:type ) \n"
						+ "FILTER (?predicate != mfi:hasFactorOrder ) \n }";
		
		Query query = QueryFactory.create(queryString, Syntax.syntaxSPARQL);
		QueryExecution qexec = QueryExecutionFactory.create(query, myModel);
		ResultSet names = qexec.execSelect();
		
		//processes the list of query results
		for (; names.hasNext() ; ) {
			QuerySolution rb = names.nextSolution();
			Resource x = (Resource) rb.getResource("predicate");
			property = x.getLocalName();
			
			if(property.equals("hasExpression")) {
				index++;
				String beginningExp=beginning + "mfi:hasExpression "+node;
				temp=totalDimension(beginningExp, index , systemOfUnits);
				factorName = temp.factorName; 
				dimension=temp.dimension;
				factorDim.add(dimension);
				
			} else if(property.equals("hasTerm")) {
				index++;
				String termName="";
				
				String queryString01 = prefixes + NL 
						+ "SELECT ?term \n"
						+ "WHERE {"+beginning+" mfi:hasTerm ?term }";
				Query query01 = QueryFactory.create(queryString01, Syntax.syntaxSPARQL);
				QueryExecution qexec01 = QueryExecutionFactory.create(query01, myModel);
				ResultSet values = qexec01.execSelect();
				for (; values.hasNext() ; ) {
					QuerySolution rb01 = values.nextSolution();
					Resource y = (Resource) rb01.getResource("term");
					termName=y.getLocalName();
					termName = "gov:" + termName;
					temp=totalDimension(termName, index, systemOfUnits);
					factorName = temp.factorName;
					dimension=temp.dimension;
					termDim.add(dimension);
				}
				qexec01.close();			

				finalDimension = termDim.get(0);
				for(int i=0; i<termDim.size()-1; i++) {
					if(termDim.get(i).equals(termDim.get(i+1))) {
						continue;
					} else {
						throw new UndeterminedDimensionException();
					}
				}
				
			} else if(property.equals("hasFactor")) {
				index++;
				String beginningFac=beginning + " mfi:hasFactor "+node;
				temp=totalDimension(beginningFac, index, systemOfUnits);
				factorName = temp.factorName;
				finalDimension = temp.dimension;
				termDim.add(finalDimension);
				
			} else if(property.equals("hasVariable")) {
				index++;
				String beginningVar=beginning + " mfi:hasVariable _:Z . \n _:Z ";
				int prefix=beginning.indexOf("gov:");
				String term = beginning.substring(prefix+4, prefix+14);
				
				String queryString01 = prefixes + NL 
						+ "SELECT ?name \n"
						+ "WHERE {"+beginningVar+" mfi:hasName ?name }";
				Query query01 = QueryFactory.create(queryString01, Syntax.syntaxSPARQL);
				QueryExecution qexec01 = QueryExecutionFactory.create(query01, myModel);
				ResultSet values = qexec01.execSelect();
				for (; values.hasNext() ; ) {
					QuerySolution rb01 = values.nextSolution();
					LiteralImpl y = (LiteralImpl) rb01.getLiteral("name");
					factorName = y.getString();
					dimension = factorDimension(term, factorName, systemOfUnits);
					factorDim.add(dimension);
				}
				qexec01.close();					
				
			} else if(property.equals("hasFraction")) {
				index++;
				String beginningNum=beginning + " mfi:hasFraction ?a . \n ?a mfi:hasNumerator " 
						+ node;
				Stringmension num = totalDimension(beginningNum, index, systemOfUnits);
				String beginningDen=beginning + " mfi:hasFraction ?a . \n ?a mfi:hasDenominator " 
						+ node;
				Stringmension den = totalDimension(beginningDen, index, systemOfUnits);
				
				Dimension numerator=num.dimension;	
				Dimension denominator=den.dimension;
				dimension = numerator.divide(denominator);	
				factorName = num.factorName + "/"+ den.factorName;
				factorDim.add(dimension);
				
			} else if(property.equals("hasAnnotationExp")) {
				index++;
				String beginningAn = beginning + " mfi:hasAnnotationExp _:Z . \n _:Z ";
				String annotation="";
				String queryString01 = prefixes + NL 
						+ "SELECT ?annotation \n"
						+ "WHERE {"+beginningAn+" mfi:hasAnnotation ?annotation }";
				Query query01 = QueryFactory.create(queryString01, Syntax.syntaxSPARQL);
				QueryExecution qexec01 = QueryExecutionFactory.create(query01, myModel);
				ResultSet values = qexec01.execSelect();
				for (; values.hasNext() ; ) {
					QuerySolution rb01 = values.nextSolution();
					LiteralImpl y = (LiteralImpl) rb01.getLiteral("annotation");
					annotation = y.getString();
				}
				qexec01.close();
				int prefix=beginning.indexOf("gov:");
				String term = beginning.substring(prefix+4, prefix+14);
				beginningAn=beginningAn + " mfi:hasExpression "+node;
				temp=totalDimension(beginningAn, index, systemOfUnits);
				dimension = factorDimension(term, temp.factorName, systemOfUnits);
				factorDim.add(dimension);
				factorName =annotation +"_"+temp.factorName;
				
			} else if(property.equals("hasValue")) {
				int prefix=beginning.indexOf("gov:");
				String term = beginning.substring(prefix+4, prefix+14);
				String queryString01 = prefixes + NL 
						+ "SELECT ?value \n"
						+ "WHERE {"+beginning+" mfi:hasValue" + node + " mfi:hasValue ?value }";
				Query query01 = QueryFactory.create(queryString01, Syntax.syntaxSPARQL);
				QueryExecution qexec01 = QueryExecutionFactory.create(query01, myModel);
				ResultSet values = qexec01.execSelect();
				for (; values.hasNext() ; ) {
					QuerySolution rb01 = values.nextSolution();
					LiteralImpl y = (LiteralImpl) rb01.getLiteral("value");
					factorName = y.toString();
				}
				qexec01.close();
				finalDimension = factorDimension(term, null, systemOfUnits);
				
			} else {
				//predicate is irrelevant to determining dimensionality
				continue;
			}
		}
		qexec.close();
		
		//combine multiple factors at same level
		if(factorDim.size()>0) {
			finalDimension = factorDim.get(0);
			for(int i=1; i<factorDim.size(); i++) {
				finalDimension = factorDim.get(i).multiply(finalDimension);
			}
		} 
		
		Stringmension answer = new Stringmension(factorName, finalDimension);
		
		return answer;
	}
	
	/*
	 * Takes inputs term String corresponding to one of the labeled term nodes in the
	 * equationModel and factor String corresponding to the name of a variable. Then
	 * determines the dimension of a named variable by finding its sub- and super-scripts 
	 * and passing the full variable name to findDimension(). Returns the dimension of the 
	 * factor parameter. 
	 */
	public Dimension factorDimension(String term, String factor, String systemOfUnits) {
		Dimension dimension = new SIDimension();
		
		String nameStatement; //customized query w name of each factor
		String checkSubscript; //query to check for presence of subscript to factor
		String checkSuperscript; //query to check for presence of superscript to factor	
		
		String variableQuery = " mfi:hasVariable _:n . \n"
				+ "_:n mfi:hasName ";		
		String valueQuery = " mfi:hasValue _:o .\n"
				+ "_:o mfi:hasValue ";
		String subQuery = "_:a mfi:hasSubscript _:e . \n"
				+ "_:e mfi:hasFactor _:f . \n"
				+ "_:f ";
		String supQuery = "_:a mfi:hasSuperscript _:h . \n"
				+ "_:h mfi:hasFactor _:i . \n"
				+ "_:i ";

		nameStatement = "gov:"+term + " mfi:hasFactor _:C . \n _:C mfi:hasVariable _:a . \n _:a mfi:hasName \""
						+ factor +"\"^^xsd:string . \n";

		//uses the ask() method to determine the presence of subscripts or
		//superscripts associated with the factor String (variable name)
		checkSubscript =  nameStatement + "_:a mfi:hasSubscript _:x "; 
		Boolean subscriptPresent = ask(checkSubscript);
		checkSuperscript =  nameStatement + " _:a mfi:hasSuperscript _:y ";
		Boolean superscriptPresent = ask(checkSuperscript);	

		//determines the full variable name by finding superscripts and/or subscripts and
		//passes this String to the findDimension() method to determine dimension
		if(subscriptPresent && superscriptPresent) { 
			String queryString;
			if(ask(nameStatement + subQuery + valueQuery + "?value") && ask(nameStatement + supQuery + valueQuery + "?value")){
				queryString =   prefixes + NL +
						"SELECT ?superscript ?subscript" + NL +
						"WHERE { \n "+ nameStatement + subQuery + valueQuery + " ?subscript "
						+ supQuery + valueQuery+" ?superscript }"
						+ "GROUP BY ?superscript ?subscript";
			} else if (!ask(nameStatement + subQuery + valueQuery + "?value") && ask(nameStatement + supQuery + valueQuery + "?value")){
				queryString =   prefixes + NL +
						"SELECT ?superscript ?subscript" + NL +
						"WHERE { \n "+ nameStatement + subQuery + variableQuery + "?subscript "
								+ supQuery + valueQuery+" ?superscript }"
						+ "GROUP BY ?superscript ?subscript";
			} else if (ask(nameStatement + subQuery + valueQuery + "?value") && !ask(nameStatement + supQuery + valueQuery + "?value")){
				throw new UndeterminedDimensionException();
			} else {
				throw new UndeterminedDimensionException();
			}

			Query query = QueryFactory.create(queryString, Syntax.syntaxSPARQL) ; 
			QueryExecution qexec = QueryExecutionFactory.create(query, myModel) ;
			ResultSet rs = qexec.execSelect();

			for ( ; rs.hasNext() ; ){
				QuerySolution rb = rs.nextSolution() ;
				LiteralImpl y = (LiteralImpl) rb.getLiteral("superscript");
				LiteralImpl z = (LiteralImpl) rb.getLiteral("subscript");
				dimension = findDimension(factor + "_" + z.getString(), systemOfUnits);
				if (y.getInt()==y.getDouble()) {
					for (int k = 1; k < y.getInt(); k++) {
						dimension = dimension.multiply(dimension);
					} 
				} else {
					//decimal exponent - what does this mean for dimensions?
					throw new UndeterminedDimensionException();
				}
			}
			qexec.close() ;

		} else if(!subscriptPresent && superscriptPresent) {
			String queryString;
			if (ask(nameStatement + supQuery + valueQuery + "?value")){
				queryString =   prefixes + NL +
						"SELECT ?superscript " + NL +
						"WHERE { \n "+ nameStatement + supQuery + valueQuery+" ?superscript }";
			} else {
				throw new UndeterminedDimensionException();
			}

			Query query = QueryFactory.create(queryString, Syntax.syntaxSPARQL) ;
			QueryExecution qexec = QueryExecutionFactory.create(query, myModel) ;
			ResultSet rs = qexec.execSelect();

			for ( ; rs.hasNext() ; ){
				QuerySolution rb = rs.nextSolution() ;
				LiteralImpl y = (LiteralImpl) rb.getLiteral("superscript");
				dimension = findDimension(factor, systemOfUnits);
				if (y.getInt()==y.getDouble()) {
					for (int k = 1; k < y.getInt(); k++) {
						dimension = dimension.multiply(dimension);
					} 
				} else {
					//decimal exponent - what does this mean for dimensions?
					throw new UndeterminedDimensionException();
				}
			}
			qexec.close() ;

		}else if(subscriptPresent && !superscriptPresent) {				
			String queryString;
			if(ask(nameStatement + subQuery + valueQuery + "?value")){
				queryString =   prefixes + NL +
						"SELECT ?subscript" + NL +
						"WHERE { \n "+ nameStatement + subQuery + valueQuery + " ?subscript }";
			} else {
				queryString =   prefixes + NL +
						"SELECT ?subscript" + NL +
						"WHERE { \n "+ nameStatement + subQuery + variableQuery +" ?subscript }";
			} 
			Query query = QueryFactory.create(queryString, Syntax.syntaxSPARQL) ;
			QueryExecution qexec = QueryExecutionFactory.create(query, myModel) ;
			ResultSet rs = qexec.execSelect();

			for ( ; rs.hasNext() ; ){
				QuerySolution rb = rs.nextSolution() ;
				LiteralImpl z = (LiteralImpl) rb.getLiteral("subscript"); 
				dimension = findDimension(factor+ "_" + z.getString(), systemOfUnits);
			}
			qexec.close() ;

		} else {
			dimension = findDimension(factor, systemOfUnits);
		}
		
		return dimension;
	}

	/*
	 * Uses a full factor name determined by the factorDimension() method to determine the 
	 * dimension vector of that variable. It queries the ontologyModel to find the units that
	 * correspond to the given variable name, then returns a dimension based on those units.
	 */
	public Dimension findDimension(String factor, String systemOfUnits) {
		Dimension dimension;
		String unit = null;;
		
		String queryString04 = prefixes + NL  + "SELECT ?dimension" + NL  + 
				"WHERE {:" + factor + " :hasUnits" + " ?dimension}";
		Query query04 = QueryFactory.create(queryString04);
		QueryExecution qexec04 = QueryExecutionFactory.create(query04, ontologyModel);
		ResultSet factorDimension = qexec04.execSelect();
		for (; factorDimension.hasNext() ;) {
			QuerySolution rb3 = factorDimension.nextSolution();
			Resource x3 = (Resource) rb3.getResource("dimension");
			unit = x3.getLocalName();
		}
		qexec04.close();
		
		switch (systemOfUnits) {
		case "US" :
			dimension = new USDimension(unit);
			break;
		case "Planck":
			dimension = new PlanckDimension(unit);
			break;
		case "CGS" :
			dimension=new CGSDimension(unit);
			break;
		default :
			dimension = new SIDimension(unit);	
		}
		return dimension;
	}
}	


