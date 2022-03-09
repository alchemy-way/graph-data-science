/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.gds.paths.traverse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.gds.BaseProcTest;
import org.neo4j.gds.GdsCypher;
import org.neo4j.gds.Orientation;
import org.neo4j.gds.catalog.GraphProjectProc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.neo4j.gds.compat.MapUtil.map;

/**
 * Graph:
 *
 *     (b)   (e)
 *   2/ 1\ 2/ 1\
 * >(a)  (d)  ((g))
 *   1\ 2/ 1\ 2/
 *    (c)   (f)
 */
class DfsStreamProcTest extends BaseProcTest {

    private static final String DB_CYPHER =
        "CREATE" +
        "  (a:Node {name:'a'})" +
        ", (b:Node {name:'b'})" +
        ", (c:Node {name:'c'})" +
        ", (d:Node {name:'d'})" +
        ", (e:Node {name:'e'})" +
        ", (f:Node {name:'f'})" +
        ", (g:Node {name:'g'})" +
        ", (a)-[:TYPE {cost:2.0}]->(b)" +
        ", (b)-[:TYPE2 {cost:2.0}]->(a)" +
        ", (a)-[:TYPE {cost:1.0}]->(c)" +
        ", (b)-[:TYPE {cost:1.0}]->(d)" +
        ", (c)-[:TYPE {cost:2.0}]->(d)" +
        ", (d)-[:TYPE {cost:1.0}]->(e)" +
        ", (d)-[:TYPE {cost:2.0}]->(f)" +
        ", (e)-[:TYPE {cost:2.0}]->(g)" +
        ", (f)-[:TYPE {cost:1.0}]->(g)";

    @BeforeEach
    void setupGraph() throws Exception {
        registerProcedures(DfsStreamProc.class, GraphProjectProc.class);
        runQuery(DB_CYPHER);
    }

    private long id(String name) {
        return runQuery("MATCH (n:Node) WHERE n.name = $name RETURN id(n) AS id", map("name", name), result -> result.<Long>columnAs("id").next());
    }

    /**
     * test if all both arrays contain the same nodes. not necessarily in
     * same order
     */
    void assertContains(String[] expected, List<Long> nodeIds) {
        assertEquals(expected.length, nodeIds.size(), "expected " + Arrays.toString(expected) + " | given [" + nodeIds + "]");
        for (String ex : expected) {
            final long id = id(ex);
            if (!nodeIds.contains(id)) {
                fail(ex + "(" + id + ") not in " + nodeIds);
            }
        }
    }

    void assertOrder(Map<String, List<Integer>> expected, List<Long> nodeIds) {
        assertEquals(expected.size(), nodeIds.size(), "expected " + expected + " | given [" + nodeIds + "]");
        for (var ex : expected.entrySet()) {
            long id = id(ex.getKey());
            var expectedPositions = ex.getValue();
            int actualPosition = nodeIds.indexOf(id);
            if (!expectedPositions.contains(actualPosition)) {
                fail(ex.getKey() + "(" + id + ") at " + actualPosition + " expected at " + expectedPositions);
            }
        }
    }

    @Test
    void testFindAnyOf() {
        var createQuery = GdsCypher.call(DEFAULT_GRAPH_NAME)
            .graphProject()
            .withNodeLabel("Node")
            .withRelationshipType("TYPE")
            .yields();
        runQuery(createQuery);
        long id = id("a");
        String query = GdsCypher.call(DEFAULT_GRAPH_NAME)
            .algo("dfs")
            .streamMode()
            .addParameter("sourceNode", id)
            .addParameter("targetNodes", Arrays.asList(id("e"), id("f")))
            .yields("sourceNode, nodeIds");

        runQueryWithRowConsumer(query, row -> {
            assertEquals(row.getNumber("sourceNode").longValue(), id);
            @SuppressWarnings("unchecked") List<Long> nodeIds = (List<Long>) row.get("nodeIds");
            assertEquals(4, nodeIds.size());
        });
    }

