package com.github.forax.amberdeconstructor;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodHandles.Lookup.ClassOption;
import java.lang.invoke.MethodType;
import java.util.Objects;import java.util.concurrent.ConcurrentHashMap;

import static java.lang.invoke.MethodType.methodType;
import static org.objectweb.asm.Opcodes.*;

public class RT {
  private static final int ACC_IDENTITY = ACC_SUPER;  // == ACC_SUPER

  private static final boolean SUPPORT_VALUE_TYPE;
  static {
    boolean supportValueType;
    try {
      Class.class.getMethod("isValue");
      supportValueType = true;
    } catch (NoSuchMethodException e) {
      supportValueType = false;
    }
    SUPPORT_VALUE_TYPE = supportValueType;
  }

  private static Lookup generateCarrierClass(MethodType methodType) {
    var cv = new ClassWriter(0);
    var owner = "com/github/forax/amberdeconstructor/Carrier";
    var classIdentity = SUPPORT_VALUE_TYPE ? 0 : ACC_IDENTITY;
    var minorVersion = SUPPORT_VALUE_TYPE ? V_PREVIEW : 0;
    cv.visit(V23 | minorVersion, ACC_PUBLIC | ACC_FINAL | classIdentity,
        owner,
        null,
        "java/lang/Object",
        null);
    var parameterList = methodType.parameterList();

    // fields
    for(var i = 0; i < parameterList.size(); i++) {
      var parameterType= parameterList.get(i);
      var strict = SUPPORT_VALUE_TYPE ? ACC_STRICT : 0;
      var fv = cv.visitField(ACC_PRIVATE | ACC_FINAL | ACC_STRICT, "field" + i, parameterType.descriptorString(), null, null);
      fv.visitEnd();
    }

    // constructor
    var init = cv.visitMethod(ACC_PRIVATE, "<init>", methodType.toMethodDescriptorString(), null, null);
    init.visitCode();
    var slot = 1;
    for (var i = 0; i < parameterList.size(); i++) {
      var parameterType = parameterList.get(i);
      init.visitVarInsn(ALOAD, 0);
      var type = Type.getType(parameterType);
      init.visitVarInsn(type.getOpcode(ILOAD), slot);
      slot += type.getSize();
      init.visitFieldInsn(PUTFIELD, owner, "field" + i, parameterType.descriptorString());
    }
    init.visitVarInsn(ALOAD, 0);
    init.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
    init.visitInsn(RETURN);
    init.visitMaxs(3, slot);
    init.visitEnd();

    // factory
    var factory = cv.visitMethod(ACC_PUBLIC | ACC_STATIC, "factory", methodType.changeReturnType(Object.class).toMethodDescriptorString(), null, null);
    factory.visitCode();
    factory.visitTypeInsn(NEW, owner);
    factory.visitInsn(DUP);
    slot = 0;
    for (var i = 0; i < parameterList.size(); i++) {
      var parameterType = parameterList.get(i);
      var type = Type.getType(parameterType);
      factory.visitVarInsn(type.getOpcode(ILOAD), slot);
      slot += type.getSize();
    }
    factory.visitMethodInsn(
        INVOKESPECIAL, owner, "<init>", methodType.toMethodDescriptorString(), false);
    factory.visitInsn(ARETURN);
    factory.visitMaxs(2 + slot, slot);
    factory.visitEnd();

    // accessors
    for(var i = 0; i < parameterList.size(); i++) {
      var parameterType= parameterList.get(i);
      var mv = cv.visitMethod(ACC_PUBLIC, "field" + i, "()" + parameterType.descriptorString(), null, null);
      mv.visitCode();
      mv.visitVarInsn(ALOAD, 0);
      var type = Type.getType(parameterType);
      mv.visitFieldInsn(GETFIELD, owner, "field" + i, parameterType.descriptorString());
      mv.visitInsn(type.getOpcode(IRETURN));
      mv.visitMaxs(2, 2);
      mv.visitEnd();
    }

    cv.visitEnd();
    var bytecode = cv.toByteArray();

    //new ClassReader(bytecode).accept(new TraceClassVisitor(new PrintWriter(System.err)), 0);
    //CheckClassAdapter.verify(new ClassReader(bytecode), true, new PrintWriter(System.err));

    try {
      return MethodHandles.lookup().defineHiddenClass(bytecode, true, ClassOption.STRONG);
    } catch (IllegalAccessException e) {
      throw (IllegalAccessError) new IllegalAccessError().initCause(e);
    }
  }

