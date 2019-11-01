package me.sunny.generator.docker.domain;


import lombok.*;
import me.sunny.generator.docker.enums.DockerDependCondition;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "service")
public class DockerDepend {
    private DockerService service;
    private DockerDependCondition condition;


    @Override
    public String toString() {
        return String.format("%s (%s)", service, condition);
    }
}
