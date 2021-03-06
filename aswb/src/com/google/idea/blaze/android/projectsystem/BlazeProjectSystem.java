/*
 * Copyright 2017 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.idea.blaze.android.projectsystem;

import static org.jetbrains.android.dom.manifest.AndroidManifestUtils.getPackageName;
import static org.jetbrains.android.facet.SourceProviderUtil.createSourceProvidersForLegacyModule;

import com.android.tools.apk.analyzer.AaptInvoker;
import com.android.tools.idea.log.LogWrapper;
import com.android.tools.idea.model.AndroidModel;
import com.android.tools.idea.projectsystem.AndroidModuleSystem;
import com.android.tools.idea.projectsystem.AndroidProjectSystem;
import com.android.tools.idea.projectsystem.ProjectSystemSyncManager;
import com.android.tools.idea.projectsystem.SourceProviders;
import com.android.tools.idea.projectsystem.SourceProvidersFactory;
import com.android.tools.idea.res.AndroidInnerClassFinder;
import com.android.tools.idea.res.AndroidResourceClassPsiElementFinder;
import com.android.tools.idea.sdk.AndroidSdks;
import com.google.common.collect.ImmutableList;
import com.google.idea.blaze.android.resources.BlazeLightResourceClassService;
import com.google.idea.blaze.android.sync.model.idea.BlazeAndroidModel;
import com.google.idea.blaze.base.actions.BlazeBuildService;
import com.intellij.facet.ProjectFacetManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElementFinder;
import com.intellij.psi.search.GlobalSearchScope;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.facet.SourceProviderManager;
import org.jetbrains.annotations.NotNull;

/**
 * Base class to implement common methods in {@link AndroidProjectSystem} for blaze with different
 * sdk
 */
public class BlazeProjectSystem implements AndroidProjectSystem {
  protected final Project project;
  protected final ProjectSystemSyncManager syncManager;
  protected final List<PsiElementFinder> myFinders;

  public BlazeProjectSystem(Project project) {
    this.project = project;
    syncManager = new BlazeProjectSystemSyncManager(project);

    myFinders =
        Arrays.asList(
            AndroidInnerClassFinder.INSTANCE,
            new AndroidResourceClassPsiElementFinder(getLightResourceClassService()));
  }

  @Override
  public boolean allowsFileCreation() {
    return true;
  }

  @Nullable
  @Override
  public VirtualFile getDefaultApkFile() {
    return null;
  }

  @Override
  public Path getPathToAapt() {
    return AaptInvoker.getPathToAapt(
        AndroidSdks.getInstance().tryToChooseSdkHandler(),
        new LogWrapper(BlazeProjectSystem.class));
  }

  @Override
  public void buildProject() {
    BlazeBuildService.getInstance().buildProject(project);
  }

  // @Override #api3.6
  public String mergeBuildFiles(
      String dependencies, String destinationContents, @Nullable String supportLibVersionFilter) {
    // TODO: check if necessary to implement.
    return "";
  }

  // #api 3.4
  public boolean upgradeProjectToSupportInstantRun() {
    return false;
  }

  @Override
  public AndroidModuleSystem getModuleSystem(Module module) {
    return BlazeModuleSystem.getInstance(module);
  }

  @Override
  public ProjectSystemSyncManager getSyncManager() {
    return syncManager;
  }

  @Nonnull
  @Override
  public Collection<PsiElementFinder> getPsiElementFinders() {
    return myFinders;
  }

  @Nonnull
  @Override
  public BlazeLightResourceClassService getLightResourceClassService() {
    return BlazeLightResourceClassService.getInstance(project);
  }

  @NotNull
  @Override
  public SourceProvidersFactory getSourceProvidersFactory() {
    return new SourceProvidersFactory() {
      @Nullable
      @Override
      public SourceProviders createSourceProvidersFor(@NotNull AndroidFacet facet) {
        BlazeAndroidModel model = ((BlazeAndroidModel) AndroidModel.get(facet));
        if (model != null) {
          return SourceProvidersCompat.forModel(model);
        } else {
          return createSourceProvidersForLegacyModule(facet);
        }
      }
    };
  }

  @NotNull
  // #api41 @Override
  public Collection<AndroidFacet> getAndroidFacetsWithPackageName(
      @NotNull Project project, @NotNull String packageName, @NotNull GlobalSearchScope scope) {
    List<AndroidFacet> facets = ProjectFacetManager.getInstance(project).getFacets(AndroidFacet.ID);
    return facets.stream()
        .filter(facet -> hasPackageName(facet, packageName))
        .filter(
            facet -> {
              VirtualFile file = SourceProviderManager.getInstance(facet).getMainManifestFile();
              if (file == null) {
                return false;
              } else {
                return scope.contains(file);
              }
            })
        .collect(Collectors.toList());
  }

  @NotNull
  @Override
  public Collection<Module> getSubmodules() {
    return ImmutableList.of();
  }

  private static boolean hasPackageName(AndroidFacet facet, String packageName) {
    String nameFromFacet = getPackageName(facet);
    if (nameFromFacet == null) {
      return false;
    }
    return nameFromFacet.equals(packageName);
  }
}
