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
package org.apache.openjpa.jdbc.meta.strats;

import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.meta.ValueMapping;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ColumnIO;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.meta.JavaTypes;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.SQLException;

// ESYNC-5945 Implement value handler for converted values.
// this currently hard-codes the implementation to strings.
@SuppressWarnings("unchecked")
public class ConvertValueHandler extends ImmutableValueHandler {

    private final Class<?> converterClass;

    public ConvertValueHandler(Class<?> converterClass) {
        this.converterClass = converterClass;
    }

    @Override
    public Column[] map(ValueMapping vm, DBIdentifier name, ColumnIO io, boolean adapt) {
        Column[] r = super.map(vm, name, io, adapt);
        r[0].setJavaType(JavaTypes.STRING);
        return r;
    }

    @Override
    public Object toDataStoreValue(ValueMapping vm, Object val, JDBCStore store) {
        return makeConverter().convertToDatabaseColumn(val);
    }

    @Override
    public Object toObjectValue(ValueMapping vm, Object val) {
        return makeConverter().convertToEntityAttribute(val);
    }

    @Override
    public Object toObjectValue(ValueMapping vm, Object val, OpenJPAStateManager sm,
                                JDBCStore store, JDBCFetchConfiguration fetch) throws SQLException {
        return toObjectValue(vm, val);
    }

    private AttributeConverter makeConverter() {
        try {
            return (AttributeConverter) converterClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Can not instantiate "+converterClass.getName(), e);
        }
    }

}
