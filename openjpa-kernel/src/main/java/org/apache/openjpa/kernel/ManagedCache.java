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
package org.apache.openjpa.kernel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.enhance.StateManager;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.ReferenceHashSet;
import org.apache.openjpa.util.Exceptions;
import org.apache.openjpa.util.UserException;

/**
 * Cache of managed objects. Must be static for serialization reasons.
 */
class ManagedCache implements Serializable {

    private transient Log _log = null;
    private static final Localizer _loc =
        Localizer.forPackage(ManagedCache.class);

    private Map<Object,StateManagerImpl> _main; // oid -> sm
    private Map<Object,StateManagerImpl> _conflicts = null; // conflict oid -> new sm
    private Map<Object,StateManagerImpl> _news = null; // tmp id -> new sm
    private Collection<StateManagerImpl> _embeds = null; // embedded/non-persistent sms
    private Collection<StateManagerImpl> _untracked = null; // hard refs to untracked sms
    private BrokerImpl broker;

    /**
     * Constructor; supply primary cache map.
     */
    ManagedCache(BrokerImpl broker) {
        this.broker = broker;
        _main = (Map<Object, StateManagerImpl>) broker.newManagedObjectCache();
    }

    private Log getLog() {
        if (_log == null) {
            _log = broker.getConfiguration().getLog(OpenJPAConfiguration.LOG_CACHE);
        }
        return _log;
    }

    /**
     * Return the instance for the given oid, optionally allowing
     * new instances.
     */
    public StateManagerImpl getById(Object oid, boolean allowNew) {

        if (oid == null) {
            return null;
        }

        // check main cache for oid
        StateManagerImpl sm = _main.get(oid);
        StateManagerImpl sm2;

        CacheGetLogInfo gli = new CacheGetLogInfo(oid, allowNew, sm);

        if (sm != null) {
            // if it's a new instance, we know it's the only match, because
            // other pers instances override new instances in _cache
            if (sm.isNew() && !sm.isDeleted()) {
                return logCache(gli, (allowNew) ? sm : null);
            }
            if (!allowNew || !sm.isDeleted()) {
                return logCache(gli, sm);
            }

            // sm is deleted; check conflict cache
            if (_conflicts != null) {
                sm2 = _conflicts.get(oid);
                gli.conflicted(sm2);
                if (sm2 != null)
                    return logCache(gli, sm2);
            }
        }

        // at this point sm is null or deleted; check the new cache for
        // any matches. this allows us to match app id objects to new
        // instances without permanent oids
        if (allowNew && _news != null && !_news.isEmpty()) {
            sm2 = _news.get(oid);
            gli.newed(sm2);
            if (sm2 != null)
                return logCache(gli, sm2);
        }
        return logCache(gli, sm);
    }

    private StateManagerImpl logCache(CacheGetLogInfo gli, StateManagerImpl sm) {

        Log log = getLog();
        if (log.isTraceEnabled()) {
            String found = String.valueOf(sm != null).toUpperCase();
            log.trace(_loc.get("object-cache-found", new Object[]{
                    broker, gli.oid, gli.allowNew, gli.mainFound, gli.isNew,
                    gli.isDeleted, gli.conflicted, gli.fromNew, found
            }));
        }
        return sm;

    }

    /**
     * Call this method when a new state manager initializes itself.
     */
    public void add(StateManagerImpl sm) {

        CacheAddLogInfo ali = new CacheAddLogInfo();

        if (!sm.isIntercepting()) {
            if (_untracked == null)
                _untracked = new HashSet<StateManagerImpl>();
            ali.untracked(sm);
            _untracked.add(sm);
        }

        if (!sm.isPersistent() || sm.isEmbedded()) {
            if (_embeds == null)
                _embeds = new ReferenceHashSet(ReferenceHashSet.WEAK);
            _embeds.add(sm);
            ali.embedded(sm);
            logAdd(ali);
            return;
        }

        // initializing new instance; put in new cache because won't have
        // permanent oid yet
        if (sm.isNew()) {
            if (_news == null)
                _news = new HashMap<Object,StateManagerImpl>();
            _news.put(sm.getId(), sm);
            ali.setNew(sm);
            logAdd(ali);
            return;
        }

        // initializing persistent instance; put in main cache
        StateManagerImpl orig = _main.put(sm.getObjectId(), sm);
        if (orig != null) {
            _main.put(sm.getObjectId(), orig);
            throw new UserException(_loc.get("dup-load", sm.getObjectId(),
                Exceptions.toString(orig.getManagedInstance())))
                .setFailedObject(sm.getManagedInstance());
        }
        ali.setMain(sm);
        logAdd(ali);
    }

