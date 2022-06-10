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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.hbase.put.PutColumn;
import org.apache.nifi.hbase.put.PutFlowFile;
import org.apache.nifi.hbase.scan.Column;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.TestRunner;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;

/**
 * Override methods to create a mock service that can return staged data
 */
public class MockService extends EEP_HbaseMaprDbClientService {

    private Table table;
    private String family;
    private String hbasePath;
    private Configuration hbaseConfiguration;
    private Map<String, Result> results = new HashMap<>();

    public MockService(Table table, String family, String hbasePath, Configuration hbaseConfiguration) {
        this.table = table;
        this.family = family;
        this.hbasePath = hbasePath;
        this.hbaseConfiguration = hbaseConfiguration;
    }


    public void addResult(String rowKey, Map<String, String> cells, long timestamp) {
        byte[] rowArray = rowKey.getBytes(StandardCharsets.UTF_8);
        Cell[] cellArray = new Cell[cells.size()];
        int i = 0;
        for (Map.Entry<String, String> cellEntry : cells.entrySet()) {
            Cell cell = Mockito.mock(Cell.class);
            when(cell.getRowArray()).thenReturn(rowArray);
            when(cell.getRowOffset()).thenReturn(0);
            when(cell.getRowLength()).thenReturn((short) rowArray.length);

            String cellValue = cellEntry.getValue();
            byte[] valueArray = cellValue.getBytes(StandardCharsets.UTF_8);
            when(cell.getValueArray()).thenReturn(valueArray);
            when(cell.getValueOffset()).thenReturn(0);
            when(cell.getValueLength()).thenReturn(valueArray.length);

            byte[] familyArray = family.getBytes(StandardCharsets.UTF_8);
            when(cell.getFamilyArray()).thenReturn(familyArray);
            when(cell.getFamilyOffset()).thenReturn(0);
            when(cell.getFamilyLength()).thenReturn((byte) familyArray.length);

            String qualifier = cellEntry.getKey();
            byte[] qualifierArray = qualifier.getBytes(StandardCharsets.UTF_8);
            when(cell.getQualifierArray()).thenReturn(qualifierArray);
            when(cell.getQualifierOffset()).thenReturn(0);
            when(cell.getQualifierLength()).thenReturn(qualifierArray.length);

            when(cell.getTimestamp()).thenReturn(timestamp);

            cellArray[i++] = cell;
        }

        Result result = Mockito.mock(Result.class);
        when(result.getRow()).thenReturn(rowArray);
        when(result.rawCells()).thenReturn(cellArray);
        results.put(rowKey, result);
    }

    @Override
    public void put(String tableName, byte[] rowId, Collection<PutColumn> columns) throws IOException {
        Put put = new Put(rowId);
        Map<String, String> map = new HashMap<>();
        for (PutColumn column : columns) {
            put.addColumn(
                    column.getColumnFamily(),
                    column.getColumnQualifier(),
                    column.getBuffer());
            map.put(new String(column.getColumnQualifier()), new String(column.getBuffer()));
        }

        table.put(put);
        addResult(new String(rowId), map, 1);
    }

    @Override
    public void put(String tableName, Collection<PutFlowFile> puts) throws IOException {
        Map<String, List<PutColumn>> sorted = new HashMap<>();
        List<Put> newPuts = new ArrayList<>();

        for (PutFlowFile putFlowFile : puts) {
            Map<String, String> map = new HashMap<>();
            String rowKeyString = new String(putFlowFile.getRow(), StandardCharsets.UTF_8);
            List<PutColumn> columns = sorted.computeIfAbsent(rowKeyString, k -> new ArrayList<>());

            columns.addAll(putFlowFile.getColumns());
            for (PutColumn column : putFlowFile.getColumns()) {
                map.put(new String(column.getColumnQualifier()), new String(column.getBuffer()));
            }

            addResult(new String(putFlowFile.getRow()), map, 1);
        }

        for (Map.Entry<String, List<PutColumn>> entry : sorted.entrySet()) {
            newPuts.addAll(buildPuts(entry.getKey().getBytes(StandardCharsets.UTF_8), entry.getValue()));
        }

        table.put(newPuts);
    }

    @Override
    public boolean checkAndPut(String tableName, byte[] rowId, byte[] family, byte[] qualifier, byte[] value, PutColumn column) throws IOException {
        for (Result result : results.values()) {
            if (Arrays.equals(result.getRow(), rowId)) {
                Cell[] cellArray = result.rawCells();
                for (Cell cell : cellArray) {
                    if (Arrays.equals(cell.getFamilyArray(), family) && Arrays.equals(cell.getQualifierArray(), qualifier)) {
                         if (value == null || !Arrays.equals(cell.getValueArray(), value)) {
                             return false;
                         }
                    }
                }
            }
        }

        List<PutColumn> putColumns = new ArrayList<>();
        putColumns.add(column);
        put(tableName, rowId, putColumns);

        return true;
    }

    protected ResultScanner getResults(Table table, byte[] startRow, byte[] endRow, Collection<Column> columns, List<String> labels) {
        ResultScanner scanner = Mockito.mock(ResultScanner.class);
        Mockito.when(scanner.iterator()).thenReturn(results.values().iterator());

        return scanner;
    }

    @Override
    protected ResultScanner getResults(Table table, Collection<Column> columns, Filter filter, long minTime, List<String> labels) {
        ResultScanner scanner = Mockito.mock(ResultScanner.class);
        Mockito.when(scanner.iterator()).thenReturn(results.values().iterator());

        return scanner;
    }

    @Override
    protected Connection createConnection(Configuration context) throws IOException {
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(connection.getTable(table.getName())).thenReturn(table);

        return connection;
    }

    public String getHbaseConfFolder() {
        return this.hbasePath;
    }

    public Configuration createConfiguration() {
        return new Configuration();
    }

    public static MockService configureHBaseClientService(TestRunner runner, Table table) throws InitializationException {
        MockService service = new MockService(table, "nifi1", "mockedPath", new Configuration());
        runner.addControllerService("hbaseClient", service);
        runner.enableControllerService(service);
        runner.setProperty(MockProcessor.HBASE_CLIENT_SERVICE, "hbaseClient");

        return service;
    }
}
