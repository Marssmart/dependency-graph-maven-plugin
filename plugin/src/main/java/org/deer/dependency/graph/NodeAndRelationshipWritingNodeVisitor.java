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

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.traversal.DependencyNodeVisitor;
import org.neo4j.driver.Session;

import java.util.List;

@Slf4j
@Builder
public class NodeAndRelationshipWritingNodeVisitor implements DependencyNodeVisitor {

    public static final String INCLUDES_DIRECTLY = "INCLUDES_DIRECTLY";
    public static final String INCLUDES_AFTER_RESOLVE = "INCLUDES_AFTER_RESOLVE";
    private final Session session;
    private final List<Dependency> dependencies;
    private final boolean ignoreRoot;
    private final boolean onlyDirectDependencies;

    @Override
    public boolean visit(DependencyNode node) {
        final boolean root = !ignoreRoot && node.getParent() == null;
        final Artifact artifact = node.getArtifact();

        log.info("Processing artifact {}", artifact);

        final NodeWriteRequest nodeWriteRequest = NodeWriteRequest.builder().artifact(artifact)
                .root(root)
                .build();
        nodeWriteRequest.executeOnSession(session);

        if (node.getParent() != null) {
            final boolean isDirectDependency = dependencies.stream()
                    .anyMatch(dependency -> new EqualsBuilder()
                            .append(dependency.getGroupId(), artifact.getGroupId())
                            .append(dependency.getArtifactId(), artifact.getArtifactId())
                            .append(dependency.getVersion(), artifact.getVersion())
                            .append(dependency.getClassifier(), artifact.getClassifier())
                            .isEquals());

            if (onlyDirectDependencies && !isDirectDependency) {
                return true;
            }

            final String relationshipName = isDirectDependency ? INCLUDES_DIRECTLY : INCLUDES_AFTER_RESOLVE;

            final RelationshipWriteRequest relationshipWriteRequest = RelationshipWriteRequest.builder()
                    .left(node.getParent().getArtifact())
                    .right(artifact)
                    .relationshipName(relationshipName)
                    .build();

            relationshipWriteRequest.executeOnSession(session);
        }

        return true;
    }

    @Override
    public boolean endVisit(DependencyNode node) {
        return true;
    }
}
