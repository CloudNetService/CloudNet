package de.dytanic.cloudnet.service.handler;

import de.dytanic.cloudnet.service.ICloudService;

public interface CloudServiceHandler {

    boolean handlePreDelete(ICloudService service);

    void handlePostDelete(ICloudService service);

    boolean handlePrePrepare(ICloudService service);

    void handlePostPrepare(ICloudService service);

    boolean handlePrePrepareStart(ICloudService service);

    void handlePostPrepareStart(ICloudService service);

    void handlePreStart(ICloudService service);

    void handlePostStart(ICloudService service);

    boolean handlePreStop(ICloudService service);

    void handlePostStop(ICloudService service, int exitValue);

}
