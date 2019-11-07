package me.sunny.generator.docker.domain;


import java.util.ArrayList;
import java.util.List;

import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "service")
public class DockerServiceDescription {

    @NonNull
    private DockerService service;
    private List<String> versions = new ArrayList<>();


    @Override
    public String toString() {
        return service.toString();
    }
}
