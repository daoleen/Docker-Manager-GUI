package me.sunny.generator.docker.domain;


import java.util.*;

import lombok.*;
import me.sunny.generator.docker.enums.DockerRestartOption;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = { "name" })
public class DockerService {
    private UUID id;
    private String name;
    private String image;
    private String buildPath;
    private DockerRestartOption restart;
    private Set<DockerPortMapping> ports;
    private Set<DockerVolumeMapping> volumes;
    private Map<String, String> environment;
    private Set<DockerDepend> depends;
    private Set<UUID> links;
    private DockerHealthchek healthcheck;


    @Override
    public String toString() {
        return String.format("%s [%s]", name, StringUtils.isNoneBlank(image) ? image : buildPath);
    }


    public DockerService copy() {
        Set<DockerPortMapping> ports = null;
        Set<DockerVolumeMapping> volumes = null;
        Map<String, String> environment = null;
        Set<DockerDepend> depends = null;
        Set<UUID> links = null;
        DockerHealthchek healthchek = null;

        if (CollectionUtils.isNotEmpty(this.ports)) {
            ports = new HashSet<>(this.ports.size());
            for (DockerPortMapping port : this.ports) {
                ports.add(port.copy());
            }
        }

        if (CollectionUtils.isNotEmpty(this.volumes)) {
            volumes = new HashSet<>(this.volumes.size());
            for (DockerVolumeMapping volume : this.volumes) {
                volumes.add(volume.copy());
            }
        }

        if (MapUtils.isNotEmpty(this.environment)) {
            environment = new HashMap<>(this.environment);
        }

        if (CollectionUtils.isNotEmpty(this.depends)) {
            depends = new HashSet<>(this.depends.size());
            for (DockerDepend depend : this.depends) {
                depends.add(depend.copy());
            }
        }

        if (CollectionUtils.isNotEmpty(this.links)) {
            links = new HashSet<>(this.links);
        }

        if (this.healthcheck != null) {
            healthchek = this.healthcheck.copy();
        }

        return new DockerService(this.id, this.name, this.image, this.buildPath, this.restart, ports, volumes,
                environment, depends, links, healthchek);
    }
}
