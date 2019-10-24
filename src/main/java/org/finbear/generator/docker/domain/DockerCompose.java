package org.finbear.generator.docker.domain;


import java.util.List;

import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DockerCompose {
    private String version = "2.1";
    private List<DockerService> services;
}
