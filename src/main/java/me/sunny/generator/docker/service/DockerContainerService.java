package me.sunny.generator.docker.service;


import java.util.List;
import java.util.Optional;

import me.sunny.generator.docker.domain.DockerContainer;
import me.sunny.generator.docker.domain.DockerService;
import me.sunny.generator.docker.enums.DockerContainerStatus;
import me.sunny.generator.docker.exception.ApplicationException;
import me.sunny.generator.docker.exception.ResourceNotFoundException;


public interface DockerContainerService {
    String create(DockerService dockerService, String version) throws ApplicationException;
    void start(String id) throws ApplicationException;
    void stop(String id);
    void remove(String id);
    void removeByName(String name);
    List<DockerContainer> getContainers(boolean showHidden);
    Optional<DockerContainer> get(String id);
    Optional<DockerContainer> getByName(String name);
    boolean isStarted(String id) throws ResourceNotFoundException;
    DockerContainerStatus getStatus(String id) throws ResourceNotFoundException;
}
