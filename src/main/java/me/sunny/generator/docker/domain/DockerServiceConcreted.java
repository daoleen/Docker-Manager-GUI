package me.sunny.generator.docker.domain;


import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = { "service", "version" })
public class DockerServiceConcreted {
    private DockerService service;
    private String version;


    @Override
    public String toString() {
        return String.format("%s [%s]", service, version);
    }
}
