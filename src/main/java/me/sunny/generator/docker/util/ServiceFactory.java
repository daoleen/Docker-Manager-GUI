package me.sunny.generator.docker.util;


import com.github.dockerjava.api.DockerClient;
import me.sunny.generator.docker.domain.Host;
import me.sunny.generator.docker.service.DockerContainerRunner;
import me.sunny.generator.docker.service.DockerContainerService;
import me.sunny.generator.docker.service.DockerRegistryService;
import me.sunny.generator.docker.service.impl.DockerContainerServiceImpl;
import me.sunny.generator.docker.service.impl.DockerRegistryServiceImpl;


public class ServiceFactory {
    public static DockerContainerService getDockerContainerService(DockerClient dockerClient, Host host) {
        return new DockerContainerServiceImpl(dockerClient, host);
    }

    public static DockerContainerRunner getDockerContainerRunner(DockerContainerService dockerContainerService) {
        return new DockerContainerRunner(dockerContainerService);
    }

    public static DockerRegistryService getDockerRegistryService() {
        return new DockerRegistryServiceImpl();
    }
}
