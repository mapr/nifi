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
package org.apache.nifi.bootstrap.util;

import org.apache.nifi.util.mapr.MapRComponentsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class MapRLibsUtil {
    private final static Logger LOGGER = LoggerFactory.getLogger(MapRLibsUtil.class);

    public final static String DEFAULT_MAPR_LIBS = MapRComponentsUtils.DEFAULT_MAPR_HOME + "/lib";
    public final static String ALL_DEPENDENCIES = "*";

    private final static List<String> MAPR_LIBS_PREFIXES = Arrays.asList(
            "hadoop-common",
            "hadoop-yarn-api",
            "hadoop-auth",
            "zookeeper"
    );

    private final static List<String> JARS_EXCEPTIONS = Arrays.asList(
            "jetty",
            "jersey",
            "jsr311-api",
            "jsp-api",
            "aws-java-sdk-bundle"
    );

    public static List<File> getMapRLibs(String libPath) throws IOException {
        Path maprLibs = Paths.get(libPath);
        if (!Files.exists(maprLibs)) {
            throw new FileNotFoundException("MapR lib folder does not exist at " + libPath);
        }

        List<File> jarLibs = new ArrayList<>(getJarsFromFolder(MAPR_LIBS_PREFIXES, maprLibs));

        Path maprHome = maprLibs.getParent();
        jarLibs.addAll(getSpecificHadoopJars(maprHome));
        jarLibs.addAll(getSpecificHBaseJars(maprHome));

        return jarLibs;
    }

    private static List<File> getSpecificHadoopJars(Path homePath) throws IOException {
        String component = "hadoop";
        Path hadoopFolder = MapRComponentsUtils.getComponentFolder(component, homePath.toString());

        Path hdfsFolder = Paths.get(hadoopFolder.toString(), "share/hadoop/hdfs");
        List<File> hadoopLibs = new ArrayList<>(getJarsFromFolder("hadoop-hdfs", hdfsFolder));

        Path mapreduceFolder = Paths.get(hadoopFolder.toString(), "share/hadoop/mapreduce");
        hadoopLibs.addAll(getJarsFromFolder("hadoop-mapreduce-client-core", mapreduceFolder));

        Path hadoopCommonLibsFolder = Paths.get(hadoopFolder.toString(), "share/hadoop/common/lib/");

        hadoopLibs.addAll(getJarsFromFolder(ALL_DEPENDENCIES, hadoopCommonLibsFolder));

        return hadoopLibs;
    }

    private static List<File> getSpecificHBaseJars(Path homePath) throws IOException {
        String component = "hbase";
        Path hbaseFolder = MapRComponentsUtils.getComponentFolder(component, homePath.toString());

        Path hbaseLibFolder = Paths.get(hbaseFolder.toString(), "/lib");
        List<String> hbaseLibsList = List.of("hbase-", "netty-", "metrics-");

        return getJarsFromFolder(hbaseLibsList, hbaseLibFolder);
    }


    private static List<File> getJarsFromFolder(String prefix, Path path) {
        return getJarsFromFolder(Collections.singletonList(prefix), path);
    }

    private static List<File> getJarsFromFolder(List<String> prefixes, Path path) {
        try {
            return Files.list(path)
                    .filter(file -> jarFilter(prefixes, file))
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error("Failed to access {} error: {}", path, e);
            return Collections.emptyList();
        }
    }

    private static boolean jarFilter(List<String> prefixes, Path file) {
        String fileName = file.getFileName().toString().toLowerCase();

        boolean containsJarsExceptions = JARS_EXCEPTIONS.stream().anyMatch(fileName::startsWith);
        if (containsJarsExceptions) {
            return false;
        }

        boolean isSuffixCorrect = fileName.endsWith(".jar") && !fileName.endsWith("-tests.jar");

        if (!prefixes.contains(ALL_DEPENDENCIES)) {
            return prefixes.stream()
                    .anyMatch(fileName::startsWith)
                    && isSuffixCorrect;
        }

        return isSuffixCorrect;
    }
}
