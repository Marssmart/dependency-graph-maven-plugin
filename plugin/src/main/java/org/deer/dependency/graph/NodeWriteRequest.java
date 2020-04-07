package org.deer.dependency.graph;

import com.google.common.base.CaseFormat;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.neo4j.driver.Session;
import org.neo4j.driver.TransactionWork;
import org.neo4j.driver.Value;

import java.util.*;

import static org.neo4j.driver.Values.parameters;

@Builder
@Slf4j
public class NodeWriteRequest implements Neo4jRequest {
    private final boolean root;
    private final Artifact artifact;
    private final Set<String> additionalLabels;

    @Override
    public void executeOnSession(Session session) {
        session.writeTransaction((TransactionWork<Void>) tx -> {
            final List<String> labels = new ArrayList<>();
            labels.add(":Artifact");
            labels.addAll(Optional.ofNullable(additionalLabels).orElse(Collections.emptySet()));

            if (root) {
                labels.add(":Root");
            }

            if (StringUtils.isNotBlank(artifact.getScope())) {
                labels.add(":" + CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.UPPER_CAMEL).convert(artifact.getScope()));
            }

            if (StringUtils.isNotBlank(artifact.getType())) {
                labels.add(":" + CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.UPPER_CAMEL).convert(artifact.getType()));
            }

            final String mergeQuery = String.join(COMMA,
                    dottedEquals("groupId", artifact.getGroupId()),
                    dottedEquals("artifactId", artifact.getArtifactId()),
                    dottedEquals("version", artifact.getVersion()));

            final String query = String.join(SPACE, mergeOnCreateSet(String.join(EMPTY, labels), mergeQuery),
                    equals("a.groupId", "$groupId"), COMMA,
                    equals("a.artifactId", "$artifactId"), COMMA,
                    equals("a.version", "$version"));

            final Value parameters = parameters("groupId", artifact.getGroupId(),
                    "artifactId", artifact.getArtifactId(),
                    "version", artifact.getVersion());

            log.debug("Executing query {} with params {}", query, parameters);

            tx.run(query, parameters);
            return null;
        });
    }
}
