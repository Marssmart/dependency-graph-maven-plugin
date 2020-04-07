package org.deer.dependency.graph;

import org.neo4j.driver.Session;

import static java.lang.String.format;

public interface Neo4jRequest {

    String SPACE = " ";
    String EMPTY = "";
    String COMMA = ",";

    void executeOnSession(Session session);

    default String set(String key, String param) {
        return format("SET %s = %s", key, param);
    }

    default String create(String labels) {
        return format("CREATE (a%s)", labels);
    }

    default String mergeOnCreateSet(String labels, String query) {
        return format("MERGE (a%s {%s} ) ON CREATE SET", labels, query);
    }

    default String dottedEquals(String key, String value) {
        return format("%s: '%s'", key, value);
    }

    default String equals(String key, String param) {
        return format("%s = %s", key, param);
    }

    default String matchSame(String labels) {
        return format("MATCH (a%s),(b%s)", labels, labels);
    }

    default String matchSameWithQueries(String labels, String queryA, String queryB) {
        return format("MATCH (a%s {%s}),(b%s {%s})", labels, queryA, labels, queryB);
    }

    default String createRelationship(String relName) {
        return format("CREATE (a)-[r:%s]->(b)", relName);
    }

    default String mergeRelationship(String relName) {
        return format("MERGE (a)-[r:%s]->(b)", relName);
    }
}
