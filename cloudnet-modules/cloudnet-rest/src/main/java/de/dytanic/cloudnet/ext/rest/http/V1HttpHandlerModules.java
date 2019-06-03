package de.dytanic.cloudnet.ext.rest.http;

import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;
import de.dytanic.cloudnet.driver.module.ModuleConfiguration;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.http.V1HttpHandler;

import java.util.function.Function;
import java.util.function.Predicate;

public final class V1HttpHandlerModules extends V1HttpHandler {

    public V1HttpHandlerModules(String permission)
    {
        super(permission);
    }

    @Override
    public void handleOptions(String path, IHttpContext context) throws Exception
    {
        this.sendOptions(context, "OPTIONS, GET");
    }

    @Override
    public void handleGet(String path, IHttpContext context) throws Exception
    {
        if (context.request().pathParameters().containsKey("name"))
            context
                .response()
                .header("Content-Type", "application/json")
                .statusCode(HttpResponseCode.HTTP_OK)
                .body(GSON.toJson(Iterables.filter(CloudNetDriver.getInstance().getModuleProvider().getModules(), new Predicate<IModuleWrapper>() {
                    @Override
                    public boolean test(IModuleWrapper moduleWrapper)
                    {
                        return context.request().pathParameters().get("name").contains(moduleWrapper.getModuleConfiguration().getName());
                    }
                })))
                ;
        else
            context
                .response()
                .header("Content-Type", "application/json")
                .statusCode(200)
                .body(GSON.toJson(Iterables.filter(Iterables.map(CloudNetDriver.getInstance().getModuleProvider().getModules(), new Function<IModuleWrapper, ModuleConfiguration>() {
                    @Override
                    public ModuleConfiguration apply(IModuleWrapper moduleWrapper)
                    {
                        return moduleWrapper.getModuleConfiguration();
                    }
                }), new Predicate<ModuleConfiguration>() {
                    @Override
                    public boolean test(ModuleConfiguration moduleConfiguration)
                    {
                        if (context.request().queryParameters().containsKey("group") &&
                            !containsStringElementInCollection(context.request().queryParameters().get("group"), moduleConfiguration.getGroup()))
                            return false;

                        if (context.request().queryParameters().containsKey("name") &&
                            !containsStringElementInCollection(context.request().queryParameters().get("name"), moduleConfiguration.getName()))
                            return false;

                        if (context.request().queryParameters().containsKey("version") &&
                            !containsStringElementInCollection(context.request().queryParameters().get("version"), moduleConfiguration.getVersion()))
                            return false;

                        return true;
                    }
                })))
                .context()
                .closeAfter(true)
                .cancelNext()
                ;
    }
}