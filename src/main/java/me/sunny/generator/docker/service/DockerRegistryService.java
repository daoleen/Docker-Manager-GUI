package me.sunny.generator.docker.service;


import java.util.List;

import me.sunny.generator.docker.exception.ApplicationException;


public interface DockerRegistryService {
    List<String> getAllImages(String host, String username, String password) throws ApplicationException;
    List<String> getImageTags(String host, String username, String password, String image) throws ApplicationException;
    void deleteImageTag(String host, String username, String password, String image, String tag) throws ApplicationException;
}
