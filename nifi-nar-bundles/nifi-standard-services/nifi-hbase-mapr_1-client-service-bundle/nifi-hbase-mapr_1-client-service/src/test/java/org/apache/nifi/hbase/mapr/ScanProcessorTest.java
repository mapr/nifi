package org.apache.nifi.hbase.mapr;

import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Table;
import org.apache.nifi.hbase.HBaseClientService;
import org.apache.nifi.hbase.scan.ResultCell;
import org.apache.nifi.hbase.scan.ResultHandler;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.apache.nifi.hbase.mapr.MockService.configureHBaseClientService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * This is Client service scan method test.
 * Scan method fetches all columns from Hbase table .
 * Original call comes from ScanHbaseProcessor
 *
 * @see EEP_HbaseMaprDbClientService -> method scan(context)
 */
public class ScanProcessorTest {

    private String tableName = "nifi";

    private String COL_FAM = "nifi1";

    @Test
    public void testScan() throws InitializationException, IOException {
        TestRunner runner = TestRunners.newTestRunner(MockProcessor.class);

        // Mock an HBase Table so we can verify the put operations later
        Table table = Mockito.mock(Table.class);
        when(table.getName()).thenReturn(TableName.valueOf(tableName));

        // create the controller service and link it to the test processor
        MockService service = configureHBaseClientService(runner, table);
        runner.assertValid(service);

        // stage some results in the mock service...
        long now = System.currentTimeMillis();

        Map<String, String> cells = new HashMap<>();
        cells.put("greeting", "hello");
        cells.put("name", "nifi");

        service.addResult("row0", cells, now - 2);
        service.addResult("row1", cells, now - 1);
        service.addResult("row2", cells, now - 1);
        service.addResult("row3", cells, now);

        // perform a scan and verify the four rows were returned
        ScanProcessorTest.CollectingResultHandler handler = new ScanProcessorTest.CollectingResultHandler();
        HBaseClientService hBaseClientService = runner.getProcessContext()
                .getProperty(MockProcessor.HBASE_CLIENT_SERVICE)
                .asControllerService(HBaseClientService.class);

        hBaseClientService.scan(tableName, new ArrayList<>(), null, now, handler);
        assertEquals(4, handler.results.size());

        // get row0 using the row id and verify it has 2 cells
        ResultCell[] results = handler.results.get("row0");
        assertNotNull(results);
        assertEquals(2, results.length);

        verifyResultCell(results[0], "greeting", "hello");
        verifyResultCell(results[1], "name", "nifi");
    }

    @Test
    public void testScanWithValidFilter() throws InitializationException, IOException {
        TestRunner runner = TestRunners.newTestRunner(MockProcessor.class);

        // Mock an HBase Table so we can verify the put operations later
        Table table = Mockito.mock(Table.class);
        when(table.getName()).thenReturn(TableName.valueOf(tableName));

        // create the controller service and link it to the test processor
        MockService service = configureHBaseClientService(runner, table);
        runner.assertValid(service);

        // perform a scan and verify the four rows were returned
        ScanProcessorTest.CollectingResultHandler handler = new ScanProcessorTest.CollectingResultHandler();
        HBaseClientService hBaseClientService = runner.getProcessContext()
                .getProperty(MockProcessor.HBASE_CLIENT_SERVICE)
                .asControllerService(HBaseClientService.class);

        // make sure we parse the filter expression without throwing an exception
        String filter = "PrefixFilter ('Row') AND PageFilter (1) AND FirstKeyOnlyFilter ()";
        hBaseClientService.scan(
                tableName,
                new ArrayList<>(),
                filter,
                System.currentTimeMillis(),
                handler);
    }

    @Test
    public void testScanWithInvalidFilter() throws InitializationException {
        TestRunner runner = TestRunners.newTestRunner(MockProcessor.class);

        // Mock an HBase Table so we can verify the put operations later
        Table table = Mockito.mock(Table.class);
        when(table.getName()).thenReturn(TableName.valueOf(tableName));

        // create the controller service and link it to the test processor
        MockService service = configureHBaseClientService(runner, table);
        runner.assertValid(service);

        // perform a scan and verify the four rows were returned
        ScanProcessorTest.CollectingResultHandler handler = new ScanProcessorTest.CollectingResultHandler();
        HBaseClientService hBaseClientService = runner.getProcessContext()
                .getProperty(MockProcessor.HBASE_CLIENT_SERVICE)
                .asControllerService(HBaseClientService.class);

        // this should throw IllegalArgumentException
        String filter = "this is not a filter";
        assertThrows(IllegalArgumentException.class,
                () -> hBaseClientService.scan(
                        tableName,
                        new ArrayList<>(),
                        filter,
                        System.currentTimeMillis(),
                        handler)
        );
    }

    private void verifyResultCell(ResultCell result, String cq, String val) {
        String colFamily = new String(
                result.getFamilyArray(),
                result.getFamilyOffset(),
                result.getFamilyLength());
        assertEquals(COL_FAM, colFamily);

        String colQualifier = new String(
                result.getQualifierArray(),
                result.getQualifierOffset(),
                result.getQualifierLength());
        assertEquals(cq, colQualifier);

        String value = new String(
                result.getValueArray(),
                result.getValueOffset(),
                result.getValueLength());
        assertEquals(val, value);
    }

    // handler that saves results for verification
    private static final class CollectingResultHandler implements ResultHandler {

        Map<String, ResultCell[]> results = new LinkedHashMap<>();

        @Override
        public void handle(byte[] row, ResultCell[] resultCells) {
            String rowStr = new String(row, StandardCharsets.UTF_8);
            results.put(rowStr, resultCells);
        }
    }
}
