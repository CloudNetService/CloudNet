package de.dytanic.cloudnet.launcher.util;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@EqualsAndHashCode
@AllArgsConstructor
@RequiredArgsConstructor
public class Dependency {

    private final String repository, group, name, version;

    private String classifier;


}