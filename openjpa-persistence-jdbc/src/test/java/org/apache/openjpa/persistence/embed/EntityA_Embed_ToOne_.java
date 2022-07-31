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

/**
 *  Generated by OpenJPA MetaModel Generator Tool.
**/

package org.apache.openjpa.persistence.embed;

import jakarta.persistence.metamodel.SingularAttribute;

@jakarta.persistence.metamodel.StaticMetamodel
(value=org.apache.openjpa.persistence.embed.EntityA_Embed_ToOne.class)
public class EntityA_Embed_ToOne_ {
    public static volatile SingularAttribute<EntityA_Embed_ToOne,Integer> age;
    public static volatile SingularAttribute<EntityA_Embed_ToOne,Embed_ToOne> embed;
    public static volatile SingularAttribute<EntityA_Embed_ToOne,Integer> id;
    public static volatile SingularAttribute<EntityA_Embed_ToOne,String> name;
}
