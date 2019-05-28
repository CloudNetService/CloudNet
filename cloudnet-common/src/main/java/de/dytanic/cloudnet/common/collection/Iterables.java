package de.dytanic.cloudnet.common.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The base utility class for some operations, which doesn't exist in the jdk 8
 */
public final class Iterables {

  private Iterables() {
    throw new UnsupportedOperationException();
  }

  public static <T> ArrayList<T> newArrayList() {
    return new ArrayList<>(1);
  }

  public static <T> ArrayList<T> newArrayList(Collection<T> collection) {
    return new ArrayList<>(collection);
  }

  public static <T> ArrayList<T> newArrayList(int capacity) {
    return new ArrayList<>(capacity);
  }

  public static <T> ArrayList<T> newArrayList(T[] entries) {
    ArrayList<T> list = new ArrayList<>(entries.length);

    for (T entry : entries) {
      list.add(entry);
    }

    return list;
  }

  public static <T> HashSet<T> newHashSet() {
    return new HashSet<>(0);
  }

  public static <T> HashSet<T> newHashSet(Collection<T> collection) {
    return new HashSet<>(collection);
  }

  public static <T> HashSet<T> newHashSet(int capacity) {
    return new HashSet<>(capacity);
  }

  public static <T> LinkedList<T> newLinkedList() {
    return new LinkedList<>();
  }

  public static <T> LinkedList<T> newLinkedList(Collection<T> collection) {
    return new LinkedList<>(collection);
  }

  public static <T> CopyOnWriteArrayList<T> newCopyOnWriteArrayList() {
    return new CopyOnWriteArrayList<>();
  }

  public static <T> CopyOnWriteArrayList<T> newCopyOnWriteArrayList(
    Collection<T> collection) {
    return new CopyOnWriteArrayList<>(collection);
  }

  public static <T> ConcurrentLinkedQueue<T> newConcurrentLinkedQueue() {
    return new ConcurrentLinkedQueue<>();
  }

  public static <T> ConcurrentLinkedQueue<T> newConcurrentLinkedQueue(
    Collection<T> collection) {
    return new ConcurrentLinkedQueue<>(collection);
  }

  public static <T> LinkedBlockingQueue<T> newLinkedBlockingQueue() {
    return new LinkedBlockingQueue<>();
  }

  public static <T> LinkedBlockingQueue<T> newLinkedBlockingQueue(
    Collection<T> collection) {
    return new LinkedBlockingQueue<>(collection);
  }

  public static <T> ConcurrentLinkedDeque<T> newConcurrentLinkedDeque() {
    return new ConcurrentLinkedDeque<>();
  }

  public static <T> ConcurrentLinkedDeque<T> newConcurrentLinkedDeque(
    Collection<T> collection) {
    return new ConcurrentLinkedDeque<>(collection);
  }

  public static <T> LinkedBlockingDeque<T> newLinkedBlockingDeque() {
    return new LinkedBlockingDeque<>();
  }

  public static <T> LinkedBlockingDeque<T> newLinkedBlockingDeque(
    Collection<T> collection) {
    return new LinkedBlockingDeque<>(collection);
  }

  public static <T> ArrayBlockingQueue<T> newArrayBlockingQueue(int capacity) {
    return new ArrayBlockingQueue<>(capacity);
  }

  public static <T> ArrayBlockingQueue<T> newArrayBlockingQueue(int capacity,
    boolean fair) {
    return new ArrayBlockingQueue<>(capacity, fair);
  }

  public static <T> ArrayBlockingQueue<T> newArrayBlockingQueue(int capacity,
    boolean fair, Collection<T> collection) {
    return new ArrayBlockingQueue<>(capacity, fair, collection);
  }

  /*= ------------------------------------------------------------------------------------------------------------ =*/

  /**
   * Iterates all keys in a Properties class instance.
   *
   * @param properties the properties which should iterate
   * @param consumer the handler which handle the following keys
   */
  public static void forEach(Properties properties, Consumer<String> consumer) {
    if (properties == null || consumer == null) {
      return;
    }

    forEach(properties.propertyNames(), new Consumer() {
      @Override
      public void accept(Object o) {
        if (o != null) {
          consumer.accept(o.toString());
        }
      }
    });
  }

  /**
   * Iterates all keys in a Enumeration<T> class instance.
   *
   * @param enumeration the items which should iterate
   * @param consumer the handler which handle the following keys
   */
  public static <T> void forEach(Enumeration<T> enumeration,
    Consumer<T> consumer) {
    if (enumeration == null || consumer == null) {
      return;
    }

    forEach(enumeration, consumer, null);
  }

