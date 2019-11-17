package me.sunny.generator.docker.service.impl;


import java.util.*;
import java.util.stream.Collectors;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.*;
import lombok.extern.slf4j.Slf4j;
import me.sunny.generator.docker.Context;
import me.sunny.generator.docker.domain.DockerContainer;
import me.sunny.generator.docker.domain.DockerService;
import me.sunny.generator.docker.enums.DockerContainerStatus;
import me.sunny.generator.docker.enums.DockerRestartOption;
import me.sunny.generator.docker.exception.ApplicationException;
import me.sunny.generator.docker.exception.ResourceNotFoundException;
import me.sunny.generator.docker.service.DockerContainerService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;


@Slf4j
public class DockerContainerServiceImpl implements DockerContainerService {
    private final DockerClient dockerClient;


    public DockerContainerServiceImpl(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }


    @Override
    public String create(DockerService dockerService, String version) throws ApplicationException {
        HostConfig hostConfig = HostConfig.newHostConfig()
                .withRestartPolicy(getRestartPolicy(dockerService.getRestart(), (dockerService.getHealthcheck() != null) ? dockerService.getHealthcheck().getRetriesCount() : Integer.MAX_VALUE));

        if (DockerRestartOption.NO.equals(dockerService.getRestart())) {
            hostConfig.withAutoRemove(true);
        }

        if (CollectionUtils.isNotEmpty(dockerService.getPorts())) {
            Ports ports = new Ports();
            dockerService.getPorts().forEach(portsMapping ->
                    ports.bind(ExposedPort.tcp(portsMapping.getContainerPort()), Ports.Binding.bindPort(portsMapping.getHostPort())));

            hostConfig.withPortBindings(ports);
        }

        if (CollectionUtils.isNotEmpty(dockerService.getVolumes())) {
            List<Bind> volumeBindings = dockerService.getVolumes().stream()
                    .map(dockerVolumeMapping -> new Bind(dockerVolumeMapping.getHostVolumePath(),
                            new Volume(dockerVolumeMapping.getContainerVolumePath())))
                    .collect(Collectors.toList());

            hostConfig.withBinds(volumeBindings);
        }

        if (CollectionUtils.isNotEmpty(dockerService.getLinks())) {
            Set<DockerService> linkedServices = Context.project.findAllServicesByIds(dockerService.getLinks());
            List<Link> links = linkedServices.stream()
                    .map(linked -> new Link(linked.getName(), String.format("%s_%s_link", dockerService.getName(), linked.getName())))
                    .collect(Collectors.toList());

            hostConfig.withLinks(links);
        }

        CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(String.format("%s:%s", dockerService.getImage(), version))
                .withName(dockerService.getName())
                .withHostName(dockerService.getName())
                .withHostConfig(hostConfig);

        if (MapUtils.isNotEmpty(dockerService.getEnvironment())) {
            List<String> environments = dockerService.getEnvironment().entrySet().stream()
                    .map(env -> String.format("%s=%s", env.getKey(), env.getValue()))
                    .collect(Collectors.toList());

            createContainerCmd.withEnv(environments);
        }

        if (dockerService.getHealthcheck() != null) {
            HealthCheck healthCheck = new HealthCheck();
            healthCheck.withTest(Arrays.asList(dockerService.getHealthcheck().getTest().trim().split(" ")));
            healthCheck.withInterval(dockerService.getHealthcheck().getIntervalSeconds() * 1_000_000_000L);
            healthCheck.withTimeout(dockerService.getHealthcheck().getTimeoutSeconds() * 1_000_000_000L);
            healthCheck.withRetries(dockerService.getHealthcheck().getRetriesCount());

            createContainerCmd.withHealthcheck(healthCheck);
        }


        CreateContainerResponse containerResponse;
        try {
            removeByName(createContainerCmd.getName());
            containerResponse = createContainerCmd.exec();
            log.info("Container {} has been created", containerResponse.getId());
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            throw new ApplicationException(ex.getMessage());
        }

        if (ArrayUtils.isNotEmpty(containerResponse.getWarnings())) {
            for (String warning : containerResponse.getWarnings()) {
                log.warn("Container warning: {}", warning);
            }
        }

        return containerResponse.getId();
    }


