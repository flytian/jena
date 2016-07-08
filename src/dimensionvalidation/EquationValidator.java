package dimensionvalidation;

import java.io.*;
import java.util.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;

public class EquationValidator {
	
	public static final String NL = System.getProperty("line.separator") ;

	public static void main(String[] args) {
		
		String systemOfUnits = "SI"; //sets system of units for query
		/*
		 * KEY for systemOfUnits:
		 * "SI" = International System
		 * "US" = US Customary System
		 * "CGS" = Gauss/Centimetre-gram-second System
		 * "Planck" = Planck System
		 */
		
		String prefixString = "PREFIX owl: <http://www.w3.org/2002/07/owl#>" + NL + 
				"PREFIX : <http://modelmeth.nist.gov/manufacturing#>" + NL + 
				"PREFIX mfi:  <http://modelmeth.nist.gov/mfi#>" + NL + 
				"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>";		//ontology prefixes
		
		Model manufacturingOntology = RDFDataMgr.loadModel("manufacturing.ttl"); //ontology defining the quantity of each variable
		Model notebookTriples = RDFDataMgr.loadModel("temp-doc-turning-triples.ttl");
		Model equationModel = RDFDataMgr.loadModel("testcase-5.ttl"); //ontology containing the Turtle code for each equation
		
		Model ontologyModel = ModelFactory.createDefaultModel();
		ontologyModel.add(manufacturingOntology);
		ontologyModel.add(notebookTriples);
		
		Model tempModel = ModelFactory.createOntologyModel(); //ontology containing Turtle triples about dimensions matching to be output
		tempModel.setNsPrefix("", "http://modelmeth.nist.gov/manufacturing#");
		tempModel.setNsPrefix("qudt", "http://qudt.org/schema/qudt#");
		
		Identifier equationID = new Identifier(equationModel, ontologyModel, prefixString);
		
		ArrayList<String> nameURI = equationID.findSubject("a", "mfi:MathContent"); //list of all equation URIs in the ontology

		Dimension dimLHS; //dimension of the RHS of each equation
		Dimension dimRHS; //dimension of the LHS of each equation
		
		TripleList RHS = equationID.createTriple("hasRHS");
		TripleList LHS= equationID.createTriple("hasLHS");
		
		//checks that the equations line up correctly
		if (LHS.getObject().size() != RHS.getObject().size()) {
			throw new UnmatchedLeftAndRightException(); //if equations didn't split into RHS and LHS correctly 
		} 
		
		for(int i =0; i<nameURI.size(); i++) { //processes each equation in the ontology based on its URI, adds triples to tempModel

			//System.out.println("Proportionality: " + equationID.checkProportionality(nameURI.get(i)));
			
			
			Property isConsistent = ResourceFactory.createProperty("http://modelmeth.nist.gov/manufacturing#isConsistentWith");
			Property isNotConsistent = ResourceFactory.createProperty("http://modelmeth.nist.gov/manufacturing#isNotConsistentWith");
			Property hasDimensionVector = ResourceFactory.createProperty("http://modelmeth.nist.gov/manufacturing#hasDimensionVector");
			Property hasDimensionalConsistency = ResourceFactory.createProperty("http://modelmeth.nist.gov/manufacturing#hasDimensionalConsistency");
			Property inconsistencyCause = ResourceFactory.createProperty("http://modelmeth.nist.gov/manufacturing#inconsistencyCause");
			
			
			//Property hasSystemOfUnits = ResourceFactory.createProperty("http://modelmeth.nist.gov/manufacturing#hasSystemOfUnits");
			//Property hasRHS = ResourceFactory.createProperty("http://modelmeth.nist.gov/manufacturing#hasDimensionVector");
			//Property hasLHS = ResourceFactory.createProperty("http://modelmeth.nist.gov/manufacturing#hasDimensionVector");
			
			
			Resource a = tempModel.createResource("http://modelmeth.nist.gov/manufacturing#" + nameURI.get(i) + "RHS");
			Resource b = tempModel.createResource("http://modelmeth.nist.gov/manufacturing#" + nameURI.get(i) + "LHS");
			Resource c = tempModel.createResource("http://modelmeth.nist.gov/manufacturing#" + nameURI.get(i));
			Resource d = tempModel.createResource("http://modelmeth.nist.gov/manufacturing#" + "UndeterminedExponent");
			Resource f = tempModel.createResource("http://modelmeth.nist.gov/manufacturing#" + "UnequalDimensions");
			
				
			
			try 
			{	
				String equationLHS = "mfi:" +nameURI.get(i)+" mfi:hasRelation _:a . \n "
						+ "_:a mfi:hasLHS _:b .\n _:b ";
				String equationRHS = "mfi:" +nameURI.get(i)+" mfi:hasRelation _:a . \n "
						+ "_:a mfi:hasRHS _:b .\n _:b ";
				dimLHS = equationID.totalDimension(equationLHS, systemOfUnits);	
				dimRHS = equationID.totalDimension(equationRHS, systemOfUnits);
				
				System.out.println("EQUATION URI: " + nameURI.get(i));
				System.out.println();
				System.out.println("RHS Dimensions: " + dimRHS.toString());
				System.out.println();
				System.out.println("LHS Dimensions: " + dimLHS.toString());
				System.out.println();
				
				//triples about dimensionality of each side
				Literal aDim = ResourceFactory.createStringLiteral(dimRHS.getVector());
				Literal bDim = ResourceFactory.createStringLiteral(dimLHS.getVector());			
				a.addLiteral(hasDimensionVector, aDim);
				b.addLiteral(hasDimensionVector, bDim);
				
				//triples comparing dimensionality of LHS and RHS
				if(dimLHS.equals(dimRHS)) { 
					a.addProperty(isConsistent, b);
					c.addLiteral(hasDimensionalConsistency, true);
					System.out.println("Consistent equation");
				} else {
					a.addProperty(isNotConsistent, b);
					c.addLiteral(hasDimensionalConsistency, false);
					c.addProperty(inconsistencyCause, d);
					System.out.println("Inconsistent equation");
				}	
			} 
			catch (UndeterminedDimensionException e) 
			{
				System.out.println("ERROR: Non-integer exponent - dimensions cannot be determined" + NL);
				c.addProperty(inconsistencyCause, f);
			}
		}
		/*
		String fileName = "tempOntology.ttl"; //name of file where triples are stored, located in workspace
		
		//write triples to a new file
		try 
		{	
			File file = new File(fileName); 
			if(!file.exists()) {
				file.createNewFile();
				System.out.println(file.getCanonicalPath());
			}
			FileWriter fw = new FileWriter(file);
			//FileWriter fw = new FileWriter(file, true);
			BufferedWriter bw = new BufferedWriter(fw);
			PrintWriter pw = new PrintWriter(bw);
			tempModel.write( pw, "TURTLE") ;	
			pw.println();
		}
		catch (IOException e)
		{
			System.out.println("I guess an error occurred when trying to write the triples to a file. ");
		}
		*/
		
		/*
		//key to explain dimension symbols
		System.out.println();	
		System.out.println();
		System.out.println("*******************************************");
		System.out.println("DIMENSION KEY (" + systemOfUnits + " Base Units) \n");
		switch (systemOfUnits) {
		case ("CGS") :
			System.out.println( "M: Mass (gram) \n"
					+ "L: Length (centimetre) \n"
					+ "T: Time (second) \n"
					+ "U: Dimensionless");
			break;
		case ("US") :
			System.out.println( "M: Mass (pound) \n"
					+ "L: Length (inch) \n"
					+ "T: Time (second) \n"
					+ "theta: Thermodynamic Temperature (Fahrenheit) \n"
					+ "U: Dimensionless");
			break;
		case ("Planck") :
			System.out.println( "M: Mass (Planck mass) \n"
					+ "L: Length (Planck length) \n"
					+ "T: Time (Planck time) \n"
					+ "Q: Electric Charge (Planck charge) \n"
					+ "theta: Thermodynamic Temperature (Planck temperature) \n"
					+ "U: Dimensionless");
			break;
		default :
			System.out.println( "M: Mass (kilogram) \n"
				+ "L: Length (meter) \n"
				+ "T: Time (second) \n"
				+ "I: Electric Current (ampere) \n"
				+ "theta: Thermodynamic Temperature (kelvin) \n"
				+ "N: Amount of Substance (mole) \n"
				+ "J: Luminous Intensity (candela) \n"
				+ "U: Dimensionless");
		} 
		*/
	
	}

}


