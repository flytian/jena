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

	//constructors

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

	//prints out a table with the answers to a given query
	public void printTable(String queryString) {	
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
				"WHERE {?subject "+predicate+" " + object + "}";

		Query query = QueryFactory.create(queryString) ;
		//query.serialize(new IndentedWriter(System.out,true)) ;

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

	//performs a SPARQL ASK query for the given String
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
	
	//returns the name and dimension of one side of an equation
	public Stringmension totalDimension(String beginning, int index, String systemOfUnits) {		 
		String factorName=null; 
		Dimension dimension;
		Stringmension temp;
		Dimension finalDimension = new SIDimension();
		String property=null;
		ArrayList<Dimension> factorDim = new ArrayList<Dimension>();
		ArrayList<Dimension> termDim = new ArrayList<Dimension>();
		String[] alphabet = {"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","z","y"};
		String node = " _:"+alphabet[index]+" . \n _:"+alphabet[index]+" ";
		String queryString = prefixes + NL 
				+ "SELECT DISTINCT ?predicate \n"
				+ "WHERE {"+beginning+" ?predicate ?node \n"
						+ " FILTER (?predicate != rdf:type ) \n"
						+ "FILTER (?predicate != mfi:hasFactorOrder ) \n }";
		//System.out.println();
		//System.out.println(queryString);
		Query query = QueryFactory.create(queryString, Syntax.syntaxSPARQL);
		//query.serialize(new IndentedWriter(System.out,true)) ;
		
		QueryExecution qexec = QueryExecutionFactory.create(query, myModel);
		ResultSet names = qexec.execSelect();
		for (; names.hasNext() ; ) {
			QuerySolution rb = names.nextSolution();
			Resource x = (Resource) rb.getResource("predicate");
			property = x.getLocalName();
			
			if(property.equals("hasExpression")) {
				//System.out.println(property);
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
					//System.out.println("termName: "+termName);
					termName = "gov:" + termName;
					temp=totalDimension(termName, index, systemOfUnits);
					factorName = temp.factorName;
					dimension=temp.dimension;
					//System.out.println(property + ": " + termName + " dimension: " + dimension);
					termDim.add(dimension);
				}
				qexec01.close();
				
				//String termList=null;
				finalDimension = termDim.get(0);
				for(int i=0; i<termDim.size()-1; i++) {
					if(termDim.get(i).equals(termDim.get(i+1))) {
						//termList = termDim.get(i).getDimensionUnits() + " equals " 
									//+ termDim.get(i+1).getDimensionUnits();
						//System.out.println("termDim: " + termDim.size() + " | "+ termList);
						continue;
					} else {
						//System.out.println(termDim.get(i).getDimensionUnits() + " DOES NOT equal " 
								//+ termDim.get(i+1).getDimensionUnits());
						throw new UndeterminedDimensionException();
					}
				}
				
			} else if(property.equals("hasFactor")) {
				//System.out.println(property);
				index++;
				String beginningFac=beginning + " mfi:hasFactor "+node;
				temp=totalDimension(beginningFac, index, systemOfUnits);
				factorName = temp.factorName;
				finalDimension = temp.dimension;
				//System.out.println("factorDim: " + finalDimension);
				termDim.add(finalDimension);
				
			} else if(property.equals("hasVariable")) {
				//System.out.println(property);
				index++;
				String beginningVar=beginning + " mfi:hasVariable _:Z . \n _:Z ";
				int prefix=beginning.indexOf("gov:");
				String term = beginning.substring(prefix+4, prefix+14);
				//System.out.println(term);
				
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
					//System.out.println("hasName: " + factorName);
					dimension = factorDimension(term, factorName, systemOfUnits);
					//System.out.println("nameDim: " + dimension);
					factorDim.add(dimension);
					//System.out.println();
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
				//System.out.println("numerator: "+numerator);
				Dimension denominator=den.dimension;
				//System.out.println("denominator: "+denominator);
				dimension = numerator.divide(denominator);	
				factorName = num.factorName + "/"+ den.factorName;
				factorDim.add(dimension);
				
			} else if(property.equals("hasAnnotationExp")) {
				//System.out.println(property);
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
				//System.out.println(annotation +", "+term);
				beginningAn=beginningAn + " mfi:hasExpression "+node;
				temp=totalDimension(beginningAn, index, systemOfUnits);
				dimension = factorDimension(term, temp.factorName, systemOfUnits);
				factorDim.add(dimension);
				factorName =annotation +"_"+temp.factorName;
				//System.out.println("hasAnnotation: " + factorName);
				
			} else if(property.equals("hasValue")) {
				int prefix=beginning.indexOf("gov:");
				String term = beginning.substring(prefix+4, prefix+14);
				//System.out.println(term);
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
				//System.out.println(property + ": " + factorName);
				finalDimension = factorDimension(term, null, systemOfUnits);
				//System.out.println("valueDim: "+finalDimension);
				
			} else {
				//System.out.println(property + " (property not relevant)");
				
			}
		}
		qexec.close();
		
		if(factorDim.size()>0) {
			finalDimension = factorDim.get(0);
			for(int i=1; i<factorDim.size(); i++) {
				finalDimension = factorDim.get(i).multiply(finalDimension);
			}
			//System.out.println("final: "+ finalDimension);
		} 
		
		
		Stringmension answer = new Stringmension(factorName, finalDimension);
		
		return answer;
	}
	
	//returns the dimension of a named variable by finding its sub- and super-scripts and passing the 
	//full name to findDimension();
	public Dimension factorDimension(String term, String factor, String systemOfUnits) {
		Dimension dimension = new SIDimension();
		
		String nameStatement; //customized query w name of each factor
		String checkSubscript; //query to check for presence of subscript to factor
		String checkSuperscript; //query to check for presence of superscript to factor	

		nameStatement = "gov:"+term + " mfi:hasFactor _:C . \n _:C mfi:hasVariable _:a . \n _:a mfi:hasName \""
						+ factor +"\"^^xsd:string . \n";

		checkSubscript =  nameStatement + "_:a mfi:hasSubscript _:x "; 
		Boolean subscriptPresent = ask(checkSubscript);

		checkSuperscript =  nameStatement + " _:a mfi:hasSuperscript _:y ";
		Boolean superscriptPresent = ask(checkSuperscript);
		
		//System.out.println(subscriptPresent+", "+superscriptPresent);
		
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
				//System.out.println("factor: (" + factor + "_" + z.getString() + ")^" + y.getString());
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
				//System.out.println("factor: " + factor +"^" + y.getString());
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
				//System.out.println("factor: " + factor + "_" + z.getString());
				dimension = findDimension(factor+ "_" + z.getString(), systemOfUnits);
			}
			qexec.close() ;

		} else {
			//System.out.println("factor: "+factor);
			dimension = findDimension(factor, systemOfUnits);
		}
		//System.out.println("factordimension: " + dimension);
		
		return dimension;

	}

	//queries the units and dimensions QUDT ontologies to return the dimension of a variable
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
		//System.out.println("factor in findDimension: " +factor);
		//System.out.println("unit: " + unit);
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
	

	//checks proportionality of variables
	public Boolean checkProportionality(String equationURI){
		Boolean consistency=true;
	
		ArrayList<String> factors = new ArrayList<String>(); //list of factors of each side of the equation
	
		String nameQuery = ":" +equationURI+" :hasRHS _:d . \n "
				+ " _:d :hasTerm _:a . \n" + 
				" _:a :hasFactor _:b . \n " + 
				" _:b :hasVar _:c . \n "
				+ "_:c :hasName ?name . \n";
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
		String checkSuperscript; //query to check for presence of superscript to factor
	
		for (int index=0;index<factors.size(); index++) {
	
			nameStatement = ":" +equationURI+" :hasRHS _:d . \n "
					+ " _:d :hasTerm _:a . \n" + 
					" _:a :hasFactor _:b . \n " + 
					" _:b :hasVar _:c . \n "
					+ "_:c :hasName \""+ factors.get(index) +"\"^^xsd:string . \n";
	
			checkSuperscript =  prefixes + NL +
					"ASK {" + nameStatement + supQuery + "}";
			Query query02 = QueryFactory.create(checkSuperscript) ;
			QueryExecution qexec02 = QueryExecutionFactory.create(query02, myModel) ;
			Boolean superscriptPresent = qexec02.execAsk();
			qexec02.close();
	
			String checkProportionality =  prefixes + NL +
					"ASK { \n :" + factors.get(index) + " :isDirectlyProportionalTo ?object }"; 
			/*
			 * PROBLEM: What does the name from the equation correspond to? Probably the variable name
			 * from the notebook. So, I need to query variable name -> concept -> description node -> 
			 * whether or not the relationship is directly or indirectly proportional
			 */
			Query query = QueryFactory.create(checkProportionality) ;
			QueryExecution qexec = QueryExecutionFactory.create(query, myModel) ;
			Boolean directlyProportional = qexec.execAsk();
			System.out.println("Directly Proportional? " + directlyProportional);
			qexec.close();
	
			if(superscriptPresent) {
	
				String queryString =   prefixes + NL +
						"SELECT ?superscript" + NL +
						"WHERE { \n "+ nameStatement + supQuery + " }";
				Query query01 = QueryFactory.create(queryString, Syntax.syntaxSPARQL) ; 
				QueryExecution qexec01 = QueryExecutionFactory.create(query01, myModel) ;
				ResultSet rs = qexec01.execSelect();
	
				for ( ; rs.hasNext() ; ){
					QuerySolution rb = rs.nextSolution() ;
					LiteralImpl y = (LiteralImpl) rb.getLiteral("superscript");
					if(!(y.getDatatype()).equals(XSDDatatype.XSDinteger)) {
						throw new UndeterminedDimensionException();
					}
					double superscript = y.getDouble();
	
					if(directlyProportional == true && superscript<0) {
						consistency=false;
					} else if (directlyProportional == false && superscript>0) {
						consistency=false;
					}
				}
				qexec.close() ;
	
			} else {
				if (directlyProportional != true) {
					consistency = false;
				}
			}
		} 
	
		return consistency;
	}
}	


