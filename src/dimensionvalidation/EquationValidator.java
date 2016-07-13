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
				"PREFIX mfi:  <http://modelmeth.nist.gov/mfi/>" + NL + 
				"PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" + NL +
				"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" + NL + 
				"PREFIX gov:	<http://gov.nist.modelmeth/> ";		//ontology prefixes

		Model manufacturingOntology = RDFDataMgr.loadModel("manufacturing.ttl"); //general manufacturing ontology
		Model notebookTriples = RDFDataMgr.loadModel("temp-doc-turning-triples.ttl");//ontology containing variable information from the IPython notebook
		Model equationModel = RDFDataMgr.loadModel("all-equations-working.ttl"); //ontology containing equation information from the IPython notebook

		Model ontologyModel = ModelFactory.createDefaultModel(); //ontology queried over
		ontologyModel.add(manufacturingOntology);
		ontologyModel.add(notebookTriples);

		Model tempModel = ModelFactory.createOntologyModel(); //ontology that triples are added to
		//alternatively, they could be added to any of the other models
		tempModel.setNsPrefix("", "http://modelmeth.nist.gov/manufacturing#");
		tempModel.setNsPrefix("qudt", "http://qudt.org/schema/qudt#");

		Identifier equationID = new Identifier(equationModel, ontologyModel, prefixString);
		ArrayList<String> nameURI = equationID.findSubject("a", "mfi:MathContent"); //list of all equation URIs in the ontology
		System.out.println(nameURI.toString());
		System.out.println();
		Stringmension one;
		Stringmension two;
		Dimension dimLHS; //dimension of the RHS of each equation
		Dimension dimRHS; //dimension of the LHS of each equation
		
		//checks that the equations line up correctly and are split into RHS and LHS equally
		TripleList RHS = equationID.createTriple("hasRHS");
		TripleList LHS= equationID.createTriple("hasLHS");
		if (LHS.getObject().size() != RHS.getObject().size()) {
			throw new UnmatchedLeftAndRightException(); 
		} 

		for(int i =0; i<nameURI.size(); i++) { //processes each equation in the ontology based on its URI, adds triples to tempModel
			
			//System.out.println("Proportionality: " + equationID.checkProportionality(nameURI.get(i)));

			Property isConsistent = ResourceFactory.createProperty("http://modelmeth.nist.gov/manufacturing#isConsistentWith");
			Property isNotConsistent = ResourceFactory.createProperty("http://modelmeth.nist.gov/manufacturing#isNotConsistentWith");
			Property hasDimensionVector = ResourceFactory.createProperty("http://modelmeth.nist.gov/manufacturing#hasDimensionVector");
			Property hasDimensionalConsistency = ResourceFactory.createProperty("http://modelmeth.nist.gov/manufacturing#hasDimensionalConsistency");
			Property inconsistencyCause = ResourceFactory.createProperty("http://modelmeth.nist.gov/manufacturing#inconsistencyCause");
			Resource a = tempModel.createResource("http://modelmeth.nist.gov/manufacturing#" + nameURI.get(i) + "RHS");
			Resource b = tempModel.createResource("http://modelmeth.nist.gov/manufacturing#" + nameURI.get(i) + "LHS");
			Resource c = tempModel.createResource("http://modelmeth.nist.gov/manufacturing#" + nameURI.get(i));
			Resource d = tempModel.createResource("http://modelmeth.nist.gov/manufacturing#" + "UndeterminedExponent");
			Resource f = tempModel.createResource("http://modelmeth.nist.gov/manufacturing#" + "UnequalDimensions");

			try 
			{	
				String equationLHS = "mfi:" +nameURI.get(i)+" mfi:hasRelation _:X . \n "
						+ "_:X mfi:hasLHS _:Y .\n _:Y ";
				String equationRHS = "mfi:" +nameURI.get(i)+" mfi:hasRelation _:X . \n "
						+ "_:X mfi:hasRHS _:Y .\n _:Y ";

				System.out.println("EQUATION URI: " + nameURI.get(i));

				one = equationID.totalDimension(equationLHS, 0, systemOfUnits);
				two=equationID.totalDimension(equationRHS, 0, systemOfUnits);
				dimLHS = one.dimension;
				dimRHS = two.dimension;

				System.out.println("LHS Dimensions: " + dimLHS.toString());
				System.out.println("RHS Dimensions: " + dimRHS.toString());

				//triples about dimensionality of each side
				Literal aDim = ResourceFactory.createStringLiteral(dimRHS.getVector());
				Literal bDim = ResourceFactory.createStringLiteral(dimLHS.getVector());			
				a.addLiteral(hasDimensionVector, aDim);
				b.addLiteral(hasDimensionVector, bDim);

				//triples comparing dimensionality of LHS and RHS
				if(dimLHS.equals(dimRHS)) { 
					a.addProperty(isConsistent, b);
					c.addLiteral(hasDimensionalConsistency, true);
					System.out.println("CONSISTENT equation");
				} else {
					a.addProperty(isNotConsistent, b);
					c.addLiteral(hasDimensionalConsistency, false);
					c.addProperty(inconsistencyCause, d);
					System.out.println("INCONSISTENT equation");
				}	
			} 
			catch (UndeterminedDimensionException e) 
			{
				System.out.println("ERROR: Non-integer exponent - dimensions cannot be determined" + NL);
				c.addProperty(inconsistencyCause, f);
			}

			System.out.println();
			System.out.println();
		}

		String fileName = "tempOntology.ttl"; //name of file where triples are stored, located in workspace		
		
		try //write triples to a new file
		{	
			File file = new File(fileName); 
			if(!file.exists()) {
				file.createNewFile();
				System.out.println(file.getCanonicalPath());
			}
			//FileWriter fw = new FileWriter(file); //REWRITE FILE
			FileWriter fw = new FileWriter(file, true); //ADD TO FILE
			BufferedWriter bw = new BufferedWriter(fw);
			PrintWriter pw = new PrintWriter(bw);
			tempModel.write( pw, "TURTLE") ;	
			pw.println();
		}
		catch (IOException e)
		{
			System.out.println("I guess an error occurred when trying to write the triples to a file. ");
		}

	}

}

/*
KEY TO DIMENSIONS:
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


