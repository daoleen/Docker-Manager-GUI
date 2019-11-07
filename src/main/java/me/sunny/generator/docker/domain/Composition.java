package me.sunny.generator.docker.domain;


import java.util.Set;

import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "name")
public class Composition {
    private String name;
    private Set<DockerServiceConcreted> services;


    @Override
    public String toString() {
        return name;
    }
}
