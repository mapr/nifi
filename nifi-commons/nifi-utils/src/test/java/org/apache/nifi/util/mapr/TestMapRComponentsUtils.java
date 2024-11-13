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

package org.apache.nifi.util.mapr;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for {@link MapRComponentsUtils}
 */
public class TestMapRComponentsUtils {
    private static final String COMPONENT_NAME = "mycomponent";
    private static final String VERSION_FILE_NAME = String.format("%sversion", COMPONENT_NAME, COMPONENT_NAME);
    private static final String VERSION = "1.2.3";
    private static final String COMPONENT_DIR_NAME = String.format("%s-%s", COMPONENT_NAME, VERSION);

    @TempDir
    private File tempDir;

    @Test
    public void testNoComponentParentDirectory() throws IOException {
        assertThrows(IOException.class, () -> {
            MapRComponentsUtils.getComponentFolder(COMPONENT_NAME, tempDir.getAbsolutePath());
        });
    }

    @Test
    public void testNoVersionFile() throws IOException {
        File componentParentDir = new File(tempDir, COMPONENT_NAME);
        assertTrue(componentParentDir.mkdir());
        File componentDir = new File(componentParentDir, COMPONENT_DIR_NAME);
        assertTrue(componentDir.mkdir());

        assertThrows(IOException.class, () -> {
            MapRComponentsUtils.getComponentFolder(COMPONENT_NAME, tempDir.getAbsolutePath());
        });
    }

    @Test
    public void testEmptyVersionFile() throws IOException {
        File componentParentDir = new File(tempDir, COMPONENT_NAME);
        assertTrue(componentParentDir.mkdir());

        File componentDir = new File(componentParentDir, COMPONENT_DIR_NAME);
        assertTrue(componentDir.mkdir());

        File versionFile = new File(componentParentDir, VERSION_FILE_NAME);
        versionFile.createNewFile();

        assertThrows(IOException.class, () -> {
            MapRComponentsUtils.getComponentFolder(COMPONENT_NAME, tempDir.getAbsolutePath());
        });
    }

    @Test
    public void testNoComponentDirectory() throws IOException {
        File componentParentDir = new File(tempDir, COMPONENT_NAME);
        assertTrue(componentParentDir.mkdir());

        File versionFile = new File(componentParentDir, VERSION_FILE_NAME);
        Files.write(versionFile.toPath(), VERSION.getBytes(), StandardOpenOption.CREATE);

        assertThrows(FileNotFoundException.class, () -> {
            MapRComponentsUtils.getComponentFolder(COMPONENT_NAME, tempDir.getAbsolutePath());
        });
    }

    @Test
    public void testUpperCaseComponentName() throws IOException {
        File componentParentDir = new File(tempDir, COMPONENT_NAME);
        assertTrue(componentParentDir.mkdir());

        File componentDir = new File(componentParentDir, COMPONENT_DIR_NAME);
        assertTrue(componentDir.mkdir());

        File versionFile = new File(componentParentDir, VERSION_FILE_NAME);
        versionFile.createNewFile();

        Files.write(versionFile.toPath(), VERSION.getBytes());

        String resultFolder = MapRComponentsUtils.getComponentFolder(COMPONENT_NAME.toUpperCase(),
                tempDir.getAbsolutePath()).toString();

        assertEquals(componentDir.getAbsolutePath(), resultFolder);
    }
}
