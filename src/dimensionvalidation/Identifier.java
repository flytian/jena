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

	public Boolean ask(String askQuery) {
		String queryString = prefixes + NL + "ASK { " + askQuery + "}";
		Query query = QueryFactory.create(queryString, Syntax.syntaxSPARQL) ;
		QueryExecution qexec = QueryExecutionFactory.create(query, myModel) ;
		Boolean answer = qexec.execAsk();
		qexec.close();

		return answer;
	}
	
	public Stringmension totalDimension(String beginning, int index) {		 
		String factorName=null;
		String systemOfUnits = "SI"; 
		Dimension dimension;
		Stringmension temp;
		Dimension finalDimension = new SIDimension();
		String property=null;
		ArrayList<Dimension> factorDim = new ArrayList<Dimension>();
		ArrayList<Dimension> termDim = new ArrayList<Dimension>();
		String[] alphabet = {"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","z","y"};
		String node = " _:"+alphabet[index]+" . \n _:"+alphabet[index]+" ";
		String queryString = prefixes + NL 
				+ "SELECT ?predicate ?node \n"
				+ "WHERE {"+beginning+" ?predicate ?node \n"
						+ " FILTER (?predicate != rdf:type ) \n"
						+ "FILTER (?predicate != mfi:hasFactorOrder ) \n }";
		System.out.println();
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
				System.out.println(property);
				index++;
				temp=totalDimension(beginning + "mfi:hasExpression "+node, index );
				factorName = temp.factorName; 
				finalDimension=temp.dimension;
				
			} else if(property.equals("hasTerm")) {
				index++;
				Resource y = (Resource) rb.getResource("node");
				String termName = y.getLocalName();
				System.out.println("termName: "+termName);
				temp=totalDimension("gov:" + termName, index);
				factorName = temp.factorName;
				dimension=temp.dimension;
				System.out.println(property + ": " + termName + " dimension: " + dimension);
				termDim.add(dimension);
				
			} else if(property.equals("hasFactor")) {
				System.out.println(property);
				index++;
				temp=totalDimension(beginning + " mfi:hasFactor "+node, index);
				factorName = temp.factorName;
				dimension = temp.dimension;
				System.out.println("factorDim: " + dimension);
				if(dimension.getDimensionUnits()!= null){
					factorDim.add(dimension);
				}
				
			} else if(property.equals("hasVariable")) {
				System.out.println(property);
				index++;
				temp=totalDimension(beginning + " mfi:hasVariable "+node, index);
				factorName = temp.factorName;
				finalDimension = temp.dimension;
				//always followed by mfi:hasName "__"^^xsd:string 
				
			} else if(property.equals("hasFraction")) {
				index++;
				
				Stringmension num = totalDimension(beginning + " mfi:hasFraction ?a . \n ?a mfi:hasNumerator " 
				+ node, index);
				Stringmension den = totalDimension(beginning + " mfi:hasFraction ?a . \n ?a mfi:hasDenominator " 
				+ node, index);
				
				Dimension numerator=num.dimension;	
				Dimension denominator=den.dimension;
				finalDimension = numerator.divide(denominator);	
				factorName = num.factorName + "/"+ den.factorName;
				
			} else if(property.equals("hasAnnotationExp")) {
				System.out.println(property);
				index++;
				temp=totalDimension(beginning + "mfi:hasAnnotationExp "+node, index);
				factorName = temp.factorName;
				finalDimension = temp.dimension;
				//always followed by mfi:hasAnnotation "__"^^xsd:string and
				//mfi:hasExpression [mfi:hasTerm ....
				
			} else if(property.equals("hasName")) {
				int prefix=beginning.indexOf("gov:");
				String term = beginning.substring(prefix+4, prefix+14);
				System.out.println(term);
				index++;
				LiteralImpl y = (LiteralImpl) rb.getLiteral("node");
				factorName = y.getString();
				System.out.println(property + ": " + factorName);
				finalDimension = factorDimension(term, factorName, systemOfUnits);
				
			} else if(property.equals("hasAnnotation")) {
				int prefix=beginning.indexOf("gov:");
				String term = beginning.substring(prefix+4, prefix+14);
				System.out.println(term);
				index++;
				LiteralImpl y = (LiteralImpl) rb.getLiteral("node");
				String annotation = y.getString();
				factorName =totalDimension(beginning + "mfi:hasExpression "+node, index ).factorName+"_"+annotation;
				System.out.println(property + ": " + factorName);
				finalDimension = factorDimension(term, factorName, systemOfUnits);
				
			} else if(property.equals("hasValue")) {
				int prefix=beginning.indexOf("gov:");
				String term = beginning.substring(prefix+4, prefix+14);
				System.out.println(term);
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
				System.out.println(property + ": " + factorName);
				finalDimension = factorDimension(term, null, systemOfUnits);
				
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
		} 
		String termList=null;
		if(termDim.size()>0&&property.equals("hasTerm")) {
			finalDimension = termDim.get(0);
			for(int i=1; i<termDim.size(); i++) {
				if(termDim.get(i).equals(termDim.get(i-1))) {
					termList = termDim.get(i).getDimensionUnits() + " equals " 
								+ termDim.get(i-1).getDimensionUnits();
					continue;
				} else {
					System.out.println("Inconsistent term dimensions");
					throw new UndeterminedDimensionException();
				}
			}
			System.out.println("termDim: " + termDim.size() + " | "+ termList);
		}
		
		Stringmension answer = new Stringmension(factorName, finalDimension);
		
		return answer;
	}
	
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
				System.out.println("factor: (" + factor + "_" + z.getString() + ")^" + y.getString());
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
				System.out.println("factor: " + factor +"^" + y.getString());
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
				System.out.println("factor: " + factor + "_" + z.getString());
				dimension = findDimension(factor+ "_" + z.getString(), systemOfUnits);
			}
			qexec.close() ;

		} else {
			dimension = findDimension(factor, systemOfUnits);
		}
		return dimension;

	}
}	
	
	
	/*
	//DELETE? (replaced by factorDimension?)
	public Dimension termDimension(ArrayList<String> factors, String systemOfUnits) {
		
		Dimension finalDimension;
		Dimension dimension; //temporary variable
		ArrayList<Dimension> dimensionsToMultiply = new ArrayList<Dimension>(); //list of dimensions that go into finalDimension
	
		String variableQuery = " mfi:hasVariable _:n ."
				+ "_:n mfi:hasName ?name .";		
		String valueQuery = " mfi:hasValue _:o .\n"
				+ "_:o mfi:hasValue ?value .";
		String subQuery = "_:n mfi:hasSubscript _:e . \n"
				+ "_:e mfi:hasFactor _:f . \n"
				+ "_:f ";
		String supQuery = "_:n mfi:hasSuperscript _:h . \n"
				+ "_:h mfi:hasFactor _:i . \n"
				+ "_:i ";

		String nameStatement; //customized query w name of each factor
		String checkSubscript; //query to check for presence of subscript to factor
		String checkSuperscript; //query to check for presence of superscript to factor	
		
		for (int index=0;index<factors.size(); index++) {
			
			if(isExpression) {
				nameStatement = "mfi:" +equationURI+" mfi:hasRelation _:g . \n "
						+ "_:g mfi:" + equationSide + " _:x. \n" 
						+"_:x mfi:hasExpression _:d .\n "
						+ " _:d mfi:hasTerm _:a . \n" + 
						" _:a mfi:hasFactor _:b . \n " + 
						" _:b mfi:hasVariable _:c . \n "
						+ "_:c mfi:hasName \""+ factors.get(index) +"\"^^xsd:string . \n";
			} else {
				nameStatement = "mfi:" +equationURI+" mfi:hasRelation _:g . \n "
						+ "_:g mfi:" + equationSide + " _:d .\n "
						+ " _:d mfi:hasTerm _:a . \n" + 
						" _:a mfi:hasFactor _:b . \n " + 
						" _:b mfi:hasVariable _:c . \n "
						+ "_:c mfi:hasName \""+ factors.get(index) +"\"^^xsd:string . \n";
			}
			
			
			nameStatement = "_:n mfi:hasName \""+ factors.get(index) +"\"^^xsd:string . \n";
			
			checkSubscript =  prefixes + NL + "ASK {"+nameStatement + "_:n mfi:hasSubscript _:e . \n}"; 
			Boolean subscriptPresent = ask(checkSubscript);

			checkSuperscript =  prefixes + NL + "ASK {" + nameStatement + " _:n mfi:hasSuperscript _:h . \n}";
			Boolean superscriptPresent = ask(checkSuperscript);

			if(subscriptPresent && superscriptPresent) { 
				String queryString;
				if(ask(nameStatement + subQuery + valueQuery) && ask(nameStatement + supQuery + valueQuery)){
					queryString =   prefixes + NL +
							"SELECT ?superscript ?subscript" + NL +
							"WHERE { \n "+ nameStatement + subQuery + valueQuery + supQuery + valueQuery+" }"
							+ "GROUP BY ?superscript ?subscript";
				} else if (!ask(nameStatement + subQuery + valueQuery) && ask(nameStatement + supQuery + valueQuery)){
					queryString =   prefixes + NL +
							"SELECT ?superscript ?subscript" + NL +
							"WHERE { \n "+ nameStatement + subQuery + variableQuery + supQuery + valueQuery+" }"
							+ "GROUP BY ?superscript ?subscript";
				} else if (ask(nameStatement + subQuery + valueQuery) && !ask(nameStatement + supQuery + valueQuery)){
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
					System.out.println("(" + factors.get(index) + "_" + z.getString() + ")^" + y.getString());
					dimension = findDimension(factors.get(index) + "_" + z.getString(), systemOfUnits);
					if (y.getInt()==y.getDouble()) {
						for (int k = 1; k < y.getInt(); k++) {
							dimension = dimension.multiply(dimension);
						} 
					} else {
						//decimal exponent - what does this mean for dimensions?
						throw new UndeterminedDimensionException();
					}
					dimensionsToMultiply.add(dimension);
				}
				qexec.close() ;


			} else if(!subscriptPresent && superscriptPresent) {
				String queryString;
				if (ask(nameStatement + supQuery + valueQuery)){
					queryString =   prefixes + NL +
							"SELECT ?superscript " + NL +
							"WHERE { \n "+ nameStatement + supQuery + valueQuery+" }";
				} else {
					throw new UndeterminedDimensionException();
				}
				
				Query query = QueryFactory.create(queryString, Syntax.syntaxSPARQL) ;
				QueryExecution qexec = QueryExecutionFactory.create(query, myModel) ;
				ResultSet rs = qexec.execSelect();

				for ( ; rs.hasNext() ; ){
					QuerySolution rb = rs.nextSolution() ;
					LiteralImpl y = (LiteralImpl) rb.getLiteral("superscript");
					System.out.println(factors.get(index) +"^" + y.getString());
					dimension = findDimension(factors.get(index), systemOfUnits);
					if (y.getInt()==y.getDouble()) {
						for (int k = 1; k < y.getInt(); k++) {
							dimension = dimension.multiply(dimension);
						} 
					} else {
						//decimal exponent - what does this mean for dimensions?
						throw new UndeterminedDimensionException();
					}
					dimensionsToMultiply.add(dimension);
				}
				qexec.close() ;

			}else if(subscriptPresent && !superscriptPresent) {				
				String queryString;
				if(ask(nameStatement + subQuery + valueQuery)){
					queryString =   prefixes + NL +
							"SELECT ?subscript" + NL +
							"WHERE { \n "+ nameStatement + subQuery + valueQuery + " }";
				} else {
					queryString =   prefixes + NL +
							"SELECT ?subscript" + NL +
							"WHERE { \n "+ nameStatement + subQuery + variableQuery +" }";
				} 
				Query query = QueryFactory.create(queryString, Syntax.syntaxSPARQL) ;
				QueryExecution qexec = QueryExecutionFactory.create(query, myModel) ;
				ResultSet rs = qexec.execSelect();

				for ( ; rs.hasNext() ; ){
					QuerySolution rb = rs.nextSolution() ;
					LiteralImpl z = (LiteralImpl) rb.getLiteral("subscript");
					dimension = findDimension(factors.get(index) + "_" + z.getString(), systemOfUnits);
					dimensionsToMultiply.add(dimension);
				}
				qexec.close() ;
				
			} else {
				dimension = findDimension(factors.get(index), systemOfUnits);
				dimensionsToMultiply.add(dimension);
			}
		}	
		
		if(dimensionsToMultiply.size()>0) {
			finalDimension = dimensionsToMultiply.get(0);
			for(int i=1; i<dimensionsToMultiply.size(); i++) {
				finalDimension = dimensionsToMultiply.get(i).multiply(finalDimension);
			}
		} else {
			finalDimension = new SIDimension();
		}
		
		return finalDimension;
	}

	//DELETE?
	//find the dimension vector of one side of an equation given starting String and system of units
	public Dimension totalDimension(String start, String systemOfUnits) { 
		

		Dimension finalDimension;	
		Dimension termDimension; //each time the do-while loop iterates the termDimension is re-written and the new value added to the terms ArrayList

		switch (systemOfUnits) {
		case "US" :
			finalDimension = new USDimension();
			termDimension = new USDimension();
			break;
		case "Planck":
			finalDimension = new PlanckDimension();
			termDimension = new PlanckDimension();
			break;
		case "CGS" :
			finalDimension=new CGSDimension();
			termDimension=new CGSDimension();
			break;
		default :
			finalDimension = new SIDimension();	
			termDimension = new SIDimension();	
		}


		String variableQuery = "mfi:hasFactor _:d . \n "
				+ "_:d mfi:hasVariable _:n ."
				+ "_:n mfi:hasName ?name .";
		String fractionQuery = " mfi:hasFactor _:d . \n "
				+ "_:d mfi:hasFraction _:k . \n";			
		String valueQuery = " mfi:hasValue _:o .\n"
				+ "_:o mfi:hasValue ?value .";

		String denomQuery = " mfi:hasFraction _:k . \n"
				+ "_:k mfi:hasDenominator _:l . \n"
				+ "_:l ";
		String numQuery = " mfi:hasFraction _:k . \n"
				+ "_:k mfi:hasNumerator _:m . \n"
				+ "_:m ";

		//String nameStatement; //customized query w name of each factor
		//String checkSubscript; //query to check for presence of subscript to factor
		//String checkSuperscript; //query to check for presence of superscript to factor	

		ArrayList<Dimension> terms = new ArrayList<Dimension>(); //list of terms in an expression

		String nameQuery;
		Boolean hasPlus=false;
		Boolean hasMinus=false;

		//expression or term?
		String expressionQuery = start + "mfi:hasExpression";
		Boolean isExpression = ask(expressionQuery);

		if(isExpression) {
			nameQuery = start
					+"mfi:hasExpression _:x .\n "
					+ " _:x mfi:hasTerm _:c . \n" + 
					" _:c ";

			String plusOpQuery = start + "  mfi:plusOp _:c . ";
			String minusOpQuery = start + "  mfi:minusOp _:c . ";
			hasPlus=ask(plusOpQuery);
			hasMinus=ask(minusOpQuery);

		} else {
			nameQuery = start
					+ " mfi:hasTerm _:c . \n" + 
					" _:c mfi:hasFactor _:d . \n"
					+ "_:d ";
		}	

		//VARIABLE	
		if (ask(prefixes + NL + "ASK { " + nameQuery + variableQuery + "}")){
			ArrayList<String> factors = new ArrayList<String>(); //list of factors of each side of the equation	

			//query to create list of names of multiplied factors in equation
			String queryString =   prefixes + NL +
					"SELECT ?name " + NL +
					"WHERE { \n " + nameQuery + variableQuery + "\n }";
			Query query = QueryFactory.create(queryString, Syntax.syntaxSPARQL);
			QueryExecution qexec = QueryExecutionFactory.create(query, myModel);
			ResultSet names = qexec.execSelect();
			for (; names.hasNext() ; ) {
				QuerySolution rb = names.nextSolution();
				LiteralImpl x = (LiteralImpl) rb.getLiteral("name");
				factors.add(x.getString());
			}
			qexec.close();

			termDimension = termDimension(factors, systemOfUnits);

			//FRACTION
		}else if (ask(prefixes + NL + "ASK {" + nameQuery + fractionQuery + "}")){
			String numStart = nameQuery + numQuery;
			String denomStart = nameQuery + denomQuery;

			Dimension numDimension = totalDimension(numStart, systemOfUnits);
			Dimension denomDimension = totalDimension(denomStart, systemOfUnits);

			//divide dimensions to get termDimension 
			termDimension = numDimension.divide(denomDimension);				

			//VALUE	
		} else if (ask(nameQuery + valueQuery)){
			System.out.println("term is a constant");
			termDimension = new SIDimension("Unitless");

			//NO MATCH- BAD
		} else {
			System.out.println("not good, not good");
		}

		terms.add(termDimension);

		if (hasPlus) {
			String otherTermQuery = start + " mfi:plusOp _:c .\n _:c ";
			terms.add(totalDimension(otherTermQuery, systemOfUnits));
		} else if (hasMinus) {
			String otherTermQuery = start + " mfi:minusOp _:c .\n _:c ";
			terms.add(totalDimension(otherTermQuery, systemOfUnits));
		}

		if(terms.size()>0) {
			finalDimension = terms.get(0);
			for(int i=1; i<terms.size(); i++) {
				if(terms.get(i).equals(terms.get(i-1))) {
					continue;
				} else {
					throw new UndeterminedDimensionException();
				}
			}
		} 

		return finalDimension;	
	}
	*/