    private void logAdd(CacheAddLogInfo ali) {
        Log log = getLog();
        if (log.isTraceEnabled()) {
            log.trace(_loc.get("object-cache-add", new Object[]{
                    broker, ali.untracked, ali.embedded, ali.isNew, ali.isMain
            }));
        }
    }

    /**
     * Remove the given state manager from the cache when it transitions
     * to transient.
     */
    public void remove(Object id, StateManagerImpl sm) {
        // if it has a permanent oid, remove from main / conflict cache,
        // else remove from embedded/nontrans cache, and if not there
        // remove from new cache
    	StateManagerImpl orig;
    	CacheDeleteLogInfo dli = new CacheDeleteLogInfo(id, sm);
        if (sm.getObjectId() != null) {
            dli.removedFromMain(orig = _main.remove(id));
            if (orig != sm) {
                if (orig != null) {
                    dli.removedWrong();
                    _main.put(id, orig); // put back
                }
                if (_conflicts != null) {
                    dli.removedFromConflicts(orig = _conflicts.remove(id));
                    if (orig != null && orig != sm) {
                        dli.removedWrongFromConflicts();
                        _conflicts.put(id, orig); // put back
                    }
                }
            }
        } else if ((_embeds == null || !dli.removedFromEmbeds(_embeds.remove(sm)))
                && _news != null) {
            dli.removeFromNews(orig = _news.remove(id));
            if (orig != null && orig != sm) {
                dli.removedWrong();
                _news.put(id, orig); // put back
            }
        }

        if (_untracked != null) {
            dli.removeUntracked(_untracked.remove(sm));
        }

        Log log = getLog();
        if (log.isTraceEnabled()) {
            // object-cache-delete: Cache "{0}" - removing ID "{1}" (obj OID: "{2}"), from main: "{3}" (wrongfully: "{4}"), \
            //  from conflict: "{5}" (wrongfully: "{6}"), from embedded: "{7}", from news: "{8}" (wrongfully: "{9}"), from \
            //  untracked: "{10}"
            log.trace(_loc.get("object-cache-delete", new Object[]{
                    broker, dli.id, dli.hasOID, dli.removeFromMain, dli.removedWrong,
                    dli.removedFromConflicts, dli.removedWrongFromConflicts, dli.removedEmbedded, dli.removedNews,
                    dli.removedWrong, dli.removedUntracked
            }));
        }

    }

    /**
     * An embedded or nonpersistent managed instance has been persisted.
     */
    public void persist(StateManagerImpl sm) {
        boolean deleted;
        if (_embeds != null) {
            deleted = _embeds.remove(sm);
        } else {
            deleted = false;
        }

        Log log = getLog();
        if (log.isTraceEnabled()) {
            log.trace(_loc.get("object-cache-delete-embedded", new Object[]{
                    broker, String.valueOf(sm), String.valueOf(deleted).toUpperCase()
            }));
        }

    }

