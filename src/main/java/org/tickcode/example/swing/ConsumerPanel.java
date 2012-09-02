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
package org.tickcode.example.swing;

import java.awt.BorderLayout;
import java.awt.ScrollPane;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.tickcode.broadcast.BroadcastConsumer;





/**
 * 
 * This class is showing how the "text changed" broadcast can
 * be consumed without being produced.  In other words,
 * we will override the method textChanged(String text) but 
 * we will never actually invoke this method from within this
 * class.
 * 
 * @author Eyon Land
 *
 */
public class ConsumerPanel extends JPanel implements TextChangedBroadcast{


	private ScrollPane scrollPane = null;  //  @jve:decl-index=0:visual-constraint="312,149"
	private JTextArea jTextArea = null;

	/**
	 * This is the default constructor
	 */
	public ConsumerPanel() {
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
		this.add(new JLabel("Consumer Panel "),BorderLayout.NORTH);
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
		}
		// used for unit testing
		jTextArea.setName("ConsumerJTextArea");
		return jTextArea;
	}
	
	@BroadcastConsumer
	public void textChanged(String text){
		jTextArea.setText(text);
	}

}
