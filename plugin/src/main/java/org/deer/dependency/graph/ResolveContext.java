/*
 * This file is part of dependency-graph-maven-plugin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (c) 2020 Jan Srnicek. All rights reserved.
 */

package org.deer.dependency.graph;

import lombok.Getter;
import org.apache.maven.artifact.Artifact;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ResolveContext {

    private final List<Artifact> failedArtifacts = new ArrayList<>();
    private final List<Artifact> processedArtifacts = new ArrayList<>();

    public void addFailedArtifact(Artifact artifact) {
        failedArtifacts.add(artifact);
    }

    public void addProcessedArtifact(Artifact artifact) {
        processedArtifacts.add(artifact);
    }

    public boolean isNotFailedArtifact(Artifact artifact) {
        return !failedArtifacts.contains(artifact);
    }

    public boolean isProcessedArtifact(Artifact artifact) {
        return processedArtifacts.stream().anyMatch(processed -> processed.compareTo(artifact) == 0);
    }

}
