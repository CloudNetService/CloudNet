package de.dytanic.cloudnet.conf;

import java.lang.reflect.Type;

public interface IConfigurationRegistry {

  IConfigurationRegistry put(String key, Object object);

  IConfigurationRegistry put(String key, String string);

  IConfigurationRegistry put(String key, Number number);

  IConfigurationRegistry put(String key, Boolean bool);

  IConfigurationRegistry put(String key, byte[] bytes);

  IConfigurationRegistry remove(String key);

  boolean contains(String key);

  <T> T getObject(String key, Class<T> clazz);

  <T> T getObject(String key, Type type);

  String getString(String key);

  String getString(String key, String def);

  Integer getInt(String key);

  Integer getInt(String key, Integer def);

  Double getDouble(String key);

  Double getDouble(String key, Double def);

  Short getShort(String key);

  Short getShort(String key, Short def);

  Long getLong(String key);

  Long getLong(String key, Long def);

  Boolean getBoolean(String key);

  Boolean getBoolean(String key, Boolean def);

  byte[] getBytes(String key);

  byte[] getBytes(String key, byte[] bytes);

  IConfigurationRegistry save();

  IConfigurationRegistry load();

}
