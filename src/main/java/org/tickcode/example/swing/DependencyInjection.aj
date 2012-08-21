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
package org.tickcode.example.swing;

import javax.swing.JComponent;

import org.tickcode.broadcast.AbstractMessageBroker;
import org.tickcode.broadcast.Broadcast;
import org.tickcode.broadcast.MessageBroker;
import org.tickcode.broadcast.VMMessageBroker;






/**
 * Here we show the power of AspectJ for dependency injection.
 * None of the components in this system need to know
 * about each other to be associated together.  When
 * Main is constructed, we will construct the appropriate
 * JPanels and provide them to Main so that 
 * Main does not have to be tightly coupled.
 * 
 * @author Eyon Land
 */
public aspect DependencyInjection {
	pointcut createMain(Main _this):
		  execution (Main+.new(..)) && this(_this);
	after(Main _this) returning: createMain(_this){
		
		MessageBroker broker = new VMMessageBroker();		
		_this.setMessageBroker(broker);
		MenuBar menuBar = new org.tickcode.example.swing.MenuBar();
		_this.setJMenuBar(menuBar);
		broker.add(menuBar);
		Broadcast panel = null;
		panel = new org.tickcode.example.swing.ProducerPanel();
		broker.add(panel);
		_this.setLeft((JComponent)panel);
		panel = new org.tickcode.example.swing.ConsumeAndProducePanel();
		broker.add(panel);
		_this.setMiddle((JComponent)panel);
		panel = new org.tickcode.example.swing.ConsumerPanel();
		broker.add(panel);
		_this.setRight((JComponent)panel);
		_this.initialize();
		_this.pack();
	}

}

