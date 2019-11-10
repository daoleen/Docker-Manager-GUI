package me.sunny.generator.docker.enums;


public enum DockerRestartOption {
    NO("no"),
    ON_FAILURE("on-failure"),
    ALWAYS("always"),
    UNLESS_STOPPED("unless-stopped");

    private final String value;


    DockerRestartOption(String value) {
        this.value = value;
    }


    @Override
    public String toString() {
        return value;
    }
}
