package de.dytanic.cloudnet.driver.module;

import lombok.*;

@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class ModuleDependency {

    private String repo, url, group, name, version;

}