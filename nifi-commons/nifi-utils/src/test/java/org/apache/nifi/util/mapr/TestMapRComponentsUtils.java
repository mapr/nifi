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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Test class for {@link MapRComponentsUtils}
 */
public class TestMapRComponentsUtils {
    private static final String MY_COMPONENT = "mycomponent";
    private static final String VERSION_PATH = String.format("%s/%sversion", MY_COMPONENT, MY_COMPONENT);

    private static final String VERSION = "1.2.3";
    private static final String COMPONENT_PATH_WITH_VERSION = String.format("%s/%s-%s", MY_COMPONENT, MY_COMPONENT,
            VERSION);

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test(expected = IOException.class)
    public void testNoComponentFolder() throws IOException {
        File homeFolder = tempFolder.newFolder();
        MapRComponentsUtils.getComponentFolder(MY_COMPONENT, homeFolder.getAbsolutePath());
    }

    @Test(expected = IOException.class)
    public void testNoVersionFile() throws IOException {
        File componentFolder = tempFolder.newFolder(MY_COMPONENT);

        File homeFolder = componentFolder.getParentFile();
        MapRComponentsUtils.getComponentFolder(MY_COMPONENT, homeFolder.getAbsolutePath());
    }

    @Test(expected = IOException.class)
    public void testEmptyVersionFile() throws IOException {
        tempFolder.newFolder(MY_COMPONENT);

        File versionFile = tempFolder.newFile(VERSION_PATH);

        File homeFolder = versionFile.getParentFile().getParentFile();
        MapRComponentsUtils.getComponentFolder(MY_COMPONENT, homeFolder.getAbsolutePath());
    }

    @Test(expected = FileNotFoundException.class)
    public void testNoComponentVersionFolder() throws IOException {
        String component = MY_COMPONENT.toLowerCase();
        tempFolder.newFolder(component);

        File versionFile = tempFolder.newFile(VERSION_PATH);
        Files.write(versionFile.toPath(), VERSION.getBytes());

        File homeFolder = versionFile.getParentFile().getParentFile();
        MapRComponentsUtils.getComponentFolder(MY_COMPONENT, homeFolder.getAbsolutePath());
    }

    @Test
    public void testUpperCaseComponentName() throws IOException {
        String component = MY_COMPONENT.toLowerCase();
        tempFolder.newFolder(component);
        String expectedFolder = tempFolder.newFolder(COMPONENT_PATH_WITH_VERSION).getAbsolutePath();

        File versionFile = tempFolder.newFile(VERSION_PATH);
        Files.write(versionFile.toPath(), VERSION.getBytes());

        File homeFolder = versionFile.getParentFile().getParentFile();
        String resultFolder = MapRComponentsUtils.getComponentFolder(MY_COMPONENT.toUpperCase(),
                homeFolder.getAbsolutePath()).toString();

        Assert.assertEquals(expectedFolder, resultFolder);
    }
}
