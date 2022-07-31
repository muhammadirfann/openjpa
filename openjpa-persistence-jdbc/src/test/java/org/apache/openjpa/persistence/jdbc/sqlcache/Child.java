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
package org.apache.openjpa.persistence.jdbc.sqlcache;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Child in a bidirectional parent-child relationship.
 *
 * Notes:
 * a) there is no mutator for id because it is generated by JPA provider.
 *
 */
@Entity
@Table(name="zchild")
public class Child {
	@Id
	@GeneratedValue
	private String id;

	private String name;

	@ManyToOne(fetch=FetchType.LAZY)
	private Parent parent;

	/**
	 * Restrict access to constructor for Parent to create the Child.
	 */
	public Child() {

	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String city) {
		this.name = city;
	}

	public Parent getParent() {
		return parent;
	}

	void setParent(Parent owner) {
		this.parent = owner;
	}
}