    @Test
    void testMaxDepthOut() {
        var createQuery = GdsCypher.call(DEFAULT_GRAPH_NAME)
            .graphProject()
            .withNodeLabel("Node")
            .withRelationshipType("TYPE")
            .yields();
        runQuery(createQuery);
        long id = id("a");
        String query = GdsCypher.call(DEFAULT_GRAPH_NAME)
            .algo("dfs")
            .streamMode()
            .addParameter("sourceNode", id)
            .addParameter("maxDepth", 2)
            .yields("sourceNode, nodeIds");
        runQueryWithRowConsumer(query, row -> {
            assertEquals(row.getNumber("sourceNode").longValue(), id);
            @SuppressWarnings("unchecked") List<Long> nodeIds = (List<Long>) row.get("nodeIds");
            assertContains(new String[]{"a", "b", "c", "d"}, nodeIds);
        });
    }

    @Test
    void testMaxDepthIn() {
        var createQuery = GdsCypher.call(DEFAULT_GRAPH_NAME)
            .graphProject()
            .withNodeLabel("Node")
            .withRelationshipType("TYPE", Orientation.REVERSE)
            .yields();
        runQuery(createQuery);

        long id = id("g");
        String query = GdsCypher.call(DEFAULT_GRAPH_NAME)
            .algo("dfs")
            .streamMode()
            .addParameter("sourceNode", id)
            .addParameter("maxDepth", 2)
            .yields("sourceNode, nodeIds");
        runQueryWithRowConsumer(query, row -> {
            assertEquals(row.getNumber("sourceNode").longValue(), id);
            @SuppressWarnings("unchecked") List<Long> nodeIds = (List<Long>) row.get("nodeIds");
            assertContains(new String[]{"g", "e", "f", "d"}, nodeIds);
        });
    }

    @Test
    void testPath() {
        var createQuery = GdsCypher.call(DEFAULT_GRAPH_NAME)
            .graphProject()
            .withNodeLabel("Node")
            .withRelationshipType("TYPE", Orientation.REVERSE)
            .yields();
        runQuery(createQuery);

        long id = id("g");
        String query = GdsCypher.call(DEFAULT_GRAPH_NAME)
            .algo("dfs")
            .streamMode()
            .addParameter("sourceNode", id)
            .yields("sourceNode, nodeIds");
        runQueryWithRowConsumer(query, row -> {
            assertEquals(row.getNumber("sourceNode").longValue(), id);
            List<Long> nodeIds = (List<Long>) row.get("nodeIds");

            var expectedOrder = new HashMap<String, List<Integer>>();
            expectedOrder.put("g", Arrays.asList(0));
            expectedOrder.put("f", Arrays.asList(1, 6));
            expectedOrder.put("d", Arrays.asList(1, 2));
            expectedOrder.put("c", Arrays.asList(3, 5));
            expectedOrder.put("a", Arrays.asList(4));
            expectedOrder.put("b", Arrays.asList(3, 5));
            expectedOrder.put("e", Arrays.asList(1, 6));

            assertOrder(expectedOrder, nodeIds);
        });
    }

    @Test
    void failOnInvalidSourceNode() {
        loadCompleteGraph(DEFAULT_GRAPH_NAME);
        String query = GdsCypher.call(DEFAULT_GRAPH_NAME)
            .algo("dfs")
            .streamMode()
            .addParameter("sourceNode", 42)
            .yields();

        assertError(query, "Source node does not exist in the in-memory graph: `42`");
    }

    @Test
    void failOnInvalidEndNode() {
        loadCompleteGraph(DEFAULT_GRAPH_NAME);
        String query = GdsCypher.call(DEFAULT_GRAPH_NAME)
            .algo("dfs")
            .streamMode()
            .addParameter("sourceNode", 0)
            .addParameter("targetNodes", Arrays.asList(0, 42, 1))
            .yields();

        assertError(query, "Target nodes do not exist in the in-memory graph: ['42']");
    }
}
