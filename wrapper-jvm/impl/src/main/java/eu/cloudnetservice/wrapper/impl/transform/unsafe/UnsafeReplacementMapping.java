/*
 * Copyright 2019-2024 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cloudnetservice.wrapper.impl.transform.unsafe;

import java.io.IOException;
import java.lang.classfile.AnnotationElement;
import java.lang.classfile.AnnotationValue;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassFileElement;
import java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.AccessFlag;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A mapping for fields and methods of {@code sun.misc.Unsafe} to their replacement methods in
 * {@code UnsafeReplacementDelegate}. Note that fields and methods in the replacement delegate class use the same type
 * as the member that they are replacing.
 *
 * @since 4.0
 */
final class UnsafeReplacementMapping {

  private static final Logger LOGGER = LoggerFactory.getLogger(UnsafeReplacementMapping.class);

  private static final String PKG = UnsafeReplacementMapping.class.getPackageName().replace('.', '/');
  private static final String CN_UNSAFE_REPLACEMENT_DELEGATE = PKG + "/UnsafeReplacementDelegate";
  private static final ClassDesc CD_UNSAFE_REPLACEMENT = ClassDesc.ofInternalName(PKG + "/UnsafeReplacement");

  private final Map<Key, FieldReplacement> fieldReplacements;
  private final Map<Key, MethodReplacement> methodReplacements;

  /**
   * Constructs a new unsafe replacement mapping.
   *
   * @param fieldReplacements  the replacements for fields in {@code sun.misc.Unsafe}.
   * @param methodReplacements the replacements for methods in {@code sun.misc.Unsafe}.
   * @throws NullPointerException if the given field or method replacements mapping is null.
   */
  private UnsafeReplacementMapping(
    @NonNull Map<Key, FieldReplacement> fieldReplacements,
    @NonNull Map<Key, MethodReplacement> methodReplacements
  ) {
    this.fieldReplacements = fieldReplacements;
    this.methodReplacements = methodReplacements;
  }

  /**
   * Loads the replacement mappings by inspecting the fields and methods in {@code UnsafeReplacementDelegate}.
   *
   * @return the replacement mapping for methods in {@code sun.misc.Unsafe}.
   */
  public static @NonNull UnsafeReplacementMapping load() {
    var fieldReplacements = new HashMap<Key, FieldReplacement>();
    var methodReplacements = new HashMap<Key, MethodReplacement>();

    //
    var cl = UnsafeReplacementMapping.class.getClassLoader();
    var unsafeReplacementFile = CN_UNSAFE_REPLACEMENT_DELEGATE + ".class";
    try (var replacementDelegateStream = cl.getResourceAsStream(unsafeReplacementFile)) {
      Objects.requireNonNull(replacementDelegateStream, "UnsafeReplacementDelegate class missing");

      // parse the unsafe delegate class file
      var classBytes = replacementDelegateStream.readAllBytes();
      var classFile = ClassFile.of();
      var classModel = classFile.parse(classBytes);

      // load replaced fields
      for (var target : classModel.fields()) {
        var flags = target.flags();
        var isPublicStatic = flags.has(AccessFlag.PUBLIC) && flags.has(AccessFlag.STATIC);
        var replacedFieldName = target.elementStream()
          .map(UnsafeReplacementMapping::replacementName)
          .filter(Objects::nonNull)
          .findFirst()
          .orElse(null);
        if (isPublicStatic && replacedFieldName != null) {
          var fieldName = target.fieldName().stringValue();
          var replacedFieldDescriptor = target.fieldTypeSymbol();
          var fieldReplacement = new FieldReplacement(replacedFieldName, fieldName, replacedFieldDescriptor);
          fieldReplacements.put(fieldReplacement.key(), fieldReplacement);
          LOGGER.debug("Registering unsafe field replacement: {}", fieldReplacement);
        }
      }

      // load replacement methods
      for (var target : classModel.methods()) {
        var flags = target.flags();
        var isPublicStatic = flags.has(AccessFlag.PUBLIC) && flags.has(AccessFlag.STATIC);
        var replacedMethodName = target.elementStream()
          .map(UnsafeReplacementMapping::replacementName)
          .filter(Objects::nonNull)
          .findFirst()
          .orElse(null);
        if (isPublicStatic && replacedMethodName != null) {
          var methodName = target.methodName().stringValue();
          var replacedMethodDesc = target.methodTypeSymbol();
          var methodReplacement = new MethodReplacement(replacedMethodName, methodName, replacedMethodDesc);
          methodReplacements.put(methodReplacement.key(), methodReplacement);
          LOGGER.debug("Registering unsafe method replacement: {}", methodReplacement);
        }
      }
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot load unsafe replacement mapping", exception);
    }

    return new UnsafeReplacementMapping(fieldReplacements, methodReplacements);
  }

