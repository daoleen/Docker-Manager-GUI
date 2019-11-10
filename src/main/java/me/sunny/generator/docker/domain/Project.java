package me.sunny.generator.docker.domain;


import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import me.sunny.generator.docker.exception.ResourceNotFoundException;


@Slf4j
@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
@ToString
public class Project {
    @NonNull
    private String name;

    private Set<DockerServiceDescription> availableServices = new HashSet<>();
    private Set<Composition> compositions = new HashSet<>();


    public DockerServiceDescription findService(String serviceName) throws ResourceNotFoundException {
        return availableServices.stream()
                .filter(service -> service.getService().getName().equals(serviceName))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("DockerService", serviceName));
    }


    public DockerServiceDescription findService(UUID id) throws ResourceNotFoundException {
        return availableServices.stream()
                .filter(service -> service.getService().getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("DockerService", id.toString()));
    }


    public Set<DockerService> findAllServicesByIds(Set<UUID> ids) {
        return ids.stream().map(id -> {
            try {
                return findService(id).getService();
            } catch (ResourceNotFoundException e) {
                log.warn(e.getMessage());
            }
            return null;
        }).collect(Collectors.toSet());
    }
}
