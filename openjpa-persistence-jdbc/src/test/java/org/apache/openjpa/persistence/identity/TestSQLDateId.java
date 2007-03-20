/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.persistence.identity;

import java.sql.Date;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import junit.framework.TestCase;
import junit.textui.TestRunner;

/**
 * Test that entities can use SQL dates as identity values.
 *
 * @author Abe White
 */
public class TestSQLDateId
    extends TestCase {

    private EntityManagerFactory emf;

    public void setUp() {
        String types = SQLDateIdEntity.class.getName();
        Map props = new HashMap(System.getProperties());
        props.put("openjpa.MetaDataFactory", "jpa(Types=" + types + ")");
        emf = Persistence.createEntityManagerFactory("test", props);
    }

    public void tearDown() {
        if (emf == null)
            return;
        try {
            EntityManager em = emf.createEntityManager();
            em.getTransaction().begin();
            em.createQuery("delete from SQLDateIdEntity").executeUpdate();
            em.getTransaction().commit();
            em.close();
            emf.close();
        } catch (Exception e) {
        }
    }

    public void testPersist() {
        long time = ((long) (System.currentTimeMillis() / 1000)) * 1000;

        SQLDateIdEntity e = new SQLDateIdEntity();
        e.setId(new Date(time));
        e.setData(1);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(e);
        em.getTransaction().commit();
        assertEquals(time, e.getId().getTime());
        em.close();

        em = emf.createEntityManager();
        e = em.find(SQLDateIdEntity.class, new Date(time));
        assertEquals(1, e.getData());
        em.close();
    }

    public static void main(String[] args) {
        TestRunner.run(SQLDateIdEntity.class);
    }
}

