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

import java.awt.Component;
import java.util.concurrent.CountDownLatch;

import javax.swing.JButton;
import javax.swing.JMenuBar;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.junit.Assert;
import org.junit.Test;
import org.tickcode.broadcast.AbstractMessageBroker;


public class MainTest {

	@Test
	public void test() throws Exception {
		AbstractMessageBroker.setUsingAspectJ(true);
		final Main main = new Main();
		
		// At this point I'm expecting that DependencyInjection.aj did my dependency
		// injection and that I have a fully functional GUI

		try {
			Assert.assertNotNull(((JMenuBar) TestUtils.getChildNamed(main,
					"MainMenuBar")));
			
			
			Assert.assertNotNull(((JTextArea) TestUtils.getChildNamed(main,
					"ProducerJTextArea")));
			Assert.assertNotNull(((JTextArea) TestUtils.getChildNamed(main,
					"ConsumeAndProduceJTextArea")));
			Assert.assertNotNull(((JTextArea) TestUtils.getChildNamed(main,
					"ConsumerJTextArea")));

			main.setVisible(true);

			Assert.assertEquals("", ((JTextArea) TestUtils.getChildNamed(main,
					"ProducerJTextArea")).getText());
			Assert.assertEquals("", ((JTextArea) TestUtils.getChildNamed(main,
					"ConsumeAndProduceJTextArea")).getText());
			Assert.assertEquals("", ((JTextArea) TestUtils.getChildNamed(main,
					"ConsumerJTextArea")).getText());

			TestUtils.simulateKeyStrokes(((JTextArea) TestUtils.getChildNamed(
					main, "ProducerJTextArea")), "Hello World");

			final CountDownLatch latch = new CountDownLatch(1);
			
			// I did not want to have to put in a Thread.sleep() to wait for the
			// event to be consume the last character(s) of Hello World 
			// so I simply put the Asserts inside the awt dispatch thread
			// so they are queued for after the Consumer/Producers broadcast
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					Assert.assertEquals("Hello World", ((JTextArea) TestUtils
							.getChildNamed(main, "ProducerJTextArea"))
							.getText());

					Assert.assertEquals("Hello World", ((JTextArea) TestUtils
							.getChildNamed(main, "ConsumeAndProduceJTextArea"))
							.getText());
					Assert.assertEquals("Hello World", ((JTextArea) TestUtils
							.getChildNamed(main, "ConsumerJTextArea"))
							.getText());
					latch.countDown();
				}
			});

			latch.await();
			
		} finally {
			main.setVisible(false);
			main.dispose();
		}
	}


	public void testPopUp(Component parent) throws Exception {
		final JButton popup = (JButton) TestUtils
				.getChildNamed(parent, "popup");
		Assert.assertNotNull(popup);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				popup.doClick();
			}
		});

		JButton ok = null;

		// The dialog box will show up shortly
		for (int i = 0; ok == null; ++i) {
			Thread.sleep(200);
			ok = (JButton) TestUtils.getChildIndexed(parent, "JButton", 0);
			Assert.assertTrue(i < 10);
		}
		Assert.assertEquals(UIManager.getString("OptionPane.okButtonText"),
				ok.getText());

		ok.doClick();
	}

}
