package net.c0nan.beanutils;

import java.beans.BeanInfo;
import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings({"unchecked","rawtypes"})
public class PropertyUtilsBean {
	
	  private Logger logger =  Logger.getLogger(PropertyUtilsBean.class.getName());
	
	  private HashMap descriptorsCache = null;
	  private HashMap mappedDescriptorsCache = null;
	  
	  public PropertyUtilsBean()
	  {
	    this.descriptorsCache = new HashMap();
	    this.mappedDescriptorsCache = new HashMap();
	  }

	  public void clearDescriptors()
	  {
	    this.descriptorsCache.clear();
	    this.mappedDescriptorsCache.clear();
	    Introspector.flushCaches();
	  }

	  public void copyProperties(Object dest, Object orig)
	    throws IllegalAccessException, InvocationTargetException, NoSuchMethodException
	  {
	    if (dest == null) {
	      throw new IllegalArgumentException("No destination bean specified");
	    }

	    if (orig == null) {
	      throw new IllegalArgumentException("No origin bean specified");
	    }

	    if ((orig instanceof Map)) {
	      Iterator names = ((Map)orig).keySet().iterator();
	      while (names.hasNext()) {
	        String name = (String)names.next();
	        if (isWriteable(dest, name)) {
	          Object value = ((Map)orig).get(name);
	          setSimpleProperty(dest, name, value);
	        }
	      }
	    }
	    else {
	      PropertyDescriptor[] origDescriptors = getPropertyDescriptors(orig);

	      for (int i = 0; i < origDescriptors.length; i++) {
	        String name = origDescriptors[i].getName();
	        if (isReadable(orig, name))
	          if (isWriteable(dest, name)) {
	            Object value = getSimpleProperty(orig, name);
	            setSimpleProperty(dest, name, value);
	          }
	      }
	    }
	  }

	  
	public Map describe(Object bean)
	    throws IllegalAccessException, InvocationTargetException, NoSuchMethodException
	  {
	    if (bean == null) {
	      throw new IllegalArgumentException("No bean specified");
	    }
	    Map description = new HashMap();
	      PropertyDescriptor[] descriptors = getPropertyDescriptors(bean);

	      for (int i = 0; i < descriptors.length; i++) {
	        String name = descriptors[i].getName();
	        if (descriptors[i].getReadMethod() != null)
	          description.put(name, getProperty(bean, name));
	      }
	    
	    return description;
	  }

	  public Object getIndexedProperty(Object bean, String name)
	    throws IllegalAccessException, InvocationTargetException, NoSuchMethodException
	  {
	    if (bean == null) {
	      throw new IllegalArgumentException("No bean specified");
	    }
	    if (name == null) {
	      throw new IllegalArgumentException("No name specified");
	    }

	    int delim = name.indexOf('[');
	    int delim2 = name.indexOf(']');
	    if ((delim < 0) || (delim2 <= delim)) {
	      throw new IllegalArgumentException("Invalid indexed property '" + name + "'");
	    }

	    int index = -1;
	    try {
	      String subscript = name.substring(delim + 1, delim2);
	      index = Integer.parseInt(subscript);
	    } catch (NumberFormatException e) {
	      throw new IllegalArgumentException("Invalid indexed property '" + name + "'");
	    }

	    name = name.substring(0, delim);

	    return getIndexedProperty(bean, name, index);
	  }

