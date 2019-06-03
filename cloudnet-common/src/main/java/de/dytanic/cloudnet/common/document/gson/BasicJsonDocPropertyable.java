package de.dytanic.cloudnet.common.document.gson;

import lombok.Getter;

public class BasicJsonDocPropertyable implements IJsonDocPropertyable {

  @Getter
  protected JsonDocument properties = new JsonDocument();

  @Override
  public <E> IJsonDocPropertyable setProperty(JsonDocProperty<E> docProperty,
      E val) {
    properties.setProperty(docProperty, val);
    return this;
  }

  @Override
  public <E> E getProperty(JsonDocProperty<E> docProperty) {
    return properties.getProperty(docProperty);
  }

  @Override
  public <E> IJsonDocPropertyable removeProperty(
      JsonDocProperty<E> docProperty) {
    properties.removeProperty(docProperty);
    return this;
  }

  @Override
  public <E> boolean hasProperty(JsonDocProperty<E> docProperty) {
    return docProperty.tester.test(properties);
  }
}