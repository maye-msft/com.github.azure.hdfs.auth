# Access HDFS Blob with MSI

This is repo is to introduce how to access HDFS Blob with [MSI](https://learn.microsoft.com/en-us/azure/active-directory/managed-identities-azure-resources/overview).

## Problem Statement

With the tutorials,

https://hadoop.apache.org/docs/stable/hadoop-azure/abfs.html

https://medium.com/azure-data-lake/connecting-your-own-hadoop-or-spark-to-azure-data-lake-store-93d426d6a5f4

We can access ADLS Gen2 with Hadoop client via OAuth2 or MSI such as

```bash
hadoop fs -ls abfss://<container-name>@<storage-account>.dfs.core.windows.net/<file-name>
```

***But we can't access Blob with Hadoop client via OAuth2 or MSI***

```bash
hadoop fs -ls wasbs://<container-name>@<storage-account>.blob.core.windows.net/<file-name>
```

In some case, Blob storage is the storage layer of Hadoop, OAuth and MSI is required. While it is not supported by Hadoop Azure production version.

## Solution

The solution contains two parts:

1. Azure Blob SDK, I changed and added some code below

   - com.microsoft.azure.storage.StorageCredentialsToken

        It supports access blob with AccessToken, but it is not supported refresh token, in OAuth2, we need to refresh token before it expires. While this class is final and not hard code in the class ***com.microsoft.azure.storage.core.StorageCredentialsHelper***, Here we need add an interface for it.

   - com.microsoft.azure.storage.IStorageCredentialsToken

        This is the interface for StorageCredentialsToken, and we put it into ***com.microsoft.azure.storage.core.StorageCredentialsHelper*** so that it can work with OAuth and MSI implementations.

   - com.microsoft.azure.storage.core.StorageCredentialsHelper

        Originally, it check if the class of credentials is StorageCredentialsToken, I changed it into check if the credentials is implementation of IStorageCredentialsToken.

2. Hadoop Azure, I added implementation of OAuth2 and MSI

    - org.apache.hadoop.fs.azure.StorageCredentialsTokenOAuth

        It is the implementation of OAuth2, it uses class in ***org.apache.hadoop.fs.azurebfs.oauth2*** to get AccessToken and refresh token.

    - org.apache.hadoop.fs.azure.StorageCredentialsTokenMSI

        It is the implementation of MSI, it uses class in ***org.apache.hadoop.fs.azurebfs.oauth2*** to get AccessToken and refresh token.

    - org.apache.hadoop.fs.azure.AzureNativeFileSystemStore

        This class need to be modified to support OAuth2 and MSI, which read configuration and then route to OAuth2 or MSI implementation.

Notes:

- Why need to change Storage SDK?
Hadoop Azure use Storage SDK to access Blob, but Storage SDK doesn't support OAuth2 and MSI. It is hard to build a new Storage client in Hadoop Azure.

- Why not add MSI and OAuth into Storage SDK?
The OAuth and MSI reuse the implementation in Hadoop Azure.


## Build Jars

```bash
mvn clean package
```

- hadoop-azure-3.3.2-blob-oauth-1.0-SNAPSHOT-adfs.jar
the replacement of hadoop-azure-3.3.2.jar

- azure-storage-8.6.4-blob-oauth-1.0-SNAPSHOT-sdk.jar
the replacement of azure-storage-8.6.4.jar

## Demonstration

1. Run OAuth Demo

```bash
mvn exec:java -Dexec.mainClass="com.github.azure.hdfs.auth.app.BlobOAuthApp" -Dexec.args="wasbs://<container-name>@<storage-account>.blob.core.windows.net/<file-name> https://login.microsoftonline.com/<tenant-id>/oauth2/token <client-id> <client-secret>"
```

1. Run MSI Demo, run this command in Azure VM with MSI enabled.

```bash
mvn exec:java -Dexec.mainClass="com.github.azure.hdfs.auth.app.BlobMSIApp" -Dexec.args="wasbs://<container-name>@<storage-account>.blob.core.windows.net/<file-name>"
```

1. Run OAuth with Hadoop CLI
Add the following configuration into ***core-site.xml***, and then run the following command

```bash
hadoop fs -ls wasbs://<container-name>@<storage-account>.blob.core.windows.net/<file-name>
```

```xml
<property>
    <name>blob.azure.account.auth.type</name>
    <value>OAuth</value>
</property>
<property>
    <name>blob.azure.account.oauth2.client.endpoint</name>
    <value>https://login.microsoftonline.com/<!--tenant-id-->/oauth2/token</value>
</property>
<property>
    <name>blob.azure.account.oauth2.client.id</name>
    <value><!--client-id--></value>
</property>
<property>
    <name>blob.azure.account.oauth2.client.secret</name>
    <value><!--client-secret--></value>
</property>
```

1. Run MSI with Hadoop CLI
Add the following configuration into ***core-site.xml***, and then run the following command

```bash
hadoop fs -ls wasbs://<container-name>@<storage-account>.blob.core.windows.net/<file-name>
```

```xml
<property>
    <name>blob.azure.account.auth.type</name>
    <value>MSI</value>
    <description>
    Use MSI authentication
    </description>
</property>
<!-- the below are optional properties -->
<property>
    <name>blob.azure.account.oauth2.msi.tenant</name>
    <value></value>
    <description>
    Optional MSI Tenant ID
    </description>
</property>
<property>
    <name>blob.azure.account.oauth2.msi.endpoint</name>
    <value></value>
    <description>
    Optional MSI endpoint
    </description>
</property>
<property>
    <name>blob.azure.account.oauth2.client.id</name>
    <value></value>
    <description>
    Optional Client ID
    </description>
</property> 
```

Refer to the repo below for setup MSI blob and VM with Azure CLI

https://github.com/maye-msft/Azure-MSI-VNET-Storage-VM-ACI