	  public Object getIndexedProperty(Object bean, String name, int index)
	    throws IllegalAccessException, InvocationTargetException, NoSuchMethodException
	  {
	    if (bean == null) {
	      throw new IllegalArgumentException("No bean specified");
	    }
	    if (name == null) {
	      throw new IllegalArgumentException("No name specified");
	    }

	    PropertyDescriptor descriptor = getPropertyDescriptor(bean, name);

	    if (descriptor == null) {
	      throw new NoSuchMethodException("Unknown property '" + name + "'");
	    }

	    if ((descriptor instanceof IndexedPropertyDescriptor)) {
	      Method readMethod = ((IndexedPropertyDescriptor)descriptor).getIndexedReadMethod();

	      if (readMethod != null) {
	        Object[] subscript = new Object[1];
	        subscript[0] = new Integer(index);
	        try {
	          return invokeMethod(readMethod, bean, subscript);
	        } catch (InvocationTargetException e) {
	          if ((e.getTargetException() instanceof ArrayIndexOutOfBoundsException))
	          {
	            throw ((ArrayIndexOutOfBoundsException)e.getTargetException());
	          }

	          throw e;
	        }

	      }

	    }

	    Method readMethod = getReadMethod(descriptor);
	    if (readMethod == null) {
	      throw new NoSuchMethodException("Property '" + name + "' has no getter method");
	    }

	    Object value = invokeMethod(readMethod, bean, new Object[0]);
	    if (!value.getClass().isArray()) {
	      if (!(value instanceof List)) {
	        throw new IllegalArgumentException("Property '" + name + "' is not indexed");
	      }

	      return ((List)value).get(index);
	    }

	    return Array.get(value, index);
	  }

	  public Object getMappedProperty(Object bean, String name)
	    throws IllegalAccessException, InvocationTargetException, NoSuchMethodException
	  {
	    if (bean == null) {
	      throw new IllegalArgumentException("No bean specified");
	    }
	    if (name == null) {
	      throw new IllegalArgumentException("No name specified");
	    }

	    int delim = name.indexOf('(');
	    int delim2 = name.indexOf(')');
	    if ((delim < 0) || (delim2 <= delim)) {
	      throw new IllegalArgumentException("Invalid mapped property '" + name + "'");
	    }

	    String key = name.substring(delim + 1, delim2);
	    name = name.substring(0, delim);

	    return getMappedProperty(bean, name, key);
	  }

	  public Object getMappedProperty(Object bean, String name, String key)
	    throws IllegalAccessException, InvocationTargetException, NoSuchMethodException
	  {
	    if (bean == null) {
	      throw new IllegalArgumentException("No bean specified");
	    }
	    if (name == null) {
	      throw new IllegalArgumentException("No name specified");
	    }
	    if (key == null) {
	      throw new IllegalArgumentException("No key specified");
	    }

	    Object result = null;

	    PropertyDescriptor descriptor = getPropertyDescriptor(bean, name);
	    if (descriptor == null) {
	      throw new NoSuchMethodException("Unknown property '" + name + "'");
	    }

	      Method readMethod = descriptor.getReadMethod();
	      if (readMethod != null) {
	        Object invokeResult = invokeMethod(readMethod, bean, new Object[0]);

	        if ((invokeResult instanceof Map))
	          result = ((Map)invokeResult).get(key);
	      }
	      else {
	        throw new NoSuchMethodException("Property '" + name + "' has no mapped getter method");
	      }

	    return result;
	  }

