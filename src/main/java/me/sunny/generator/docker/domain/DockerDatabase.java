package me.sunny.generator.docker.domain;


import java.util.Set;

import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DockerDatabase {
    private Set<DockerServiceDescription> services;
}
