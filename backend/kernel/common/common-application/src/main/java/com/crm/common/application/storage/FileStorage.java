package com.crm.common.application.storage;

import java.time.Duration;
import java.util.Optional;

/**
 * Armazenamento de arquivos (capa de evento, logo, galeria de capas), por chave.
 *
 * <p>É um contrato da nossa própria infraestrutura (um armazenamento de objetos compatível com
 * S3, autohospedado), não de terceiro: por isso não passa por Translator (architecture.md §4.3
 * e §12). Nenhum tipo do SDK aparece aqui.
 *
 * <h2>Regras que valem para quem usa</h2>
 *
 * <ul>
 *   <li>O banco guarda só a <b>chave</b> do objeto, nunca a URL: o endereço público muda com o
 *       ambiente, e a chave não.
 *   <li>O upload é feito <b>pelo navegador, direto no armazenamento</b>, com a URL de
 *       {@link #prepareUpload}. O arquivo nunca passa pelo Tomcat.
 *   <li>A URL pré-assinada <b>não limita o tamanho</b> do que é enviado. Quem a entrega precisa,
 *       depois do envio, conferir {@link #find} (tamanho e tipo) e chamar {@link #delete} se o
 *       objeto não servir.
 * </ul>
 *
 * <p>A chave só aceita letras, dígitos, {@code / _ . -}, sem {@code ..} e sem barra no começo.
 * Chave fora disso é recusada com {@link com.crm.common.application.exceptions.InternalServerErrorException}:
 * ela vem de montagem nossa, e um valor ruim aqui é bug, não entrada de usuário.
 */
public interface FileStorage {

    /**
     * URL para o navegador enviar o objeto com {@code PUT}, com o {@code Content-Type} exatamente
     * igual a {@code contentType} (ele entra na assinatura).
     */
    UploadTicket prepareUpload(String key, String contentType, Duration validFor);

    /** Os metadados do objeto, ou vazio se ele ainda não existe. */
    Optional<StoredObject> find(String key);

    /** Apaga o objeto. Apagar o que não existe não é erro. */
    void delete(String key);

    /** Endereço de leitura de um objeto do bucket público. Não confere se o objeto existe. */
    String publicUrl(String key);
}