	  public Object getNestedProperty(Object bean, String name)
	    throws IllegalAccessException, InvocationTargetException, NoSuchMethodException
	  {
	    if (bean == null) {
	      throw new IllegalArgumentException("No bean specified");
	    }
	    if (name == null) {
	      throw new IllegalArgumentException("No name specified");
	    }

	    int indexOfINDEXED_DELIM = -1;
	    int indexOfMAPPED_DELIM = -1;
	    int indexOfMAPPED_DELIM2 = -1;
	    int indexOfNESTED_DELIM = -1;
	    while (true) {
	      indexOfNESTED_DELIM = name.indexOf('.');
	      indexOfMAPPED_DELIM = name.indexOf('(');
	      indexOfMAPPED_DELIM2 = name.indexOf(')');
	      if ((indexOfMAPPED_DELIM2 >= 0) && (indexOfMAPPED_DELIM >= 0) && ((indexOfNESTED_DELIM < 0) || (indexOfNESTED_DELIM > indexOfMAPPED_DELIM)))
	      {
	        indexOfNESTED_DELIM = name.indexOf('.', indexOfMAPPED_DELIM2);
	      }
	      else {
	        indexOfNESTED_DELIM = name.indexOf('.');
	      }
	      if (indexOfNESTED_DELIM < 0) {
	        break;
	      }
	      String next = name.substring(0, indexOfNESTED_DELIM);
	      indexOfINDEXED_DELIM = next.indexOf('[');
	      indexOfMAPPED_DELIM = next.indexOf('(');
	      if ((bean instanceof Map))
	        bean = ((Map)bean).get(next);
	      else if (indexOfMAPPED_DELIM >= 0)
	        bean = getMappedProperty(bean, next);
	      else if (indexOfINDEXED_DELIM >= 0)
	        bean = getIndexedProperty(bean, next);
	      else {
	        bean = getSimpleProperty(bean, next);
	      }
	      if (bean == null) {
	        throw new IllegalArgumentException("Null property value for '" + name.substring(0, indexOfNESTED_DELIM) + "'");
	      }

	      name = name.substring(indexOfNESTED_DELIM + 1);
	    }

	    indexOfINDEXED_DELIM = name.indexOf('[');
	    indexOfMAPPED_DELIM = name.indexOf('(');

	    if ((bean instanceof Map))
	      bean = ((Map)bean).get(name);
	    else if (indexOfMAPPED_DELIM >= 0)
	      bean = getMappedProperty(bean, name);
	    else if (indexOfINDEXED_DELIM >= 0)
	      bean = getIndexedProperty(bean, name);
	    else {
	      bean = getSimpleProperty(bean, name);
	    }
	    return bean;
	  }

	  public Object getProperty(Object bean, String name)
	    throws IllegalAccessException, InvocationTargetException, NoSuchMethodException
	  {
	    return getNestedProperty(bean, name);
	  }

	  public PropertyDescriptor getPropertyDescriptor(Object bean, String name)
	    throws IllegalAccessException, InvocationTargetException, NoSuchMethodException
	  {
	    if (bean == null) {
	      throw new IllegalArgumentException("No bean specified");
	    }
	    if (name == null) {
	      throw new IllegalArgumentException("No name specified");
	    }

	    while (true)
	    {
	      int period = findNextNestedIndex(name);
	      if (period < 0) {
	        break;
	      }
	      String next = name.substring(0, period);
	      int indexOfINDEXED_DELIM = next.indexOf('[');
	      int indexOfMAPPED_DELIM = next.indexOf('(');
	      if ((indexOfMAPPED_DELIM >= 0) && ((indexOfINDEXED_DELIM < 0) || (indexOfMAPPED_DELIM < indexOfINDEXED_DELIM)))
	      {
	        bean = getMappedProperty(bean, next);
	      }
	      else if (indexOfINDEXED_DELIM >= 0)
	        bean = getIndexedProperty(bean, next);
	      else {
	        bean = getSimpleProperty(bean, next);
	      }

	      if (bean == null) {
	        throw new IllegalArgumentException("Null property value for '" + name.substring(0, period) + "'");
	      }

	      name = name.substring(period + 1);
	    }

	    int left = name.indexOf('[');
	    if (left >= 0) {
	      name = name.substring(0, left);
	    }
	    left = name.indexOf('(');
	    if (left >= 0) {
	      name = name.substring(0, left);
	    }

	    if ((bean == null) || (name == null)) {
	      return null;
	    }

	    PropertyDescriptor[] descriptors = getPropertyDescriptors(bean);
	    if (descriptors != null)
	    {
	      for (int i = 0; i < descriptors.length; i++) {
	        if (name.equals(descriptors[i].getName())) {
	          return descriptors[i];
	        }
	      }
	    }
	    PropertyDescriptor result = null;
	    HashMap mappedDescriptors = (HashMap)this.mappedDescriptorsCache.get(bean.getClass());

	    if (mappedDescriptors == null) {
	      mappedDescriptors = new HashMap();
	      this.mappedDescriptorsCache.put(bean.getClass(), mappedDescriptors);
	    }
	    result = (PropertyDescriptor)mappedDescriptors.get(name);
	    if (result == null)
	    {
	      try {
	        result = new MappedPropertyDescriptor(name, bean.getClass());
	      }
	      catch (IntrospectionException ie) {
	      }
	      if (result != null) {
	        mappedDescriptors.put(name, result);
	      }
	    }

	    return result;
	  }

