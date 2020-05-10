package me.sunny.generator.docker.domain;


import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class HostVariable {
    private String variable;
    private String value;


    @Override
    public String toString() {
        return String.format("%s:%s", variable, value);
    }
}
