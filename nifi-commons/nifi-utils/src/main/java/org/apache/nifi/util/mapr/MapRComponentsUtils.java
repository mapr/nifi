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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * Class for providing path to components
 */
public final class MapRComponentsUtils {

    /**
     * Default location of MapR Home
     */
    public final static String DEFAULT_MAPR_HOME = "/opt/mapr";

    /**
     * Returns component folder in default MapR Home
     *
     * @param componentName name of MapR ECO component
     * @return {@link Path} to component folder
     * @throws IOException if fails to find path
     */
    public static Path getComponentFolder(final String componentName) throws IOException {
        return getComponentFolder(componentName, DEFAULT_MAPR_HOME);
    }

    /**
     * Returns component folder in custom MapR Home
     *
     * @param componentName name of MapR ECO component
     * @param homeFolder home folder for MapR
     * @return {@link Path} to component folder
     * @throws IOException if fails to find path
     */
    public static Path getComponentFolder(final String componentName, final String homeFolder) throws IOException {
        String component = componentName.toLowerCase();
        String version;
        try {
           version = getComponentVersion(component, homeFolder);
        } catch (IOException e) {
            throw new IOException(String.format("Could not get version file for %s: %s", componentName, e.getMessage()), e);
        }

        Path path = Paths.get(homeFolder, component, String.format("%s-%s", component, version));
        if (!Files.exists(path)) {
            throw new FileNotFoundException(String.format("%s folder does not exists in: %s", componentName, path));
        }

        return path;
    }

    private static String getComponentVersion(final String component, final String homeFolder) throws IOException {
        Path versionPath = Paths.get(homeFolder, component, component + "version");
        try {
            return Files.readAllLines(versionPath).get(0);
        } catch (IndexOutOfBoundsException e) {
            throw new IOException("Version file is empty " + versionPath, e);
        }

    }
}
