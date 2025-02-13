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
package org.apache.nifi.web.api.entity;

import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Set;

@XmlRootElement(name = "versionedReportingTaskImportResponseEntity")
public class VersionedReportingTaskImportResponseEntity extends Entity {

    private Set<ReportingTaskEntity> reportingTasks;
    private Set<ControllerServiceEntity> controllerServices;

    @ApiModelProperty("The reporting tasks created by the import")
    public Set<ReportingTaskEntity> getReportingTasks() {
        return reportingTasks;
    }

    public void setReportingTasks(Set<ReportingTaskEntity> reportingTasks) {
        this.reportingTasks = reportingTasks;
    }

    @ApiModelProperty("The controller services created by the import")
    public Set<ControllerServiceEntity> getControllerServices() {
        return controllerServices;
    }

    public void setControllerServices(Set<ControllerServiceEntity> controllerServices) {
        this.controllerServices = controllerServices;
    }
}
