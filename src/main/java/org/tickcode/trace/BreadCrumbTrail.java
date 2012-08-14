package org.tickcode.trace;

import java.util.ArrayList;

public class BreadCrumbTrail {

	private static ThreadLocal<BreadCrumbTrail> activeTrail = new ThreadLocal<BreadCrumbTrail>(){
		@Override
		protected BreadCrumbTrail initialValue() {
			return new BreadCrumbTrail();
		}
	};
	
	public static BreadCrumbTrail getActiveTrail(){
		return activeTrail.get();
	}
	
	ArrayList<BreadCrumb> trail = new ArrayList<BreadCrumb>();
	public void add(BreadCrumb crumb){
		trail.add(crumb);
	}
	
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		for(int i=0; i < trail.size(); i++){
			if(i > 0)
				buffer.append("\n");
			buffer.append(trail.get(i));
		}
		return buffer.toString();
	}
	
	public BreadCrumbTrail reset(){
		trail.clear();
		return this;
	}
	
	public int size(){
		return trail.size();
	}
}
