package com.crm;

import java.time.Duration;
import java.util.function.BooleanSupplier;

/** Espera, por até 10 s, uma condição que depende de trabalho assíncrono (e-mail, listener). */
final class Await {

    private Await() {
    }

    static void until(BooleanSupplier condition) {
        var deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();

        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline)
                throw new AssertionError("a condição não ficou verdadeira em 10 s");

            try {
                Thread.sleep(50);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();

                throw new AssertionError("interrompido esperando a condição", e);
            }
        }
    }
}
