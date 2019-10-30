package me.sunny.generator.docker.domain;


import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DockerPortMapping {
    private int hostPort;
    private int containerPort;


    @Override
    public String toString() {
        return String.format("%d:%d", hostPort, containerPort);
    }
}
