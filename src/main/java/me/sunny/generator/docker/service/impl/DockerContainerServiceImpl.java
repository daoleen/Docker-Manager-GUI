package me.sunny.generator.docker.service.impl;


import java.util.*;
import java.util.stream.Collectors;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.command.PullImageResultCallback;
import lombok.extern.slf4j.Slf4j;
import me.sunny.generator.docker.Context;
import me.sunny.generator.docker.domain.DockerContainer;
import me.sunny.generator.docker.domain.DockerService;
import me.sunny.generator.docker.domain.Host;
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
    private final Host host;


    public DockerContainerServiceImpl(DockerClient dockerClient, Host host) {
        this.dockerClient = dockerClient;
        this.host = host;
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
                    .map(dockerVolumeMapping -> {
                        final String[] containerVolumePath = {dockerVolumeMapping.getContainerVolumePath()};
                        if (containerVolumePath[0].contains("#{")) {
                            // value contains HostVariable
                            host.getHostVariables().forEach(hostVariable -> {
                                containerVolumePath[0] = containerVolumePath[0].replace(hostVariable.getVariable(), hostVariable.getValue());
                            });
                        }

                        return new Bind(dockerVolumeMapping.getHostVolumePath(), new Volume(containerVolumePath[0]));
                    })
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

        final String imageVersion = String.format("%s:%s", dockerService.getImage(), version);
        CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(imageVersion)
                .withName(dockerService.getName())
                .withHostName(dockerService.getName())
                .withHostConfig(hostConfig);

        if (MapUtils.isNotEmpty(dockerService.getEnvironment())) {
            List<String> environments = dockerService.getEnvironment().entrySet().stream()
                    .map(env -> {
                        final String[] value = {env.getValue()};
                        if (value[0].contains("#{")) {
                            // value contains HostVariable
                            host.getHostVariables().forEach(hostVariable -> {
                                value[0] = value[0].replace(hostVariable.getVariable(), hostVariable.getValue());
                            });
                        }

                        return String.format("%s=%s", env.getKey(), value[0]);
                    })
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

        if (!hasImageOnHost(imageVersion)) {
            pullImage(imageVersion);
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
                if (StringUtils.isNotBlank(dockerContainer.getStatus())) {
                    if (dockerContainer.getStatus().contains("(healthy")) {
                        return DockerContainerStatus.HEALTHY;
                    }

                    if (dockerContainer.getStatus().contains("(unhealthy")) {
                        return DockerContainerStatus.UNHEALTHY;
                    }
                }

                return  DockerContainerStatus.RUNNING;
            case "exited":
                return DockerContainerStatus.EXITED;
        }

        return DockerContainerStatus.UNKNOWN;
    }


    @Override
    public boolean hasImageOnHost(String name) {
        return getImage(name).isPresent();
    }


    @Override
    public Optional<Image> getImage(String name) {
        List<Image> images = dockerClient.listImagesCmd()
                .withImageNameFilter(name)
                .withShowAll(true)
                .exec();

        if (CollectionUtils.isEmpty(images)) {
            return Optional.empty();
        }

        return Optional.of(images.get(0));
    }


    @Override
    public void pullImage(String name) throws ApplicationException {
        Context.HOST_STATUS_OBSERVABLE.notifyObservers("Pulling image " + name);
        PullImageResultCallback resultCallback = dockerClient.pullImageCmd(name)
                .exec(new PullImageResultCallback());

        try {
            resultCallback.awaitCompletion();
        } catch (InterruptedException e) {
            log.warn(e.getMessage(), e);
            Context.HOST_STATUS_OBSERVABLE.notifyObservers("Error pulling image " + name);
            throw new ApplicationException("Image pulling interrupted");
        }
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
