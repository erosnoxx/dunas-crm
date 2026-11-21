package com.crm;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Um navegador: guarda os cookies que o servidor manda, devolve-os em toda requisição e copia o
 * {@code XSRF-TOKEN} para o cabeçalho {@code X-XSRF-TOKEN} nas escritas, como o frontend fará.
 *
 * <p>O token de sessão e as senhas vistos pelo navegador vão para {@code secrets}, que o teste usa
 * para garantir que nenhum segredo foi ao log.
 */
final class Browser {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpClient http;
    private final int port;
    private final Set<String> secrets;
    private final String device;
    private final Map<String, String> cookies = new ConcurrentHashMap<>();

    Browser(
            HttpClient http,
            int port,
            Set<String> secrets,
            String device
    ) {
        this.http = http;
        this.port = port;
        this.secrets = secrets;
        this.device = device;
    }

    Reply get(String path) {
        return send(request("GET", path, null));
    }

    Reply post(
            String path,
            Object body
    ) {
        return send(withCsrf(request("POST", path, body)));
    }

    Reply put(
            String path,
            Object body
    ) {
        return send(withCsrf(request("PUT", path, body)));
    }

    Reply delete(String path) {
        return send(withCsrf(request("DELETE", path, null)));
    }

    HttpRequest.Builder request(
            String method,
            String path,
            Object body
    ) {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("X-Device-Name", device)
                .header("Content-Type", "application/json")
                .method(method, bodyOf(body));

        var cookieHeader = cookies.entrySet().stream()
                .map(cookie -> cookie.getKey() + "=" + cookie.getValue())
                .toList();

        if (!cookieHeader.isEmpty())
            builder.header("Cookie", String.join("; ", cookieHeader));

        return builder;
    }

    Reply send(HttpRequest.Builder request) {
        try {
            var response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
            var setCookies = response.headers().allValues("Set-Cookie");

            for (var header : setCookies)
                keep(header);

            return new Reply(response.statusCode(), response.body(), setCookies);
        }
        catch (Exception e) {
            throw new AssertionError("falha na requisição HTTP", e);
        }
    }

    private HttpRequest.Builder withCsrf(HttpRequest.Builder request) {
        var token = cookies.get("XSRF-TOKEN");

        if (token != null)
            request.header("X-XSRF-TOKEN", token);

        return request;
    }

    private void keep(String header) {
        var pair = header.split(";", 2)[0];
        var name = pair.substring(0, pair.indexOf('='));
        var value = pair.substring(pair.indexOf('=') + 1);

        if ("crm_session".equals(name) && !value.isEmpty())
            secrets.add(value);

        if (value.isEmpty() || header.contains("Max-Age=0")) {
            cookies.remove(name);

            return;
        }

        cookies.put(name, value);
    }

    private HttpRequest.BodyPublisher bodyOf(Object body) {
        if (body == null)
            return HttpRequest.BodyPublishers.noBody();

        if (body instanceof Map<?, ?> map)
            for (var value : map.values())
                if (value instanceof String text && text.contains("#"))
                    secrets.add(text);

        try {
            return HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body));
        }
        catch (Exception e) {
            throw new AssertionError("corpo não serializável", e);
        }
    }
}
