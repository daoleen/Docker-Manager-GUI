package me.sunny.generator.docker.domain;


import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DockerVolumeMapping {
    private String hostVolumePath;
    private String containerVolumePath;


    @Override
    public String toString() {
        return String.format("%s:%s");
    }
}
