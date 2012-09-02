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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.tickcode.broadcast.BroadcastConsumer;
import org.tickcode.broadcast.BroadcastProducer;
import org.tickcode.broadcast.MessageBroker;




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
