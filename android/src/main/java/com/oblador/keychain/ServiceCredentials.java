package com.oblador.keychain;

/**
 * ServiceCredentials is representing a single pair (user/pass) of credentials stored in the Keychain.
 *
 * @author Miroslav Genov <miroslav.genov@clouway.com>
 */
public final class ServiceCredentials {
    public final String service;
    public final String username;
    public final String password;

    public ServiceCredentials(String service, String username, String password) {
        this.service = service;
        this.username = username;
        this.password = password;
    }
}
