/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.ide.idea.project;

import com.intellij.ProjectTopics;
import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.facet.ProjectFacetManager;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.javaee.web.facet.WebFacetConfiguration;
import com.intellij.javaee.web.facet.WebFacetType;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.ModuleListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;

import com.liferay.ide.idea.util.FileUtil;
import com.liferay.ide.idea.util.LiferayWorkspaceUtil;

import org.jetbrains.annotations.NotNull;

/**
 * @author Dominik Marks
 * @author Joye Luo
 * @author Charles Wu
 */
public class LiferayProjectComponent implements ProjectComponent {

	@Override
	public void disposeComponent() {
		_messageBusConnection.disconnect();
	}

	@Override
	public void initComponent() {
		MessageBus messageBus = _project.getMessageBus();

		_messageBusConnection = messageBus.connect();

		ModuleListener moduleListener = new ModuleListener() {

			@Override
			public void moduleAdded(@NotNull Project project, @NotNull Module module) {
				if (LiferayWorkspaceUtil.isValidWorkspaceLocation(project)) {
					_addWebRoot(module);
				}
			}

		};

		_messageBusConnection.subscribe(ProjectTopics.MODULES, moduleListener);
	}

	protected LiferayProjectComponent(Project project) {
		_project = project;
	}

	private void _addWebRoot(Module module) {
		ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);

		VirtualFile[] sourceRoots = moduleRootManager.getSourceRoots();

		if (sourceRoots.length > 0) {
			for (VirtualFile sourceRoot : sourceRoots) {
				String sourcePath = sourceRoot.getPath();

				if (sourcePath.contains("src/main/resources")) {
					String resourcesPath = sourcePath.concat("/META-INF/resources");

					LocalFileSystem localFileSystem = LocalFileSystem.getInstance();

					VirtualFile resources = localFileSystem.findFileByPath(resourcesPath);

					if (FileUtil.exist(resources)) {
						boolean hasWebFacet = false;

						FacetManager facetManager = FacetManager.getInstance(module);

						Facet<?>[] facets = facetManager.getAllFacets();

						for (Facet<?> facet : facets) {
							WebFacetType webFacetType = WebFacetType.getInstance();

							FacetType<?, ?> facetType = facet.getType();

							String facetTypePresentableName = facetType.getPresentableName();

							if (facetTypePresentableName.equals(webFacetType.getPresentableName())) {
								hasWebFacet = true;

								break;
							}
						}

						if (!hasWebFacet) {
							ProjectFacetManager projectFacetManager = ProjectFacetManager.getInstance(
								module.getProject());

							WebFacetConfiguration webFacetConfiguration =
								projectFacetManager.createDefaultConfiguration(WebFacetType.getInstance());

							ModifiableFacetModel modifiableFacetModel = facetManager.createModifiableModel();

							WebFacetType webFacetType = WebFacetType.getInstance();

							WebFacet webFacet = facetManager.createFacet(
								webFacetType, webFacetType.getPresentableName(), webFacetConfiguration, null);

							webFacet.addWebRoot(resources, "/");

							modifiableFacetModel.addFacet(webFacet);

							Application application = ApplicationManager.getApplication();

							application.invokeLater(() -> application.runWriteAction(modifiableFacetModel::commit));
						}
					}
				}
			}
		}
	}

	private MessageBusConnection _messageBusConnection;
	private final Project _project;

}