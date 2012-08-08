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
