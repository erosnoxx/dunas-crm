package com.crm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

/** Resposta crua: status, corpo e os {@code Set-Cookie} exatamente como o Tomcat os mandou. */
record Reply(
        int status,
        String raw,
        List<String> setCookies
) {

    private static final ObjectMapper JSON = new ObjectMapper();

    JsonNode body() {
        try {
            return JSON.readTree(raw);
        }
        catch (Exception e) {
            throw new AssertionError("corpo não é JSON: " + raw, e);
        }
    }

    String code() {
        if (raw == null || raw.isBlank())
            return null;

        return Optional.ofNullable(body().get("code")).map(JsonNode::asText).orElse(null);
    }

    Optional<String> setCookie(String name) {
        return setCookies.stream().filter(header -> header.startsWith(name + "=")).findFirst();
    }
}
