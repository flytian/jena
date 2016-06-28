package dimensionvalidation;

import java.util.*;

public class TripleList {
	private ArrayList<String> subject;
	private ArrayList<String> object;
	private String predicate;
	
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
	
	public void setSubject(ArrayList<String> subjectIn){
		subject = new ArrayList<String>(subjectIn);
	}
	
	public void setObject(ArrayList<String> objectIn){
		object = new ArrayList<String>(objectIn);
	}
	
	public void setPredicate(String predicateIn) {
		predicate = predicateIn;
	}
	
	public ArrayList<String> getSubject() {
		ArrayList<String> subjectCopy = new ArrayList<String>(subject);
		return subjectCopy;
	}
	
	public ArrayList<String> getObject() {
		ArrayList<String> objectCopy = new ArrayList<String>(object);
		return objectCopy;
	}
	public String getPredicate() {
		return predicate;
	}
	
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
