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

	//find the dimensions of one side of an equation given the side and the equation URI
	public Dimension findDimensionality(String equationURI, String equationSide) {
		return findDimensionality(equationURI, equationSide, "SI");
	}

	//find the dimension vector of one side of an equation
	public Dimension findDimensionality(String equationURI, String equationSide, String systemOfUnits) {
		Dimension finalDimension;	
		Dimension termDimension;
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
		ArrayList<Dimension> terms = new ArrayList<Dimension>(); //list of terms in an expression

		String nameQuery;
		Boolean hasTerm = false;
		do {
			//expression or term?
			String expressionQuery = prefixes + NL
					+"ASK { mfi:" +equationURI+" mfi:hasRelation _:a . \n "
					+ "_:a mfi:" + equationSide + " _:b .\n "
					+ " _:b mfi:hasExpression _:c . } \n";
			Boolean isExpression = ask(expressionQuery);
			
			if(isExpression) {
				nameQuery = "mfi:" +equationURI+" mfi:hasRelation _:a . \n "
						+ "_:a mfi:" + equationSide + " _:b . \n" 
						+"_:b mfi:hasExpression _:x .\n "
						+ " _:x mfi:hasTerm _:c . \n" + 
						" _:c mfi:hasFactor _:d . \n "
						+ "_:d ";

				String additionalTermQuery = prefixes + NL
						+"ASK { mfi:" +equationURI+" mfi:hasRelation _:a . \n "
						+ "_:a mfi:" + equationSide + " _:b .\n "
						+ " _:b mfi:hasTerm _:c . } \n";
				hasTerm = ask(additionalTermQuery);
			} else {
				nameQuery = "mfi:" +equationURI+" mfi:hasRelation _:a . \n "
						+ "_:a mfi:" + equationSide + " _:b .\n "
						+ " _:b mfi:hasTerm _:c . \n" + 
						" _:c mfi:hasFactor _:d . \n"
						+ "_:d ";
			}	

			String denomQuery = " mfi:hasFraction _:k . \n"
					+ "_:k mfi:hasDenominator _:l . \n"
					+ "_:l ";
			String numQuery = " mfi:hasFraction _:k . \n"
					+ "_:k mfi:hasNumerator _:m . \n"
					+ "_:m ";
			String factorQuery = " mfi:hasVariable _:n ."
					+ "_:n mfi:hasName ?name .";
			String subQuery = "_:n mfi:hasSubscript _:e . \n"
					+ "_:e mfi:hasFactor _:f . \n"
					+ "_:f mfi:hasVariable _:g . \n"
					+ "_:g mfi:hasName ?subscript . \n";
			String supQuery = "_:n mfi:hasSuperscript _:h . \n"
					+ "_:h mfi:hasFactor _:i . \n"
					+ "_:i mfi:hasVariable _:j . \n"
					+ "_:j mfi:hasName ?subscript . \n";	
			String valueQuery = " mfi:hasValue _:o .\n"
					+ "_:o mfi:hasValue ?value .";

			String nameStatement; //customized query w name of each factor
			String checkSubscript; //query to check for presence of subscript to factor
			String checkSuperscript; //query to check for presence of superscript to factor		
			
			if (ask(nameQuery + factorQuery)){

				//query to create list of names of multiplied factors in equation
				String queryString03 =   prefixes + NL +
						"SELECT ?name " + NL +
						"WHERE { \n " + nameQuery + factorQuery + "\n }";
				Query query03 = QueryFactory.create(queryString03, Syntax.syntaxSPARQL);
				QueryExecution qexec03 = QueryExecutionFactory.create(query03, myModel);
				ResultSet names = qexec03.execSelect();
				for (; names.hasNext() ; ) {
					QuerySolution rb = names.nextSolution();
					LiteralImpl x = (LiteralImpl) rb.getLiteral("name");
					factors.add(x.getString());
				}
				qexec03.close();
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

					checkSubscript =  prefixes + NL + "ASK {"+nameStatement + subQuery +"}"; 
					Boolean subscriptPresent = ask(checkSubscript);

					checkSuperscript =  prefixes + NL + "ASK {" + nameStatement + supQuery + "}";
					Boolean superscriptPresent = ask(checkSuperscript);

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
					termDimension = factorsToMultiply.get(0);
					for(int i=1; i<factorsToMultiply.size(); i++) {
						termDimension = factorsToMultiply.get(i).multiply(termDimension);
					}
				} 

			}else if (ask(nameQuery + numQuery + NL + nameQuery + denomQuery)){

				//query to add names of fraction factors
				String queryString05 =   prefixes + NL +
						"SELECT ?name " + NL +
						"WHERE { \n " + nameQuery + numQuery + factorQuery + denomQuery + factorQuery + "\n }";
				Query query05 = QueryFactory.create(queryString05, Syntax.syntaxSPARQL);
				QueryExecution qexec05 = QueryExecutionFactory.create(query05, myModel);
				ResultSet fractionNames = qexec05.execSelect();
				for (; fractionNames.hasNext() ; ) {
					QuerySolution rb = fractionNames.nextSolution();
					LiteralImpl x = (LiteralImpl) rb.getLiteral("name");
					factors.add(x.getString());
				}
				qexec05.close();

			} else {
				System.out.println("not good");
			}		
		} while (hasTerm=false);
		
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

	public Boolean ask(String queryString) {

		Query query = QueryFactory.create(queryString, Syntax.syntaxSPARQL) ;
		QueryExecution qexec = QueryExecutionFactory.create(query, myModel) ;
		Boolean answer = qexec.execAsk();
		qexec.close();

		return answer;
	}

	public void hasFactor() {

	}
}


