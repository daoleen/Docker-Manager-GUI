package me.sunny.generator.docker.domain;


import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public class DockerContainer {
    private String id;
    private String image;
    private String[] names;
    private String state;
    private String status;


    @Override
    public String toString() {
        return String.format("%s [%s:%s]", image, state, status);
    }
}
