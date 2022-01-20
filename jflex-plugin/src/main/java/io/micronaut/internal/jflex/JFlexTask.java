/*
 * Copyright 2003-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.internal.jflex;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;

@CacheableTask
public abstract class JFlexTask extends DefaultTask {

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getSourceDirectory();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @Inject
    protected abstract WorkerExecutor getWorkerExecutor();

    @TaskAction
    public void generateSources() {
        // We're using classloader isolation, because the JFlex API
        // uses static state!
        getWorkerExecutor()
                .classLoaderIsolation()
                .submit(JFlexAction.class, params -> {
                    params.getSourceDirectory().set(getSourceDirectory());
                    params.getSourceFiles().from(getSourceDirectory());
                    params.getOutputDirectory().set(getOutputDirectory());
                });
    }
}
