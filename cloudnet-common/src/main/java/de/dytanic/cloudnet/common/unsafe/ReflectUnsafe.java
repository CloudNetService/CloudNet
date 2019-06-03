package de.dytanic.cloudnet.common.unsafe;

import de.dytanic.cloudnet.common.annotation.UnsafeClass;
import lombok.Getter;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * This class allows you to use the Unsafe class from the sun.misc package
 * and gives access to the object, which is available at runtime in
 * order to implement complex problems that can be solved with it.
 *
 * @see sun.misc.Unsafe
 */
@UnsafeClass
public final class ReflectUnsafe {

    /**
     * The Unsafe object reference of the private field "theUnsafe"
     */
    @Getter
    private static Unsafe unsafe;

    static
    {
        try
        {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

}