	  private int findNextNestedIndex(String expression)
	  {
	    int bracketCount = 0;
	    int i = 0; for (int size = expression.length(); i < size; i++) {
	      char at = expression.charAt(i);
	      switch (at) {
	      case '.':
	        if (bracketCount >= 1) continue;
	        return i;
	      case '(':
	      case '[':
	        bracketCount++;
	        break;
	      case ')':
	      case ']':
	        bracketCount--;
	      }

	    }

	    return -1;
	  }

	  public PropertyDescriptor[] getPropertyDescriptors(Class beanClass)
	  {
	    if (beanClass == null) {
	      throw new IllegalArgumentException("No bean class specified");
	    }

	    PropertyDescriptor[] descriptors = null;
	    descriptors = (PropertyDescriptor[])this.descriptorsCache.get(beanClass);

	    if (descriptors != null) {
	      return descriptors;
	    }

	    BeanInfo beanInfo = null;
	    try {
	      beanInfo = Introspector.getBeanInfo(beanClass);
	    } catch (IntrospectionException e) {
	      return new PropertyDescriptor[0];
	    }
	    descriptors = beanInfo.getPropertyDescriptors();
	    if (descriptors == null) {
	      descriptors = new PropertyDescriptor[0];
	    }
	    this.descriptorsCache.put(beanClass, descriptors);
	    return descriptors;
	  }

	  public PropertyDescriptor[] getPropertyDescriptors(Object bean)
	  {
	    if (bean == null) {
	      throw new IllegalArgumentException("No bean specified");
	    }
	    return getPropertyDescriptors(bean.getClass());
	  }

	  public Class getPropertyEditorClass(Object bean, String name)
	    throws IllegalAccessException, InvocationTargetException, NoSuchMethodException
	  {
	    if (bean == null) {
	      throw new IllegalArgumentException("No bean specified");
	    }
	    if (name == null) {
	      throw new IllegalArgumentException("No name specified");
	    }

	    PropertyDescriptor descriptor = getPropertyDescriptor(bean, name);

	    if (descriptor != null) {
	      return descriptor.getPropertyEditorClass();
	    }
	    return null;
	  }

	  public Class getPropertyType(Object bean, String name)
	    throws IllegalAccessException, InvocationTargetException, NoSuchMethodException
	  {
	    if (bean == null) {
	      throw new IllegalArgumentException("No bean specified");
	    }
	    if (name == null) {
	      throw new IllegalArgumentException("No name specified");
	    }

	    PropertyDescriptor descriptor = getPropertyDescriptor(bean, name);

	    if (descriptor == null)
	      return null;
	    if ((descriptor instanceof IndexedPropertyDescriptor)) {
	      return ((IndexedPropertyDescriptor)descriptor).getIndexedPropertyType();
	    }
	    if ((descriptor instanceof MappedPropertyDescriptor)) {
	      return ((MappedPropertyDescriptor)descriptor).getMappedPropertyType();
	    }

	    return descriptor.getPropertyType();
	  }

	  public Method getReadMethod(PropertyDescriptor descriptor)
	  {
	    return descriptor.getReadMethod();
	  }

	  public Object getSimpleProperty(Object bean, String name)
	    throws IllegalAccessException, InvocationTargetException, NoSuchMethodException
	  {
	    if (bean == null) {
	      throw new IllegalArgumentException("No bean specified");
	    }
	    if (name == null) {
	      throw new IllegalArgumentException("No name specified");
	    }

	    if (name.indexOf('.') >= 0) {
	      throw new IllegalArgumentException("Nested property names are not allowed");
	    }
	    if (name.indexOf('[') >= 0) {
	      throw new IllegalArgumentException("Indexed property names are not allowed");
	    }
	    if (name.indexOf('(') >= 0) {
	      throw new IllegalArgumentException("Mapped property names are not allowed");
	    }

	    PropertyDescriptor descriptor = getPropertyDescriptor(bean, name);

	    if (descriptor == null) {
	      throw new NoSuchMethodException("Unknown property '" + name + "'");
	    }

	    Method readMethod = getReadMethod(descriptor);
	    if (readMethod == null) {
	      throw new NoSuchMethodException("Property '" + name + "' has no getter method");
	    }

	    Object value = invokeMethod(readMethod, bean, new Object[0]);
	    return value;
	  }

