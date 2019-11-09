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
import me.sunny.generator.docker.domain.Composition;
import me.sunny.generator.docker.domain.DockerService;
import me.sunny.generator.docker.domain.DockerServiceConcreted;
import me.sunny.generator.docker.exception.ApplicationException;
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


    private static void addServiceToMap(Map<String, Object> servicesMap, DockerServiceConcreted service) {
        Map<String, Object> serviceValueMap = new LinkedHashMap<>();
        serviceValueMap.put("image", service.getService().getImage() + ":" + service.getVersion());
        serviceValueMap.put("restart", service.getService().getRestart().toString());

        if (CollectionUtils.isNotEmpty(service.getService().getPorts())) {
            List<String> ports = service.getService().getPorts().stream()
                    .map(mapping -> mapping.getHostPort() + ":" + mapping.getContainerPort())
                    .collect(Collectors.toList());

            serviceValueMap.put("ports", ports);
        }

        if (CollectionUtils.isNotEmpty(service.getService().getVolumes())) {
            List<String> volumes = service.getService().getVolumes().stream()
                    .map(mapping -> mapping.getHostVolumePath() + ":" + mapping.getContainerVolumePath())
                    .collect(Collectors.toList());

            serviceValueMap.put("volumes", volumes);
        }

        if (MapUtils.isNotEmpty(service.getService().getEnvironment())) {
            serviceValueMap.put("environment", service.getService().getEnvironment());
        }

        if (CollectionUtils.isNotEmpty(service.getService().getLinks())) {
            List<String> links = service.getService().getLinks().stream()
                    .map(DockerService::getName)
                    .collect(Collectors.toList());

            serviceValueMap.put("links", links);
        }

        if (CollectionUtils.isNotEmpty(service.getService().getDepends())) {
            Map<String, Object> depends = new LinkedHashMap<>(service.getService().getDepends().size());
            service.getService().getDepends().forEach(depend -> {
                Map<String, String> condition = new HashMap<>(1);
                condition.put("condition", depend.getCondition().toString());
                depends.put(depend.getService().getName(), condition);
            });

            serviceValueMap.put("depends_on", depends);
        }

        if (service.getService().getHealthcheck() != null && StringUtils.isNotEmpty(service.getService().getHealthcheck().getTest())) {
            Map<String, Object> healthcheck = new HashMap<>(4);
            healthcheck.put("test", service.getService().getHealthcheck().getTest().split(" "));
            healthcheck.put("interval", service.getService().getHealthcheck().getIntervalSeconds() + "s");
            healthcheck.put("timeout", service.getService().getHealthcheck().getTimeoutSeconds() + "s");
            healthcheck.put("retries", Integer.toString(service.getService().getHealthcheck().getRetriesCount()));

            serviceValueMap.put("healthcheck", healthcheck);
        }

        servicesMap.put(service.getService().getName(), serviceValueMap);
    }

}
