package org.deer.dependency.graph;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.Artifact;
import org.neo4j.driver.Session;

@Builder
@Slf4j
public class RelationshipWriteRequest implements Neo4jRequest {

    public static final String WHERE = "WHERE";
    public static final String AND = "AND";

    private final Artifact left;
    private final Artifact right;
    private final String relationshipName;

    @Override
    public void executeOnSession(Session session) {
        session.writeTransaction(tx -> {

            final String matchQueryLeft = String.join(COMMA, dottedEquals("groupId", left.getGroupId()),
                    dottedEquals("artifactId", left.getArtifactId()),
                    dottedEquals("version", left.getVersion()));

            final String matchQueryRight = String.join(COMMA, dottedEquals("groupId", right.getGroupId()),
                    dottedEquals("artifactId", right.getArtifactId()),
                    dottedEquals("version", right.getVersion()));

            final String matchQuery = matchSameWithQueries(":Artifact", matchQueryLeft, matchQueryRight);

            final String query = String.join(SPACE, matchQuery, mergeRelationship(relationshipName));

            log.debug("Running query {}", query);

            tx.run(query);
            return null;
        });
    }
}
