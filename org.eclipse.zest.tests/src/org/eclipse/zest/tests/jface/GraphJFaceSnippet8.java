/*******************************************************************************
 * Copyright (c) 2011 Fabian Steeg. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * <p/>
 * Contributors: Fabian Steeg - initial implementation
 *******************************************************************************/
package org.eclipse.zest.tests.jface;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.zest.core.viewers.GraphViewer;
import org.eclipse.zest.core.viewers.IGraphEntityContentProvider;
import org.eclipse.zest.layouts.algorithms.RadialLayoutAlgorithm;

/**
 * Shows curved edges.
 * 
 * @author Fabian Steeg
 */
public class GraphJFaceSnippet8 {

	static class MyContentProvider implements IGraphEntityContentProvider {

		public Object[] getConnectedTo(Object entity) {
			if (entity.equals("First")) {
				return new Object[] { "First" };
			}
			if (entity.equals("Second")) {
				return new Object[] { "Third" };
			}
			if (entity.equals("Third")) {
				return new Object[] { "Second" };
			}
			return null;
		}

		public Object[] getElements(Object inputElement) {
			return new String[] { "First", "Second", "Third" };
		}

		public double getWeight(Object entity1, Object entity2) {
			return 0;
		}

		public void dispose() {

		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

		}
	}

	static GraphViewer viewer = null;

	public static void main(String[] args) {
		Display d = new Display();
		Shell shell = new Shell(d);
		shell.setLayout(new FillLayout(SWT.VERTICAL));
		shell.setSize(400, 400);
		viewer = new GraphViewer(shell, SWT.NONE);
		viewer.setContentProvider(new MyContentProvider());
		viewer.setLayoutAlgorithm(new RadialLayoutAlgorithm());
		viewer.setInput(new Object());

		shell.open();
		while (!shell.isDisposed()) {
			while (!d.readAndDispatch()) {
				d.sleep();
			}
		}

	}
}
