/*******************************************************************************
 * Copyright (c) 2012, tickcode.org
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of tickcode, nor tickcode.org, nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package org.tickcode.trace;

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
	
	String thumbprint;
	
	ArrayList<BreadCrumb> trail = new ArrayList<BreadCrumb>();
	
	public void add(BreadCrumb crumb){
		trail.add(crumb);
	}
	
	private BreadCrumbTrail(){
		
	}

	
	public String getThumbprint() {
		return thumbprint;
	}

	public void setThumbprint(String thumbprint) {
		this.thumbprint = thumbprint;
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
