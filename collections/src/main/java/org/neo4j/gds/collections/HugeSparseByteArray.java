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
package org.neo4j.gds.collections;

import java.util.function.LongConsumer;

@HugeSparseArray(valueType = byte.class)
public interface HugeSparseByteArray {

    long capacity();

    byte get(long index);

    boolean contains(long index);

    static Builder builder(byte defaultValue, LongConsumer trackAllocation) {
        return builder(defaultValue, 0, trackAllocation);
    }

    static Builder builder(byte defaultValue, long initialCapacity, LongConsumer trackAllocation) {
        return new HugeSparseByteArraySon.GrowingBuilder(defaultValue, initialCapacity, trackAllocation);
    }

    interface Builder {
        void set(long index, byte value);

        boolean setIfAbsent(long index, byte value);

        void addTo(long index, byte value);

        HugeSparseByteArray build();
    }
}