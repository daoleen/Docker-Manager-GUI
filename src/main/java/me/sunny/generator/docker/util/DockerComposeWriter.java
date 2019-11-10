package me.sunny.generator.docker.util;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import me.sunny.generator.docker.Context;
import me.sunny.generator.docker.domain.Composition;
import me.sunny.generator.docker.domain.DockerService;
import me.sunny.generator.docker.domain.DockerServiceConcreted;
import me.sunny.generator.docker.exception.ApplicationException;
import me.sunny.generator.docker.exception.ResourceNotFoundException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;


@Slf4j
public class DockerComposeWriter {
    private final static Yaml yaml = new Yaml();


    public static void generateCompose(Composition composition, File outputFile) throws ApplicationException {
        log.info("Generating docker-compose.yml for composition: {}", composition.getName());

        Map<String, Object> compositionMap = createMapFromComposition(composition);
        try (FileWriter fw = new FileWriter(outputFile, false)) {
            DockerComposeWriter.yaml.dump(compositionMap, fw);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new ApplicationException(e.getMessage(), e);
        }
    }


    private static Map<String, Object> createMapFromComposition(Composition composition) {
        Map<String, Object> composeMap = new LinkedHashMap<>();
        composeMap.put("version", "2.1");

        Map<String, Object> servicesMap = new LinkedHashMap<>(composition.getServices().size());
        composition.getServices().forEach(serviceConcreted -> addServiceToMap(servicesMap, serviceConcreted));

        composeMap.put("services", servicesMap);
        return composeMap;
    }


    private static void addServiceToMap(Map<String, Object> servicesMap, DockerServiceConcreted serviceConcreted) {
        Map<String, Object> serviceValueMap = new LinkedHashMap<>();
        DockerService service;

        try {
            service = Context.project.findService(serviceConcreted.getServiceId()).getService();
        } catch (ResourceNotFoundException e) {
            log.error(e.getMessage());
            return;
        }

        if (StringUtils.isNotBlank(service.getBuildPath())) {
            serviceValueMap.put("build", service.getBuildPath());
        }

        if (StringUtils.isNotBlank(service.getImage())) {
            String image = service.getImage();

            if (StringUtils.isNotBlank(serviceConcreted.getVersion())) {
                image += ":" + serviceConcreted.getVersion();
            }

            serviceValueMap.put("image", image);
        }

        serviceValueMap.put("restart", service.getRestart().toString());

        if (CollectionUtils.isNotEmpty(service.getPorts())) {
            List<String> ports = service.getPorts().stream()
                    .map(mapping -> mapping.getHostPort() + ":" + mapping.getContainerPort())
                    .collect(Collectors.toList());

            serviceValueMap.put("ports", ports);
        }

        if (CollectionUtils.isNotEmpty(service.getVolumes())) {
            List<String> volumes = service.getVolumes().stream()
                    .map(mapping -> mapping.getHostVolumePath() + ":" + mapping.getContainerVolumePath())
                    .collect(Collectors.toList());

            serviceValueMap.put("volumes", volumes);
        }

        if (MapUtils.isNotEmpty(service.getEnvironment())) {
            serviceValueMap.put("environment", service.getEnvironment());
        }

        if (CollectionUtils.isNotEmpty(service.getLinks())) {
            List<String> links = service.getLinks().stream()
                    .map(serviceId -> {
                        try {
                            return Context.project.findService(serviceId).getService().getName();
                        } catch (ResourceNotFoundException e) {
                            log.error(e.getMessage());
                        }
                        return null;
                    })
                    .collect(Collectors.toList());

            serviceValueMap.put("links", links);
        }

        if (CollectionUtils.isNotEmpty(service.getDepends())) {
            Map<String, Object> depends = new LinkedHashMap<>(service.getDepends().size());
            service.getDepends().forEach(depend -> {
                Map<String, String> condition = new HashMap<>(1);
                condition.put("condition", depend.getCondition().toString());
                try {
                    depends.put(Context.project.findService(depend.getServiceId()).getService().getName(), condition);
                } catch (ResourceNotFoundException e) {
                    log.error(e.getMessage());
                }
            });

            serviceValueMap.put("depends_on", depends);
        }

        if (service.getHealthcheck() != null && StringUtils.isNotEmpty(service.getHealthcheck().getTest())) {
            Map<String, Object> healthcheck = new HashMap<>(4);
            healthcheck.put("test", service.getHealthcheck().getTest().split(" "));
            healthcheck.put("interval", service.getHealthcheck().getIntervalSeconds() + "s");
            healthcheck.put("timeout", service.getHealthcheck().getTimeoutSeconds() + "s");
            healthcheck.put("retries", Integer.toString(service.getHealthcheck().getRetriesCount()));

            serviceValueMap.put("healthcheck", healthcheck);
        }

        servicesMap.put(service.getName(), serviceValueMap);
    }

}
