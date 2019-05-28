package de.dytanic.cloudnet.ext.signs;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import java.lang.reflect.Type;
import lombok.Data;

@Data
public class Sign implements Comparable<Sign> {

  public static final Type TYPE = new TypeToken<Sign>() {
  }.getType();

  protected long signId;

  protected String providedGroup, targetGroup, templatePath;

  protected SignPosition worldPosition;

  //*=-- ---------------------------------------------------

  private volatile ServiceInfoSnapshot serviceInfoSnapshot;

  public Sign(String providedGroup, String targetGroup,
      SignPosition worldPosition, String templatePath) {
    this.signId = System.currentTimeMillis();
    //=- * | * -=//
    this.providedGroup = providedGroup;
    this.targetGroup = targetGroup;
    this.templatePath = templatePath;
    this.worldPosition = worldPosition;
  }

  @Override
  public int compareTo(Sign o) {
    return Long.compare(signId, o.getSignId());
  }
}