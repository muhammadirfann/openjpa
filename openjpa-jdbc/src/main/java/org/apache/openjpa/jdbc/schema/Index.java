/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openjpa.jdbc.schema;

import org.apache.openjpa.jdbc.identifier.DBIdentifier;

import java.util.Arrays;

/**
 * Represents a database index. Can also represent a partial index,
 * aligning with {@link java.sql.DatabaseMetaData}.
 *
 * @author Abe White
 * @author Stephen Kim
 */
public class Index extends LocalConstraint {
    private static final long serialVersionUID = 1L;
    private boolean _unique = false;
    private String[] _fun;

    /**
     * Default constructor.
     */
    public Index() {
    }

    /**
     * Constructor.
     *
     * @param name the name of the index
     * @param table the table of the index
     * @deprecated
     */
    @Deprecated
    public Index(String name, Table table) {
        super(name, table);
    }

    public Index(DBIdentifier name, Table table) {
        super(name, table);
    }

    /**
     * Return true if this is a UNIQUE index.
     */
    public boolean isUnique() {
        return _unique;
    }

    /**
     * Set whether this is a UNIQUE index.
     */
    public void setUnique(boolean unique) {
        _unique = unique;
    }

    @Override
    public boolean isLogical() {
        return false;
    }

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public String getFullName() {
        return getFullIdentifier().getName();
    }

    @Override
    public DBIdentifier getFullIdentifier() {
        return getQualifiedPath().getIdentifier();
    }

    /**
     * Return all the columns the constraint spans.
     */
    public String[] getFunctions() {
        if (_fun == null) {
            return new String[0];
        }
        return _fun;
    }


    /**
     * Indexes are equal if they have the same name, the same columns, and
     * are both unique/not unique.
     */
    public boolean equalsIndex(Index idx) {
        if (idx == this)
            return true;
        if (idx == null)
            return false;

        if (isUnique() != idx.isUnique())
            return false;
        if (!getQualifiedPath().equals(idx.getQualifiedPath()))
            return false;
        return equalsLocalConstraint(idx);
    }

    /**
     * Set the columns the constraint spans.
     */
    public void setFunctions(String[] funs) {

        _fun = new String[funs.length];
        System.arraycopy(funs, 0, _fun, 0, funs.length);

    }

    public boolean functionsMatch(String[] functions) {

        if (_fun.length != functions.length) { return false; }

        for (String f : functions) {
            boolean found = false;
            for (String s : _fun) {
                if (s.equals(f)) {
                    found = true;
                    break;
                }
            }
            if (!found) { return false; }
        }

        return true;

    }

    public void addFunction(String function) {

        if (_fun == null) {
            _fun = new String[] { function };
        } else {
            String [] fun = Arrays.copyOf(_fun, _fun.length + 1);
            fun[_fun.length] = function;
            _fun = fun;
        }

    }
}