    @Override
    public void start(String id) throws ApplicationException {
        try {
            if (isStarted(id)) {
                log.info("Container {} is already started", id);
                return;
            }

            dockerClient.startContainerCmd(id).exec();
            log.info("Container {} has been started", id);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            throw new ApplicationException(ex.getMessage());
        }
    }


    @Override
    public void stop(String id) {
        try {
            if (isStarted(id)) {
                dockerClient.stopContainerCmd(id).exec();
                log.info("Container {} has been stopped", id);
            }
        } catch (ResourceNotFoundException e) {
            log.warn(e.getMessage());
        }
    }


    @Override
    public void remove(String id) {
        get(id).ifPresent(container -> {
            try {
                dockerClient.removeContainerCmd(container.getId()).exec();
                log.info("Container {} has been removed", id);
            } catch (NotFoundException ex) {
                log.warn(ex.getMessage());
            }
        });
    }


    @Override
    public void removeByName(String name) {
        List<Container> existingContainers = getContainersByName(name);

        if (CollectionUtils.isNotEmpty(existingContainers)) {
            existingContainers.forEach(container -> {
                remove(container.getId());
            });
        }
    }


    @Override
    public List<DockerContainer> getContainers(boolean showHidden) {
        ListContainersCmd listContainersCmd = dockerClient.listContainersCmd();

        if (showHidden) {
            listContainersCmd.withShowAll(true);
        }

        return listContainersCmd.exec().stream()
                .map(container -> new DockerContainer(container.getId(), container.getImage(), container.getNames(),
                        container.getState(), container.getStatus()))
                .collect(Collectors.toList());
    }


    @Override
    public Optional<DockerContainer> get(String id) {
        List<Container> foundContainers = dockerClient.listContainersCmd()
                .withIdFilter(Collections.singleton(id))
                .withShowAll(true)
                .exec();

        return getOne(foundContainers);
    }


    @Override
    public Optional<DockerContainer> getByName(String name) {
        List<Container> foundContainers = getContainersByName(name);
        return getOne(foundContainers);
    }


    @Override
    public boolean isStarted(String id) throws ResourceNotFoundException {
        DockerContainerStatus status = getStatus(id);
        log.debug("Container {} Status: {}", id, status);

        switch (status) {
            case RUNNING:
            case HEALTHY:
                return true;
        }

        return false;
    }


    @Override
    public DockerContainerStatus getStatus(String id) throws ResourceNotFoundException {
        DockerContainer dockerContainer = get(id)
                .orElseThrow(() -> new ResourceNotFoundException("Container", id));

        switch (dockerContainer.getState()) {
            case "created":
                return DockerContainerStatus.CREATED;
            case "running":
                return StringUtils.isNotBlank(dockerContainer.getStatus()) && dockerContainer.getStatus().contains("(healthy")
                        ? DockerContainerStatus.HEALTHY
                        : DockerContainerStatus.RUNNING;
            case "exited":
                return DockerContainerStatus.EXITED;
        }

        return DockerContainerStatus.UNKNOWN;
    }


    private List<Container> getContainersByName(String name) {
        return dockerClient.listContainersCmd()
                .withNameFilter(Collections.singletonList(name))
                .withShowAll(true)
                .exec()
                .stream()
                .filter(container -> ArrayUtils.isNotEmpty(container.getNames())
                        && Arrays.asList(container.getNames()).contains("/" + name))
                .collect(Collectors.toList());
    }


    private Optional<DockerContainer> getOne(List<Container> containers) {
        if (CollectionUtils.isNotEmpty(containers)) {
            Container c = containers.get(0);
            return Optional.of(new DockerContainer(c.getId(), c.getImage(), c.getNames(), c.getState(), c.getStatus()));
        }

        return Optional.empty();
    }


    private RestartPolicy getRestartPolicy(DockerRestartOption restart, int maxRetryCount) {
        switch (restart) {
            case ALWAYS:
                return RestartPolicy.alwaysRestart();

            case ON_FAILURE:
                return RestartPolicy.onFailureRestart(maxRetryCount);

            case UNLESS_STOPPED:
                return RestartPolicy.unlessStoppedRestart();

            default:
                return RestartPolicy.noRestart();
        }
    }
}