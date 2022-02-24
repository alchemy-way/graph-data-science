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
package org.neo4j.gds.ml.logisticregression;

import org.apache.commons.lang3.mutable.MutableInt;
import org.neo4j.gds.ml.Features;
import org.neo4j.gds.ml.Trainer;
import org.neo4j.gds.ml.core.ComputationContext;
import org.neo4j.gds.ml.core.Variable;
import org.neo4j.gds.ml.core.batch.Batch;
import org.neo4j.gds.ml.core.batch.SingletonBatch;
import org.neo4j.gds.ml.core.functions.Constant;
import org.neo4j.gds.ml.core.functions.EWiseAddMatrixScalar;
import org.neo4j.gds.ml.core.functions.MatrixMultiplyWithTransposedSecondOperand;
import org.neo4j.gds.ml.core.functions.ReducedSoftmax;
import org.neo4j.gds.ml.core.functions.Softmax;
import org.neo4j.gds.ml.core.subgraph.LocalIdMap;
import org.neo4j.gds.ml.core.tensor.Matrix;
import org.neo4j.gds.ml.nodemodels.logisticregression.NodeLogisticRegressionData;

public class LogisticRegressionClassifier implements Trainer.Classifier {

    private final LogisticRegressionData data;

    public LogisticRegressionClassifier(
        LogisticRegressionData data
    ) {
        this.data = data;
    }

    //TODO: remove me with NodeLogisticRegressionData
    public LogisticRegressionClassifier(NodeLogisticRegressionData data) {
        this(convertModelData(data));
    }

    private static LogisticRegressionData convertModelData(NodeLogisticRegressionData modelData) {
        return LogisticRegressionData.create(modelData.weights(), modelData.bias(), modelData.classIdMap());
    }

    @Override
    public LocalIdMap classIdMap() {
        return data.classIdMap();
    }

    @Override
    public double[] predictProbabilities(long id, Features features) {
        var batch = new SingletonBatch(id);
        ComputationContext ctx = new ComputationContext();
        return ctx.forward(predictionsVariable(batchFeatureMatrix(batch, features))).data();
    }

    Variable<Matrix> predictionsVariable(Constant<Matrix> batchFeatures) {
        var weights = data.weights();
        var weightedFeatures = MatrixMultiplyWithTransposedSecondOperand.of(
            batchFeatures,
            weights
        );
        var softmaxInput = data.bias().isPresent()
            ? new EWiseAddMatrixScalar(weightedFeatures, data.bias().get())
            : weightedFeatures;
        return weights.data().rows() == numberOfClasses()
            ? new Softmax(softmaxInput)
            : new ReducedSoftmax(softmaxInput);
    }

    static Constant<Matrix> batchFeatureMatrix(Batch batch, Features features) {
        var batchFeatures = new Matrix(batch.size(), features.get(0).length);
        var batchFeaturesOffset = new MutableInt();

        batch.nodeIds().forEach(id -> batchFeatures.setRow(batchFeaturesOffset.getAndIncrement(), features.get(id)));

        return new Constant<>(batchFeatures);
    }

    @Override
    public LogisticRegressionData data() {
        return data;
    }
}