/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.paimon.predicate;

import org.apache.paimon.format.FieldStats;
import org.apache.paimon.types.DataType;

import java.util.List;
import java.util.Optional;

import static org.apache.paimon.predicate.CompareUtils.compareLiteral;

/** A {@link NullFalseLeafBinaryFunction} to eval not equal. */
public class NotEqual extends NullFalseLeafBinaryFunction {

    public static final NotEqual INSTANCE = new NotEqual();

    private NotEqual() {}

    @Override
    public boolean test(DataType type, Object field, Object literal) {
        return compareLiteral(type, literal, field) != 0;
    }

    @Override
    public boolean test(DataType type, long rowCount, FieldStats fieldStats, Object literal) {
        return compareLiteral(type, literal, fieldStats.minValue()) != 0
                || compareLiteral(type, literal, fieldStats.maxValue()) != 0;
    }

    @Override
    public Optional<LeafFunction> negate() {
        return Optional.of(Equal.INSTANCE);
    }

    @Override
    public <T> T visit(FunctionVisitor<T> visitor, FieldRef fieldRef, List<Object> literals) {
        return visitor.visitNotEqual(fieldRef, literals.get(0));
    }
}