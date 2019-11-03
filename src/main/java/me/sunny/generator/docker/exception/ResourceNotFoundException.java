package me.sunny.generator.docker.exception;


import lombok.Getter;


@Getter
public class ResourceNotFoundException extends Exception {
    private final String resource;
    private final String id;


    public ResourceNotFoundException(String resource, String id) {
        super(String.format("Resource '%s' [%s] is not found", resource, id));
        this.resource = resource;
        this.id = id;
    }


    @Override
    public String toString() {
        return String.format("Resource '%s' [%s] is not found", resource, id);
    }
}
