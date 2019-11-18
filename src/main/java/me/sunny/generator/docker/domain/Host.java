package me.sunny.generator.docker.domain;


import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Host {
    private String address;
    private boolean useSSL;
    private String certificatesPath;


    @Override
    public String toString() {
        return address;
    }
}
