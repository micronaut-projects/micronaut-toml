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

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

public class JFlexPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(JavaPlugin.class);
        JavaPluginExtension javaExt = project.getExtensions().getByType(JavaPluginExtension.class);
        TaskProvider<JFlexTask> generateLexer = project.getTasks().register("generateLexer", JFlexTask.class, task -> {
            task.setGroup(LifecycleBasePlugin.BUILD_GROUP);
            task.setDescription("Generates lexer files from JFlex grammar files.");
            task.getSourceDirectory().convention(
                    project.getLayout().getProjectDirectory().dir("src/main/jflex")
            );
            task.getOutputDirectory().convention(
                    project.getLayout().getBuildDirectory().dir("generated/jflex")
            );
        });
        // Register the output of the JFlex task as generated sources
        javaExt.getSourceSets()
                .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
                .getJava()
                .srcDir(generateLexer);
    }
}
