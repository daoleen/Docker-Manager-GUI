package org.finbear.generator.docker.enums;


public enum DockerDependCondition {
    SERVICE_HEALTHY("service_healthy"),
    SERVICE_STARTED("service_started");


    private String value;


    DockerDependCondition(String value) {
        this.value = value;
    }


    @Override
    public String toString() {
        return value;
    }
}
