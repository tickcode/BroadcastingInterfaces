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