	  public Method getWriteMethod(PropertyDescriptor descriptor)
	  {
	    return descriptor.getWriteMethod();
	  }

	  public boolean isReadable(Object bean, String name)
	  {
	    if (bean == null) {
	      throw new IllegalArgumentException("No bean specified");
	    }
	    if (name == null) {
	      throw new IllegalArgumentException("No name specified");
	    }

	    try {
	      PropertyDescriptor desc = getPropertyDescriptor(bean, name);

	      if (desc != null) {
	        Method readMethod = desc.getReadMethod();
	        if ((readMethod == null) && ((desc instanceof IndexedPropertyDescriptor)))
	        {
	          readMethod = ((IndexedPropertyDescriptor)desc).getIndexedReadMethod();
	        }
	        return readMethod != null;
	      }
	      return false;
	    }
	    catch (IllegalAccessException e) {
	      return false;
	    } catch (InvocationTargetException e) {
	      return false; } catch (NoSuchMethodException e) {
	    }
	    return false;
	  }

	  public boolean isWriteable(Object bean, String name)
	  {
	    if (bean == null) {
	      throw new IllegalArgumentException("No bean specified");
	    }
	    if (name == null) {
	      throw new IllegalArgumentException("No name specified");
	    }

	    try {
	      PropertyDescriptor desc = getPropertyDescriptor(bean, name);

	      if (desc != null) {
	        Method writeMethod = desc.getWriteMethod();
	        if ((writeMethod == null) && ((desc instanceof IndexedPropertyDescriptor)))
	        {
	          writeMethod = ((IndexedPropertyDescriptor)desc).getIndexedWriteMethod();
	        }
	        return writeMethod != null;
	      }
	      return false;
	    }
	    catch (IllegalAccessException e) {
	      return false;
	    } catch (InvocationTargetException e) {
	      return false; } catch (NoSuchMethodException e) {
	    }
	    return false;
	  }

	  public void setIndexedProperty(Object bean, String name, Object value)
	    throws IllegalAccessException, InvocationTargetException, NoSuchMethodException
	  {
	    if (bean == null) {
	      throw new IllegalArgumentException("No bean specified");
	    }
	    if (name == null) {
	      throw new IllegalArgumentException("No name specified");
	    }

	    int delim = name.indexOf('[');
	    int delim2 = name.indexOf(']');
	    if ((delim < 0) || (delim2 <= delim)) {
	      throw new IllegalArgumentException("Invalid indexed property '" + name + "'");
	    }

	    int index = -1;
	    try {
	      String subscript = name.substring(delim + 1, delim2);
	      index = Integer.parseInt(subscript);
	    } catch (NumberFormatException e) {
	      throw new IllegalArgumentException("Invalid indexed property '" + name + "'");
	    }

	    name = name.substring(0, delim);

	    setIndexedProperty(bean, name, index, value);
	  }

