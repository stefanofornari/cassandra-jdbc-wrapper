/*
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.ing.data.cassandra.jdbc;

import java.sql.Types;

/**
 * JDBC description of {@code ASCII} CQL type (corresponding Java type: {@link String}).
 * <p>CQL type description: US-ASCII character string.</p>
 */
public class JdbcAscii extends AbstractJdbcType<String> {

    /**
     * Gets a {@code JdbcAscii} instance.
     */
    public static final JdbcAscii INSTANCE = new JdbcAscii();

    JdbcAscii() {
    }

    @Override
    public boolean isCaseSensitive() {
        return true;
    }

    @Override
    public int getScale(final String obj) {
        return DEFAULT_SCALE;
    }

    @Override
    public int getPrecision(final String obj) {
        if (obj != null) {
            return obj.length();
        }
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isCurrency() {
        return false;
    }

    @Override
    public boolean isSigned() {
        return false;
    }

    @Override
    public String toString(final String obj) {
        return obj;
    }

    @Override
    public boolean needsQuotes() {
        return true;
    }

    public String getString(final Object obj) {
        return obj.toString();
    }

    @Override
    public Class<String> getType() {
        return String.class;
    }

    @Override
    public int getJdbcType() {
        return Types.VARCHAR;
    }

    @Override
    public String compose(final Object obj) {
        if (obj == null) {
            return null;
        } else {
            return obj.toString();
        }
    }

    @Override
    public Object decompose(final String value) {
        return value;
    }

}