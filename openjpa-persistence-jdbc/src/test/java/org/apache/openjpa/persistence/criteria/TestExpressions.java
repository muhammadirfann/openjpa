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
package org.apache.openjpa.persistence.criteria;

import org.apache.openjpa.lib.log.Log;

import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;
import java.util.List;

public class TestExpressions extends CriteriaTest {

    public void testFunctionExpressions() {

        Log log = emf.getConfiguration().getLog("test");

        em.getTransaction().begin();

        Order o1 = new Order();
        em.createQuery("delete from Order o").executeUpdate();
        em.persist(o1);
        em.flush();

        for (int i=0; i<2; i++) {

            for (int j=0; j<2; j++) {

                for (int k=0; k<2; k++) {

                    boolean willFind = i == 0;
                    boolean useOr = j==0;
                    boolean useEq = k==0;

                    cb.createQuery(Order.class);

                    CriteriaQuery<Order> q = cb.createQuery(Order.class);
                    Root<Order> o = q.from(Order.class);

                    Expression<Boolean> trueOrFalse = useEq ?
                            willFind ?
                                    cb.equal(cb.literal(1), cb.literal(1))
                                    : cb.notEqual(cb.literal(1), cb.literal(1))
                            : cb.literal(willFind);

                    Expression<Boolean> fun = cb.function("COALESCE", Boolean.class, o.get(Order_.name), trueOrFalse);
                    if (useOr) {
                        q.where(cb.or(cb.lessThan(o.get(Order_.id), cb.literal(0)), fun));
                    } else {
                        q.where(fun);
                    }

                    q.select(o);

                    List<Order> orders = em.createQuery(q).getResultList();
                    log.info("Found:"+orders.size());
                    orders.forEach(e->log.info(e.getId() + "-"+e.getName()));
                    log.info(q.toString());

                    assertEquals(willFind, orders.size() == 1);


                }
            }

        }

        em.getTransaction().rollback();

    }

}
