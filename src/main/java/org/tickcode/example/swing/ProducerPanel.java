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
import java.awt.ScrollPane;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.tickcode.broadcast.BroadcastProducer;





/**
 * The purpose of this class is to show how a JPanel can simply
 * implement the {@link example.broadcast.TextChangedBroadcast}
 * to be able to send "text changed" broadcasts to all other classes
 * that have implemented TextChangedBroadcast. 
 * @author Eyon Land
 *
 */
public class ProducerPanel extends JPanel implements TextChangedBroadcast{


	private ScrollPane scrollPane = null;  //  @jve:decl-index=0:visual-constraint="312,149"
	private JTextArea jTextArea = null;

	/**
	 * This is the default constructor
	 */
	public ProducerPanel() {
		super();
		initialize();
	}
	

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setLayout(new BorderLayout());
		this.add(new JLabel("Producer Panel "),BorderLayout.NORTH);
		this.add(getScrollPane(),BorderLayout.CENTER);
	}

	/**
	 * This method initializes scrollPane	
	 * 	
	 * @return java.awt.ScrollPane	
	 */    
	private ScrollPane getScrollPane() {
		if (scrollPane == null) {
			scrollPane = new ScrollPane();
			scrollPane.add(getJTextArea(), null);
		}
		return scrollPane;
	}

	/**
	 * This method initializes jTextArea	
	 * 	
	 * @return javax.swing.JTextArea	
	 */    
	private JTextArea getJTextArea() {
		if (jTextArea == null) {
			jTextArea = new JTextArea();
			jTextArea.addKeyListener(new KeyAdapter(){
			    public void keyTyped(KeyEvent e) {
			    	char c = e.getKeyChar();
					// I put this on the awt event queue since
					// I want the broadcast to be invoked after the
					// key has been consumed.
					SwingUtilities.invokeLater(
							new Runnable(){
								public void run(){
									textChanged(jTextArea.getText());
								}
							});
			    }
			});
		}
		// used for unit testing
		jTextArea.setName("ProducerJTextArea");
		return jTextArea;
	}

	/**
	 * Calling this method will cause all implementing interfaces of TextChangedBroadcast that have the
	 * @BroadcastConsumer annotation to be invoked.
	 */
	@Override
	@BroadcastProducer
	public void textChanged(String text) {
		// nothing to do given that we are only a producer
	}

}

