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
