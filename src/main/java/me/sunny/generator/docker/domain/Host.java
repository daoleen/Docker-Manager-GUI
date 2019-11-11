package me.sunny.generator.docker.domain;


import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Host {
    private String address;


    @Override
    public String toString() {
        return address;
    }
}