    /**
     * A new instance has just been assigned a permanent oid.
     */
    public void assignObjectId(Object id, StateManagerImpl sm) {
        // if assigning oid, remove from new cache and put in primary; may
        // not be in new cache if another new instance had same id

        boolean removedNews = false;
        boolean removedNewsWrong = false;
        boolean conflict = false;

        StateManagerImpl orig;
        if (_news != null) {
            removedNews = (orig = _news.remove(id)) != null;
            if (removedNewsWrong = (orig != null && orig != sm)) {
                _news.put(id, orig); // put back
            }
        }

        // put in main cache, but make sure we don't replace another
        // instance with the same oid
        orig = _main.put(sm.getObjectId(), sm);
        if (orig != null) {
            conflict = true;
            _main.put(sm.getObjectId(), orig);
            if (!orig.isDeleted())
                throw new UserException(_loc.get("dup-oid-assign",
                    sm.getObjectId(),
                    Exceptions.toString(sm.getManagedInstance())))
                    .setFailedObject(sm.getManagedInstance());

            // same oid as deleted instance; put in conflict cache
            if (_conflicts == null)
                _conflicts = new HashMap<Object,StateManagerImpl>();
            _conflicts.put(sm.getObjectId(), sm);
        }

        Log log = getLog();
        if (log.isTraceEnabled()) {
            log.trace(_loc.get("object-cache-assign", new Object[]{
                    broker, id, sm.getObjectId(), removedNews?"YES":"NO",
                    removedNewsWrong?"YES":"NO", conflict?"YES":"NO"
            }));
        }

    }

    /**
     * A new instance has committed; recache under permanent oid.
     */
    public void commitNew(Object id, StateManagerImpl sm) {
        // if the id didn't change, the instance was already assigned an
        // id, but it could have been in conflict cache
        StateManagerImpl orig;
        Log log = getLog();
        if (sm.getObjectId() == id) {
            boolean removedConflict = (orig = (_conflicts == null) ? null : _conflicts.remove(id)) != null;
            boolean poppedDeleted = false;
            if (orig == sm) {
                poppedDeleted = (orig = _main.put(id, sm)) != null;
                if (orig != null && !orig.isDeleted()) {
                    _main.put(sm.getObjectId(), orig);
                    throw new UserException(_loc.get("dup-oid-assign",
                        sm.getObjectId(), Exceptions.toString(
                            sm.getManagedInstance())))
                        .setFailedObject(sm.getManagedInstance())
                        .setFatal(true);
                }
            }
            if (log.isTraceEnabled()) {
                log.trace(_loc.get("object-cache-commit-same", new Object[]{
                        broker, id, removedConflict?"YES":"NO", poppedDeleted?"YES":"NO"
                }));
            }
            return;
        }

        // oid changed, so it must previously have been a new instance
        // without an assigned oid.  remove it from the new cache; ok if
        // we end up removing another instance with same id
        boolean newRemoved = _news != null && _news.remove(id) != null;

        // and put into main cache now that id is assigned
        boolean replaced = (orig = _main.put(sm.getObjectId(), sm)) != null;
        if (orig != null && orig != sm && !orig.isDeleted()) {
            // put back orig and throw error
            _main.put(sm.getObjectId(), orig);
            throw new UserException(_loc.get("dup-oid-assign",
                sm.getObjectId(), Exceptions.toString(sm.getManagedInstance())))
                    .setFailedObject(sm.getManagedInstance()).setFatal(true);
        }

        if (log.isTraceEnabled()) {
            log.trace(_loc.get("object-cache-commit-update", new Object[]{
                    broker, id, sm.getObjectId(), newRemoved?"YES":"NO", replaced?"YES":"NO"
            }));
        }

    }

    /**
     * Return a copy of all cached persistent objects.
     */
    public Collection<StateManagerImpl> copy() {
        // proxies not included here because the state manager is always
        // present in other caches too

        int size = _main.size();
        if (_conflicts != null)
            size += _conflicts.size();
        if (_news != null)
            size += _news.size();
        if (_embeds != null)
            size += _embeds.size();
        if (size == 0)
            return Collections.EMPTY_LIST;

        List<StateManagerImpl> copy = new ArrayList<StateManagerImpl>(size);
        for (StateManagerImpl sm : _main.values())
            copy.add(sm);
        if (_conflicts != null && !_conflicts.isEmpty())
            for (StateManagerImpl sm : _conflicts.values())
                copy.add(sm);
        if (_news != null && !_news.isEmpty())
        	 for (StateManagerImpl sm : _news.values())
                 copy.add(sm);
        if (_embeds != null && !_embeds.isEmpty())
        	for (StateManagerImpl sm : _embeds)
                copy.add(sm);
        return copy;
    }