  /**
   * Iterates all keys in a List class instance.
   *
   * @param list the items which should iterate
   * @param consumer the handler which handle the following keys
   */
  public static <T> void forEach(List<T> list, Consumer<T> consumer) {
    if (list == null || consumer == null) {
      return;
    }

    for (int i = 0; i < list.size(); i++) {
      consumer.accept(list.get(i));
    }
  }

  /**
   * Iterates all keys in a Enumeration<T> class instance.
   *
   * @param enumeration the items which should iterate
   * @param consumer the handler which handle the following keys
   * @param throwableConsumer will called if the consumer handler throws an
   * Throwable
   */
  public static <T> void forEach(Enumeration<T> enumeration,
    Consumer<T> consumer, Consumer<Throwable> throwableConsumer) {
    while (enumeration.hasMoreElements()) {
      try {
        consumer.accept(enumeration.nextElement());
      } catch (Throwable th) {
        if (throwableConsumer != null) {
          throwableConsumer.accept(th);
        } else {
          th.printStackTrace();
        }
      }
    }
  }


  /**
   * Iterates all keys in a List class instance.
   *
   * @param iterator the items which should iterate
   * @param consumer the handler which handle the following keys
   */
  public static <T> void forEach(Iterator<T> iterator, Consumer<T> consumer) {
    forEach(iterator, consumer, null);
  }

  /**
   * Iterates all keys in a Iterator<T> class instance.
   *
   * @param iterator the items which should iterate
   * @param consumer the handler which handle the following keys
   * @param throwableConsumer will called if the consumer handler throws an
   * Throwable
   */
  public static <T> void forEach(Iterator<T> iterator, Consumer<T> consumer,
    Consumer<Throwable> throwableConsumer) {
    while (iterator.hasNext()) {
      try {
        consumer.accept(iterator.next());
      } catch (Throwable th) {
        if (throwableConsumer != null) {
          throwableConsumer.accept(th);
        } else {
          th.printStackTrace();
        }
      }
    }
  }

  /**
   * Filters the first item in an Iterable, which the predicate returns true
   */
  public static <T> T first(Iterable<T> iterable, Predicate<T> predicate) {
    if (iterable == null || predicate == null) {
      return null;
    }

    for (T entry : iterable) {
      if (predicate.test(entry)) {
        return entry;
      }
    }

    return null;
  }

  /**
   *
   */
  public static <T> T first(T[] iterable, Predicate<T> predicate) {
    if (iterable == null || predicate == null) {
      return null;
    }

    for (T entry : iterable) {
      if (predicate.test(entry)) {
        return entry;
      }
    }

    return null;
  }

  public static <T> List<T> filter(Iterable<T> iterable,
    Predicate<T> predicate) {
    if (iterable == null || predicate == null) {
      return newArrayList();
    }

    List<T> collection = newArrayList();

    for (T entry : iterable) {
      if (predicate.test(entry)) {
        collection.add(entry);
      }
    }

    return collection;
  }

  public static <T> void filter(Iterable<T> iterable, Predicate<T> predicate,
    Collection<T> out) {
    if (iterable == null || predicate == null || out == null) {
      return;
    }

    for (T entry : iterable) {
      if (predicate.test(entry)) {
        out.add(entry);
      }
    }
  }

  public static <T, V> List<V> map(T[] array, Function<T, V> function) {
    if (array == null || function == null) {
      return null;
    }

    List<V> collection = newArrayList(array.length);

    if (function == null) {
      return collection;
    }

    for (T entry : array) {
      collection.add(function.apply(entry));
    }

    return collection;
  }

  public static <T, V> List<V> map(Collection<T> coll,
    Function<T, V> function) {
    if (coll == null || function == null) {
      return null;
    }

    List<V> collection = newArrayList(coll.size());

    if (function == null) {
      return collection;
    }

    for (T entry : coll) {
      collection.add(function.apply(entry));
    }

    return collection;
  }

  public static <K, T> T reduce(K[] array, BiFunction<T, K, T> function, T t) {
    if (array == null || function == null || t == null) {
      return t;
    }

    T item = t;

    for (K entry : array) {
      item = function.apply(t, entry);
    }

    return item;
  }

  public static <K, T> T reduce(Collection<K> collection,
    BiFunction<T, K, T> function, T t) {
    if (collection == null || function == null || t == null) {
      return t;
    }

    T item = t;

    for (K entry : collection) {
      item = function.apply(t, entry);
    }

    return item;
  }

  public static <T> boolean contains(T t, T[] array) {
    if (array == null) {
      return false;
    }

    for (T item : array) {
      if (item != null && item.equals(t)) {
        return true;
      }
    }

    return false;
  }
}
