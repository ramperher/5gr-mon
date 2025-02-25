/*
* Copyright 2018 Nextworks s.r.l.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package it.nextworks.nfvmano.configmanager.exporters.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ExporterDescription {
    @JsonProperty("name")
    private String name = null;

    @JsonProperty("endpoint")
    private List<Endpoint> endpoint = null;

    @JsonProperty("collectionPeriod")
    private Integer collectionPeriod = null;

    @JsonProperty("nsId")
    private String nsId;

    @JsonProperty("vnfdId")
    private String vnfdId;

    @JsonProperty("metrics_path")
    private String metricsPath = "/metrics";

    @JsonProperty("instance")
    private String instance;


    public ExporterDescription name(String name) {
        this.name = name;
        return this;
    }

    public String getNsId() {
        return nsId;
    }

    public void setNsId(String nsId) {
        this.nsId = nsId;
    }

    public String getVnfdId() {
        return vnfdId;
    }

    public void setVnfdId(String vnfdId) {
        this.vnfdId = vnfdId;
    }

    /**
     * Human-readable description of the exporter, e.g. NSI-XXX_web-server_VM-XXX
     *
     * @return name
     **/
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ExporterDescription endpoint(List<Endpoint> endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public ExporterDescription addEndpointItem(Endpoint endpointItem) {
        if (this.endpoint == null) {
            this.endpoint = new ArrayList<>();
        }
        this.endpoint.add(endpointItem);
        return this;
    }

    /**
     * The list of endpoints of the instances of this job
     *
     * @return endpoint
     **/
    public List<Endpoint> getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(List<Endpoint> endpoint) {
        this.endpoint = endpoint;
    }

    public ExporterDescription collectionPeriod(Integer collectionPeriod) {
        this.collectionPeriod = collectionPeriod;
        return this;
    }

    /**
     * the interval (in milliseconds) between scrapes
     *
     * @return collectionPeriod
     **/
    public Integer getCollectionPeriod() {
        return collectionPeriod;
    }

    public void setCollectionPeriod(Integer collectionPeriod) {
        this.collectionPeriod = collectionPeriod;
    }

    public String getMetricsPath() {
        return metricsPath;
    }

    public void setMetricsPath(String metricsPath) {
        this.metricsPath = metricsPath;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExporterDescription that = (ExporterDescription) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(endpoint, that.endpoint) &&
                Objects.equals(collectionPeriod, that.collectionPeriod) &&
                Objects.equals(nsId, that.nsId) &&
                Objects.equals(vnfdId, that.vnfdId) &&
                Objects.equals(metricsPath, that.metricsPath) &&
                Objects.equals(instance, that.instance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, endpoint, collectionPeriod, nsId, vnfdId, metricsPath, instance);
    }

    @Override
    public String toString() {
        return "ExporterDescription{" +
                "name='" + name + '\'' +
                ", endpoint=" + endpoint +
                ", collectionPeriod=" + collectionPeriod +
                ", nsId='" + nsId + '\'' +
                ", vnfdId='" + vnfdId + '\'' +
                ", metricsPath='" + metricsPath + '\'' +
                ", instance='" + instance + '\'' +
                '}';
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }

}

