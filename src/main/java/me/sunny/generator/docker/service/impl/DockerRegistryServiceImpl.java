package me.sunny.generator.docker.service.impl;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.sunny.generator.docker.exception.ApplicationException;
import me.sunny.generator.docker.service.DockerRegistryService;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;


@Slf4j
public class DockerRegistryServiceImpl implements DockerRegistryService {
    private final ObjectMapper objectMapper = new ObjectMapper();


    @Override
    public List<String> getAllImages(String host, String username, String password) throws ApplicationException {
        List<String> images = new ArrayList<>();
        BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(host, AuthScope.ANY_PORT),
                new UsernamePasswordCredentials(username, password)
        );

        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build();

        HttpGet request = new HttpGet(String.format("http://%s/v2/_catalog", host));

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getStatusLine().getStatusCode() == 200) {
                String responseStr = EntityUtils.toString(response.getEntity());
                JsonNode rootNode = objectMapper.readTree(responseStr);
                JsonNode repositories = rootNode.get("repositories");

                for (int i = 0; i < repositories.size(); i++) {
                    String image = repositories.get(i).asText();
                    images.add(image);
                }
            }
        } catch (Exception ex) {
            throw new ApplicationException(ex.getMessage(), ex);
        }

        return images;
    }


    @Override
    public List<String> getImageTags(String host, String username, String password, String image)
            throws ApplicationException
    {
        List<String> tags = new ArrayList<>();

        BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(host, AuthScope.ANY_PORT),
                new UsernamePasswordCredentials(username, password)
        );

        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build();

        HttpGet request = new HttpGet(String.format("http://%s/v2/%s/tags/list", host, image));

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getStatusLine().getStatusCode() == 200) {
                String responseStr = EntityUtils.toString(response.getEntity());
                JsonNode rootNode = objectMapper.readTree(responseStr);
                JsonNode tagsNode = rootNode.get("tags");

                for (int i = 0; i < tagsNode.size(); i++) {
                    String tag = tagsNode.get(i).asText();
                    tags.add(tag);
                }
            }
        } catch (Exception ex) {
            throw new ApplicationException(ex.getMessage(), ex);
        }

        return tags;
    }


    @Override
    public void deleteImageTag(String host, String username, String password, String image, String tag)
            throws ApplicationException
    {
        BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(host, AuthScope.ANY_PORT),
                new UsernamePasswordCredentials(username, password)
        );

        String imageTagDigest = getImageTagDigest(credsProvider, host, image, tag);

        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build();

// curl -svIX DELETE -H "Accept: application/vnd.docker.distribution.manifest.v2+json" "http://<registry_host:port>/v2/<repo_name>/manifests/<digest_hash>"
        HttpDelete request = new HttpDelete(String.format("http://%s/v2/%s/manifests/%s", host, image, imageTagDigest));
        request.setHeader("Accept", "application/vnd.docker.distribution.manifest.v2+json");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getStatusLine().getStatusCode() == 200) {
                String responseStr = EntityUtils.toString(response.getEntity());
                log.info(responseStr);
            }
        } catch (Exception ex) {
            throw new ApplicationException(ex.getMessage(), ex);
        }
    }


    private String getImageTagDigest(BasicCredentialsProvider credsProvider, String host, String image, String tag) throws ApplicationException {
        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build();

// curl -sI GET -H "Accept: application/vnd.docker.distribution.manifest.v2+json" "http://<registry_host:port>/v2/<repo_name>/manifests/<tag_name>"
        HttpGet request = new HttpGet(String.format("http://%s/v2/%s/manifests/%s", host, image, tag));
        request.setHeader("Accept", "application/vnd.docker.distribution.manifest.v2+json");
        String digest = "";

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getStatusLine().getStatusCode() == 200) {
                digest = response.getFirstHeader("Docker-Content-Digest").getValue();
            }
        } catch (Exception ex) {
            throw new ApplicationException(ex.getMessage(), ex);
        }

        return digest;
    }

}
