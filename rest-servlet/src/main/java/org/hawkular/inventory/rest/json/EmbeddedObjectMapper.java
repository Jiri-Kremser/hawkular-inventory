/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkular.inventory.rest.json;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.hawkular.inventory.api.model.Relationship;

/**
 * @author Jirka Kremser
 * @since 0.1.0
 */
public class EmbeddedObjectMapper extends ObjectMapper {

    public EmbeddedObjectMapper() {
        JacksonConfig.initializeObjectMapper(this);
        SimpleModule relationshipModule = new SimpleModule("RelationshipEmbeddedModule", new Version(0, 1, 0, null,
                                                           "org.hawkular.inventory", "inventory-rest-api"));
        relationshipModule.addSerializer(Relationship.class, new RelationshipEmbeddedJacksonSerializer());
        registerModule(relationshipModule);
    }
}