	  public void setIndexedProperty(Object bean, String name, int index, Object value)
	    throws IllegalAccessException, InvocationTargetException, NoSuchMethodException
	  {
	    if (bean == null) {
	      throw new IllegalArgumentException("No bean specified");
	    }
	    if (name == null) {
	      throw new IllegalArgumentException("No name specified");
	    }

	    PropertyDescriptor descriptor = getPropertyDescriptor(bean, name);

	    if (descriptor == null) {
	      throw new NoSuchMethodException("Unknown property '" + name + "'");
	    }

	    if ((descriptor instanceof IndexedPropertyDescriptor)) {
	      Method writeMethod = ((IndexedPropertyDescriptor)descriptor).getIndexedWriteMethod();

	      if (writeMethod != null) {
	        Object[] subscript = new Object[2];
	        subscript[0] = new Integer(index);
	        subscript[1] = value;
	        try {

	          invokeMethod(writeMethod, bean, subscript);
	        } catch (InvocationTargetException e) {
	          if ((e.getTargetException() instanceof ArrayIndexOutOfBoundsException))
	          {
	            throw ((ArrayIndexOutOfBoundsException)e.getTargetException());
	          }

	          throw e;
	        }

	        return;
	      }

	    }

	    Method readMethod = descriptor.getReadMethod();
	    if (readMethod == null) {
	      throw new NoSuchMethodException("Property '" + name + "' has no getter method");
	    }

	    Object array = invokeMethod(readMethod, bean, new Object[0]);
	    if (!array.getClass().isArray()) {
	      if ((array instanceof List))
	      {
	        ((List)array).set(index, value);
	      }
	      else throw new IllegalArgumentException("Property '" + name + "' is not indexed");

	    }
	    else
	    {
	      Array.set(array, index, value);
	    }
	  }

	  public void setMappedProperty(Object bean, String name, Object value)
	    throws IllegalAccessException, InvocationTargetException, NoSuchMethodException
	  {
	    if (bean == null) {
	      throw new IllegalArgumentException("No bean specified");
	    }
	    if (name == null) {
	      throw new IllegalArgumentException("No name specified");
	    }

	    int delim = name.indexOf('(');
	    int delim2 = name.indexOf(')');
	    if ((delim < 0) || (delim2 <= delim)) {
	      throw new IllegalArgumentException("Invalid mapped property '" + name + "'");
	    }

	    String key = name.substring(delim + 1, delim2);
	    name = name.substring(0, delim);

	    setMappedProperty(bean, name, key, value);
	  }

	  public void setMappedProperty(Object bean, String name, String key, Object value)
	    throws IllegalAccessException, InvocationTargetException, NoSuchMethodException
	  {
	    if (bean == null) {
	      throw new IllegalArgumentException("No bean specified");
	    }
	    if (name == null) {
	      throw new IllegalArgumentException("No name specified");
	    }
	    if (key == null) {
	      throw new IllegalArgumentException("No key specified");
	    }

	    PropertyDescriptor descriptor = getPropertyDescriptor(bean, name);

	    if (descriptor == null) {
	      throw new NoSuchMethodException("Unknown property '" + name + "'");
	    }

	    if ((descriptor instanceof MappedPropertyDescriptor))
	    {
	      Method mappedWriteMethod = ((MappedPropertyDescriptor)descriptor).getMappedWriteMethod();

	      if (mappedWriteMethod != null) {
	        Object[] params = new Object[2];
	        params[0] = key;
	        params[1] = value;

	        invokeMethod(mappedWriteMethod, bean, params);
	      } else {
	        throw new NoSuchMethodException("Property '" + name + "' has no mapped setter method");
	      }

	    }
	    else
	    {
	      Method readMethod = descriptor.getReadMethod();
	      if (readMethod != null) {
	        Object invokeResult = invokeMethod(readMethod, bean, new Object[0]);

	        if ((invokeResult instanceof Map))
	          ((Map)invokeResult).put(key, value);
	      }
	      else {
	        throw new NoSuchMethodException("Property '" + name + "' has no mapped getter method");
	      }
	    }
	  }

