package de.dytanic.cloudnet.driver.network.cluster;

import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class NetworkClusterNodeExtensionSnapshot extends
    BasicJsonDocPropertyable {

  protected String group, name, version, author, website, description;

}