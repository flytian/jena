package dimensionvalidation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;

public class Test {

	public static void main(String[] args) {
		
		Model manufacturingOntology = RDFDataMgr.loadModel("manufacturing.ttl"); //ontology defining the quantity of each variable
		Model notebookTriples = RDFDataMgr.loadModel("temp-doc-turning-triples.ttl");
		
		Model ontologyModel = ModelFactory.createDefaultModel();
		ontologyModel.add(manufacturingOntology);
		ontologyModel.add(notebookTriples);
		
		try 
		{	
			File file = new File("testontology.ttl"); 
			if(!file.exists()) {
				file.createNewFile();
				System.out.println(file.getCanonicalPath());
			}
			FileWriter fw = new FileWriter(file);
			BufferedWriter bw = new BufferedWriter(fw);
			PrintWriter pw = new PrintWriter(bw);
			ontologyModel.write( pw, "TURTLE") ;	
			pw.println();
		}
		catch (IOException e)
		{
			System.out.println("I guess an error occurred when trying to write the triples to a file. ");
		}
	}

}
