package com.crm.common.application.storage;

/** O que o armazenamento sabe de um objeto já enviado. */
public record StoredObject(long sizeInBytes, String contentType) {
}
