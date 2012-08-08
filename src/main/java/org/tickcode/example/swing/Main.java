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

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.tickcode.broadcast.BroadcastConsumer;
import org.tickcode.broadcast.BroadcastProducer;




/**
 * 
 * This class will depend on WireFramework.aj for using dependency injection to
 * obtain the left, middle and right panels.
 * 
 * @author Eyon Land
 * 
 */
public class Main extends JFrame implements ShuttingDownBroadcast {

	private JPanel jContentPane = null;

	private JComponent left;
	private JComponent middle;
	private JComponent right;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Main main = new Main();
		main.setVisible(true);
	}

	/**
	 * This is the default constructor
	 */
	public Main() {
		super();

		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				shuttingDown();
				System.exit(0);
			}
		});
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	protected void initialize() {
		getJContentPane().add(left, BorderLayout.WEST);
		getJContentPane().add(middle, BorderLayout.CENTER);
		getJContentPane().add(right, BorderLayout.EAST);
		this.setContentPane(getJContentPane());
		this.setTitle("JFrame");
	}

	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel();
			jContentPane.setLayout(new BorderLayout());
		}
		return jContentPane;
	}

	public void setLeft(JComponent left) {
		this.left = left;
	}

	public void setMiddle(JComponent middle) {
		this.middle = middle;
	}

	public void setRight(JComponent right) {
		this.right = right;
	}

	@BroadcastProducer
	@BroadcastConsumer
	public void shuttingDown() {
		this.setVisible(false);
	}

}
