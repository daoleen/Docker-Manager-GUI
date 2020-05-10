package me.sunny.generator.docker.domain;


import java.util.Set;

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
    private Set<HostVariable> hostVariables;


    @Override
    public String toString() {
        return address;
    }
}