  private record Carrier(MethodHandle factory, MethodHandle[] accessors) {
    Carrier as(MethodType methodType) {
      var factory = this.factory.asType(methodType.changeReturnType(Object.class));
      var accessors = new MethodHandle[this.accessors.length];
      var parameterTypes = methodType.parameterList();
      for(var i = 0; i < accessors.length; i++) {
        var parameterType = parameterTypes.get(i);
        accessors[i] = this.accessors[i].asType(methodType(parameterType, Object.class));
      }
      return new Carrier(factory, accessors);
    }
  }

  private static Carrier generateCarrier(MethodType methodType) {
    var lookup = generateCarrierClass(methodType);
    var lookupClass = lookup.lookupClass();
    var parameterList = methodType.parameterList();
    try {
      //var factory = lookup.findConstructor(lookupClass, methodType)
      //    .asType(methodType.changeReturnType(Object.class));
      var factory = lookup.findStatic(lookupClass, "factory", methodType.changeReturnType(Object.class));

      var accessors = new MethodHandle[parameterList.size()];
      for (var i = 0; i < parameterList.size(); i++) {
        var parameterType = parameterList.get(i);
        var accessor = lookup.findVirtual(lookupClass, "field" + i, methodType(parameterType))
            .asType(methodType(parameterType, Object.class));
        accessors[i] = accessor;
      }

      return new Carrier(factory, accessors);
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }

  private static final ConcurrentHashMap<MethodType, Carrier> CARRIER_CLASS_MAP =
      new ConcurrentHashMap<>();

  private static Carrier carrierClass(MethodType methodType) {
    var carrier = CARRIER_CLASS_MAP.get(methodType);
    if (carrier != null) {
      return carrier;
    }

    var erasedMethodType = methodType.erase();
    if (!erasedMethodType.equals(methodType)) {
      carrier = carrierClass(erasedMethodType).as(methodType);
    } else {
      carrier = generateCarrier(erasedMethodType);
    }
    var oldCarrier = CARRIER_CLASS_MAP.putIfAbsent(methodType, carrier);
    if (oldCarrier != null) {
      return oldCarrier;
    }
    return carrier;
  }

  public static MethodHandle carrierFactory(Lookup lookup, String name, Class<?> type, MethodType matcherDescriptor) {
    if (matcherDescriptor.returnType() != void.class) {
      throw new LinkageError("invalid method type " + matcherDescriptor);
    }
    return carrierClass(matcherDescriptor).factory;
  }

  public static MethodHandle carrierAccessor(Lookup lookup, String name, Class<?> type, MethodType matcherDescriptor, int bindingNo) {
    if (matcherDescriptor.returnType() != void.class) {
      throw new LinkageError("invalid method type " + matcherDescriptor);
    }
    if (bindingNo < 0 || bindingNo >= matcherDescriptor.parameterCount()) {
      throw new LinkageError("invalid binding number " + bindingNo);
    }
    return carrierClass(matcherDescriptor).accessors[bindingNo];
  }


  // alternative API !

  public static Object carrier(Lookup lookup, String name, Class<?> type, MethodType matcherDescriptor) {
    if (matcherDescriptor.returnType() != void.class) {
      throw new LinkageError("invalid method type " + matcherDescriptor);
    }
    return carrierClass(matcherDescriptor.erase()).as(matcherDescriptor);
  }

  public static MethodHandle carrierFactory(Lookup lookup, String name, Class<?> type, Object carrier) {
    Objects.requireNonNull(carrier, "carrier is null");
    return ((Carrier) carrier).factory;
  }

  public static MethodHandle carrierAccessor(Lookup lookup, String name, Class<?> type, Object carrier, int bindingNo) {
    Objects.requireNonNull(carrier, "carrier is null");
    return ((Carrier)carrier).accessors[bindingNo];
  }
}
