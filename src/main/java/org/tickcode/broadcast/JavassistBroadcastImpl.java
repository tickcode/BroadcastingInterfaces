package org.tickcode.broadcast;

import java.io.IOException;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

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
