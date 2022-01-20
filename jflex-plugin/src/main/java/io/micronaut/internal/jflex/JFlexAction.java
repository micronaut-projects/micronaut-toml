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

import jflex.core.OptionUtils;
import jflex.generator.LexGenerator;
import jflex.option.Options;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public abstract class JFlexAction implements WorkAction<JFlexAction.Parameters> {
    @Override
    public void execute() {
        OptionUtils.setDefaultOptions();
        Path sourcePath = getParameters().getSourceDirectory().getAsFile().get().toPath();
        File outputDirectory = getParameters().getOutputDirectory().getAsFile().get();
        OptionUtils.setDir(outputDirectory);
        File skeletonFile = getParameters().getSkeletonFile().getAsFile().get();
        OptionUtils.setSkeleton(skeletonFile);
        Options.dump = false;
        Options.encoding = StandardCharsets.UTF_8;
        Options.no_backup = true;
        getParameters().getSourceFiles()
                .getAsFileTree()
                .getFiles()
                .forEach(jflexFile -> generateSourceFileFor(jflexFile, outputDirectory, sourcePath));
    }

    private void generateSourceFileFor(File jflexFile, File outputDirectory, Path sourcePath) {
        String relativePath = sourcePath.relativize(jflexFile.getParentFile().toPath()).toString();
        OptionUtils.setDir(new File(outputDirectory, relativePath));
        new LexGenerator(jflexFile).generate();
    }

    public interface Parameters extends WorkParameters {
        DirectoryProperty getSourceDirectory();
        ConfigurableFileCollection getSourceFiles();
        DirectoryProperty getOutputDirectory();
        RegularFileProperty getSkeletonFile();
    }
}
