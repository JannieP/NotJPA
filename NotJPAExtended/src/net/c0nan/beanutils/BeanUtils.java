package net.c0nan.beanutils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Map;

@SuppressWarnings({"rawtypes"})
public class BeanUtils {
	
	
	private PropertyUtilsBean propertyUtilsBean;	

	private static BeanUtils beanutils = new BeanUtils();
	
	public static BeanUtils getInstance(){
		return beanutils;
	}
	  
	public BeanUtils()
	  {
	    this(new PropertyUtilsBean());
	  }	
	
	  public BeanUtils(PropertyUtilsBean propertyUtilsBean)
	  {
	    this.propertyUtilsBean = propertyUtilsBean;
	  }

	public PropertyUtilsBean getPropertyUtils()
	  {
	    return this.propertyUtilsBean;
	  }
	
	  public void copyProperties(Object dest, Object orig)
	    throws IllegalAccessException, InvocationTargetException
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
	        if (getPropertyUtils().isWriteable(dest, name)) {
	          Object value = ((Map)orig).get(name);
	          copyProperty(dest, name, value);
	        }
	      }
	    } else {
	      PropertyDescriptor[] origDescriptors = getPropertyUtils().getPropertyDescriptors(orig);

	      for (int i = 0; i < origDescriptors.length; i++) {
	        String name = origDescriptors[i].getName();
	        if ("class".equals(name)) {
	          continue;
	        }
	        if ((!getPropertyUtils().isReadable(orig, name)) || (!getPropertyUtils().isWriteable(dest, name)))
	          continue;
	        try {
	          Object value = getPropertyUtils().getSimpleProperty(orig, name);

	          copyProperty(dest, name, value);
	        }
	        catch (NoSuchMethodException e)
	        {
	        }
	      }
	    }
	  }

	  public void copyProperty(Object bean, String name, Object value)
	    throws IllegalAccessException, InvocationTargetException
	  {
	    Object target = bean;
	    int delim = name.lastIndexOf('.');
	    if (delim >= 0) {
	      try {
	        target = getPropertyUtils().getProperty(bean, name.substring(0, delim));
	      }
	      catch (NoSuchMethodException e) {
	        return;
	      }
	      name = name.substring(delim + 1);

	    }

	    String propName = null;
	    Class type = null;
	    int index = -1;
	    String key = null;

	    propName = name;
	    int i = propName.indexOf('[');
	    if (i >= 0) {
	      int k = propName.indexOf(']');
	      try {
	        index = Integer.parseInt(propName.substring(i + 1, k));
	      }
	      catch (NumberFormatException e)
	      {
	      }
	      propName = propName.substring(0, i);
	    }
	    int j = propName.indexOf('(');
	    if (j >= 0) {
	      int k = propName.indexOf(')');
	      try {
	        key = propName.substring(j + 1, k);
	      }
	      catch (IndexOutOfBoundsException e) {
	      }
	      propName = propName.substring(0, j);
	    }

	
	      PropertyDescriptor descriptor = null;
	      try {
	        descriptor = getPropertyUtils().getPropertyDescriptor(target, name);

	        if (descriptor == null)
	          return;
	      }
	      catch (NoSuchMethodException e) {
	        return;
	      }
	      type = descriptor.getPropertyType();
	      if (type == null)
	      {
	        return;
	      }

	    if (index >= 0) {
	      try {
	        getPropertyUtils().setIndexedProperty(target, propName, index, value);
	      }
	      catch (NoSuchMethodException e) {
	        throw new InvocationTargetException(e, "Cannot set " + propName);
	      }
	    }
	    else if (key != null)
	    {
	      try
	      {
	        getPropertyUtils().setMappedProperty(target, propName, key, value);
	      }
	      catch (NoSuchMethodException e) {
	        throw new InvocationTargetException(e, "Cannot set " + propName);
	      }
	    }
	    else {
	      try {
	        getPropertyUtils().setSimpleProperty(target, propName, value);
	      } catch (NoSuchMethodException e) {
	        throw new InvocationTargetException(e, "Cannot set " + propName);
	      }
	    }
	  }
	
}