	  public void setNestedProperty(Object bean, String name, Object value)
	    throws IllegalAccessException, InvocationTargetException, NoSuchMethodException
	  {
	    if (bean == null) {
	      throw new IllegalArgumentException("No bean specified");
	    }
	    if (name == null) {
	      throw new IllegalArgumentException("No name specified");
	    }

	    int indexOfINDEXED_DELIM = -1;
	    int indexOfMAPPED_DELIM = -1;
	    while (true) {
	      int delim = name.indexOf('.');
	      if (delim < 0) {
	        break;
	      }
	      String next = name.substring(0, delim);
	      indexOfINDEXED_DELIM = next.indexOf('[');
	      indexOfMAPPED_DELIM = next.indexOf('(');
	      if ((bean instanceof Map))
	        bean = ((Map)bean).get(next);
	      else if (indexOfMAPPED_DELIM >= 0)
	        bean = getMappedProperty(bean, next);
	      else if (indexOfINDEXED_DELIM >= 0)
	        bean = getIndexedProperty(bean, next);
	      else {
	        bean = getSimpleProperty(bean, next);
	      }
	      if (bean == null) {
	        throw new IllegalArgumentException("Null property value for '" + name.substring(0, delim) + "'");
	      }

	      name = name.substring(delim + 1);
	    }

	    indexOfINDEXED_DELIM = name.indexOf('[');
	    indexOfMAPPED_DELIM = name.indexOf('(');

	    if ((bean instanceof Map))
	    {
	      PropertyDescriptor descriptor = getPropertyDescriptor(bean, name);

	      if (descriptor == null)
	      {
	        ((Map)bean).put(name, value);
	      }
	      else
	        setSimpleProperty(bean, name, value);
	    }
	    else if (indexOfMAPPED_DELIM >= 0) {
	      setMappedProperty(bean, name, value);
	    } else if (indexOfINDEXED_DELIM >= 0) {
	      setIndexedProperty(bean, name, value);
	    } else {
	      setSimpleProperty(bean, name, value);
	    }
	  }

	  public void setProperty(Object bean, String name, Object value)
	    throws IllegalAccessException, InvocationTargetException, NoSuchMethodException
	  {
	    setNestedProperty(bean, name, value);
	  }

	  public void setSimpleProperty(Object bean, String name, Object value)
	    throws IllegalAccessException, InvocationTargetException, NoSuchMethodException
	  {
	    if (bean == null) {
	      throw new IllegalArgumentException("No bean specified");
	    }
	    if (name == null) {
	      throw new IllegalArgumentException("No name specified");
	    }

	    if (name.indexOf('.') >= 0) {
	      throw new IllegalArgumentException("Nested property names are not allowed");
	    }
	    if (name.indexOf('[') >= 0) {
	      throw new IllegalArgumentException("Indexed property names are not allowed");
	    }
	    if (name.indexOf('(') >= 0) {
	      throw new IllegalArgumentException("Mapped property names are not allowed");
	    }

	    PropertyDescriptor descriptor = getPropertyDescriptor(bean, name);

	    if (descriptor == null) {
	      throw new NoSuchMethodException("Unknown property '" + name + "'");
	    }

	    Method writeMethod = getWriteMethod(descriptor);
	    if (writeMethod == null) {
	      throw new NoSuchMethodException("Property '" + name + "' has no setter method");
	    }

	    Object[] values = new Object[1];
	    
	    Class<?> clazz = writeMethod.getParameterTypes()[0];
	    
	    
	    if (value instanceof java.util.Date && ("java.sql.Date".equals(clazz.getName()))){
	    	java.sql.Date val1Date = new Date(((java.util.Date)value).getTime());
	    	values[0] = val1Date;
	    }else{
	    	values[0] = value;
	    }
	    

	    invokeMethod(writeMethod, bean, values);
	  }

	  private Object invokeMethod(Method method, Object bean, Object[] values)
	    throws IllegalAccessException, InvocationTargetException
	  {
	    try
	    {
	      return method.invoke(bean, values);
	    }
	    catch (IllegalArgumentException e)
	    {
	    	String message = "{Copy-E}:Method:" + method.getName() + "Value" + values[0].toString() + "Error Message:" + e.getLocalizedMessage();
	    	logger.log(Level.FINE,message);
	    	try {
	    		if (values[0] == null){
	    			//method = bean.getClass().getMethod(method.getName());
	    			return null; 
	    		}else{
	    			method = bean.getClass().getMethod(method.getName(),values[0].getClass());
	    		}
				
				return method.invoke(bean,values);
			} catch (Exception e1) {
		    	//throw new IllegalArgumentException("Cannot invoke " + method.getDeclaringClass().getName() + "." + method.getName() + " - " + e1.getMessage());
			}
			return null;
//	    	throw new IllegalArgumentException("Cannot invoke " + method.getDeclaringClass().getName() + "." + method.getName() + " - " + e.getMessage());
	    }
	  }

}