  /**
   * Get the name of the member that replaces the given element, null if the element has no replacement.
   *
   * @param element the element to get the replacement member name of.
   * @return the name of the member that replaces the element, null if none.
   * @throws NullPointerException if the given element is null.
   */
  private static @Nullable String replacementName(@NonNull ClassFileElement element) {
    if (element instanceof RuntimeVisibleAnnotationsAttribute rva) {
      var annotations = rva.annotations();
      var unsafeReplacementAnnotation = annotations.stream()
        .filter(annotation -> annotation.classSymbol().equals(CD_UNSAFE_REPLACEMENT))
        .findFirst()
        .orElse(null);
      if (unsafeReplacementAnnotation != null) {
        return unsafeReplacementAnnotation.elements().stream()
          .filter(ae -> ae.name().equalsString("name"))
          .map(AnnotationElement::value)
          .map(av -> av instanceof AnnotationValue.OfString strVal ? strVal.stringValue() : null)
          .filter(Objects::nonNull)
          .findFirst()
          .orElse(null);
      }
    }

    return null;
  }

  /**
   * Get the name of the field in {@code UnsafeReplacementDelegate} that replaces the field in {@code sun.misc.Unsafe}
   * with the given name and type. The replacement field can only be accessed once from this mapping.
   *
   * @param name the name of the field to get the replacement field name of.
   * @param type the type of the field to get the replacement field name of.
   * @return the name of the field in {@code UnsafeReplacementDelegate} that replaces the given field.
   * @throws NullPointerException if the given name or type is null.
   */
  public @Nullable String replacementFieldName(@NonNull String name, @NonNull ClassDesc type) {
    var key = new Key(name, type);
    var replacement = this.fieldReplacements.remove(key);
    return replacement == null ? null : replacement.replacementName();
  }

  /**
   * Get the name of the method in {@code UnsafeReplacementDelegate} that replaces the method in {@code sun.misc.Unsafe}
   * with the given name and method type. The replacement method can only be accessed once from this mapping.
   *
   * @param name       the name of the method to get the replacement method name of.
   * @param methodType the method type of the method to get the replacement method name of.
   * @return the name of the method in {@code UnsafeReplacementDelegate} that replaces the given method.
   * @throws NullPointerException if the given name or method type is null.
   */
  public @Nullable String replacementMethodName(@NonNull String name, @NonNull MethodTypeDesc methodType) {
    var key = new Key(name, methodType);
    var replacement = this.methodReplacements.remove(key);
    return replacement == null ? null : replacement.replacementName();
  }

  /**
   * Get the registered field replacements.
   *
   * @return the registered field replacements.
   */
  public @NonNull Collection<FieldReplacement> fieldReplacements() {
    return this.fieldReplacements.values();
  }

  /**
   * Get the registered method replacements.
   *
   * @return the registered method replacements.
   */
  public @NonNull Collection<MethodReplacement> methodReplacements() {
    return this.methodReplacements.values();
  }

  /**
   * A key for a method or field. Composed of the name and a member-specific key.
   *
   * @param name      the name of the method or field.
   * @param memberKey the member-specific key.
   */
  public record Key(@NonNull String name, @NonNull Object memberKey) {

  }

  /**
   * A replacement for a field.
   *
   * @param sourceName      the name of the field in {@code sun.misc.Unsafe} that is being replaced.
   * @param replacementName the name of the replacement field in {@code UnsafeReplacementDelegate}.
   * @param type            the type of the field in {@code sun.misc.Unsafe} that is being replaced.
   */
  public record FieldReplacement(
    @NonNull String sourceName,
    @NonNull String replacementName,
    @NonNull ClassDesc type
  ) {

    /**
     * Constructs a new key from this field replacement.
     *
     * @return a new key from this field replacement.
     */
    public @NonNull Key key() {
      return new Key(this.sourceName, this.type);
    }
  }

  /**
   * A replacement for a method.
   *
   * @param sourceName      the name of the method in {@code sun.misc.Unsafe} that is being replaced.
   * @param replacementName the name of the replacement method in {@code UnsafeReplacementDelegate}.
   * @param type            the method type of the method in {@code sun.misc.Unsafe} that is being replaced.
   */
  public record MethodReplacement(
    @NonNull String sourceName,
    @NonNull String replacementName,
    @NonNull MethodTypeDesc type
  ) {

    /**
     * Constructs a new key from this method replacement.
     *
     * @return a new key from this method replacement.
     */
    public @NonNull Key key() {
      return new Key(this.sourceName, this.type);
    }
  }
}
