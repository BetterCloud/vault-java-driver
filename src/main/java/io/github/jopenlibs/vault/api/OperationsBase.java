package io.github.jopenlibs.vault.api;

import io.github.jopenlibs.vault.VaultConfig;
import io.github.jopenlibs.vault.VaultException;


/**
 * The base class for all operation.
 */
public abstract class OperationsBase {
    protected final VaultConfig config;

    protected OperationsBase(VaultConfig config) {
        this.config = config;
    }

    protected  <T> T retry(final EndpointOperation<T> op) throws VaultException {
        return retry(op, config.getMaxRetries(), config.getRetryIntervalMilliseconds());
    }

    /**
     */
    static <T> T retry(final EndpointOperation<T> op, int retryCount, long retryIntervalMs) throws VaultException {
        int attempt = 0;

        while (true) {
            try {
                return op.run(attempt);
            } catch (final Exception e) {
                // If there are retries to perform, then pause for the configured interval and then execute the loop again...
                if (attempt < retryCount) {
                    attempt++;

                    sleep(retryIntervalMs);
                } else if (e instanceof VaultException) {
                    // ... otherwise, give up.
                    throw (VaultException) e;
                } else {
                    throw new VaultException(e);
                }
            }
        }
    }

    public interface EndpointOperation<T> {
        /**
         * Run an operation.
         *
         * @param attempt Number of current attempt.
         * @return Operation response.
         */
        T run(int attempt) throws Exception;
    }

    private static void sleep(long delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
