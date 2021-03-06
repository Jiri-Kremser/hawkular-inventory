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
package org.hawkular.inventory.api.model;

import com.google.gson.annotations.Expose;

import javax.xml.bind.annotation.XmlAttribute;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A common super class of both entities and relationships.
 *
 * @param <B> the blueprint class. The blueprint is used to create a new element.
 * @param <U> the update class. The update class is used to update the element.
 *
 * @author Lukas Krejci
 * @since 0.0.1
 */
public abstract class AbstractElement<B, U extends AbstractElement.Update> {
    public static final String ID_PROPERTY = "id";

    @XmlAttribute
    @Expose
    protected final String id;
    @Expose
    protected final Map<String, Object> properties;

    //JAXB support
    AbstractElement() {
        id = null;
        properties = null;
    }

    AbstractElement(String id, Map<String, Object> properties) {
        if (id == null) {
            throw new IllegalArgumentException("id == null");
        }

        this.id = id;
        if (properties == null) {
            this.properties = null;
        } else {
            this.properties = new HashMap<>(properties);
            this.properties.remove(ID_PROPERTY);
        }
    }

    /**
     * @return the id of the entity.
     */
    public String getId() {
        return id;
    }

    /**
     * @return a map of arbitrary properties of this entity.
     */
    public Map<String, Object> getProperties() {
        if (properties == null) {
            return Collections.emptyMap();
        }
        return properties;
    }

    protected static <T> T valueOrDefault(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }

    /**
     * @return a new updater object to modify this entity and produce a new one.
     */
    //if only Java had "Self" type like Rust :(
    public abstract Updater<U, ? extends AbstractElement<?, U>> update();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractElement<?, ?> entity = (AbstractElement<?, ?>) o;

        return id.equals(entity.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public abstract static class Update {
        private final Map<String, Object> properties;

        public Update(Map<String, Object> properties) {
            this.properties = properties == null ? null : Collections.unmodifiableMap(new HashMap<>(properties));
        }

        public Map<String, Object> getProperties() {
            if (properties == null) {
                return Collections.emptyMap();
            }
            return properties;
        }

        public abstract static class Builder<U extends Update, This extends Builder<U, This>> {
            protected final Map<String, Object> properties = new HashMap<>();

            public This withProperty(String key, Object value) {
                properties.put(key, value);
                return castThis();
            }

            public This withProperties(Map<String, Object> properties) {
                this.properties.putAll(properties);
                return castThis();
            }

            public abstract U build();

            @SuppressWarnings("unchecked")
            protected This castThis() {
                return (This) this;
            }
        }
    }

    public static final class Updater<U extends Update, E extends AbstractElement<?, U>> {
        private final Function<U, E> updater;

        Updater(Function<U, E> updater) {
            this.updater = updater;
        }

        public E with(U update) {
            return updater.apply(update);
        }
    }
}
