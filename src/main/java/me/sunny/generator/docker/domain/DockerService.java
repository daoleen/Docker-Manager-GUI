package me.sunny.generator.docker.domain;


import java.util.Map;
import java.util.Set;
import java.util.UUID;

import lombok.*;
import me.sunny.generator.docker.enums.DockerRestartOption;
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
}
