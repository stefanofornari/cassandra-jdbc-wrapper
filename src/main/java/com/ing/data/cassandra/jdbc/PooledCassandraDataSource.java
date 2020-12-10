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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.DataSource;
import javax.sql.PooledConnection;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.HashSet;
import java.util.Set;

import static com.ing.data.cassandra.jdbc.Utils.NOT_SUPPORTED;

public class PooledCassandraDataSource implements DataSource, ConnectionEventListener {
    private static final int CONNECTION_IS_VALID_TIMEOUT = 5;
    private static final int MIN_POOL_SIZE = 4;
    private static final Logger log = LoggerFactory.getLogger(PooledCassandraDataSource.class);

    private final CassandraDataSource connectionPoolDataSource;
    private final Set<PooledCassandraConnection> freeConnections = new HashSet<>();
    private final Set<PooledCassandraConnection> usedConnections = new HashSet<>();

    public PooledCassandraDataSource(final CassandraDataSource connectionPoolDataSource) {
        this.connectionPoolDataSource = connectionPoolDataSource;
    }

    @Override
    public synchronized Connection getConnection() throws SQLException {
        final PooledCassandraConnection pooledConnection;
        if (freeConnections.isEmpty()) {
            pooledConnection = connectionPoolDataSource.getPooledConnection();
            pooledConnection.addConnectionEventListener(this);
        } else {
            pooledConnection = freeConnections.iterator().next();
            freeConnections.remove(pooledConnection);
        }
        usedConnections.add(pooledConnection);
        return new ManagedConnection(pooledConnection);
    }

    @Override
    public Connection getConnection(final String username, final String password) throws SQLException {
        throw new SQLFeatureNotSupportedException(NOT_SUPPORTED);
    }

    @Override
    public synchronized void connectionClosed(final ConnectionEvent event) {
        final PooledCassandraConnection connection = (PooledCassandraConnection) event.getSource();
        usedConnections.remove(connection);
        final int freeConnectionsCount = freeConnections.size();
        if (freeConnectionsCount < MIN_POOL_SIZE) {
            freeConnections.add(connection);
        } else {
            try {
                connection.close();
            } catch (final SQLException e) {
                log.error(e.getMessage());
            }
        }
    }

    @Override
    public synchronized void connectionErrorOccurred(final ConnectionEvent event) {
        final PooledCassandraConnection connection = (PooledCassandraConnection) event.getSource();
        try {
            if (!connection.getConnection().isValid(CONNECTION_IS_VALID_TIMEOUT)) {
                connection.getConnection().close();
            }
        } catch (final SQLException e) {
            log.error(e.getMessage());
        }
        usedConnections.remove(connection);
    }

    public synchronized void close() {
        closePooledConnections(usedConnections);
        closePooledConnections(freeConnections);
    }

    private void closePooledConnections(final Set<PooledCassandraConnection> usedConnections) {
        for (final PooledConnection connection : usedConnections) {
            try {
                connection.close();
            } catch (final SQLException e) {
                log.error(e.getMessage());
            }
        }
    }

    @Override
    public int getLoginTimeout() {
        return connectionPoolDataSource.getLoginTimeout();
    }

    @Override
    public void setLoginTimeout(final int seconds) {
        connectionPoolDataSource.setLoginTimeout(seconds);
    }

    @Override
    public PrintWriter getLogWriter() {
        return connectionPoolDataSource.getLogWriter();
    }

    @Override
    public void setLogWriter(final PrintWriter writer) {
        connectionPoolDataSource.setLogWriter(writer);
    }

    @Override
    public boolean isWrapperFor(final Class<?> arg0) {
        return connectionPoolDataSource.isWrapperFor(arg0);
    }

    @Override
    public <T> T unwrap(final Class<T> arg0) throws SQLException {
        return connectionPoolDataSource.unwrap(arg0);
    }

    // Method not annotated with @Override since getParentLogger() is a new method
    // in the CommonDataSource interface starting with JDK7 and this annotation
    // would cause compilation errors with JDK6.
    @SuppressWarnings("override")
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return connectionPoolDataSource.getParentLogger();
    }
}
