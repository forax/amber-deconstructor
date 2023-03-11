# amber-deconstructor
A prototype showing how deconstructor can work at runtime for Amber OpenJDK project

This prototype implements the two entry points
```java
  public static MethodHandle carrierFactory(Lookup lookup, String name, Class<?> type, MethodType matcherDescriptor) {
    ...
  }

  public static MethodHandle carrierAccessor(Lookup lookup, String name, Class<?> type, MethodType matcherDescriptor, int bindingNo) {
    ...
  }
```
as defined by Brian Goetz on the amber-spec-experts mailing list
  https://mail.openjdk.org/pipermail/amber-spec-experts/2023-March/003793.html

This prototype both works with the jdk 20 and the Valhalla jdk 20 early access build, using a value class as carrier
in the later case.

This prototype also proposes an alternate API than the one defined by Brian
```java
  public static Object carrier(Lookup lookup, String name, Class<?> type, MethodType matcherDescriptor) {
    ...
  }

  public static MethodHandle carrierFactory(Lookup lookup, String name, Class<?> type, Object carrier) {
    ...
  }

  public static MethodHandle carrierAccessor(Lookup lookup, String name, Class<?> type, Object carrier, int bindingNo) {
    ...
  }
```

which asks the class containing deconstructors to store the carrier definitions tailored for the deconstructors
instead of using a global cache (implementing a concurrent cache able to unload classes is hard).

This alternate API is perhaps a premature optimization, I don't know ... 
