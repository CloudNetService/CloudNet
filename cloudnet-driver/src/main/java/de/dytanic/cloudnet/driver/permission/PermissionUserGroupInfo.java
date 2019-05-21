package de.dytanic.cloudnet.driver.permission;

import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PermissionUserGroupInfo extends BasicJsonDocPropertyable {

    protected String group;

    protected long timeOutMillis;

}