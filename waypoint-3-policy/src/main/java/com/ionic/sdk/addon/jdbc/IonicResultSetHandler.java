package com.ionic.sdk.addon.jdbc;

import com.ionic.sdk.agent.Agent;
import com.ionic.sdk.agent.cipher.chunk.ChunkCipherV2;
import com.ionic.sdk.agent.cipher.chunk.data.ChunkCrypto;
import com.ionic.sdk.error.IonicException;
import org.apache.commons.dbutils.ResultSetHandler;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collection;

/**
 * Implementation of commons-dbutils interface {@link ResultSetHandler}.
 * <p>
 * Business logic for loading data from a {@link java.sql.ResultSet}.  This sample application applies the Ionic SDK
 * decrypt transformation to the data as it is loaded from the ResultSet.
 */
public class IonicResultSetHandler implements ResultSetHandler<RowSet> {

    /**
     * Test Ionic agent, used to protect data on insert into database, and unprotect data on fetch from database.
     */
    private final Agent agent;

    /**
     * Text labels of the database columns that are Ionic-protected at rest in the database.
     */
    private final Collection<String> ionicColumns;

    /**
     * Constructor.
     *
     * @param agent        Ionic agent, used to protect data on insert into database, and unprotect data on fetch
     * @param ionicColumns text labels of the database columns that are Ionic-protected at rest in the database
     */
    public IonicResultSetHandler(final Agent agent, final Collection<String> ionicColumns) {
        super();
        this.agent = agent;
        this.ionicColumns = ionicColumns;
    }

    /**
     * Turn the ResultSet into an Object.
     *
     * For "waypoint-3", Ionic-protected columns are being tracked as they are encountered in the {@link ResultSet}.
     * If all Ionic decrypt operations for a given record fail, the row is filtered out of the data returned by the
     * function.
     *
     * @param resultSet the JDBC {@link ResultSet} from the database
     * @return the Ionic-filtered representation of the input {@link ResultSet}
     * @throws SQLException on errors reading from the {@link ResultSet}
     */
    @Override
    public RowSet handle(final ResultSet resultSet) throws SQLException {
        final ChunkCipherV2 chunkCipher = new ChunkCipherV2(agent);
        final RowSet rowSet = new RowSet();
        final ResultSetMetaData metaData = resultSet.getMetaData();
        final int columnCount = metaData.getColumnCount();
        while (resultSet.next()) {
            int ionicProtected = 0;
            int ionicAllowed = 0;
            final Object[] row = new Object[columnCount];
            for (int i = 0; i < columnCount; ++i) {
                final String columnName = metaData.getColumnName(i + 1);
                final Object value = resultSet.getObject(i + 1);
                final String valueText = (value == null) ? null : value.toString();
                row[i] = valueText;
                if (ionicColumns.contains(columnName)) {
                    if (ChunkCrypto.getChunkInfo(valueText).isEncrypted()) {
                        ++ionicProtected;
                        try {
                            row[i] = chunkCipher.decrypt(valueText);
                            ++ionicAllowed;
                        } catch (IonicException e) {
                            row[i] = "[RESTRICTED]";
                        }
                    }
                }
            }
            if (ionicAllowed > 0) {
                rowSet.add(row);
            }
        }
        return rowSet;
    }
}
