/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.hbase.mapr;

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.ClusterStatus;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.exceptions.DeserializationException;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.ParseFilter;
import org.apache.hadoop.hbase.security.visibility.Authorizations;
import org.apache.hadoop.hbase.security.visibility.CellVisibility;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.nifi.annotation.behavior.DynamicProperty;
import org.apache.nifi.annotation.behavior.RequiresInstanceClassLoading;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnDisabled;
import org.apache.nifi.annotation.lifecycle.OnEnabled;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.hbase.DeleteRequest;
import org.apache.nifi.hbase.HBaseClientService;
import org.apache.nifi.hbase.put.PutColumn;
import org.apache.nifi.hbase.put.PutFlowFile;
import org.apache.nifi.hbase.scan.Column;
import org.apache.nifi.hbase.scan.ResultCell;
import org.apache.nifi.hbase.scan.ResultHandler;
import org.apache.nifi.util.mapr.MapRComponentsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * Class for HPE hbase and maprdb configuration,servicing,holding and interaction connection within MapR cluster.
 * Managing connection.
 * {@link #customValidate(ValidationContext validationContext)} - checks that hbase is installed and hbase-site.xml configuration is present
 * {@link #createConfiguration()} - hbase configuration
 * {@link #onEnabled(ConfigurationContext context)} - connection servicing
 * {@link #shutdown()} - connection servicing
 * <p>
 * Methods that provide CRUD operations with Hbase and maprDb via mapR cluster
 *
 * @link -> methods delete ,scan etc
 */

@RequiresInstanceClassLoading
@Tags({"hbase", "client", "maprdb"})
@CapabilityDescription("Implementation of Hbase and maprdb service which can establish connection to hbase within MapR cluster. Caution !!! -> Service can be launched only on the cluster's nodes with installed and configured hbase-client.")
@DynamicProperty(name = "The name of an HBase configuration property.", value = "The value of the given HBase configuration property.", description = "These properties will be set on the HBase configuration after loading any provided configuration files.")
public class EEP_HbaseMaprDbClientService extends AbstractControllerService implements HBaseClientService {
    private static final Logger logger = LoggerFactory.getLogger(EEP_HbaseMaprDbClientService.class);
    protected volatile Connection connection;
    private volatile String masterAddress;
    private volatile String hbaseConfPath = null;
    private static final String HBASE_COMPONENT_NAME = "hbase";
    private static final String HBASE_CONF_INTERNAL_PATH = "/conf";
    private static final String HBASE_CONF_SITE_XML = "hbase-site.xml";
    /**
     * This is custom realization of hbase_service for eep maprdb and hbase connection.
     *
     * @see EEP_HbaseMaprDbClientService -> method createConnection(context)
     */
    @OnEnabled
    public void onEnabled(ConfigurationContext context) throws IOException {
        Configuration conf = createConfiguration();
        connection = createConnection(conf);
        connectionCheck(context);
    }
    @Override
    protected Collection<ValidationResult> customValidate(ValidationContext validationContext) {
        List<ValidationResult> problems = new ArrayList<>();
        try {
            hbaseConfPath = getHbaseConfFolder();
        } catch (RuntimeException e) {
            problems.add(
                    new ValidationResult
                            .Builder()
                            .valid(false)
                            .subject(this.getClass().getSimpleName())
                            .explanation("Hbase client not installed !!!")
                            .build()
            );
        }

        return problems;
    }

    public String getHbaseConfFolder() {
        String hbaseFolder;
        try {
            hbaseFolder = MapRComponentsUtils
                    .getComponentFolder(HBASE_COMPONENT_NAME)
                    .toString();
        } catch (IOException e) {
            throw new RuntimeException("Hbase not installed!!!", e);
        }

        return Paths.get(hbaseFolder, HBASE_CONF_INTERNAL_PATH).toString();
    }

    protected Configuration createConfiguration() {
        Configuration hbaseConfig = HBaseConfiguration.create();
        hbaseConfig.addResource(new Path(hbaseConfPath, HBASE_CONF_SITE_XML));

        return hbaseConfig;
    }
    @OnDisabled
    public void shutdown() {
        if (connection != null) {
            try {
                connection.close();
            } catch (IOException ioe) {
                logger.error("Failed to close connection to HBase due to {}",
                        ioe.getMessage());
            }
        }
    }
    protected void connectionCheck(ConfigurationContext context) throws IOException {
        // connection check
        if (this.connection != null) {
            Admin admin = this.connection.getAdmin();
            if (admin != null) {
                admin.listTableNames();

                ClusterStatus clusterStatus = admin.getClusterStatus();
                if (clusterStatus != null) {
                    ServerName master = clusterStatus.getMaster();
                    if (master != null) {
                        masterAddress = master.getHostAndPort();
                    } else {
                        masterAddress = null;
                    }
                }
            }
        }
    }
    protected Connection createConnection(Configuration conf) throws IOException {
        Connection connection = null;
        try {
           logger.debug("Connections Start");

            connection = ConnectionFactory.createConnection(conf);

            logger.debug("Connections Established");

        } catch (IOException e) {
            logger.error("Connections failed e={}, {}", e.getMessage(), e);
        }

        return connection;
    }
    protected List<Put> buildPuts(byte[] rowKey, List<PutColumn> columns) throws IOException {
        List<Put> retVal = new ArrayList<>();

        try {
            Put put = null;

            for (PutColumn column : columns) {

                //visibility - comes from PutHbaseRecordProcessor, not obligatory
                if (put == null || (put.getCellVisibility() == null && column.getVisibility() != null) || (put.getCellVisibility() != null && !put.getCellVisibility().getExpression().equals(column.getVisibility()))) {
                    put = new Put(rowKey);

                    if (column.getVisibility() != null) {
                        put.setCellVisibility(new CellVisibility(column.getVisibility()));
                    }
                    retVal.add(put);
                }

                if (column.getTimestamp() != null) {
                    put.addColumn(column.getColumnFamily(), column.getColumnQualifier(), column.getTimestamp(), column.getBuffer());
                } else {
                    put.addColumn(column.getColumnFamily(), column.getColumnQualifier(), column.getBuffer());
                }
            }
        } catch (DeserializationException de) {
            logger.error("Error writing cell visibility statement.", de);
            throw new IOException(de);
        }

        return retVal;
    }
    @Override
    public void put(String tableName, Collection<PutFlowFile> puts) throws IOException {

        try (Table table = connection.getTable(TableName.valueOf(tableName))) {
            // Create one Put per row....
            Map<String, List<PutColumn>> sorted = new HashMap<>();
            List<Put> newPuts = new ArrayList<>();

            for (PutFlowFile putFlowFile : puts) {
                String rowKeyString = new String(
                        putFlowFile.getRow(),
                        StandardCharsets.UTF_8);
                List<PutColumn> columns = sorted.computeIfAbsent(
                        rowKeyString,
                        k -> new ArrayList<>());

                columns.addAll(putFlowFile.getColumns());
            }

            for (Map.Entry<String, List<PutColumn>> entry : sorted.entrySet()) {
                newPuts.addAll(buildPuts(
                        entry.getKey().getBytes(StandardCharsets.UTF_8),
                        entry.getValue()));
            }

            table.put(newPuts);
        }

    }
    @Override
    public void put(String tableName, byte[] rowId, Collection<PutColumn> columns)
            throws IOException
    {
        try (Table table = connection.getTable(TableName.valueOf(tableName))) {
            table.put(buildPuts(rowId, new ArrayList(columns)));
        }
    }

    @Override
    public boolean checkAndPut(
            String tableName,
            byte[] rowId,
            byte[] family,
            byte[] qualifier,
            byte[] value,
            PutColumn column
    )
            throws IOException
    {
        try (Table table = connection.getTable(TableName.valueOf(tableName))) {
            Put put = new Put(rowId);
            put.addColumn(column.getColumnFamily(),
                    column.getColumnQualifier(),
                    column.getBuffer());

            return table.checkAndPut(rowId, family, qualifier, value, put);
        }
    }

    @Override
    public void delete(String tableName, byte[] rowId) throws IOException {
        delete(tableName, rowId, null);
    }

    @Override
    public void delete(String tableName, byte[] rowId, String visibilityLabel) throws IOException {
        try (Table table = connection.getTable(TableName.valueOf(tableName))) {
            Delete delete = new Delete(rowId);
            if (!StringUtils.isEmpty(visibilityLabel)) {
                delete.setCellVisibility(new CellVisibility(visibilityLabel));
            }
            table.delete(delete);
        }

    }

    @Override
    public void delete(String tableName, List<byte[]> rowIds) throws IOException {
        delete(tableName, rowIds, null);
    }

    @Override
    public void deleteCells(String tableName, List<DeleteRequest> deletes) throws IOException {
        List<Delete> deleteRequests = new ArrayList<>();
        for (DeleteRequest req : deletes) {
            Delete delete = new Delete(req.getRowId())
                    .addColumn(
                            req.getColumnFamily(),
                            req.getColumnQualifier()
                    );
            if (!StringUtils.isEmpty(req.getVisibilityLabel())) {
                delete.setCellVisibility(new CellVisibility(req.getVisibilityLabel()));
            }
            deleteRequests.add(delete);
        }
        batchDelete(tableName, deleteRequests);
    }

    @Override
    public void delete(String tableName, List<byte[]> rowIds, String visibilityLabel) throws IOException {
        List<Delete> deletes = new ArrayList<>();
        for (byte[] rowId : rowIds) {
            Delete delete = new Delete(rowId);
            if (!StringUtils.isBlank(visibilityLabel)) {
                delete.setCellVisibility(new CellVisibility(visibilityLabel));
            }
            deletes.add(delete);
        }
        batchDelete(tableName, deletes);
    }

    private void batchDelete(String tableName, List<Delete> deletes) throws IOException {

        try (Table table = connection.getTable(TableName.valueOf(tableName))) {
            table.delete(deletes);
        }

    }

    @Override
    public void scan(String tableName, Collection<Column> columns, String filterExpression, long minTime, ResultHandler handler) throws IOException {
        scan(tableName, columns, filterExpression, minTime, null, handler);
    }

    @Override
    public void scan(String tableName, Collection<Column> columns, String filterExpression, long minTime, List<String> visibilityLabels, ResultHandler handler) throws IOException {

        Filter filter = null;
        if (!StringUtils.isBlank(filterExpression)) {
            ParseFilter parseFilter = new ParseFilter();
            filter = parseFilter.parseFilterString(filterExpression);
        }

        try (
                Table table = connection.getTable(TableName.valueOf(tableName));
             ResultScanner scanner = getResults(
                     table, columns, filter, minTime, visibilityLabels
             ))
        {

            for (Result result : scanner) {
                byte[] rowKey = result.getRow();
                Cell[] cells = result.rawCells();

                if (cells == null) {
                    continue;
                }

                // convert HBase cells to NiFi cells
                ResultCell[] resultCells = new ResultCell[cells.length];
                for (int i = 0; i < cells.length; i++) {
                    Cell cell = cells[i];
                    ResultCell resultCell = getResultCell(cell);
                    resultCells[i] = resultCell;
                }

                // delegate to the handler
                handler.handle(rowKey, resultCells);
            }
        }
    }

    @Override
    public void scan(
            String tableName,
            byte[] startRow,
            byte[] endRow,
            Collection<Column> columns,
            List<String> authorizations,
            ResultHandler handler)
            throws IOException
    {
        try (
                Table table = connection.getTable(TableName.valueOf(tableName));
                ResultScanner scanner = getResults(
                        table, startRow, endRow, columns, authorizations)
        ) {
            for (Result result : scanner) {
                byte[] rowKey = result.getRow();
                Cell[] cells = result.rawCells();

                if (cells == null) {
                    continue;
                }

                // convert HBase cells to NiFi cells
                ResultCell[] resultCells = new ResultCell[cells.length];
                for (int i = 0; i < cells.length; i++) {
                    Cell cell = cells[i];
                    ResultCell resultCell = getResultCell(cell);
                    resultCells[i] = resultCell;
                }

                // delegate to the handler
                handler.handle(rowKey, resultCells);
            }
        }

    }

    @Override
    public void scan(
            String tableName,
            String startRow,
            String endRow,
            String filterExpression,
            Long timerangeMin,
            Long timerangeMax,
            Integer limitRows,
            Boolean isReversed,
            Boolean blockCache,
            Collection<Column> columns,
            List<String> visibilityLabels,
            ResultHandler handler)
            throws IOException
    {

        try (
                Table table = connection.getTable(TableName.valueOf(tableName));
                ResultScanner scanner = getResults(
                        table,
                        startRow,
                        endRow,
                        filterExpression,
                        timerangeMin, timerangeMax,
                        limitRows,
                        isReversed,
                        blockCache,
                        columns,
                        visibilityLabels)
        ) {

            int cnt = 0;
            //lim comes from processor properties that we set manually on the ui.
            // Limit rows
            int lim = limitRows != null ? limitRows : 0;
            for (Result result : scanner) {

                if (lim > 0 && ++cnt > lim) {
                    break;
                }

                byte[] rowKey = result.getRow();
                Cell[] cells = result.rawCells();

                if (cells == null) {
                    continue;
                }

                // convert HBase cells to NiFi cells
                ResultCell[] resultCells = new ResultCell[cells.length];
                for (int i = 0; i < cells.length; i++) {
                    Cell cell = cells[i];
                    ResultCell resultCell = getResultCell(cell);
                    resultCells[i] = resultCell;
                }

                // delegate to the handler
                handler.handle(rowKey, resultCells);
            }
        }

    }

    protected ResultScanner getResults(
            Table table,
            String startRow,
            String endRow,
            String filterExpression,
            Long timerangeMin,
            Long timerangeMax,
            Integer limitRows,
            Boolean isReversed,
            Boolean blockCache,
            Collection<Column> columns, List<String> authorizations
    ) throws IOException
    {
        Scan scan = new Scan();
        if (!StringUtils.isBlank(startRow)) {
            scan.withStartRow(startRow.getBytes(StandardCharsets.UTF_8));
        }
        if (!StringUtils.isBlank(endRow)) {
            scan.withStopRow(endRow.getBytes(StandardCharsets.UTF_8));
        }

        if (authorizations != null && authorizations.size() > 0) {
            scan.setAuthorizations(new Authorizations(authorizations));
        }

        Filter filter = null;
        if (columns != null) {
            for (Column col : columns) {
                if (col.getQualifier() == null) {
                    scan.addFamily(col.getFamily());
                } else {
                    scan.addColumn(col.getFamily(), col.getQualifier());
                }
            }
        }
        if (!StringUtils.isBlank(filterExpression)) {
            ParseFilter parseFilter = new ParseFilter();
            filter = parseFilter.parseFilterString(filterExpression);
        }
        if (filter != null) {
            scan.setFilter(filter);
        }

        if (timerangeMin != null && timerangeMax != null) {
            scan.setTimeRange(timerangeMin, timerangeMax);
        }

        // ->>> reserved for HBase v 2 or later
        //if (limitRows != null && limitRows > 0){
        //    scan.setLimit(limitRows)
        //}

        if (isReversed != null) {
            scan.setReversed(isReversed);
        }

        scan.setCacheBlocks(blockCache);

        return table.getScanner(scan);
    }

    // protected and extracted into separate method for testing
    protected ResultScanner getResults(
            Table table,
            byte[] startRow,
            byte[] endRow, Collection<Column> columns,
            List<String> authorizations
    ) throws IOException {
        Scan scan = new Scan();
        scan.withStartRow(startRow);
        scan.withStopRow(endRow);

        if (authorizations != null && authorizations.size() > 0) {
            scan.setAuthorizations(new Authorizations(authorizations));
        }

        if (columns != null && columns.size() > 0) {
            for (Column col : columns) {
                if (col.getQualifier() == null) {
                    scan.addFamily(col.getFamily());
                } else {
                    scan.addColumn(col.getFamily(), col.getQualifier());
                }
            }
        }

        return table.getScanner(scan);
    }

    // protected and extracted into separate method for testing
    protected ResultScanner getResults(
            Table table,
            Collection<Column> columns,
            Filter filter,
            long minTime,
            List<String> authorizations) throws IOException {
        // Create a new scan. We will set the min timerange as the latest timestamp that
        // we have seen so far. The minimum timestamp is inclusive, so we will get duplicates.
        // We will record any cells that have the latest timestamp, so that when we scan again,
        // we know to throw away those duplicates.
        Scan scan = new Scan();
        scan.setTimeRange(minTime, Long.MAX_VALUE);

        if (authorizations != null && authorizations.size() > 0) {
            scan.setAuthorizations(new Authorizations(authorizations));
        }

        if (filter != null) {
            scan.setFilter(filter);
        }

        if (columns != null) {
            for (Column col : columns) {
                if (col.getQualifier() == null) {
                    scan.addFamily(col.getFamily());
                } else {
                    scan.addColumn(col.getFamily(), col.getQualifier());
                }
            }
        }

        return table.getScanner(scan);
    }

    private ResultCell getResultCell(Cell cell) {
        ResultCell resultCell = new ResultCell();
        resultCell.setRowArray(cell.getRowArray());
        resultCell.setRowOffset(cell.getRowOffset());
        resultCell.setRowLength(cell.getRowLength());

        resultCell.setFamilyArray(cell.getFamilyArray());
        resultCell.setFamilyOffset(cell.getFamilyOffset());
        resultCell.setFamilyLength(cell.getFamilyLength());

        resultCell.setQualifierArray(cell.getQualifierArray());
        resultCell.setQualifierOffset(cell.getQualifierOffset());
        resultCell.setQualifierLength(cell.getQualifierLength());

        resultCell.setTimestamp(cell.getTimestamp());
        resultCell.setTypeByte(cell.getTypeByte());
        resultCell.setSequenceId(cell.getSequenceId());

        resultCell.setValueArray(cell.getValueArray());
        resultCell.setValueOffset(cell.getValueOffset());
        resultCell.setValueLength(cell.getValueLength());

        resultCell.setTagsArray(cell.getTagsArray());
        resultCell.setTagsOffset(cell.getTagsOffset());
        resultCell.setTagsLength(cell.getTagsLength());
        return resultCell;
    }

    @Override
    public byte[] toBytes(boolean b) {
        return Bytes.toBytes(b);
    }

    @Override
    public byte[] toBytes(float f) {
        return Bytes.toBytes(f);
    }

    @Override
    public byte[] toBytes(int i) {
        return Bytes.toBytes(i);
    }

    @Override
    public byte[] toBytes(long l) {
        return Bytes.toBytes(l);
    }

    @Override
    public byte[] toBytes(double d) {
        return Bytes.toBytes(d);
    }

    @Override
    public byte[] toBytes(String s) {
        return Bytes.toBytes(s);
    }

    @Override
    public byte[] toBytesBinary(String s) {
        return Bytes.toBytesBinary(s);
    }

    @Override
    public String toTransitUri(String tableName, String rowKey) {
        if (connection == null) {
            logger.warn("Connection has not been established, could not create a transit URI. Returning null.");
            return null;
        }
        String transitUriMasterAddress =
                StringUtils.isEmpty(masterAddress) ? "unknown" : masterAddress;
        return "hbase://"
                + transitUriMasterAddress
                + "/"
                + tableName
                + (StringUtils.isEmpty(rowKey) ? "" : "/" + rowKey);
    }
}
