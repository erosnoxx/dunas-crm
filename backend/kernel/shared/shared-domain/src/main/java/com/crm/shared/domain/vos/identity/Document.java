package com.crm.shared.domain.vos.identity;

import com.crm.common.domain.exceptions.ValidationException;
import com.crm.common.domain.vos.StringValueObject;
import com.crm.shared.domain.vos.identity.validators.CnpjValidator;
import com.crm.shared.domain.vos.identity.validators.CpfValidator;

/**
 * CPF ou CNPJ, guardado só com os caracteres significativos — sem ponto, barra ou traço.
 *
 * <p>O tipo é <b>inferido do comprimento</b>, não informado por quem chama: 11 caracteres é CPF,
 * 14 é CNPJ. Pedir o tipo junto do valor abriria espaço para os dois discordarem.
 *
 * <p>Os dígitos verificadores são conferidos na construção. Um `Document` que existe é um
 * documento estruturalmente válido — o que não significa que exista na Receita.
 *
 * <p>O validador de CNPJ aceita a forma alfanumérica, adotada a partir de 2026.
 */
public final class Document extends StringValueObject {

    private final DocumentType type;

    private Document(String value, DocumentType type) {
        super(value);
        this.type = type;
    }

    public static Document of(String value) {
        var normalized = normalize(value);

        return switch (normalized.length()) {
            case 11 -> createCpf(normalized);
            case 14 -> createCnpj(normalized);
            default -> throw new ValidationException("documento inválido: deve ser um cpf ou cnpj válido");
        };
    }

    public DocumentType getType() {
        return type;
    }

    /**
     * Nome pra mensagem de erro da base ({@code StringValueObject}). Não colide com o campo
     * {@code type} (a enum CPF/CNPJ) — são namespaces diferentes em Java (campo vs. método) —
     * mas na prática nunca é chamado: {@link #normalize} já barra nulo/vazio antes de chegar ao
     * construtor, então os caminhos que usam isto na base nunca são exercitados por este VO.
     */
    @Override
    protected String type() {
        return "documento";
    }

    public boolean isIndividual() {
        return type == DocumentType.CPF;
    }

    public boolean isOrganization() {
        return type == DocumentType.CNPJ;
    }

    /** Pra mostrar na tela sem expor o documento inteiro: só os últimos 3 caracteres, resto `*`. */
    public String maskedForDisplay() {
        var value = getValue();
        var visible = value.substring(value.length() - 3);

        return "*".repeat(value.length() - 3) + visible;
    }

    private static Document createCpf(String value) {
        if (!CpfValidator.isValid(value))
            throw new ValidationException("cpf inválido");

        return new Document(value, DocumentType.CPF);
    }

    private static Document createCnpj(String value) {
        if (!CnpjValidator.isValid(value))
            throw new ValidationException("cnpj inválido");

        return new Document(value, DocumentType.CNPJ);
    }

    private static String normalize(String value) {
        if (value == null)
            throw new ValidationException("documento não pode ser nulo");

        return value
                .replaceAll("[^a-zA-Z0-9]", "")
                .toUpperCase();
    }

    public enum DocumentType {
        CPF(11),
        CNPJ(14);

        public final int length;

        DocumentType(int length) {
            this.length = length;
        }
    }
}
