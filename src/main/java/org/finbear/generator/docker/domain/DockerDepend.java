package org.finbear.generator.docker.domain;


import lombok.*;
import org.finbear.generator.docker.enums.DockerDependCondition;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "service")
public class DockerDepend {
    private DockerService service;
    private DockerDependCondition condition;
}
