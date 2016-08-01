package dimensionvalidation;

import java.util.*;

/*
 * TripleList objects are used to store ArrayLists of subjects and objects corresponding to
 * a specific predicate in an RDF triple. The three instance variables in the object are a 
 * subject ArrayList, an object ArrayList, and a predicate String. All are private, and 
 * since a TripleList's purpose is mainly storage, there are getter and setter methods 
 * in addition to a toString method, but no methods that use the instance variables to 
 * compute new information.  
 */
public class TripleList {
	private ArrayList<String> subject;
	private ArrayList<String> object;
	private String predicate;
	
	//CONSTRUCTORS
	
	public TripleList() {
		subject = new ArrayList<String>();
		object = new ArrayList<String>();
		predicate = null;
	}
	
	public TripleList(TripleList otherTriple) {
		subject = otherTriple.getSubject();
		object = otherTriple.getObject();
		predicate = otherTriple.getPredicate();
	}

	public TripleList(ArrayList<String> subjectIn, ArrayList<String> objectIn, String predicateIn) {
		subject = new ArrayList<String>(subjectIn);
		object = new ArrayList<String>(objectIn);
		predicate = predicateIn;
	}
	
	//METHODS
	
	//assigns an ArrayList to the subject variable
	public void setSubject(ArrayList<String> subjectIn){
		subject = new ArrayList<String>(subjectIn);
	}
	
	//assigns an ArrayList to the object variable
	public void setObject(ArrayList<String> objectIn){
		object = new ArrayList<String>(objectIn);
	}
	
	//assigns a String to the predicate variable
	public void setPredicate(String predicateIn) {
		predicate = predicateIn;
	}
	
	//returns a copy of the subject ArrayList
	public ArrayList<String> getSubject() {
		ArrayList<String> subjectCopy = new ArrayList<String>(subject);
		return subjectCopy;
	}
	
	//returns a copy of the object ArrayList
	public ArrayList<String> getObject() {
		ArrayList<String> objectCopy = new ArrayList<String>(object);
		return objectCopy;
	}
	
	//returns the predicate String
	public String getPredicate() {
		return predicate;
	}
	
	//returns a String representation of the information stored in a TripleList
	public String toString() {
		String subjectString = subject.get(0);
		String objectString = object.get(0);
		for(int i=1; i<subject.size(); i++) {
			subjectString +=  ", " + subject.get(i);
		}
		for(int i=1; i<object.size(); i++) {
			objectString +=  ", " + object.get(i) ;
		}
		return subjectString + "\n" + predicate + "\n" + objectString;
	}
}
