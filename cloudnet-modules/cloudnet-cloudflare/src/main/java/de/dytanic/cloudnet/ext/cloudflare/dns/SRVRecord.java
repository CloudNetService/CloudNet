/*
 * Copyright (c) Tarek Hosni El Alaoui 2017
 */

package de.dytanic.cloudnet.ext.cloudflare.dns;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;

/**
 * A representation of an SRV DNS record
 */
public class SRVRecord extends DNSRecord {

    public SRVRecord(String name, String content, String service, String proto, String name_, int priority, int weight, int port, String target) {
        super(
                DNSType.SRV.name(),
                name,
                content,
                1,
                false,
                new JsonDocument()
                        .append("service", service)
                        .append("proto", proto)
                        .append("name", name_)
                        .append("priority", priority)
                        .append("weight", weight)
                        .append("port", port)
                        .append("target", target)
                        .toJsonObject()
        );
    }

}
