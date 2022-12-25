package org.apache.hadoop.fs.azure;

import com.microsoft.azure.storage.IStorageCredentialsToken;
import com.microsoft.azure.storage.OperationContext;
import com.microsoft.azure.storage.StorageCredentials;
import com.microsoft.azure.storage.StorageUri;
import org.apache.hadoop.fs.azurebfs.constants.AbfsHttpConstants;
import org.apache.hadoop.fs.azurebfs.constants.AuthConfigurations;
import org.apache.hadoop.fs.azurebfs.oauth2.AzureADToken;
import org.apache.hadoop.fs.azurebfs.oauth2.MsiTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

/**
 * Represents storage account credentials, based on OAuth2, for accessing the Microsoft Azure
 * storage services.
 */
public final class StorageCredentialsTokenMSI extends StorageCredentials implements IStorageCredentialsToken {

    private static final Logger LOG = LoggerFactory.getLogger(StorageCredentialsTokenMSI.class);

    private final String clientId;

    private final String accountName;
    private final String authEndpoint;
    private final String tenantGuid;
    private String authority;

    private MsiTokenProvider tokenProvider;
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
     * Initializes a new instance of the StorageCredentialsTokenMSI class.
     *
     * @param accountName the storage account name.
     * @param authEndpoint the authentication endpoint.
     * @param tenantGuid the tenant GUID.
     * @param clientId the client ID.
     * @param authority the authority.
     */

    public StorageCredentialsTokenMSI(String accountName, String authEndpoint, String tenantGuid, String clientId, String authority) {
        this.accountName = accountName;
        this.authEndpoint = (authEndpoint==null || authEndpoint.trim().length()==0)?AuthConfigurations.DEFAULT_FS_AZURE_ACCOUNT_OAUTH_MSI_ENDPOINT:authEndpoint;
        this.tenantGuid = tenantGuid;
        this.authority = (authority==null || authority.trim().length()==0)?AuthConfigurations.DEFAULT_FS_AZURE_ACCOUNT_OAUTH_MSI_AUTHORITY:authority;
        this.clientId = clientId;

        if (!this.authority.endsWith(AbfsHttpConstants.FORWARD_SLASH)) {
            this.authority = this.authority + AbfsHttpConstants.FORWARD_SLASH;
        }
        this.tokenProvider = new MsiTokenProvider(this.authEndpoint, this.tenantGuid,
                this.clientId, this.authority);

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