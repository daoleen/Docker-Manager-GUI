package me.sunny.generator.docker.service;


import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import me.sunny.generator.docker.Context;
import me.sunny.generator.docker.domain.*;
import me.sunny.generator.docker.enums.DockerContainerStatus;
import me.sunny.generator.docker.enums.DockerDependCondition;
import me.sunny.generator.docker.exception.ApplicationException;
import me.sunny.generator.docker.exception.ContainerStartException;
import me.sunny.generator.docker.exception.ResourceNotFoundException;
import org.apache.commons.collections4.CollectionUtils;


@Slf4j
public class DockerContainerRunner {
    private final DockerContainerService dockerContainerService;


    public DockerContainerRunner(DockerContainerService dockerContainerService) {
        this.dockerContainerService = dockerContainerService;
    }


    public void startService(DockerService dockerService, String version, DockerDepend dependOfMe)
            throws ApplicationException, ContainerStartException, ResourceNotFoundException
    {
        // starting all depends first
        if (CollectionUtils.isNotEmpty(dockerService.getDepends())) {
            for (DockerDepend dockerDepend : dockerService.getDepends()) {
                DockerServiceDescription serviceDescription = null;
                try {
                    serviceDescription = Context.project.findService(dockerDepend.getServiceId());
                } catch (ResourceNotFoundException e) {
                    throw new ApplicationException(String.format("Could not find service %s", dockerDepend.getServiceId().toString()));
                }

                if (CollectionUtils.isEmpty(serviceDescription.getVersions())) {
                    throw new ApplicationException(String.format("Could not start service %s because it has no any version", serviceDescription.getService().getName()));
                }

                startService(serviceDescription.getService(), serviceDescription.getVersions().get(serviceDescription.getVersions().size() - 1), dockerDepend);
            }
        }

        String containerId;
        Optional<DockerContainer> container = dockerContainerService.getByName(dockerService.getName());

        if (container.isPresent()) {
            containerId = container.get().getId();
        } else {
            containerId = dockerContainerService.create(dockerService, version);
        }

        dockerContainerService.start(containerId);
        checkDependHealthCheck(containerId, dockerService, dependOfMe);
    }


    private void checkDependHealthCheck(String containerId, DockerService dockerService, DockerDepend dependOfMe)
            throws ContainerStartException, ApplicationException, ResourceNotFoundException
    {
        if (dependOfMe == null) {
            if (dockerService.getHealthcheck() == null) {
                checkHealthJustStarted(containerId);
            } else {
                checkServiceHealthy(containerId, dockerService);
            }
        } else {
            if (DockerDependCondition.SERVICE_STARTED.equals(dependOfMe.getCondition())) {
                checkHealthJustStarted(containerId);
            } else if (DockerDependCondition.SERVICE_HEALTHY.equals(dependOfMe.getCondition())) {
                checkServiceHealthy(containerId, dockerService);
            }
        }
    }


    /**
     * Waiting while container became to started status
     * If it won't in one minute the exception will be raised
     * @param containerId container to check
     * @throws ContainerStartException when start time limits exceeded
     */
    private void checkHealthJustStarted(String containerId) throws ContainerStartException, ResourceNotFoundException {
        for (int i = 0; i < 60; i++) {
            if (dockerContainerService.isStarted(containerId)) {
                return;
            }

            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                log.warn(e.getMessage());
            }
        }

        log.error("Error waiting for starting container {}", containerId);
        DockerContainerStatus status = dockerContainerService.getStatus(containerId);
        throw new ContainerStartException(containerId, status.name());
    }


    /**
     * Waiting while container became to healthy status
     * If it won't in healthcheck-specified time the exception will be raised
     * @param containerId container to check
     * @param dockerService service to check
     * @throws ContainerStartException when start time limits exceeded
     * @throws ApplicationException if healthcheck for the service is not specified
     * @throws ResourceNotFoundException if container was not found
     */
    private void checkServiceHealthy(String containerId, DockerService dockerService)
            throws ApplicationException, ContainerStartException, ResourceNotFoundException
    {
        DockerHealthchek healthcheck = dockerService.getHealthcheck();
        DockerContainerStatus status;

        if (healthcheck == null) {
            throw new ApplicationException("Healthcheck is not specified for service " + dockerService.getName());
        }

        for (int i = 0; i < healthcheck.getRetriesCount() + 1; i++) {
            status = dockerContainerService.getStatus(containerId);

            if (DockerContainerStatus.HEALTHY.equals(status)) {
                log.info("Container {} for service {} is healthy", containerId, dockerService.getName());
                return;
            }

            try {
                Thread.sleep((healthcheck.getIntervalSeconds() + healthcheck.getTimeoutSeconds()) * 1000L);
            } catch (InterruptedException e) {
                log.warn(e.getMessage());
            }
        }

        log.error("Error waiting for healthy container {} for service {}", containerId, dockerService.getName());
        status = dockerContainerService.getStatus(containerId);
        throw new ContainerStartException(containerId, status.name());
    }
}
