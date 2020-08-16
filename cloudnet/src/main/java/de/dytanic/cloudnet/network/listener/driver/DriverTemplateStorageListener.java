package de.dytanic.cloudnet.network.listener.driver;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.concurrent.ThrowableFunction;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.api.DriverAPICategory;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.FileInfo;
import de.dytanic.cloudnet.driver.template.SpecificTemplateStorage;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import de.dytanic.cloudnet.driver.template.TemplateStorageResponse;

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

public class DriverTemplateStorageListener extends CategorizedDriverAPIListener {
    public DriverTemplateStorageListener() {
        super(DriverAPICategory.TEMPLATE_STORAGE);

        super.registerHandler(DriverAPIRequestType.GET_TEMPLATES, (channel, packet, input) -> {
            Collection<ServiceTemplate> templates = this.read(input).getTemplates();
            return ProtocolBuffer.create().writeObjectCollection(templates);
        });

        super.registerHandler(DriverAPIRequestType.CLOSE_STORAGE, this.throwableResponseHandler(input -> {
            this.read(input).close();
            return TemplateStorageResponse.SUCCESS;
        }));

        super.registerHandler(DriverAPIRequestType.LIST_FILES, this.throwableHandler(input -> {
            FileInfo[] files = this.readSpecific(input).listFiles(input.readString());
            ProtocolBuffer buffer = ProtocolBuffer.create().writeEnumConstant(TemplateStorageResponse.SUCCESS);
            buffer.writeBoolean(files != null);
            if (files != null) {
                buffer.writeObjectArray(files);
            }
            return buffer;
        }));

        super.registerHandler(DriverAPIRequestType.CREATE_FILE, this.throwableResponseHandler(input -> TemplateStorageResponse.of(this.readSpecific(input).createFile(input.readString()))));
        super.registerHandler(DriverAPIRequestType.CREATE_DIRECTORY, this.throwableResponseHandler(input -> TemplateStorageResponse.of(this.readSpecific(input).createDirectory(input.readString()))));
        super.registerHandler(DriverAPIRequestType.CONTAINS_FILE, this.throwableResponseHandler(input -> TemplateStorageResponse.of(this.readSpecific(input).hasFile(input.readString()))));
        super.registerHandler(DriverAPIRequestType.DELETE_FILE, this.throwableResponseHandler(input -> TemplateStorageResponse.of(this.readSpecific(input).deleteFile(input.readString()))));

        super.registerHandler(DriverAPIRequestType.CONTAINS_TEMPLATE, this.throwableResponseHandler(input -> TemplateStorageResponse.of(this.readSpecific(input).exists())));
        super.registerHandler(DriverAPIRequestType.CREATE_TEMPLATE, this.throwableResponseHandler(input -> TemplateStorageResponse.of(this.readSpecific(input).create())));
        super.registerHandler(DriverAPIRequestType.DELETE_TEMPLATE, this.throwableResponseHandler(input -> TemplateStorageResponse.of(this.readSpecific(input).delete())));

        super.registerHandler(DriverAPIRequestType.DELETE_FILE, this.throwableResponseHandler(input -> TemplateStorageResponse.of(this.readSpecific(input).deleteFile(input.readString()))));

        super.registerHandler(DriverAPIRequestType.LOAD_TEMPLATE_ARRAY, this.throwableHandler(input -> ProtocolBuffer.create().writeEnumConstant(TemplateStorageResponse.SUCCESS).writeArray(this.readSpecific(input).toZipByteArray())));

        // TODO
        super.registerHandler(DriverAPIRequestType.DEPLOY_TEMPLATE_BYTE_ARRAY, (channel, packet, buffer) -> ProtocolBuffer.EMPTY);
        super.registerHandler(DriverAPIRequestType.DEPLOY_TEMPLATE_STREAM, (channel, packet, buffer) -> ProtocolBuffer.EMPTY);
        super.registerHandler(DriverAPIRequestType.LOAD_TEMPLATE_ARRAY, (channel, packet, buffer) -> ProtocolBuffer.EMPTY);
        super.registerHandler(DriverAPIRequestType.LOAD_TEMPLATE_STREAM, (channel, packet, buffer) -> ProtocolBuffer.EMPTY);
        super.registerHandler(DriverAPIRequestType.APPEND_FILE_CONTENT, (channel, packet, buffer) -> ProtocolBuffer.EMPTY);
        super.registerHandler(DriverAPIRequestType.SET_FILE_CONTENT, (channel, packet, buffer) -> ProtocolBuffer.EMPTY);
        super.registerHandler(DriverAPIRequestType.SHOULD_SYNC_IN_CLUSTER, (channel, packet, buffer) -> ProtocolBuffer.EMPTY);

        super.registerHandler(
                DriverAPIRequestType.GET_TEMPLATE_STORAGES,
                (channel, packet, input) -> ProtocolBuffer.create().writeStringCollection(
                        CloudNet.getInstance().getAvailableTemplateStorages().stream()
                                .map(INameable::getName)
                                .collect(Collectors.toList())
                )
        );

    }

    private DriverAPIHandler throwableHandler(ThrowableFunction<ProtocolBuffer, ProtocolBuffer, IOException> function) {
        return (channel, packet, input) -> {
            try {
                return function.apply(input);
            } catch (Response response) {
                return ProtocolBuffer.create().writeEnumConstant(response.getResponse());
            } catch (IOException exception) {
                return ProtocolBuffer.create().writeEnumConstant(TemplateStorageResponse.EXCEPTION).writeThrowable(exception);
            }
        };
    }

    private DriverAPIHandler throwableResponseHandler(ThrowableFunction<ProtocolBuffer, TemplateStorageResponse, IOException> function) {
        return (channel, packet, input) -> {
            try {
                return ProtocolBuffer.create().writeEnumConstant(function.apply(input));
            } catch (Response response) {
                return ProtocolBuffer.create().writeEnumConstant(response.getResponse());
            } catch (IOException exception) {
                return ProtocolBuffer.create().writeEnumConstant(TemplateStorageResponse.EXCEPTION).writeThrowable(exception);
            }
        };
    }

    private TemplateStorage read(ProtocolBuffer buffer) {
        TemplateStorage storage = CloudNetDriver.getInstance().getTemplateStorage(buffer.readString());
        if (storage == null) {
            throw new Response(TemplateStorageResponse.TEMPLATE_STORAGE_NOT_FOUND);
        }
        return storage;
    }

    private SpecificTemplateStorage readSpecific(ProtocolBuffer buffer) {
        SpecificTemplateStorage storage = buffer.readObject(ServiceTemplate.class).nullableStorage();
        if (storage == null) {
            throw new Response(TemplateStorageResponse.TEMPLATE_STORAGE_NOT_FOUND);
        }
        return storage;
    }

    private static class Response extends RuntimeException {

        private final TemplateStorageResponse response;

        public Response(TemplateStorageResponse response) {
            this.response = response;
        }

        public TemplateStorageResponse getResponse() {
            return this.response;
        }
    }


}
