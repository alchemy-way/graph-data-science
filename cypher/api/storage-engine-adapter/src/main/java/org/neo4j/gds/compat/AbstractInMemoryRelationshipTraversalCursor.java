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
package org.neo4j.gds.compat;

import org.neo4j.gds.core.cypher.CypherGraphStore;
import org.neo4j.gds.storageengine.InMemoryRelationshipCursor;
import org.neo4j.storageengine.api.RelationshipSelection;
import org.neo4j.storageengine.api.RelationshipVisitor;
import org.neo4j.storageengine.api.StorageRelationshipTraversalCursor;
import org.neo4j.token.TokenHolders;

public abstract class AbstractInMemoryRelationshipTraversalCursor extends InMemoryRelationshipCursor implements StorageRelationshipTraversalCursor, RelationshipVisitor<RuntimeException> {

    public AbstractInMemoryRelationshipTraversalCursor(CypherGraphStore graphStore, TokenHolders tokenHolders) {
        super(graphStore, tokenHolders);
    }

    @Override
    public long neighbourNodeReference() {
        return targetId;
    }

    @Override
    public long originNodeReference() {
        return sourceId;
    }

    @Override
    public void init(long nodeReference, long reference, RelationshipSelection selection) {
        reset();
        sourceId = nodeReference;
    }
}
