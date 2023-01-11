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
package org.neo4j.gds.beta.indexInverse;

import org.neo4j.gds.MutateComputationResultConsumer;
import org.neo4j.gds.RelationshipType;
import org.neo4j.gds.core.loading.SingleTypeRelationshipImportResult;
import org.neo4j.gds.core.utils.ProgressTimer;
import org.neo4j.gds.executor.AlgorithmSpec;
import org.neo4j.gds.executor.ComputationResult;
import org.neo4j.gds.executor.ComputationResultConsumer;
import org.neo4j.gds.executor.ExecutionContext;
import org.neo4j.gds.executor.ExecutionMode;
import org.neo4j.gds.executor.GdsCallable;
import org.neo4j.gds.executor.NewConfigFunction;
import org.neo4j.gds.result.AbstractResultBuilder;
import org.neo4j.gds.results.StandardMutateResult;

import java.util.Map;
import java.util.stream.Stream;

@GdsCallable(name = IndexInverseSpec.CALLABLE_NAME, executionMode = ExecutionMode.MUTATE_RELATIONSHIP, description = IndexInverseSpec.DESCRIPTION)
public class IndexInverseSpec implements AlgorithmSpec<IndexInverse, SingleTypeRelationshipImportResult, IndexInverseConfig, Stream<IndexInverseSpec.MutateResult>, IndexInverseAlgorithmFactory> {

    public static final String DESCRIPTION = "The IndexInverse procedure indexes directed relationships to allow an efficient inverse access for other algorithms.";
    static final String CALLABLE_NAME = "gds.beta.graph.relationships.indexInverse";

    @Override
    public String name() {
        return CALLABLE_NAME;
    }

    @Override
    public IndexInverseAlgorithmFactory algorithmFactory() {
        return new IndexInverseAlgorithmFactory();
    }

    @Override
    public NewConfigFunction<IndexInverseConfig> newConfigFunction() {
        return ((__, config) -> IndexInverseConfig.of(config));
    }

    protected AbstractResultBuilder<MutateResult> resultBuilder(
        ComputationResult<IndexInverse, SingleTypeRelationshipImportResult, IndexInverseConfig> computeResult,
        ExecutionContext executionContext
    ) {
        return new MutateResult.Builder().withInputRelationships(computeResult.graph().relationshipCount());
    }

    @Override
    public ComputationResultConsumer<IndexInverse, SingleTypeRelationshipImportResult, IndexInverseConfig, Stream<MutateResult>> computationResultConsumer() {
        return new MutateComputationResultConsumer<>(this::resultBuilder) {
            @Override
            protected void updateGraphStore(
                AbstractResultBuilder<?> resultBuilder,
                ComputationResult<IndexInverse, SingleTypeRelationshipImportResult, IndexInverseConfig> computationResult,
                ExecutionContext executionContext
            ) {
                try (ProgressTimer ignored = ProgressTimer.start(resultBuilder::withMutateMillis)) {
                    var result = computationResult.result();
                    if (result != null) {
                        computationResult
                            .graphStore()
                            .addInverseIndex(
                                RelationshipType.of(computationResult.config().relationshipType()),
                                result
                            );
                    }
                }
            }
        };
    }

    public static final class MutateResult extends StandardMutateResult {
        public final long inputRelationships;

        private MutateResult(
            long preProcessingMillis,
            long computeMillis,
            long mutateMillis,
            long postProcessingMillis,
            long inputRelationships,
            Map<String, Object> configuration
        ) {
            super(preProcessingMillis, computeMillis, postProcessingMillis, mutateMillis, configuration);
            this.inputRelationships = inputRelationships;
        }

        public static class Builder extends AbstractResultBuilder<MutateResult> {

            private long inputRelationships;

            Builder withInputRelationships(long inputRelationships) {
                this.inputRelationships = inputRelationships;
                return this;
            }

            @Override
            public MutateResult build() {
                return new MutateResult(
                    preProcessingMillis,
                    computeMillis,
                    mutateMillis,
                    0,
                    inputRelationships,
                    config.toMap()
                );
            }
        }
    }
}