package me.sunny.generator.docker.domain;


import java.util.UUID;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import me.sunny.generator.docker.Context;
import me.sunny.generator.docker.exception.ResourceNotFoundException;


@Slf4j
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = { "serviceId", "version" })
public class DockerServiceConcreted {
    private UUID serviceId;
    private String version;


    @Override
    public String toString() {
        String name = null;
        try {
            name = Context.project.findService(serviceId).getService().getName();
        } catch (ResourceNotFoundException e) {
            log.error(e.getMessage());
        }
        return String.format("%s [%s]", name, version);
    }
}
