/*
 * Copyright (c) 2017-2019 "Neo4j,"
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
package org.neo4j.graphalgo.impl.betweenness;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.graphalgo.TestDatabaseCreator;
import org.neo4j.graphalgo.TestSupport.AllGraphTypesWithoutCypherTest;
import org.neo4j.graphalgo.api.Graph;
import org.neo4j.graphalgo.api.GraphFactory;
import org.neo4j.graphalgo.core.GraphLoader;
import org.neo4j.graphalgo.core.utils.Pools;
import org.neo4j.kernel.internal.GraphDatabaseAPI;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 *  (A)-->(B)-->(C)-->(D)-->(E)
 *  0.0   3.0   4.0   3.0   0.0
 */
@ExtendWith(MockitoExtension.class)
class BetweennessCentralityTest {

    private static final String DB_CYPHER =
            "CREATE" +
            "  (a:Node {name:'a'})" +
            ", (b:Node {name: 'b'})" +
            ", (c:Node {name: 'c'})" +
            ", (d:Node {name: 'd'})" +
            ", (e:Node {name: 'e'})" +
            ", (a)-[:TYPE]->(b)" +
            ", (b)-[:TYPE]->(c)" +
            ", (c)-[:TYPE]->(d)" +
            ", (d)-[:TYPE]->(e)";

    private static GraphDatabaseAPI DB;
    private static Graph graph;

    interface TestConsumer {
        void accept(String name, double centrality);
    }

    @Mock
    private TestConsumer testConsumer;

    @BeforeAll
    static void setupGraphDb() {
        DB = TestDatabaseCreator.createTestDatabase();
        DB.execute(DB_CYPHER);
    }

    @AfterAll
    static void shutdownGraphDb() {
        if (DB != null) DB.shutdown();
    }

    void verifyMock(TestConsumer mock) {
        verify(mock, times(1)).accept(eq("a"), eq(0.0));
        verify(mock, times(1)).accept(eq("b"), eq(3.0));
        verify(mock, times(1)).accept(eq("c"), eq(4.0));
        verify(mock, times(1)).accept(eq("d"), eq(3.0));
        verify(mock, times(1)).accept(eq("e"), eq(0.0));
    }

    @AllGraphTypesWithoutCypherTest
    void testBC(Class<? extends GraphFactory> graphFactory) {
        setup(graphFactory);
        new BetweennessCentrality(graph)
                .compute()
                .resultStream()
                .forEach(r -> testConsumer.accept(name(r.nodeId), r.centrality));
        verifyMock(testConsumer);
    }

    @AllGraphTypesWithoutCypherTest
    void testRABrandesForceCompleteSampling(Class<? extends GraphFactory> graphFactory) {
        setup(graphFactory);
        new RABrandesBetweennessCentrality(graph, Pools.DEFAULT, 3, new RandomSelectionStrategy(graph, 1.0))
                .compute()
                .resultStream()
                .forEach(r -> testConsumer.accept(name(r.nodeId), r.centrality));
        verifyMock(testConsumer);
    }

    @AllGraphTypesWithoutCypherTest
    void testRABrandesForceEmptySampling(Class<? extends GraphFactory> graphFactory) {
        setup(graphFactory);
        new RABrandesBetweennessCentrality(graph, Pools.DEFAULT, 3, new RandomSelectionStrategy(graph, 0.0))
                .compute()
                .resultStream()
                .forEach(r -> testConsumer.accept(name(r.nodeId), r.centrality));
        verify(testConsumer, times(1)).accept(eq("a"), eq(0.0));
        verify(testConsumer, times(1)).accept(eq("b"), eq(0.0));
        verify(testConsumer, times(1)).accept(eq("c"), eq(0.0));
        verify(testConsumer, times(1)).accept(eq("d"), eq(0.0));
        verify(testConsumer, times(1)).accept(eq("e"), eq(0.0));
    }

    @Disabled
    void testRABrandes(Class<? extends GraphFactory> graphFactory) {
        setup(graphFactory);
        new RABrandesBetweennessCentrality(graph, Pools.DEFAULT, 3, new RandomSelectionStrategy(graph, 0.3, 5))
                .compute()
                .resultStream()
                .forEach(r -> testConsumer.accept(name(r.nodeId), r.centrality));
        verifyMock(testConsumer);
    }

    @AllGraphTypesWithoutCypherTest
    void testPBC(Class<? extends GraphFactory> graphFactory) {
        setup(graphFactory);
        new ParallelBetweennessCentrality(graph, Pools.DEFAULT, 4)
                .compute()
                .resultStream()
                .forEach(r -> testConsumer.accept(name(r.nodeId), r.centrality));
        verifyMock(testConsumer);
    }

    private void setup(Class<? extends GraphFactory> graphFactory) {
        graph = new GraphLoader(DB)
                .withAnyRelationshipType()
                .withAnyLabel()
                .load(graphFactory);
    }

    private String name(long id) {
        String[] name = {""};
        DB.execute("MATCH (n:Node) WHERE id(n) = " + id + " RETURN n.name as name")
                .accept(row -> {
                    name[0] = row.getString("name");
                    return false;
                });
        return name[0];
    }
}