    /**
     * Clear the cache.
     */
    public void clear() {

        Log log = getLog();
        if (log.isTraceEnabled()) {
            log.trace(_loc.get("object-cache-cleared", broker));
        }

        _main = (Map<Object, StateManagerImpl>) broker.newManagedObjectCache();
        if (_conflicts != null)
            _conflicts = null;
        if (_news != null)
            _news = null;
        if (_embeds != null)
            _embeds = null;
        if (_untracked != null)
            _untracked = null;
    }

    /**
     * Clear new instances without permanent oids.
     */
    public void clearNew() {
        Log log = getLog();
        if (log.isTraceEnabled()) {
            log.trace(_loc.get("object-cache-cleared-new", broker));
        }
        if (_news != null)
            _news = null;
    }

    void dirtyCheck() {
        if (_untracked == null)
            return;

        for (StateManagerImpl sm : _untracked)
        	sm.dirtyCheck();
    }

    static class CacheGetLogInfo {
        final String mainFound;
        String isNew = "UNKNOWN";
        String isDeleted = "UNKNOWN";
        final String allowNew;
        String conflicted = "UNKNOWN";
        String fromNew = "UNKNOWN";
        final Object oid;

        CacheGetLogInfo(Object oid, boolean allowNew, StateManagerImpl main) {
            this.allowNew = String.valueOf(allowNew).toUpperCase();
            this.mainFound = String.valueOf(main != null).toUpperCase();
            this.oid = oid;
            if (main != null) {
                isNew = String.valueOf(main.isNew()).toUpperCase();
                isDeleted = String.valueOf(main.isDeleted()).toUpperCase();
            }
        }

        private void conflicted(StateManagerImpl sm) {
            conflicted = String.valueOf(sm != null).toUpperCase();
        }

        private void newed(StateManagerImpl sm) {
            fromNew = String.valueOf(sm != null).toUpperCase();
        }

    }

    static class CacheAddLogInfo {

        String untracked = "NO";
        String embedded = "NO";
        String isNew = "NO";
        String isMain = "NO";

        CacheAddLogInfo() {
        }

        private void untracked(StateManager sm) {
            untracked = String.valueOf(sm);
        }

        private void embedded(StateManager sm) {
            embedded = String.valueOf(sm);
        }

        private void setNew(StateManagerImpl sm) {
            isNew = String.valueOf(sm.getId());
        }

        private void setMain(StateManagerImpl sm) {
            isMain = String.valueOf(sm.getObjectId());
        }

    }

    static class CacheDeleteLogInfo {

        final String id;
        final String hasOID;
        String removeFromMain = "NO";
        String removedWrong = "NO";
        String removedUntracked = "NO";
        String removedEmbedded = "NO";
        String removedNews = "NO";
        String removedFromConflicts = "NO";
        String removedWrongFromConflicts = "NO";

        CacheDeleteLogInfo(Object id, StateManagerImpl sm) {
            hasOID = sm.getObjectId() == null ? "NO" :"YES";
            this.id = String.valueOf(id);
        }

        void removedFromMain(StateManagerImpl sm) {
            if (sm != null) {
                this.removeFromMain = "YES";
            }
        }

        void removedWrong() {
            this.removedWrong = "YES";
        }

        void removedFromConflicts(StateManagerImpl sm) {
            if (sm != null) {
                this.removedFromConflicts = "YES";
            }
        }

        void removedWrongFromConflicts() {
            this.removedWrongFromConflicts = "YES";
        }

        boolean removedFromEmbeds(boolean in) {
            if (in) {
                this.removedEmbedded = "YES";
            }
            return in;
        }

        void removeFromNews(StateManager sm) {
            if (sm != null) {
                this.removedNews = "YES";
            }
        }

        void removeUntracked(boolean in) {
            if (in) { removedUntracked = "YES"; }
        }

    }

}
