/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.inventory.impl.tinkerpop.sql.impl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.VertexQuery;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 * @author Lukas Krejci
 * @since 1.0
 */
public final class SqlVertex extends SqlElement implements Vertex {

    public static final ElementGenerator<SqlVertex>
            GENERATOR = new ElementGenerator<SqlVertex>() {
        @Override
        public SqlVertex generate(SqlGraph graph, ResultSet rs) {
            try {
                return new SqlVertex(graph, rs.getLong(1));
            } catch (SQLException e) {
                throw new SqlGraphException("Failed to generate SqlVertex from resultset", e);
            }
        }
    };

    public static final List<String> DISALLOWED_PROPERTY_NAMES = Arrays.asList("id");

    SqlVertex(SqlGraph graph, long id) {
        super(graph, id);
    }

    public static String getPropertyTableForeignKey() {
        return "vertex_id";
    }

    @Override
    public void remove() {
        synchronized (graph) {
            try {
                PreparedStatement stmt = graph.getStatements().getRemoveVertex(getId());
                stmt.executeUpdate();
                graph.setDirty();
            } catch (SQLException e) {
                throw new SqlGraphException(e);
            }
        }
    }

    @Override
    protected String getPropertiesTableName() {
        return graph.getVertexPropertiesTableName();
    }

    @Override
    protected String getPropertyTableElementIdName() {
        return getPropertyTableForeignKey();
    }

    @Override
    protected List<String> getDisallowedPropertyNames() {
        return DISALLOWED_PROPERTY_NAMES;
    }

    @Override
    public Iterable<Edge> getEdges(Direction direction, String... labels) {
        StringBuilder sql = new StringBuilder(
            "SELECT e.id, e.vertex_in, e.vertex_out, e.label FROM edges e, vertices v WHERE v.id = ? ");

        switch (direction) {
        case IN:
            sql.append("AND e.vertex_in = v.id ");
            break;
        case OUT:
            sql.append("AND e.vertex_out = v.id ");
            break;
        case BOTH:
            sql.append("AND e.vertex_in = v.id ");
            addLabelConditions(sql, "e", labels);
            sql.append(
                " UNION ALL SELECT e.id, e.vertex_in, e.vertex_out, e.label FROM edges e, vertices v WHERE v.id = ? AND e.vertex_out = v.id ");
            break;
        }

        addLabelConditions(sql, "e", labels);

        synchronized (graph) {
            try {
                PreparedStatement stmt = graph.getConnection()
                        .prepareStatement(sql.toString(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet
                                .CONCUR_READ_ONLY);
                stmt.setLong(1, getId());
                int inc = 2;
                if (direction == Direction.BOTH) {
                    for (int i = 0; i < labels.length; ++i) {
                        stmt.setString(i + inc, labels[i]);
                    }

                    inc += labels.length;

                    stmt.setLong(inc, getId());

                    inc++;
                }

                for (int i = 0; i < labels.length; ++i) {
                    stmt.setString(i + inc, labels[i]);
                }

                return new StatementIterable<Edge>(SqlEdge.GENERATOR, graph, stmt);
            } catch (SQLException e) {
                throw new SqlGraphException(e);
            }
        }
    }

    @Override
    public Iterable<Vertex> getVertices(Direction direction, String... labels) {
        StringBuilder sql = new StringBuilder("SELECT v.id FROM vertices v, edges e WHERE ");

        switch (direction) {
        case IN:
            sql.append("e.vertex_in = ? AND e.vertex_out = v.id ");
            break;
        case OUT:
            sql.append("e.vertex_out = ? AND e.vertex_in = v.id ");
            break;
        case BOTH:
            sql.append("e.vertex_in = ? AND e.vertex_out = v.id ");
            addLabelConditions(sql, "e", labels);
            sql.append(" UNION ALL SELECT v.id FROM vertices v, edges e WHERE e.vertex_out = ? AND e.vertex_in = v.id ");
            break;
        }

        addLabelConditions(sql, "e", labels);

        synchronized (graph) {
            try {
                PreparedStatement stmt = graph.getConnection()
                        .prepareStatement(sql.toString(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet
                                .CONCUR_READ_ONLY);
                stmt.setLong(1, getId());
                int inc = 2;
                if (direction == Direction.BOTH) {
                    for (int i = 0; i < labels.length; ++i) {
                        stmt.setString(i + inc, labels[i]);
                    }

                    inc += labels.length;

                    stmt.setLong(inc, getId());

                    inc++;
                }

                for (int i = 0; i < labels.length; ++i) {
                    stmt.setString(i + inc, labels[i]);
                }

                return new StatementIterable<Vertex>(
                        SqlVertex.GENERATOR, graph, stmt);
            } catch (SQLException e) {
                throw new SqlGraphException(e);
            }
        }
    }

    @Override
    public VertexQuery query() {
        return new SqlVertexQuery(graph, getId());
    }

    @Override
    public SqlEdge addEdge(String label, Vertex inVertex) {
        return graph.addEdge(null, this, inVertex, label);
    }

    private boolean addLabelConditions(StringBuilder sql, String tableName, String... labels) {
        if (labels.length > 0) {
            sql.append("AND ").append(tableName).append(".label IN (?");
        }

        for (int i = 1; i < labels.length; ++i) {
            sql.append(", ?");
        }

        if (labels.length > 0) {
            sql.append(") ");
        }

        return labels.length > 0;
    }
}
