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
 ******************************************************************************/
package org.tickcode.broadcast;

import java.io.IOException;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

/**
 * Still a work in progress....
 * @author eyon
 *
 */
public class JavassistBroadcastImpl {
	public void enhance(ClassPool classPool, String className){
		try{
			CtClass ct = classPool.get(className);
			boolean atLeastOneProducerMethod = false;
			for (CtMethod method : ct.getMethods()) {
				if (method.hasAnnotation(BroadcastProducer.class)) {
					atLeastOneProducerMethod = true;
					StringBuffer content = new StringBuffer();
					content.append("\n { getMessageBroker().broadcast($0, \"").append(method.getName()).append("\", $args); }");
					method.insertAfter(content.toString());
				}
			}
			if(atLeastOneProducerMethod){
				ct.writeFile();
			}
		}catch(IOException ex){
			throw new JavassistException(ex);			
		}catch(NotFoundException ex){
			throw new JavassistException(ex);
		}catch(CannotCompileException ex){
			throw new JavassistException(ex);
		}
	}

}
