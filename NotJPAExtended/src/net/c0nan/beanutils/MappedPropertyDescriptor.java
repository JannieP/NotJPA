package net.c0nan.beanutils;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Hashtable;

@SuppressWarnings({"unchecked","rawtypes"})
public class MappedPropertyDescriptor extends PropertyDescriptor {
	
	  private Class mappedPropertyType;
	  private Method mappedReadMethod;
	  private Method mappedWriteMethod;
	  private static final Class[] stringClassArray = { String.class };

	  private static Hashtable declaredMethodCache = new Hashtable();

	  public MappedPropertyDescriptor(String propertyName, Class beanClass)
	    throws IntrospectionException
	  {
	    super(propertyName, null, null);

	    if ((propertyName == null) || (propertyName.length() == 0)) {
	      throw new IntrospectionException("bad property name: " + propertyName + " on class: " + beanClass.getClass().getName());
	    }

	    setName(propertyName);
	    String base = capitalizePropertyName(propertyName);
	    try
	    {
	      this.mappedReadMethod = findMethod(beanClass, "get" + base, 1, stringClassArray);

	      Class[] params = { String.class, this.mappedReadMethod.getReturnType() };
	      this.mappedWriteMethod = findMethod(beanClass, "set" + base, 2, params);
	    }
	    catch (IntrospectionException e)
	    {
	    }

	    if (this.mappedReadMethod == null) {
	      this.mappedWriteMethod = findMethod(beanClass, "set" + base, 2);
	    }

	    if ((this.mappedReadMethod == null) && (this.mappedWriteMethod == null)) {
	      throw new IntrospectionException("Property '" + propertyName + "' not found on " + beanClass.getName());
	    }

	    findMappedPropertyType();
	  }

	  public MappedPropertyDescriptor(String propertyName, Class beanClass, String mappedGetterName, String mappedSetterName)
	    throws IntrospectionException
	  {
	    super(propertyName, null, null);

	    if ((propertyName == null) || (propertyName.length() == 0)) {
	      throw new IntrospectionException("bad property name: " + propertyName);
	    }

	    setName(propertyName);

	    this.mappedReadMethod = findMethod(beanClass, mappedGetterName, 1, stringClassArray);

	    if (this.mappedReadMethod != null) {
	      Class[] params = { String.class, this.mappedReadMethod.getReturnType() };
	      this.mappedWriteMethod = findMethod(beanClass, mappedSetterName, 2, params);
	    }
	    else {
	      this.mappedWriteMethod = findMethod(beanClass, mappedSetterName, 2);
	    }

	    findMappedPropertyType();
	  }

	  public MappedPropertyDescriptor(String propertyName, Method mappedGetter, Method mappedSetter)
	    throws IntrospectionException
	  {
	    super(propertyName, mappedGetter, mappedSetter);

	    if ((propertyName == null) || (propertyName.length() == 0)) {
	      throw new IntrospectionException("bad property name: " + propertyName);
	    }

	    setName(propertyName);
	    this.mappedReadMethod = mappedGetter;
	    this.mappedWriteMethod = mappedSetter;
	    findMappedPropertyType();
	  }

	  public Class getMappedPropertyType()
	  {
	    return this.mappedPropertyType;
	  }

	  public Method getMappedReadMethod()
	  {
	    return this.mappedReadMethod;
	  }

	  public void setMappedReadMethod(Method mappedGetter)
	    throws IntrospectionException
	  {
	    this.mappedReadMethod = mappedGetter;
	    findMappedPropertyType();
	  }

	  public Method getMappedWriteMethod()
	  {
	    return this.mappedWriteMethod;
	  }

	  public void setMappedWriteMethod(Method mappedSetter)
	    throws IntrospectionException
	  {
	    this.mappedWriteMethod = mappedSetter;
	    findMappedPropertyType();
	  }

	  private void findMappedPropertyType()
	    throws IntrospectionException
	  {
	    try
	    {
	      this.mappedPropertyType = null;
	      if (this.mappedReadMethod != null) {
	        if (this.mappedReadMethod.getParameterTypes().length != 1) {
	          throw new IntrospectionException("bad mapped read method arg count");
	        }

	        this.mappedPropertyType = this.mappedReadMethod.getReturnType();
	        if (this.mappedPropertyType == Void.TYPE) {
	          throw new IntrospectionException("mapped read method " + this.mappedReadMethod.getName() + " returns void");
	        }

	      }

	      if (this.mappedWriteMethod != null) {
	        Class[] params = this.mappedWriteMethod.getParameterTypes();
	        if (params.length != 2) {
	          throw new IntrospectionException("bad mapped write method arg count");
	        }

	        if ((this.mappedPropertyType != null) && (this.mappedPropertyType != params[1]))
	        {
	          throw new IntrospectionException("type mismatch between mapped read and write methods");
	        }

	        this.mappedPropertyType = params[1];
	      }
	    } catch (IntrospectionException ex) {
	      throw ex;
	    }
	  }

	  private static String capitalizePropertyName(String s)
	  {
	    if (s.length() == 0) {
	      return s;
	    }

	    char[] chars = s.toCharArray();
	    chars[0] = Character.toUpperCase(chars[0]);
	    return new String(chars);
	  }

