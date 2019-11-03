package me.sunny.generator.docker.domain;


import java.util.HashSet;
import java.util.Set;

import lombok.*;
import me.sunny.generator.docker.exception.ResourceNotFoundException;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Project {
    private Set<DockerServiceDescription> availableServices = new HashSet<>();


    public DockerServiceDescription findService(String serviceName) throws ResourceNotFoundException {
        return availableServices.stream()
                .filter(service -> service.getService().getName().equals(serviceName))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("DockerService", serviceName));
    }
}
