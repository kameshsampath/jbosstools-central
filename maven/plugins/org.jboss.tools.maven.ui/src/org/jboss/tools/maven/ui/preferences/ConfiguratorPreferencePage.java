/*************************************************************************************
 * Copyright (c) 2009-2011 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.maven.ui.preferences;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jboss.tools.maven.ui.Activator;
import org.jboss.tools.maven.ui.Messages;
import org.jboss.tools.maven.ui.wizard.ConfigureMavenRepositoriesWizard;
import org.osgi.framework.Bundle;

/**
 * 
 * @author snjeza
 *
 */
public class ConfiguratorPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	
	private static final String ORG_JBOSS_TOOLS_MAVEN_JSF = "org.jboss.tools.maven.jsf"; //$NON-NLS-1$
	private static final String ORG_JBOSS_TOOLS_MAVEN_PORTLET = "org.jboss.tools.maven.portlet"; //$NON-NLS-1$
	private static final String ORG_JBOSS_TOOLS_MAVEN_CDI = "org.jboss.tools.maven.cdi"; //$NON-NLS-1$
	private static final String ORG_JBOSS_TOOLS_MAVEN_HIBERNATE = "org.jboss.tools.maven.hibernate"; //$NON-NLS-1$
	private static final String ORG_JBOSS_TOOLS_MAVEN_SEAM = "org.jboss.tools.maven.seam"; //$NON-NLS-1$
	private static final String ORG_JBOSS_TOOLS_MAVEN_JAXRS = "org.jboss.tools.maven.jaxrs"; //$NON-NLS-1$
	private static final String ORG_JBOSS_TOOLS_MAVEN_JPA = "org.jboss.tools.maven.jpa"; //$NON-NLS-1$
	private static final String ORG_JBOSS_TOOLS_MAVEN_GWT = "org.jboss.tools.maven.gwt"; //$NON-NLS-1$

	private static final String ORG_ECLIPSE_M2E_WTP_JAXRS = "org.eclipse.m2e.wtp.jaxrs"; //$NON-NLS-1$
	private static final String ORG_ECLIPSE_M2E_WTP_JPA = "org.eclipse.m2e.wtp.jpa"; //$NON-NLS-1$
	private static final String ORG_ECLIPSE_M2E_WTP_JSF = "org.eclipse.m2e.wtp.jsf"; //$NON-NLS-1$

	private Button configureSeamButton;
	private Button configureSeamRuntimeButton;
	private Button configureSeamArtifactsButton;
	private Button configureJSFButton;
	private Button configureWebxmlJSF20Button;
	private Button configurePortletButton;
	private Button configureJSFPortletButton;
	private Button configureSeamPortletButton;
	private Button configureCDIButton;
	private Button configureHibernateButton;
	private Button configureJaxRsButton;
	private Button configureJpaButton;
	private Button configureGwtButton;
	private Image jbossImage;
	
	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);
		
		Label label = new Label(composite, SWT.NONE);
		label.setText(Messages.ConfiguratorPreferencePage_When_importing_Maven_projects_configure_the_following);
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		
		if (bundleExists(ORG_JBOSS_TOOLS_MAVEN_SEAM)) { 
			configureSeamButton = new Button(composite,SWT.CHECK);
			configureSeamButton.setText(Messages.ConfiguratorPreferencePage_Configure_Seam_when_importing_Maven_projects);
			boolean configureSeam = store.getBoolean(Activator.CONFIGURE_SEAM);
			configureSeamButton.setSelection(configureSeam);
		
			configureSeamRuntimeButton = new Button(composite,SWT.CHECK);
			configureSeamRuntimeButton.setText(Messages.ConfiguratorPreferencePage_Configure_Seam_Runtime);
			boolean configureSeamRuntime = store.getBoolean(Activator.CONFIGURE_SEAM_RUNTIME);
			configureSeamRuntimeButton.setSelection(configureSeamRuntime);
			configureSeamRuntimeButton.setEnabled(configureSeam);
		
			configureSeamArtifactsButton = new Button(composite,SWT.CHECK);
			configureSeamArtifactsButton.setText(Messages.ConfiguratorPreferencePage_Configure_Seam_Artifacts);
			boolean configureSeamArtifacts = store.getBoolean(Activator.CONFIGURE_SEAM_ARTIFACTS);
			configureSeamArtifactsButton.setSelection(configureSeamArtifacts);
			configureSeamArtifactsButton.setEnabled(configureSeam);
		
			configureSeamButton.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					configureSeamRuntimeButton.setEnabled(configureSeamButton.getSelection());
					configureSeamArtifactsButton.setEnabled(configureSeamButton.getSelection());
				}
		
			});
		}
		
		if (bundleExists(ORG_JBOSS_TOOLS_MAVEN_JSF) && !bundleExists(ORG_ECLIPSE_M2E_WTP_JSF) ) { 
			configureJSFButton = new Button(composite,SWT.CHECK);
			configureJSFButton.setText(Messages.ConfiguratorPreferencePage_Configure_JSF_facet);
			boolean configureJSF = store.getBoolean(Activator.CONFIGURE_JSF);
			configureJSFButton.setSelection(configureJSF);
			
			configureWebxmlJSF20Button = new Button(composite,SWT.CHECK);
			configureWebxmlJSF20Button.setText(Messages.ConfiguratorPreferencePage_Configure_Webxml_JSF20);
			boolean configureWebxml = store.getBoolean(Activator.CONFIGURE_WEBXML_JSF20);
			configureWebxmlJSF20Button.setSelection(configureWebxml);
		}
		
		if (bundleExists(ORG_JBOSS_TOOLS_MAVEN_PORTLET)) { 
			configurePortletButton = new Button(composite,SWT.CHECK);
			configurePortletButton.setText(Messages.ConfiguratorPreferencePage_Configure_JBoss_Portlet_Core_facet);
			boolean configurePortlet = store.getBoolean(Activator.CONFIGURE_PORTLET);
			configurePortletButton.setSelection(configurePortlet);
		
		
			configureJSFPortletButton = new Button(composite,SWT.CHECK);
			configureJSFPortletButton.setText(Messages.ConfiguratorPreferencePage_Configure_JBoss_JSF_Portlet_facet);
			boolean configureJSFPortlet = store.getBoolean(Activator.CONFIGURE_JSFPORTLET);
			configureJSFPortletButton.setSelection(configureJSFPortlet);
		
			configureSeamPortletButton = new Button(composite,SWT.CHECK);
			configureSeamPortletButton.setText(Messages.ConfiguratorPreferencePage_Configure_JBoss_Seam_Portlet_facet);
			boolean configureSeamPortlet = store.getBoolean(Activator.CONFIGURE_SEAMPORTLET);
			configureSeamPortletButton.setSelection(configureSeamPortlet);
		}
		
		if (bundleExists(ORG_JBOSS_TOOLS_MAVEN_CDI)) { 
			configureCDIButton = new Button(composite,SWT.CHECK);
			configureCDIButton.setText(Messages.ConfiguratorPreferencePage_Configure_CDI_facet);
			boolean configureCDI = store.getBoolean(Activator.CONFIGURE_CDI);
			configureCDIButton.setSelection(configureCDI);
		}
		
		if (bundleExists(ORG_JBOSS_TOOLS_MAVEN_HIBERNATE)) { 
			configureHibernateButton = new Button(composite,SWT.CHECK);
			configureHibernateButton.setText(Messages.ConfiguratorPreferencePage_Configure_Hibernate);
			boolean configureHibernate = store.getBoolean(Activator.CONFIGURE_HIBERNATE);
			configureHibernateButton.setSelection(configureHibernate);
		}
		
		if (bundleExists(ORG_JBOSS_TOOLS_MAVEN_JAXRS) && !bundleExists(ORG_ECLIPSE_M2E_WTP_JAXRS) ) { 
			configureJaxRsButton = new Button(composite,SWT.CHECK);
			configureJaxRsButton.setText(Messages.ConfiguratorPreferencePage_Configure_JAX_RS_facet);
			boolean configureJaxRs = store.getBoolean(Activator.CONFIGURE_JAXRS);
			configureJaxRsButton.setSelection(configureJaxRs);
		}
		
		if (bundleExists(ORG_JBOSS_TOOLS_MAVEN_JPA) && !bundleExists(ORG_ECLIPSE_M2E_WTP_JPA) ) { 
			configureJpaButton = new Button(composite,SWT.CHECK);
			configureJpaButton.setText(Messages.ConfiguratorPreferencePage_Configure_JPA_facet);
			boolean configureJpa = store.getBoolean(Activator.CONFIGURE_JPA);
			configureJpaButton.setSelection(configureJpa);
		}
		
		if (bundleExists(ORG_JBOSS_TOOLS_MAVEN_GWT)) { 
			configureGwtButton = new Button(composite,SWT.CHECK);
			configureGwtButton.setText(Messages.ConfiguratorPreferencePage_Configure_GWT);
			boolean configureJpa = store.getBoolean(Activator.CONFIGURE_GWT);
			configureGwtButton.setSelection(configureJpa);
		}

		Button configureMavenButton = new Button(composite, SWT.PUSH);
		configureMavenButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, true, false));
		configureMavenButton.setText("Configure Maven Repositories...");
		configureMavenButton.setImage(getJBossImage());
		
		configureMavenButton.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				ConfigureMavenRepositoriesWizard wizard = new ConfigureMavenRepositoriesWizard();
				WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);
				wizard.init(null, null);
				dialog.create();
				dialog.open(); 
			}
		});
		
		return composite;
	}
	
	private Image getJBossImage() {
		if (jbossImage == null) {
			ImageDescriptor desc = Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
					"icons/jboss.png"); //$NON-NLS-1$
			jbossImage = desc.createImage();
		}
		return jbossImage;
	}

	private boolean bundleExists(String bundleId) {
		Bundle bundle = Platform.getBundle(bundleId);
		return bundle != null;
	}

	public void init(IWorkbench workbench) {
	}

	@Override
	protected void performDefaults() {
		if (bundleExists(ORG_JBOSS_TOOLS_MAVEN_JSF) && !bundleExists(ORG_ECLIPSE_M2E_WTP_JSF) ) { 
			configureJSFButton.setSelection(Activator.CONFIGURE_JSF_VALUE);
			configureWebxmlJSF20Button.setSelection(Activator.CONFIGURE_WEBXML_JSF20_VALUE);
		}
		if (bundleExists(ORG_JBOSS_TOOLS_MAVEN_SEAM)) { 
			configureSeamButton.setSelection(Activator.CONFIGURE_SEAM_VALUE);
			configureSeamRuntimeButton.setSelection(Activator.CONFIGURE_SEAM_RUNTIME_VALUE);
			configureSeamArtifactsButton.setSelection(Activator.CONFIGURE_SEAM_ARTIFACTS_VALUE);
		}
		
		if (bundleExists(ORG_JBOSS_TOOLS_MAVEN_PORTLET)) { 
			configurePortletButton.setSelection(Activator.CONFIGURE_PORTLET_VALUE);
			configureJSFPortletButton.setSelection(Activator.CONFIGURE_JSFPORTLET_VALUE);
			configureSeamPortletButton.setSelection(Activator.CONFIGURE_SEAMPORTLET_VALUE);
		}
		
		if (bundleExists(ORG_JBOSS_TOOLS_MAVEN_CDI)) { 
			configureCDIButton.setSelection(Activator.CONFIGURE_CDI_VALUE);
		}
		
		if (bundleExists(ORG_JBOSS_TOOLS_MAVEN_HIBERNATE)) { 
			configureHibernateButton.setSelection(Activator.CONFIGURE_HIBERNATE_VALUE);
		}
		
		if (bundleExists(ORG_JBOSS_TOOLS_MAVEN_JAXRS) && !bundleExists(ORG_ECLIPSE_M2E_WTP_JAXRS)) { 
			configureJaxRsButton.setSelection(Activator.CONFIGURE_JAXRS_VALUE);
		}

		if (bundleExists(ORG_JBOSS_TOOLS_MAVEN_JPA) && !bundleExists(ORG_ECLIPSE_M2E_WTP_JPA)) { 
			configureJpaButton.setSelection(Activator.CONFIGURE_JPA_VALUE);
		}
		
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		
		if (bundleExists(ORG_JBOSS_TOOLS_MAVEN_JSF) && !bundleExists(ORG_ECLIPSE_M2E_WTP_JSF)) { 
			store.setValue(Activator.CONFIGURE_JSF, Activator.CONFIGURE_JSF_VALUE);
		}
		
		if (bundleExists(ORG_JBOSS_TOOLS_MAVEN_PORTLET)) { 
			store.setValue(Activator.CONFIGURE_JSFPORTLET, Activator.CONFIGURE_JSFPORTLET_VALUE);
			store.setValue(Activator.CONFIGURE_SEAMPORTLET, Activator.CONFIGURE_SEAMPORTLET_VALUE);
			store.setValue(Activator.CONFIGURE_PORTLET, Activator.CONFIGURE_PORTLET_VALUE);
		}
		
		if (bundleExists(ORG_JBOSS_TOOLS_MAVEN_SEAM)) { 
			store.setValue(Activator.CONFIGURE_SEAM, Activator.CONFIGURE_SEAM_VALUE);
			store.setValue(Activator.CONFIGURE_SEAM_RUNTIME, Activator.CONFIGURE_SEAM_RUNTIME_VALUE);
			store.setValue(Activator.CONFIGURE_SEAM_ARTIFACTS, Activator.CONFIGURE_SEAM_ARTIFACTS_VALUE);
			configureSeamRuntimeButton.setEnabled(configureSeamButton.getSelection());
			configureSeamArtifactsButton.setEnabled(configureSeamButton.getSelection());
		}
		
		if (bundleExists(ORG_JBOSS_TOOLS_MAVEN_CDI)) { 
			store.setValue(Activator.CONFIGURE_CDI, Activator.CONFIGURE_CDI_VALUE);
		}
		
		if (bundleExists(ORG_JBOSS_TOOLS_MAVEN_HIBERNATE)) { 
			store.setValue(Activator.CONFIGURE_HIBERNATE, Activator.CONFIGURE_HIBERNATE_VALUE);
		}

		if (bundleExists(ORG_JBOSS_TOOLS_MAVEN_JAXRS) && !bundleExists(ORG_ECLIPSE_M2E_WTP_JAXRS)) { 
			store.setValue(Activator.CONFIGURE_JAXRS, Activator.CONFIGURE_JAXRS_VALUE);
		}

		if (bundleExists(ORG_JBOSS_TOOLS_MAVEN_JPA) && !bundleExists(ORG_ECLIPSE_M2E_WTP_JPA)) { 
			store.setValue(Activator.CONFIGURE_JPA, Activator.CONFIGURE_JPA_VALUE);
		}
		
		if (bundleExists(ORG_JBOSS_TOOLS_MAVEN_GWT)) { 
			store.setValue(Activator.CONFIGURE_GWT, Activator.CONFIGURE_GWT_VALUE);
		}
		
		super.performDefaults();
	}

	@Override
	public boolean performOk() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		if (bundleExists(ORG_JBOSS_TOOLS_MAVEN_JSF) && !bundleExists(ORG_ECLIPSE_M2E_WTP_JSF)) { 
			store.setValue(Activator.CONFIGURE_JSF, configureJSFButton.getSelection());
			store.setValue(Activator.CONFIGURE_WEBXML_JSF20, configureWebxmlJSF20Button.getSelection());
		}
		
		if (bundleExists(ORG_JBOSS_TOOLS_MAVEN_PORTLET)) {
			store.setValue(Activator.CONFIGURE_PORTLET, configurePortletButton.getSelection());
			store.setValue(Activator.CONFIGURE_JSFPORTLET, configureJSFPortletButton.getSelection());
			store.setValue(Activator.CONFIGURE_SEAMPORTLET, configureSeamPortletButton.getSelection());
		}
		
		if (bundleExists(ORG_JBOSS_TOOLS_MAVEN_SEAM)) {
			store.setValue(Activator.CONFIGURE_SEAM, configureSeamButton.getSelection());
			store.setValue(Activator.CONFIGURE_SEAM_RUNTIME, configureSeamRuntimeButton.getSelection());
			store.setValue(Activator.CONFIGURE_SEAM_ARTIFACTS, configureSeamArtifactsButton.getSelection());
		}
		
		if (bundleExists(ORG_JBOSS_TOOLS_MAVEN_CDI)) {
			store.setValue(Activator.CONFIGURE_CDI, configureCDIButton.getSelection());
		}
		
		if (bundleExists(ORG_JBOSS_TOOLS_MAVEN_HIBERNATE)) {
			store.setValue(Activator.CONFIGURE_HIBERNATE, configureHibernateButton.getSelection());
		}

		if (bundleExists(ORG_JBOSS_TOOLS_MAVEN_JAXRS) && !bundleExists(ORG_ECLIPSE_M2E_WTP_JAXRS)) {
			store.setValue(Activator.CONFIGURE_JAXRS, configureJaxRsButton.getSelection());
		}

		if (bundleExists(ORG_JBOSS_TOOLS_MAVEN_JPA) && !bundleExists(ORG_ECLIPSE_M2E_WTP_JPA)) {
			store.setValue(Activator.CONFIGURE_JPA, configureJpaButton.getSelection());
		}
		
		if (bundleExists(ORG_JBOSS_TOOLS_MAVEN_GWT)) { 
			store.setValue(Activator.CONFIGURE_GWT, configureGwtButton.getSelection());
		}
		
		return super.performOk();
	}

	@Override
	public void dispose() {
		if (jbossImage != null) {
			jbossImage.dispose();
		}
		super.dispose();
	}

		
}
