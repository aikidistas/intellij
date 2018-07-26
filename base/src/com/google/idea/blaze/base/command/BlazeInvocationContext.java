/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
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
package com.google.idea.blaze.base.command;

import com.google.idea.blaze.base.run.ExecutorType;
import com.intellij.execution.configurations.ConfigurationType;

/**
 * The context in which a blaze command is invoked. Depending on the {@link ContextType}, may
 * include additional information, such as the run configuration type.
 */
public interface BlazeInvocationContext {

  /** The context in which a blaze command is invoked. */
  enum ContextType {
    Sync,
    RunConfiguration,
    Other,
  }

  ContextType type();

  SyncContext SYNC_CONTEXT = new SyncContext();

  static RunConfigurationContext runConfigContext(
      ExecutorType executorType, ConfigurationType configurationType) {
    return new RunConfigurationContext(executorType, configurationType);
  }

  /** Invocation context for sync-related build actions. */
  final class SyncContext implements BlazeInvocationContext {
    private SyncContext() {}

    @Override
    public final ContextType type() {
      return ContextType.Sync;
    }
  }

  /** Invocation context for run configuration build actions. */
  final class RunConfigurationContext implements BlazeInvocationContext {
    public final ExecutorType executorType;
    public final ConfigurationType configurationType;

    private RunConfigurationContext(
        ExecutorType executorType, ConfigurationType configurationType) {
      this.executorType = executorType;
      this.configurationType = configurationType;
    }

    @Override
    public final ContextType type() {
      return ContextType.RunConfiguration;
    }
  }
}
