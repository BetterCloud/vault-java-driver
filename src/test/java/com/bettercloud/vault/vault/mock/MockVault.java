package com.bettercloud.vault.vault.mock;

import org.eclipse.jetty.server.handler.AbstractHandler;

/**
 * <p>A common ancestor for all mock Vault implementations used in unit tests.  This is only necessary
 * because <code>VaultTestUtils</code> provides methods for setting up and shutting down mock Vault
 * instances, and having a common parent avoids the need to overload all of those methods for every mock type.</p>
 */
public abstract class MockVault extends AbstractHandler {
}
