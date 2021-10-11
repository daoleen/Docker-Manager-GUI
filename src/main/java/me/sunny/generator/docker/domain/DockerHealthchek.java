package me.sunny.generator.docker.domain;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DockerHealthchek {
    private String test;
    private int intervalSeconds;
    private int timeoutSeconds;
    private int retriesCount;


    @Override
    public String toString() {
        return String.format("test: %s\ninterval: %ds\ntimeout: %ds\nretries: %d",
                test, intervalSeconds, timeoutSeconds, retriesCount);
    }


    public DockerHealthchek copy() {
        return new DockerHealthchek(test, intervalSeconds, timeoutSeconds, retriesCount);
    }
}
