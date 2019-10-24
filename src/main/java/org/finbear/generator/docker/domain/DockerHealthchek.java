package org.finbear.generator.docker.domain;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DockerHealthchek {
    private String[] test;
    private int intervalSeconds;
    private int timeoutSeconds;
    private int retriesCount;
}
