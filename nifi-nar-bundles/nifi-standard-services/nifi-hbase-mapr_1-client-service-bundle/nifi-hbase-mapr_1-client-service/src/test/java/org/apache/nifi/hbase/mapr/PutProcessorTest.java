package org.apache.nifi.hbase.mapr;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.nifi.hbase.HBaseClientService;
import org.apache.nifi.hbase.put.PutColumn;
import org.apache.nifi.hbase.put.PutFlowFile;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import static org.apache.nifi.hbase.mapr.MockService.configureHBaseClientService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test logic of nifi hbase Put processors range
 */
public class PutProcessorTest {
    private final String tableName = "nifi";
    private final String row = "row1";
    private final String columnFamily = "family1";
    private final String columnQualifier = "qualifier1";
    private final String content1 = "content1";
    private final String content2 = "content2";

    @Test
    public void testSinglePut() throws InitializationException, IOException {
        String content = "content";
        Collection<PutColumn> columns = Collections.singletonList(
                new PutColumn(
                        columnFamily.getBytes(StandardCharsets.UTF_8),
                        columnQualifier.getBytes(StandardCharsets.UTF_8),
                        content.getBytes(StandardCharsets.UTF_8)));
        PutFlowFile putFlowFile = new PutFlowFile(
                tableName,
                row.getBytes(StandardCharsets.UTF_8),
                columns, null);

        TestRunner runner = TestRunners.newTestRunner(MockProcessor.class);

        // Mock an HBase Table so we can verify the put operations later
        Table table = Mockito.mock(Table.class);
        when(table.getName()).thenReturn(TableName.valueOf(tableName));

        // create the controller service and link it to the test processor
        HBaseClientService service = configureHBaseClientService(runner, table);
        runner.assertValid(service);

        // try to put a single cell
        HBaseClientService hBaseClientService = runner.getProcessContext()
                .getProperty(MockProcessor.HBASE_CLIENT_SERVICE)
                .asControllerService(HBaseClientService.class);

        hBaseClientService.put(tableName, Arrays.asList(putFlowFile));

        // verify only one call to put was made
        ArgumentCaptor<List> capture = ArgumentCaptor.forClass(List.class);
        verify(table, times(1)).put(capture.capture());

        // verify only one put was in the list of puts
        List<Put> puts = capture.getValue();
        assertEquals(1, puts.size());
        verifyPut(puts.get(0));
    }

    @Test
    public void testMultiplePutsSameRow() throws IOException, InitializationException {
        Collection<PutColumn> columns1 = Collections.singletonList(
                new PutColumn(
                        columnFamily.getBytes(StandardCharsets.UTF_8),
                        columnQualifier.getBytes(StandardCharsets.UTF_8),
                        content1.getBytes(StandardCharsets.UTF_8)));
        PutFlowFile putFlowFile1 = new PutFlowFile(
                tableName,
                row.getBytes(StandardCharsets.UTF_8),
                columns1,
                null);

        Collection<PutColumn> columns2 = Collections.singletonList(
                new PutColumn(
                        columnFamily.getBytes(StandardCharsets.UTF_8),
                        columnQualifier.getBytes(StandardCharsets.UTF_8),
                        content2.getBytes(StandardCharsets.UTF_8)));
        PutFlowFile putFlowFile2 = new PutFlowFile(
                tableName,
                row.getBytes(StandardCharsets.UTF_8),
                columns2,
                null);

        TestRunner runner = TestRunners.newTestRunner(MockProcessor.class);

        // Mock an HBase Table so we can verify the put operations later
        Table table = Mockito.mock(Table.class);
        when(table.getName()).thenReturn(TableName.valueOf(tableName));

        // create the controller service and link it to the test processor
        HBaseClientService service = configureHBaseClientService(runner, table);
        runner.assertValid(service);

        // try to put a multiple cells for the same row
        HBaseClientService hBaseClientService = runner.getProcessContext()
                .getProperty(MockProcessor.HBASE_CLIENT_SERVICE)
                .asControllerService(HBaseClientService.class);

        hBaseClientService.put(tableName, Arrays.asList(putFlowFile1, putFlowFile2));

        // verify put was only called once
        ArgumentCaptor<List> capture = ArgumentCaptor.forClass(List.class);
        verify(table, times(1)).put(capture.capture());

        // verify there was only one put in the list of puts
        List<Put> puts = capture.getValue();
        assertEquals(1, puts.size());

        // verify two cells were added to this one put operation
        NavigableMap<byte[], List<Cell>> familyCells = puts.get(0).getFamilyCellMap();
        Map.Entry<byte[], List<Cell>> entry = familyCells.firstEntry();
        assertEquals(2, entry.getValue().size());
    }

    @Test
    public void testMultiplePutsDifferentRow() throws IOException, InitializationException {

        Collection<PutColumn> columns1 = Collections.singletonList(
                new PutColumn(
                        columnFamily.getBytes(StandardCharsets.UTF_8),
                        columnQualifier.getBytes(StandardCharsets.UTF_8),
                        content1.getBytes(StandardCharsets.UTF_8)));
        String row1 = "row1";
        PutFlowFile putFlowFile1 = new PutFlowFile(
                tableName,
                row1.getBytes(StandardCharsets.UTF_8),
                columns1,
                null);

        Collection<PutColumn> columns2 = Collections.singletonList(
                new PutColumn(
                        columnFamily.getBytes(StandardCharsets.UTF_8),
                        columnQualifier.getBytes(StandardCharsets.UTF_8),
                        content2.getBytes(StandardCharsets.UTF_8)));
        String row2 = "row2";
        PutFlowFile putFlowFile2 = new PutFlowFile(
                tableName,
                row2.getBytes(StandardCharsets.UTF_8),
                columns2,
                null);

        TestRunner runner = TestRunners.newTestRunner(MockProcessor.class);

        // Mock an HBase Table so we can verify the put operations later
        Table table = Mockito.mock(Table.class);
        when(table.getName()).thenReturn(TableName.valueOf(tableName));

        // create the controller service and link it to the test processor
        HBaseClientService service = configureHBaseClientService(runner, table);
        runner.assertValid(service);

        // try to put a multiple cells with different rows
        HBaseClientService hBaseClientService = runner.getProcessContext().
                getProperty(MockProcessor.HBASE_CLIENT_SERVICE).
                asControllerService(HBaseClientService.class);

        hBaseClientService.put(tableName, Arrays.asList(putFlowFile1, putFlowFile2));

        // verify put was only called once
        ArgumentCaptor<List> capture = ArgumentCaptor.forClass(List.class);
        verify(table, times(1)).put(capture.capture());

        // verify there were two puts in the list
        List<Put> puts = capture.getValue();
        assertEquals(2, puts.size());
    }

    private void verifyPut(Put put) {
        assertEquals("row1", new String(put.getRow()));

        NavigableMap<byte[], List<Cell>> familyCells = put.getFamilyCellMap();
        assertEquals(1, familyCells.size());

        Map.Entry<byte[], List<Cell>> entry = familyCells.firstEntry();
        assertEquals("family1", new String(entry.getKey()));
        assertEquals(1, entry.getValue().size());

        Cell cell = entry.getValue().get(0);
        assertEquals("qualifier1", new String(
                cell.getQualifierArray(),
                cell.getQualifierOffset(),
                cell.getQualifierLength()));
        assertEquals("content", new String(
                cell.getValueArray(),
                cell.getValueOffset(),
                cell.getValueLength()));
    }
}
