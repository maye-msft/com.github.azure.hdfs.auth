package org.apache.hadoop.fs.azure;

import com.microsoft.azure.storage.IStorageCredentialsToken;
import com.microsoft.azure.storage.OperationContext;
import com.microsoft.azure.storage.StorageCredentials;
import com.microsoft.azure.storage.StorageUri;
import org.apache.hadoop.fs.azurebfs.oauth2.AzureADToken;
import org.apache.hadoop.fs.azurebfs.oauth2.ClientCredsTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

/**
 * Represents storage account credentials, based on OAuth2, for accessing the Microsoft Azure
 * storage services.
 */
public final class StorageCredentialsTokenOAuth extends StorageCredentials implements IStorageCredentialsToken {

    private static final Logger LOG = LoggerFactory.getLogger(StorageCredentialsTokenOAuth.class);

    private final String clientEndpoint;
    private final String clientId;
    private final String clientSecret;

    private final String accountName;

    private ClientCredsTokenProvider tokenProvider;
    /**
     * Stores the token for the credentials.
     */
    private volatile AzureADToken adToken;

    /**
     * Five minutes before the token expires, we will refresh it.
     * Copy from org.apache.hadoop.fs.azurebfs.oauth2.AccessTokenProvider.java
     */
    private static final long FIVE_MINUTES = 300 * 1000;

    private Thread monitorTokenExpiredThread;

    /**
     * Represents the setting name for the token credential.
     * Copy from com.microsoft.azure.storage.CloudStorageAccount.java
     */
    protected static final String ACCOUNT_TOKEN_NAME = "AccountToken";

    private String token;

    /**
     * Creates an instance of the <code>StorageCredentialsOAtuh</code> class, using the specified client_endpoint .
     * Token credentials must only be used with HTTPS requests on the blob and queue services.
     * The specified clientEndpoint is stored as a <code>String</code>.
     * @param accountName
     *           A <code>String</code> that represents the storage account name.
     *           This value is used to construct the endpoint URI for the token service.
     * @param clientEndpoint
     *           A <code>String</code> that represents the client_endpoint.
     *           The client_endpoint is the endpoint of the Azure Active Directory service.
     *           For example, https://login.microsoftonline.com.
     *           The client_endpoint must be HTTPS.
     * @param clientId
     *          A <code>String</code> that represents the client_id.
     *          The client_id is the application ID of the registered application.
     *
     * @param clientSecret
     *         A <code>String</code> that represents the client_secret.
     *         The client_secret is the application secret of the registered application.

     */
    public StorageCredentialsTokenOAuth(String accountName, String clientEndpoint, String clientId, String clientSecret) {
        this.accountName = accountName;
        this.clientEndpoint = clientEndpoint;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenProvider = new ClientCredsTokenProvider(clientEndpoint, clientId, clientSecret);
    }

    /**
     * Gets the token.
     *
     * @return A <code>String</code> that contains the token.
     */
    public String getToken()  {
        try {
            this.adToken = tokenProvider.getToken();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.token = this.adToken.getAccessToken();
        return this.token;
    }


    /**
     * Returns a <code>String</code> that represents this instance, optionally including sensitive data.
     *
     * @param exportSecrets
     *            <code>true</code> to include sensitive data in the return string; otherwise, <code>false</code>.
     *
     * @return A <code>String</code> that represents this object, optionally including sensitive data.
     */
    @Override
    public String toString(final boolean exportSecrets) {
        return String.format("%s=%s", ACCOUNT_TOKEN_NAME, exportSecrets ? this.token
                : "[token hidden]");
    }

    @Override
    public URI transformUri(URI resourceUri, OperationContext opContext) {
        return resourceUri;
    }

    @Override
    public StorageUri transformUri(StorageUri resourceUri, OperationContext opContext) {
        return resourceUri;
    }
}