	private static synchronized Method[] getPublicDeclaredMethods(Class clz)
	  {
	    final Class fclz = clz;
	    Method[] result = (Method[])declaredMethodCache.get(fclz);
	    if (result != null) {
	      return result;
	    }

	    
	    
	    PrivilegedAction<Object> pa = new PrivilegedAction<Object>() {

	      public Object run() { Method[] methods;
	        
	        try { return fclz.getDeclaredMethods();
	        }
	        catch (SecurityException ex)
	        {
	          methods = fclz.getMethods();
	          int i = 0; for (int size = methods.length; i < size; i++) {
	            Method method = methods[i];
	            if (!fclz.equals(method.getDeclaringClass()))
	              methods[i] = null;
	          }
	        }
	        return methods;
	      }};


	      result = (Method[])AccessController.doPrivileged(pa);
	      
	    for (int i = 0; i < result.length; i++) {
	      Method method = result[i];
	      if (method != null) {
	        int mods = method.getModifiers();
	        if (!Modifier.isPublic(mods)) {
	          result[i] = null;
	        }
	      }

	    }

	    declaredMethodCache.put(clz, result);
	    return result;
	  }

	  private static Method internalFindMethod(Class start, String methodName, int argCount)
	  {
	    for (Class cl = start; cl != null; cl = cl.getSuperclass()) {
	      Method[] methods = getPublicDeclaredMethods(cl);
	      for (int i = 0; i < methods.length; i++) {
	        Method method = methods[i];
	        if (method == null)
	        {
	          continue;
	        }
	        int mods = method.getModifiers();
	        if (Modifier.isStatic(mods)) {
	          continue;
	        }
	        if ((method.getName().equals(methodName)) && (method.getParameterTypes().length == argCount))
	        {
	          return method;
	        }

	      }

	    }

	    Class[] ifcs = start.getInterfaces();
	    for (int i = 0; i < ifcs.length; i++) {
	      Method m = internalFindMethod(ifcs[i], methodName, argCount);
	      if (m != null) {
	        return m;
	      }
	    }

	    return null;
	  }

	  private static Method internalFindMethod(Class start, String methodName, int argCount, Class[] args)
	  {
	    for (Class cl = start; cl != null; cl = cl.getSuperclass()) {
	      Method[] methods = getPublicDeclaredMethods(cl);
	      for (int i = 0; i < methods.length; i++) {
	        Method method = methods[i];
	        if (method == null)
	        {
	          continue;
	        }
	        int mods = method.getModifiers();
	        if (Modifier.isStatic(mods))
	        {
	          continue;
	        }
	        Class[] params = method.getParameterTypes();
	        if ((!method.getName().equals(methodName)) || (params.length != argCount))
	          continue;
	        boolean different = false;
	        if (argCount > 0) {
	          for (int j = 0; j < argCount; j++) {
	            if (params[j] != args[j]) {
	              different = true;
	            }
	          }

	          if (different)
	            continue;
	        }
	        else {
	          return method;
	        }

	      }

	    }

	    Class[] ifcs = start.getInterfaces();
	    for (int i = 0; i < ifcs.length; i++) {
	      Method m = internalFindMethod(ifcs[i], methodName, argCount);
	      if (m != null) {
	        return m;
	      }
	    }

	    return null;
	  }

	  static Method findMethod(Class cls, String methodName, int argCount)
	    throws IntrospectionException
	  {
	    if (methodName == null) {
	      return null;
	    }

	    Method m = internalFindMethod(cls, methodName, argCount);
	    if (m != null) {
	      return m;
	    }

	    throw new IntrospectionException("No method \"" + methodName + "\" with " + argCount + " arg(s)");
	  }

	  static Method findMethod(Class cls, String methodName, int argCount, Class[] args)
	    throws IntrospectionException
	  {
	    if (methodName == null) {
	      return null;
	    }

	    Method m = internalFindMethod(cls, methodName, argCount, args);
	    if (m != null) {
	      return m;
	    }

	    throw new IntrospectionException("No method \"" + methodName + "\" with " + argCount + " arg(s) of matching types.");
	  }

	  static boolean isSubclass(Class a, Class b)
	  {
	    if (a == b) {
	      return true;
	    }

	    if ((a == null) || (b == null)) {
	      return false;
	    }

	    for (Class x = a; x != null; x = x.getSuperclass()) {
	      if (x == b) {
	        return true;
	      }

	      if (b.isInterface()) {
	        Class[] interfaces = x.getInterfaces();
	        for (int i = 0; i < interfaces.length; i++) {
	          if (isSubclass(interfaces[i], b)) {
	            return true;
	          }
	        }
	      }
	    }

	    return false;
	  }

	@SuppressWarnings("unused")
	private boolean throwsException(Method method, Class exception)
	  {
	    Class[] exs = method.getExceptionTypes();
	    for (int i = 0; i < exs.length; i++) {
	      if (exs[i] == exception) {
	        return true;
	      }
	    }

	    return false;
	  }
}
