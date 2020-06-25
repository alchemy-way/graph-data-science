/*
 * Copyright (c) 2017-2020 "Neo4j,"
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
package org.neo4j.gds.embeddings.graphsage.proc;

import org.neo4j.gds.embeddings.graphsage.algo.GraphSage;
import org.neo4j.gds.embeddings.graphsage.algo.GraphSageWriteConfig;
import org.neo4j.graphalgo.AlgorithmFactory;
import org.neo4j.graphalgo.WriteProc;
import org.neo4j.graphalgo.config.GraphCreateConfig;
import org.neo4j.graphalgo.core.CypherMapWrapper;
import org.neo4j.graphalgo.core.write.PropertyTranslator;
import org.neo4j.graphalgo.result.AbstractResultBuilder;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.neo4j.gds.embeddings.graphsage.proc.GraphSageStreamProc.GRAPHSAGE_DESCRIPTION;

public class GraphSageWriteProc extends WriteProc<GraphSage, GraphSage.GraphSageResult, GraphSageWriteProc.GraphSageWriteResult, GraphSageWriteConfig> {

    @Procedure(name = "gds.alpha.graphSage.write", mode = Mode.READ)
    @Description(GRAPHSAGE_DESCRIPTION)
    public Stream<GraphSageWriteResult> write(
        @Name(value = "graphName") Object graphNameOrConfig,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        return write(compute(graphNameOrConfig, configuration));
    }

    @Override
    protected GraphSageWriteConfig newConfig(
        String username,
        Optional<String> graphName,
        Optional<GraphCreateConfig> maybeImplicitCreate,
        CypherMapWrapper config
    ) {
        return GraphSageWriteConfig.of(username, graphName, maybeImplicitCreate, config);
    }

    @Override
    protected AlgorithmFactory<GraphSage, GraphSageWriteConfig> algorithmFactory(GraphSageWriteConfig config) {
        return new GraphSageAlgorithmFactory<>();
    }

    @Override
    protected PropertyTranslator<GraphSage.GraphSageResult> nodePropertyTranslator(ComputationResult<GraphSage, GraphSage.GraphSageResult, GraphSageWriteConfig> computationResult) {
        return (PropertyTranslator.OfDoubleArray<GraphSage.GraphSageResult>) (data, nodeId) -> data.embeddings().get(nodeId);
    }

    @Override
    protected AbstractResultBuilder<GraphSageWriteResult> resultBuilder(ComputationResult<GraphSage,GraphSage.GraphSageResult, GraphSageWriteConfig> computeResult) {
        return new GraphSageWriteResult.Builder()
            .withStartLoss(computeResult.result().startLoss())
            .withEpochLosses(computeResult.result().epochLosses());
    }

    public static final class GraphSageWriteResult {

        public final double startLoss;
        public final Map<String, Double> epochLosses;

        public final long nodeCount;
        public final long nodePropertiesWritten;
        public final long createMillis;
        public final long computeMillis;
        public final long writeMillis;
        public final Map<String, Object> configuration;

        GraphSageWriteResult(
            double startLoss,
            Map<String, Double> epochLosses,
            long nodeCount,
            long nodePropertiesWritten,
            long createMillis,
            long computeMillis,
            long writeMillis,
            Map<String, Object> configuration
        ) {
            this.startLoss = startLoss;
            this.epochLosses = epochLosses;
            this.nodeCount = nodeCount;
            this.nodePropertiesWritten = nodePropertiesWritten;
            this.createMillis = createMillis;
            this.computeMillis = computeMillis;
            this.writeMillis = writeMillis;
            this.configuration = configuration;
        }

        static class Builder extends AbstractResultBuilder<GraphSageWriteResult> {
            private double startLoss;

            public Builder withStartLoss(double startLoss) {
                this.startLoss = startLoss;
                return this;
            }
            private Map<String, Double> epochLosses;

            public Builder withEpochLosses(Map<String, Double> epochLosses) {
                this.epochLosses = epochLosses;
                return this;
            }

            @Override
            public GraphSageWriteResult build() {
                return new GraphSageWriteResult(
                    startLoss,
                    epochLosses,
                    nodeCount,
                    nodePropertiesWritten,
                    createMillis,
                    computeMillis,
                    writeMillis,
                    config.toMap()
                );
            }
        }
    }
}
