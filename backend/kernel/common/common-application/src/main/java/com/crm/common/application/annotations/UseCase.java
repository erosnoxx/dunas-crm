package com.crm.common.application.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca uma classe como caso de uso e define o comportamento transversal aplicado a ela pelo
 * aspect de infraestrutura.
 *
 * <p>A application não depende de Spring: é esta anotação, e não {@code @Transactional}, que pede
 * transação, retentativa e circuit breaker.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface UseCase {
    boolean transactional() default true;
    boolean retriable() default false;
    int maxRetries() default 3;
    boolean circuitBreaker() default false;
}
