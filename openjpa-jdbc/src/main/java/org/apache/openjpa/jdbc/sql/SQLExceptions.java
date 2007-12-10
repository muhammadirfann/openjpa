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
package org.apache.openjpa.jdbc.sql;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.apache.openjpa.lib.util.Localizer.Message;
import org.apache.openjpa.util.LockException;
import org.apache.openjpa.util.ObjectExistsException;
import org.apache.openjpa.util.ObjectNotFoundException;
import org.apache.openjpa.util.OpenJPAException;
import org.apache.openjpa.util.OptimisticException;
import org.apache.openjpa.util.ReferentialIntegrityException;
import org.apache.openjpa.util.StoreException;

/**
 * Helper class for converting a {@link SQLException} into
 * the appropriate OpenJPA type.
 *
 * @author Marc Prud'hommeaux
 * @nojavadoc
 */
public class SQLExceptions {

    private static final SQLException[] EMPTY_EXCEPS = new SQLException[0];

    /**
     * Convert the specified exception into a {@link StoreException}.
     */
    public static OpenJPAException getStore(SQLException se) {
        return getStore(se, null, null);
    }

    /**
     * Convert the specified exception into a {@link OpenJPAException}.
     */
    public static OpenJPAException getStore(SQLException se, Object failed) {
        return getStore(se, failed, null);
    }

    /**
     * Convert the specified exception into a {@link StoreException}.
     */
    public static OpenJPAException getStore(SQLException se,
        DBDictionary dict) {
        return getStore(se.getMessage(), se, dict);
    }

    /**
     * Convert the specified exception into a {@link StoreException}.
     */
    public static OpenJPAException getStore(SQLException se, Object failed,
        DBDictionary dict) {
        return getStore(se.getMessage(), se, failed, dict);
    }

    /**
     * Convert the specified exception into a {@link StoreException}.
     */
    public static OpenJPAException getStore(Message msg, SQLException se,
        DBDictionary dict) {
        return getStore(msg.getMessage(), se, null, dict);
    }

    /**
     * Convert the specified exception into a {@link StoreException}.
     */
    public static OpenJPAException getStore(String msg, SQLException se,
        DBDictionary dict) {
        return getStore(msg, se, null, dict);
    }

    /**
     * Convert the specified exception into a {@link StoreException}.
     */
    public static OpenJPAException getStore(String msg, SQLException se,
        Object failed, DBDictionary dict) {
        if (msg == null)
            msg = se.getClass().getName();
        SQLException[] ses = getSQLExceptions(se);
        if (dict == null)
            return new StoreException(msg).setFailedObject(failed).
                setNestedThrowables(ses);
        return dict.newStoreException(msg, ses, failed);
    }
    
    /**
     * Returns an array of {@link SQLException} instances for the
     * specified exception.
     */
    private static SQLException[] getSQLExceptions(SQLException se) {
        if (se == null)
            return EMPTY_EXCEPS;

        List errs = new LinkedList();
        while (se != null && !errs.contains(se)) {
            errs.add(se);
            se = se.getNextException();
        }
        return (SQLException[]) errs.toArray(new SQLException[errs.size()]);
    }
    
    /**
     * Narrows the given SQLException to a specific type of 
     * {@link StoreException#getSubtype() StoreException} by analyzing the
     * SQLState code supplied by SQLException. Each database-specific 
     * {@link DBDictionary dictionary} can supply a set of error codes that will
     * map to a specific specific type of StoreException via 
     * {@link DBDictionary#getSQLStates(int) getSQLStates()} method.
     * The default behavior is to return generic {@link StoreException 
     * StoreException}.
     */
    public static OpenJPAException narrow(String msg, SQLException se, 
    		DBDictionary dict) {
        String e = se.getSQLState();
        if (dict.getSQLStates(StoreException.LOCK).contains(e)) 
            return new LockException(msg);
        else if (dict.getSQLStates(StoreException.OBJECT_EXISTS).contains(e))
            return new ObjectExistsException(msg);
        else if (dict.getSQLStates(StoreException.OBJECT_NOT_FOUND).contains(e))
            return new ObjectNotFoundException(msg);
        else if (dict.getSQLStates(StoreException.OPTIMISTIC).contains(e))
            return new OptimisticException(msg);
        else if (dict.getSQLStates(StoreException.REFERENTIAL_INTEGRITY)
        		.contains(e)) 
            return new ReferentialIntegrityException(msg);
        else
            return new StoreException(msg);
    }
}
