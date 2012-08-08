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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.tickcode.broadcast.BroadcastProducer;




public class MenuBar extends JMenuBar implements ShuttingDownBroadcast {

	JMenu fileMenu;
	JMenuItem exitMenuItem;

	public MenuBar() {
		this.setName("MainMenuBar");
		fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F);

		// Creating the MenuItems

		// Create the Exit menu
		exitMenuItem = new JMenuItem("Exit", KeyEvent.VK_X);
		// Accelerators, offer keyboard shortcuts to bypass navigating the menu
		// hierarchy.
		exitMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X,
				ActionEvent.ALT_MASK));
		exitMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				shutDownGui(e);
			}
		});
		fileMenu.add(exitMenuItem);

		this.add(fileMenu);

	}
	
	public void loadHistoricalData(ActionEvent e){
		
	}
	
	public void shutDownGui(ActionEvent e){
		shuttingDown();
		System.exit(0);
	}
	
	
	@Override
	@BroadcastProducer
	public void shuttingDown() {
		
	}

	
	

}
