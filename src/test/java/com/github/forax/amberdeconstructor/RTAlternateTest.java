package com.github.forax.amberdeconstructor;

import static java.lang.invoke.MethodType.methodType;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;import java.lang.invoke.MethodType;
import org.junit.jupiter.api.Test;

public class RTAlternateTest {
  static class Point {
    private int x;
    private int y;

    public Point(int x, int y) {
      this.x = x;
      this.y = y;
    }

    public static final Object CARRIER =
        RT.carrier(MethodHandles.lookup(), "", Object.class, methodType(void.class, int.class, int.class));
    private static final MethodHandle FACTORY =
        RT.carrierFactory(MethodHandles.lookup(), "", MethodHandle.class, CARRIER);

    public static /*matcher*/ Object Point(Point that, int x, int y) throws Throwable {
      return FACTORY.invokeExact(that.x, that.y);
    }
  }

  private static final MethodHandle POINT_ACCESSOR_0 =
      RT.carrierAccessor(MethodHandles.lookup(), "", MethodHandle.class, Point.CARRIER, 0);
  private static final MethodHandle POINT_ACCESSOR_1 =
      RT.carrierAccessor(MethodHandles.lookup(), "", MethodHandle.class, Point.CARRIER, 1);

  @Test
  public void testPoint() throws Throwable {
    var point = new Point(2, 3);
    switch (point) {
      case Point p -> {
        var carrier = Point.Point(p, 0, 0);
        var x = (int) POINT_ACCESSOR_0.invokeExact(carrier);
        var y = (int) POINT_ACCESSOR_1.invokeExact(carrier);

        assertAll(
            () -> assertEquals(2, x),
            () -> assertEquals(3, y)
        );
      }
    }
  }



  static class Box {
    private String name;

    public Box(String name) {
      this.name = name;
    }

    public static final Object CARRIER =
        RT.carrier(MethodHandles.lookup(), "", Object.class, methodType(void.class, String.class));
    private static final MethodHandle FACTORY =
        RT.carrierFactory(MethodHandles.lookup(), "", MethodHandle.class, CARRIER);

    public static /*matcher*/ Object Box(Box that, String name) throws Throwable {
      return FACTORY.invokeExact(that.name);
    }
  }

  private static final MethodHandle BOX_ACCESSOR_0 =
      RT.carrierAccessor(MethodHandles.lookup(), "", MethodHandle.class, Box.CARRIER, 0);

  @Test
  public void testBox() throws Throwable {
    var box = new Box("foo");
    switch (box) {
      case Box b -> {
        var carrier = Box.Box(b, null);
        var name = (String) BOX_ACCESSOR_0.invokeExact(carrier);

        assertEquals("foo", name);
      }
    }
  }


  static class Android {
    private String name;

    public Android(String name) {
      this.name = name;
    }

    public static final Object CARRIER_NAME_ID =
        RT.carrier(MethodHandles.lookup(), "", Object.class, methodType(void.class, String.class, long.class));
    private static final MethodHandle FACTORY_NAME_ID =
        RT.carrierFactory(MethodHandles.lookup(), "", MethodHandle.class, CARRIER_NAME_ID);
    public static final Object CARRIER_NAME =
        RT.carrier(MethodHandles.lookup(), "", Object.class, methodType(void.class, String.class));
    private static final MethodHandle FACTORY_NAME =
        RT.carrierFactory(MethodHandles.lookup(), "", MethodHandle.class, CARRIER_NAME);

    public static /*matcher*/ Object Android(Android that, String name, long id) throws Throwable {
      return FACTORY_NAME_ID.invokeExact(that.name, 1235233L);
    }
    public static /*matcher*/ Object Android(Android that, String name) throws Throwable {
      return FACTORY_NAME.invokeExact(that.name);
    }
  }

  private static final MethodHandle ANDROID_ACCESSOR_NAME_0 =
      RT.carrierAccessor(MethodHandles.lookup(), "", MethodHandle.class, Android.CARRIER_NAME, 0);
  private static final MethodHandle ANDROID_ACCESSOR_NAME_ID_0 =
      RT.carrierAccessor(MethodHandles.lookup(), "", MethodHandle.class, Android.CARRIER_NAME_ID, 0);
  private static final MethodHandle ANDROID_ACCESSOR_NAME_ID_1 =
      RT.carrierAccessor(MethodHandles.lookup(), "", MethodHandle.class, Android.CARRIER_NAME_ID, 1);

  @Test
  public void testAndroid() throws Throwable {
    var android = new Android("D2R2");
    var name1 = switch (android) {
      case Android a -> {
        var carrier = Android.Android(a, null);
        yield (String) ANDROID_ACCESSOR_NAME_0.invokeExact(carrier);
      }
    };
    var name2 = switch (android) {
      case Android a -> {
        var carrier = Android.Android(a, null, 0L);
        var name = (String) ANDROID_ACCESSOR_NAME_ID_0.invokeExact(carrier);
        var id = (long) ANDROID_ACCESSOR_NAME_ID_1.invokeExact(carrier);

        assertEquals(1235233L, id);

        yield name;
      }
    };
    assertAll(
        () -> assertEquals("D2R2", name1),
        () -> assertEquals("D2R2", name2)
    );
  }
}
