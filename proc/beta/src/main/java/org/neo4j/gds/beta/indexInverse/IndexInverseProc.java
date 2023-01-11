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

import org.neo4j.gds.BaseProc;
import org.neo4j.gds.core.loading.SingleTypeRelationshipImportResult;
import org.neo4j.gds.executor.MemoryEstimationExecutor;
import org.neo4j.gds.executor.ProcedureExecutor;
import org.neo4j.gds.executor.ProcedureExecutorSpec;
import org.neo4j.gds.results.MemoryEstimateResult;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Internal;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.stream.Stream;

import static org.neo4j.procedure.Mode.READ;

public class IndexInverseProc extends BaseProc {

    @Internal // waiting for PM decision
    @Procedure(value = IndexInverseSpec.CALLABLE_NAME, mode = READ)
    @Description(IndexInverseSpec.DESCRIPTION)
    public Stream<IndexInverseSpec.MutateResult> mutate(
        @Name(value = "graphName") String graphName,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        var mutateSpec = new IndexInverseSpec();
        var pipelineSpec = new ProcedureExecutorSpec<IndexInverse, SingleTypeRelationshipImportResult, IndexInverseConfig>();

        return new ProcedureExecutor<>(
            mutateSpec,
            pipelineSpec,
            executionContext()
        ).compute(graphName, configuration, true, true);
    }

    @Internal // waiting for PM decision
    @Procedure(value = IndexInverseSpec.CALLABLE_NAME + ".estimate", mode = READ)
    @Description(ESTIMATE_DESCRIPTION)
    public Stream<MemoryEstimateResult> estimate(
        @Name(value = "graphNameOrConfiguration") Object graphNameOrConfiguration,
        @Name(value = "algoConfiguration") Map<String, Object> algoConfiguration
    ) {
        var mutateSpec = new IndexInverseSpec();
        var pipelineSpec = new ProcedureExecutorSpec<IndexInverse, SingleTypeRelationshipImportResult, IndexInverseConfig>();

        return new MemoryEstimationExecutor<>(
            mutateSpec,
            pipelineSpec,
            executionContext()
        ).computeEstimate(graphNameOrConfiguration, algoConfiguration);
    }
}