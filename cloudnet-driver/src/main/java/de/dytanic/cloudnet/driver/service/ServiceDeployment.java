package de.dytanic.cloudnet.driver.service;

import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.Collection;

@Getter
@ToString
@RequiredArgsConstructor
public final class ServiceDeployment extends BasicJsonDocPropertyable {

    private final ServiceTemplate template;

    private final Collection<String> excludes;

}