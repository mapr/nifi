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

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public class TestMapRLibsUtil {
    @Test (expected = FileNotFoundException.class)
    public void testWrongLibDirectory() throws IOException {
        MapRLibsUtil.getMapRLibs("/abc");
    }

    @Test(expected = IOException.class)
    public void testWrongRootDirectory() throws IOException {
        MapRLibsUtil.getMapRLibs("/tmp");
    }

    @Test
    public void testGetMaprLibs() throws IOException {
        List<File> libs = MapRLibsUtil.getMapRLibs(MapRLibsUtil.DEFAULT_MAPR_LIBS);
        Assert.assertFalse("Libs list should be not empty", libs.isEmpty());
    }
}
