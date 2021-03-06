/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.gef.examples.ediagram.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.MouseWheelHandler;
import org.eclipse.gef.MouseWheelZoomHandler;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.requests.GroupRequest;
import org.eclipse.gef.ui.actions.DirectEditAction;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.actions.ToggleSnapToGeometryAction;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.gef.ui.palette.PaletteCustomizer;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;

import org.eclipse.gef.examples.ediagram.EDiagramPlugin;
import org.eclipse.gef.examples.ediagram.edit.parts.EDiagramPartFactory;
import org.eclipse.gef.examples.ediagram.model.Diagram;
import org.eclipse.gef.examples.ediagram.model.commands.DeleteCommand;
import org.eclipse.gef.examples.ediagram.outline.EDiagramOutlinePage;

public class EDiagramEditor 
	extends GraphicalEditorWithFlyoutPalette
{

protected static final String PALETTE_DOCK_LOCATION = "Dock location"; //$NON-NLS-1$
protected static final String PALETTE_SIZE = "Palette Size"; //$NON-NLS-1$
protected static final String PALETTE_STATE = "Palette state"; //$NON-NLS-1$

private Diagram diagram;
private PaletteRoot paletteRoot;
private final ResourceSet rsrcSet = new ResourceSetImpl();

public EDiagramEditor() {
	setEditDomain(new DefaultEditDomain(this));
}

public void commandStackChanged(EventObject event) {
	firePropertyChange(PROP_DIRTY);
	super.commandStackChanged(event);
}

protected void configureGraphicalViewer() {
	super.configureGraphicalViewer();
	GraphicalViewer viewer = getGraphicalViewer();

	ScalableFreeformRootEditPart root = new ScalableFreeformRootEditPart();
	viewer.setRootEditPart(root);
	viewer.setEditPartFactory(EDiagramPartFactory.getInstance());
	
	getActionRegistry().registerAction(new ToggleSnapToGeometryAction(viewer));
	
	KeyHandler keyHandler = new GraphicalViewerKeyHandler(viewer) {
		public boolean keyPressed(KeyEvent event) {
			if (event.stateMask == SWT.MOD1 && event.keyCode == SWT.DEL) {
				List objects = getGraphicalViewer().getSelectedEditParts();
				if (objects == null || objects.isEmpty())
					return true;
				GroupRequest deleteReq = new GroupRequest(RequestConstants.REQ_DELETE);
				deleteReq.getExtendedData().put(
						DeleteCommand.KEY_DELETE_FROM_ECORE, Boolean.TRUE);
				CompoundCommand compoundCmd = new CompoundCommand("Delete");
				for (int i = 0; i < objects.size(); i++) {
					EditPart object = (EditPart) objects.get(i);
					Command cmd = object.getCommand(deleteReq);
					if (cmd != null) compoundCmd.add(cmd);
				}
				getCommandStack().execute(compoundCmd);
				return true;
			}
			return super.keyPressed(event);
		}
	};
	keyHandler.put(KeyStroke.getPressed(SWT.F2, 0),
			getActionRegistry().getAction(GEFActionConstants.DIRECT_EDIT));
	viewer.setKeyHandler(keyHandler);
	
	// Scroll-wheel Zoom
	viewer.setProperty(MouseWheelHandler.KeyGenerator.getKey(SWT.MOD1), 
			MouseWheelZoomHandler.SINGLETON);
	// Keyboard zoom
	IAction zoomIn = new ZoomInAction(root.getZoomManager());
	IAction zoomOut = new ZoomOutAction(root.getZoomManager());
	getActionRegistry().registerAction(zoomIn);
	getActionRegistry().registerAction(zoomOut);
	getSite().getKeyBindingService().registerAction(zoomIn);
	getSite().getKeyBindingService().registerAction(zoomOut);
}

protected void createActions() {
	super.createActions();
	
	Action action = new DirectEditAction((IWorkbenchPart)this);
	getActionRegistry().registerAction(action);
	getSelectionActions().add(action.getId());
}

protected PaletteViewerProvider createPaletteViewerProvider() {
	return new PaletteViewerProvider(getEditDomain()) {
		protected void configurePaletteViewer(PaletteViewer viewer) {
			super.configurePaletteViewer(viewer);
			viewer.setCustomizer(new PaletteCustomizer() {
				public void revertToSaved() {}
				public void save() {}
			});
			viewer.addDragSourceListener(new TemplateTransferDragSourceListener(viewer));
		}
	};
}

public Object getAdapter(Class type) {
	if (type == IContentOutlinePage.class)
		return new EDiagramOutlinePage(diagram, getActionRegistry());
	return super.getAdapter(type);
}

protected PaletteRoot getPaletteRoot() {
	if (paletteRoot == null)
		paletteRoot = EDiagramPaletteFactory.createPalette();
	return paletteRoot;
}

public void doSave(IProgressMonitor monitor) {
	ArrayList saveFailed = new ArrayList();
	for (Iterator iter = rsrcSet.getResources().iterator(); iter.hasNext();) {
		Resource rsrc = (Resource)iter.next();
		try {
			if (!isReadOnly(rsrc))
				rsrc.save(Collections.EMPTY_MAP);
		} catch (Exception e) {
			EDiagramPlugin.INSTANCE.log(e);
			saveFailed.add(rsrc);
		}
	}
	if (saveFailed.isEmpty())
		getCommandStack().markSaveLocation();
	else {
		String error = "The following resources could not be saved:\n";
		for (Iterator iter = saveFailed.iterator(); iter.hasNext();)
			error += '\n' + ((Resource)iter.next()).getURI().toString();
		error += "\n\nSee the error log for details.";
		MessageDialog dialog = new MessageDialog(getGraphicalControl().getShell(), 
				"Errors Detected", null, error, MessageDialog.ERROR,
				new String[] {"OK"}, 0);
		dialog.open();		
	}
}

public void doSaveAs() {
}

protected void initializeGraphicalViewer() {
	super.initializeGraphicalViewer();
	GraphicalViewer viewer = getGraphicalViewer();
	viewer.setContents(diagram);
	viewer.addDropTargetListener(
			(TransferDropTargetListener)new DiagramDropTargetListener(viewer));
	viewer.addDropTargetListener(
			(TransferDropTargetListener)new EDiagramPaletteDropListener(viewer));
}

protected boolean isReadOnly(Resource resource) {
	return resource.getURI().toString().startsWith("platform:/plugin/"); //$NON-NLS-1$
}

public boolean isSaveAsAllowed() {
	return false;
}

protected void setInput(IEditorInput input) {
	super.setInput(input);

	IFile file = ((IFileEditorInput)input).getFile();
	URI uri = URI.createPlatformResourceURI(file.getFullPath().toString());
	Resource resource = rsrcSet.getResource(uri, true);
	diagram = (Diagram)resource.getContents().get(0);
	
	setPartName(file.getName());
}

}