package org.tickcode.trace;


public class DefaultBreadCrumb implements BreadCrumb{

	String from;
	String to;
	public DefaultBreadCrumb(String from, String to){
		this.from = from;
		this.to = to;
	}

	public String toString(){
		return from + "->" + to;
	}
}
