/*******************************************************************************
 * Copyright © 2012 tickcode.org All rights reserved. 
 *  
 * This file is part of the Tickcode collection of software
 *  
 * This file may be distributed under the terms of the tickcode.org 
 * license as defined by tickcode.org and appearing in the file 
 * license.txt included in the packaging of this file. 
 *  
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING 
 * THE WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A 
 * PARTICULAR PURPOSE. 
 *  
 *  See http://www.tickcode.org/LICENSE for licensing information. 
 *   
 *  Contact ask@tickcode.org if any conditions of this licensing 
 *  are not clear to you.
 ******************************************************************************/package org.tickcode.trace;

import java.util.ArrayList;

public class BreadCrumbTrail {

	private static ThreadLocal<BreadCrumbTrail> activeTrail = new ThreadLocal<BreadCrumbTrail>(){
		@Override
		protected BreadCrumbTrail initialValue() {
			return new BreadCrumbTrail();
		}
	};
	
	public static BreadCrumbTrail get(){
		return activeTrail.get();
	}
	
	ArrayList<BreadCrumb> trail = new ArrayList<BreadCrumb>();
	public void add(BreadCrumb crumb){
		trail.add(crumb);
	}
	
	private BreadCrumbTrail(){
		
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
