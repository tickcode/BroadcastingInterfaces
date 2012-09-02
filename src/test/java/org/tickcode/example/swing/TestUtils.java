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

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.JMenu;
import javax.swing.SwingUtilities;

/**
 * From JavaWorld article <a href=
 * "http://www.javaworld.com/javaworld/jw-11-2004/jw-1115-swing.html?page=2"
 * >http://www.javaworld.com/javaworld/jw-11-2004/jw-1115-swing.html?page=2</a>
 * 
 * @author eyon
 * 
 */
public class TestUtils {

	static int counter;

	public static Component getChildNamed(Component parent, String name) {
		if (name.equals(parent.getName())) {
			return parent;
		}

		if (parent instanceof Container) {
			// menu items are not created until they are needed so we need to
			// call getMenuComponents for them
			Component[] children = (parent instanceof JMenu) ? ((JMenu) parent)
					.getMenuComponents() : ((Container) parent).getComponents();

			for (int i = 0; i < children.length; ++i) {
				Component child = getChildNamed(children[i], name);
				if (child != null) {
					return child;
				}
			}
		}

		return null;
	}

	public static Component getChildIndexed(Component parent, String klass,
			int index) {
		counter = 0;

		// Step in only owned windows and ignore its components in JFrame
		if (parent instanceof Window) {
			Component[] children = ((Window) parent).getOwnedWindows();

			for (int i = 0; i < children.length; ++i) {
				// Take only active windows
				if (children[i] instanceof Window
						&& !((Window) children[i]).isActive()) {
					continue;
				}

				Component child = getChildIndexedInternal(children[i], klass,
						index);
				if (child != null) {
					return child;
				}
			}
		}

		return null;
	}

	private static Component getChildIndexedInternal(Component parent,
			String klass, int index) {

		// Debug line
		// System.out.println("Class: " + parent.getClass() +
		// " Name: " + parent.getName());

		if (parent.getClass().toString().endsWith(klass)) {
			if (counter == index) {
				return parent;
			}
			++counter;
		}

		if (parent instanceof Container) {
			Component[] children = (parent instanceof JMenu) ? ((JMenu) parent)
					.getMenuComponents() : ((Container) parent).getComponents();

			for (int i = 0; i < children.length; ++i) {
				Component child = getChildIndexedInternal(children[i], klass,
						index);
				if (child != null) {
					return child;
				}
			}
		}

		return null;
	}

	/**
	 * Example came from
	 * http://tech.chitgoks.com/2010/08/31/simulate-enter-key-on
	 * -any-component-using-java/
	 * 
	 * @param c
	 */
	public static void simulateEnterKey(Component c) {
		try {
			KeyEvent ke = new KeyEvent(c, KeyEvent.KEY_RELEASED,
					System.currentTimeMillis(), -1, KeyEvent.VK_ENTER,
					(char) KeyEvent.VK_ENTER);
			c.requestFocusInWindow();
			dispatchEvent(ke, c);
		} catch (Exception ex) {
		}
	}

	public static void simulateKeyStrokes(Component comp, String text)
			throws Exception {
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			simulateKeyStroke(comp, c);
		}
	}

	public static void simulateKeyStroke(Component c, char ch) {
		try {
			KeyEvent ke = new KeyEvent(c, KeyEvent.KEY_TYPED,
					System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED,
					(char) ch);
			c.requestFocusInWindow();
			dispatchEvent(ke, c);
		} catch (Exception ex) {
		}
	}

	private static void dispatchEvent(final KeyEvent ke, final Component comp)
			throws Exception {
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					comp.dispatchEvent(ke);
				}
			});
		} else {
			comp.dispatchEvent(ke);
		}
	}
	
	public static void clickOnComponent(Component comp) throws Exception {
		MouseEvent event = new MouseEvent(comp, MouseEvent.MOUSE_CLICKED,
				System.currentTimeMillis(), 0, 0, 0, 1, false);
		java.lang.reflect.Field f = AWTEvent.class
				.getDeclaredField("focusManagerIsDispatching");
		f.setAccessible(true);
		f.set(event, Boolean.TRUE);
		((java.awt.Component) comp).dispatchEvent(event);
	}


}
