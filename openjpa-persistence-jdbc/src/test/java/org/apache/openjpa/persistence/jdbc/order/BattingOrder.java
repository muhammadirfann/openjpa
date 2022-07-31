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
package org.apache.openjpa.persistence.jdbc.order;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

@Entity
public class BattingOrder {

    @Id
    @GeneratedValue
    int id;

    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @OrderColumn
    private List<Player> batters;

    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @OrderColumn(name="pinch_order")
    private List<Player> pinch_hitters;

    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @OrderColumn(insertable=true, updatable=false)
    private List<Player> fixedBatters;

    @ElementCollection
    @OrderColumn(insertable=false)
    private List<String> titles;

    public void setBatters(List<Player> batters) {
        this.batters = batters;
    }

    public List<Player> getBatters() {
        return batters;
    }

    public void setPinchHitters(List<Player> pinch_hitters) {
        this.pinch_hitters = pinch_hitters;
    }

    public List<Player> getPinchHitters() {
        return pinch_hitters;
    }

    public void setFixedBatters(List<Player> fixedBatters) {
        this.fixedBatters = fixedBatters;
    }

    public List<Player> getFixedBatters() {
        return fixedBatters;
    }

    public void setTitles(List<String> titles) {
        this.titles = titles;
    }

    public List<String> getTitles() {
        return titles;
    }
}
