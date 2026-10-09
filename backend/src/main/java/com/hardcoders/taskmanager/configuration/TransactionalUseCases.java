package com.hardcoders.taskmanager.configuration;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Decorador de infraestructura: envuelve cada método de un puerto de entrada en una transacción,
 * para que los casos de uso permanezcan libres de anotaciones de Spring (constitución, punto 7).
 * Los triggers diferidos de V004 se validan al confirmar esta transacción.
 */
final class TransactionalUseCases {

    private TransactionalUseCases() {
    }

    static <T> T wrap(Class<T> port, T target, PlatformTransactionManager transactionManager, boolean readOnly) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setReadOnly(readOnly);
        Object proxy = Proxy.newProxyInstance(
                port.getClassLoader(),
                new Class<?>[] {port},
                (self, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return invoke(method, target, args);
                    }
                    return template.execute(status -> invoke(method, target, args));
                });
        return port.cast(proxy);
    }

    private static Object invoke(Method method, Object target, Object[] args) {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException(cause);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
