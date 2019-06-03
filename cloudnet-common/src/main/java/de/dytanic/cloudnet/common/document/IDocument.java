package de.dytanic.cloudnet.common.document;

import de.dytanic.cloudnet.common.document.gson.IJsonDocPropertyable;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

/**
 * A document is a wrapper to persistence data or read data in the heap or easy
 * into the following implementation format of this interface.
 */
public interface IDocument<T extends IDocument> extends IJsonDocPropertyable,
    Serializable, IPersistable, IReadable, Iterable<String> {

  Collection<String> keys();

  int size();

  T clear();

  T remove(String key);

  boolean contains(String key);

  <T> T toInstanceOf(Class<T> clazz);

  <T> T toInstanceOf(Type clazz);

  /*= --------------------------------------------------- =*/

  T append(String key, Object value);

  T append(String key, Number value);

  T append(String key, Boolean value);

  T append(String key, String value);

  T append(String key, Character value);

  T append(String key, T value);

  T append(Properties properties);

  T append(Map<String, Object> map);

  T append(String key, Properties properties);

  T append(String key, byte[] bytes);

  T append(T t);

  T getDocument(String key);

  int getInt(String key);

  double getDouble(String key);

  float getFloat(String key);

  byte getByte(String key);

  short getShort(String key);

  long getLong(String key);

  boolean getBoolean(String key);

  String getString(String key);

  char getChar(String key);

  BigDecimal getBigDecimal(String key);

  BigInteger getBigInteger(String key);

  Properties getProperties(String key);

  byte[] getBinary(String key);

  <T> T get(String key, Class<T> clazz);

  <T> T get(String key, Type type);

  /*= --------------------------------------------------- =*/

  default boolean isEmpty() {
    return this.size() == 0;
  }
}