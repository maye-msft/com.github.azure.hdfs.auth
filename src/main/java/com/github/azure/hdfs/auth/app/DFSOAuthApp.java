package com.github.azure.hdfs.auth.app;

import java.io.InputStream;
import java.net.URI;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;

public class DFSOAuthApp {

    public static void main(String[] args) throws Exception {
        String uri = args[0];
        Configuration conf = new Configuration();
        conf.set("fs.azure.account.auth.type", "OAuth");
        conf.set("fs.azure.account.oauth.provider.type", args[1]);
        conf.set("fs.azure.account.oauth2.client.endpoint", args[2]);
        conf.set("fs.azure.account.oauth2.client.id", args[3]);
        conf.set("fs.azure.account.oauth2.client.secret", args[4]);

        FileSystem fs = FileSystem.get(URI.create(uri), conf);
        InputStream in = null;
        try {
            in = fs.open(new Path(uri));
            IOUtils.copyBytes(in, System.out, 4096, false);
        } finally {
            IOUtils.closeStream(in);
        }
    }
}
