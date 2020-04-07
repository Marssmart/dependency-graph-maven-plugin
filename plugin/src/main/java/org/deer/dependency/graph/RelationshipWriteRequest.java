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
