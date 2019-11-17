package me.sunny.generator.docker.exception;


public class ContainerStartException extends Exception {
    public ContainerStartException(String containerId, String lastStatus) {
        super(String.format("Error starting container %s. Last checked status was: %s", containerId, lastStatus));
    }
}
