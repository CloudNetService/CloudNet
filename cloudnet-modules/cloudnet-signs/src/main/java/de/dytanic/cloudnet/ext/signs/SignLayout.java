package de.dytanic.cloudnet.ext.signs;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class SignLayout {

  protected String[] lines;

  protected String blockType;

  protected int subId;

  public SignLayout(String[] lines, String blockType, int subId) {
    this.lines = lines;
    this.blockType = blockType;
    this.subId = subId;
  }

  public SignLayout() {
  }

  public String[] getLines() {
    return this.lines;
  }

  public void setLines(String[] lines) {
    this.lines = lines;
  }

  public String getBlockType() {
    return this.blockType;
  }

  public void setBlockType(String blockType) {
    this.blockType = blockType;
  }

  public int getSubId() {
    return this.subId;
  }

  public void setSubId(int subId) {
    this.subId = subId;
  }

}
