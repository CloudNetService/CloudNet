package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.driver.network.http.HttpVersion;

public class NettyHttpMessage {

    protected HttpVersion getCloudNetHttpVersion(io.netty.handler.codec.http.HttpVersion httpVersion) {
        if (httpVersion == io.netty.handler.codec.http.HttpVersion.HTTP_1_0) {
            return HttpVersion.HTTP_1_0;
        }

        if (httpVersion == io.netty.handler.codec.http.HttpVersion.HTTP_1_1) {
            return HttpVersion.HTTP_1_1;
        }

        return HttpVersion.HTTP_1_0;
    }

    protected io.netty.handler.codec.http.HttpVersion getNettyHttpVersion(HttpVersion httpVersion) {
        if (httpVersion == HttpVersion.HTTP_1_0) {
            return io.netty.handler.codec.http.HttpVersion.HTTP_1_0;
        }

        if (httpVersion == HttpVersion.HTTP_1_1) {
            return io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
        }

        return io.netty.handler.codec.http.HttpVersion.HTTP_1_0;
    }

}
