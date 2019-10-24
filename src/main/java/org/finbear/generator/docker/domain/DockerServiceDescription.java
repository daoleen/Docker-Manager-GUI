package org.finbear.generator.docker.domain;


import java.util.ArrayList;
import java.util.List;

import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "service")
public class DockerServiceDescription {
    private DockerService service;
    private List<String> versions = new ArrayList<>();
}
