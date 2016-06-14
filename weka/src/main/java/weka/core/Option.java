/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    Option.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Class to store information about an option.
 * <p>
 *
 * Typical usage:
 * <p>
 *
 * <code>Option myOption = new Option("Uses extended mode.", "E", 0, "-E")); </code>
 * <p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 12505 $
 */
public class Option implements RevisionHandler {

  /** A cache of property descriptors */
  private static final Map<Class<?>, PropertyDescriptor[]> s_descriptorCache =
    new HashMap<Class<?>, PropertyDescriptor[]>();

  /** What does this option do? */
  private String m_Description;

  /** The synopsis. */
  private String m_Synopsis;

  /** What's the option's name? */
  private String m_Name;

  /** How many arguments does it take? */
  private int m_NumArguments;

  /**
   * Creates new option with the given parameters.
   *
   * @param description the option's description
   * @param name the option's name
   * @param numArguments the number of arguments
   * @param synopsis the option's synopsis
   */
  public Option(String description, String name, int numArguments,
    String synopsis) {

    m_Description = description;
    m_Name = name;
    m_NumArguments = numArguments;
    m_Synopsis = synopsis;
  }

  /**
   * Get a list of options for a class. Options identified by this method are
   * bean properties (with get/set methods) annotated using the OptionMetadata
   * annotation. All options from the class up to, but not including, the
   * supplied oldest superclass are returned.
   * 
   * 
   * @param childClazz the class to get options for
   * @param oldestAncestorClazz the oldest superclass (inclusive) at which to
   *          stop getting options from
   * @return a list of options
   */
  public static Vector<Option> listOptionsForClassHierarchy(Class<?> childClazz,
    Class<?> oldestAncestorClazz) {
    Vector<Option> results = listOptionsForClass(childClazz);

    Class<?> parent = childClazz;
    do {
      parent = parent.getSuperclass();
      if (parent == null) {
        break;
      }
      results.addAll(listOptionsForClass(parent));
    } while (!parent.equals(oldestAncestorClazz));

    return results;
  }

  /**
   * Adds all methods from the supplied class to the supplied list of methods.
   * 
   * @param clazz the class to get methods from
   * @param methList the list to add them to
   */
  protected static void addMethodsToList(Class<?> clazz,
    List<Method> methList) {
    Method[] methods = clazz.getDeclaredMethods();
    for (Method m : methods) {
      methList.add(m);
    }
  }

  /**
   * Gets a list of options for the supplied class. Only examines immediate
   * methods in the class (does not consider superclasses). Options identified
   * by this method are bean properties (with get/set methods) annotated using
   * the OptionMetadata annotation.
   * 
   * @param clazz the class to examine for options
   * @return a list of options
   */
  public static Vector<Option> listOptionsForClass(Class<?> clazz) {
    Vector<Option> results = new Vector<Option>();
    List<Method> allMethods = new ArrayList<Method>();
    addMethodsToList(clazz, allMethods);

    Class<?>[] interfaces = clazz.getInterfaces();
    for (Class c : interfaces) {
      addMethodsToList(c, allMethods);
    }

    Option[] unsorted = new Option[allMethods.size()];
    int[] opOrder = new int[allMethods.size()];
    for (int i = 0; i < opOrder.length; i++) {
      opOrder[i] = Integer.MAX_VALUE;
    }
    int index = 0;
    for (Method m : allMethods) {
      OptionMetadata o = m.getAnnotation(OptionMetadata.class);
      if (o != null) {
        if (o.commandLineParamName().length() > 0) {
          opOrder[index] = o.displayOrder();
          String description = o.description();
          if (!description.startsWith("\t")) {
            description = "\t" + description;
          }
          description = description.replace("\n", "\n\t");
          String name = o.commandLineParamName();
          if (name.startsWith("-")) {
            name = name.substring(1, name.length());
          }
          String synopsis = o.commandLineParamSynopsis();
          if (!synopsis.startsWith("-")) {
            synopsis = "-" + synopsis;
          }
          int numParams = o.commandLineParamIsFlag() ? 0 : 1;
          Option option = new Option(description, name, numParams, synopsis);
          unsorted[index] = option;
          index++;
        }
      }
    }

    int[] sortedOpts = Utils.sort(opOrder);
    for (int i = 0; i < opOrder.length; i++) {
      if (opOrder[i] < Integer.MAX_VALUE) {
        results.add(unsorted[sortedOpts[i]]);
      }
    }

    return results;
  }

  /**
   * Get the settings of the supplied object. Settings identified by this method
   * are bean properties (with get/set methods) annotated using the
   * OptionMetadata annotation. All options from the class up to, but not
   * including, the supplied oldest superclass are returned.
   * 
   * @param target the target object to get settings for
   * @param oldestAncestorClazz the oldest superclass at which to stop getting
   *          options from
   * @return
   */
  public static String[] getOptionsForHierarchy(Object target,
    Class<?> oldestAncestorClazz) {

    ArrayList<String> options = new ArrayList<String>();
    for (String s : getOptions(target, target.getClass())) {
      options.add(s);
    }

    Class<?> parent = target.getClass();
    do {
      parent = parent.getSuperclass();
      if (parent == null) {
        break;
      }
      for (String s : getOptions(target, parent)) {
        options.add(s);
      }
    } while (!parent.equals(oldestAncestorClazz));

    return options.toArray(new String[options.size()]);
  }

  /**
   * Get the settings of the supplied object. Settings identified by this method
   * are bean properties (with get/set methods) annotated using the
   * OptionMetadata annotation. Options belonging to the targetClazz (either the
   * class of the target or one of its superclasses) are returned.
   * 
   * @param target the target to extract settings from
   * @param targetClazz the class to consider for obtaining settings - i.e.
   *          annotated methods from this class will have their values
   *          extracted. This class is expected to be either the class of the
   *          target or one of its superclasses
   * @return an array of settings
   */
  public static String[] getOptions(Object target, Class<?> targetClazz) {

    ArrayList<String> options = new ArrayList<String>();
    try {
      Object[] args = {};
      Class<?> parent = targetClazz.getSuperclass();
      PropertyDescriptor[] properties =
        getPropertyDescriptors(targetClazz, parent);

      for (PropertyDescriptor p : properties) {
        Method getter = p.getReadMethod();
        Method setter = p.getWriteMethod();
        if (getter == null || setter == null) {
          continue;
        }
        OptionMetadata parameterDescription = null;
        parameterDescription = getter.getAnnotation(OptionMetadata.class);
        if (parameterDescription == null) {
          parameterDescription = setter.getAnnotation(OptionMetadata.class);
        }

        if (parameterDescription != null
          && parameterDescription.commandLineParamName().length() > 0) {
          Object value = getter.invoke(target, args);
          if (value != null) {
            if (!parameterDescription.commandLineParamIsFlag()) {
              options.add("-" + parameterDescription.commandLineParamName());
            }

            if (value.getClass().isArray()) {
              if (parameterDescription.commandLineParamIsFlag()) {
                throw new IllegalArgumentException(
                  "Getter method for a command "
                    + "line flag should return a boolean value");
              }
              int index = 0;
              for (Object element : (Object[]) value) {
                if (index > 0) {
                  options
                    .add("-" + parameterDescription.commandLineParamName());
                }
                if (element instanceof OptionHandler) {
                  options.add(
                    getOptionStringForOptionHandler((OptionHandler) element));
                } else {
                  options.add(element.toString());
                }
                index++;
              }
            } else if (value instanceof OptionHandler) {
              if (parameterDescription.commandLineParamIsFlag()) {
                throw new IllegalArgumentException(
                  "Getter method for a command "
                    + "line flag should return a boolean value");
              }
              options
                .add(getOptionStringForOptionHandler((OptionHandler) value));
            } else if (value instanceof SelectedTag) {
              options
                .add("" + ((SelectedTag) value).getSelectedTag().getReadable());
            } else {
              // check for boolean/flag
              if (parameterDescription.commandLineParamIsFlag()) {
                if (!(value instanceof Boolean)) {
                  throw new IllegalArgumentException(
                    "Getter method for a command "
                      + "line flag should return a boolean value");
                }
                if (((Boolean) value).booleanValue()) {
                  options
                    .add("-" + parameterDescription.commandLineParamName());
                }
              } else {
                if (value.toString().length() > 0) {
                  options.add(value.toString());
                } else {
                  // don't allow empty strings
                  options.remove(options.size() - 1);
                }
              }
            }
          }
        }
      }

    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return options.toArray(new String[options.size()]);
  }

  /**
   * Construct a String containing the class name of an OptionHandler and its
   * option settings
   *
   * @param handler the OptionHandler to construct an option string for
   * @return a String containing the name of the handler class and its options
   */
  protected static String
    getOptionStringForOptionHandler(OptionHandler handler) {
    String optHandlerClassName = handler.getClass().getCanonicalName();
    String optsVal = Utils.joinOptions(handler.getOptions());
    String totalOptVal = optHandlerClassName + " " + optsVal;

    return totalOptVal;
  }

  /**
   * Sets options on the target object. Settings identified by this method are
   * bean properties (with get/set methods) annotated using the OptionMetadata
   * annotation. All options from the class up to, but not including, the
   * supplied oldest superclass are processed in order.
   *
   * @param options the options to set
   * @param target the target on which to set options
   * @param oldestAncestorClazz the oldest superclass at which to stop setting
   *          options
   */
  public static void setOptionsForHierarchy(String[] options, Object target,
    Class<?> oldestAncestorClazz) {

    setOptions(options, target, target.getClass());

    Class<?> parent = target.getClass();
    do {
      parent = parent.getSuperclass();
      if (parent == null) {
        break;
      }

      setOptions(options, target, parent);
    } while (!parent.equals(oldestAncestorClazz));
  }

  /**
   * Get property descriptors for a target class. Checks a cache first before
   * using introspection.
   *
   * @param targetClazz the target to get the descriptors for
   * @param parent the parent class at which to stop getting descriptors
   * @return an array of property descriptors
   * @throws IntrospectionException if a problem occurs
   */
  private static PropertyDescriptor[] getPropertyDescriptors(
    Class<?> targetClazz, Class<?> parent) throws IntrospectionException {

    PropertyDescriptor[] result = s_descriptorCache.get(targetClazz);
    if (result == null) {
      BeanInfo bi = Introspector.getBeanInfo(targetClazz, parent);
      result = bi.getPropertyDescriptors();
      s_descriptorCache.put(targetClazz, result);
    }

    return result;
  }

  /**
   * Sets options on the target object. Settings identified by this method are
   * bean properties (with get/set methods) annotated using the OptionMetadata
   * annotation. Options from just the supplied targetClazz (which is expected
   * to be either the class of the target or one of its superclasses) are set.
   *
   * @param options the options to set
   * @param target the target on which to set options
   * @param targetClazz the class containing options to be be set - i.e.
   *          annotated option methods in this class will have their values set.
   *          This class is expected to be either the class of the target or one
   *          of its superclasses
   */
  public static void setOptions(String[] options, Object target,
    Class<?> targetClazz) {
    if (options != null && options.length > 0) {
      // Set the options just for this class
      try {
        Object[] getterArgs = {};
        Class<?> parent = targetClazz.getSuperclass();
        PropertyDescriptor[] properties =
          getPropertyDescriptors(targetClazz, parent);

        for (PropertyDescriptor p : properties) {
          Method getter = p.getReadMethod();
          Method setter = p.getWriteMethod();
          if (getter == null || setter == null) {
            continue;
          }
          OptionMetadata parameterDescription = null;
          parameterDescription = getter.getAnnotation(OptionMetadata.class);
          if (parameterDescription == null) {
            parameterDescription = setter.getAnnotation(OptionMetadata.class);
          }

          if (parameterDescription != null
            && parameterDescription.commandLineParamName().length() > 0) {
            boolean processOpt = false;
            String optionValue = "";
            Object valueToSet = null;
            if (parameterDescription.commandLineParamIsFlag()) {
              processOpt = true;
              valueToSet = (Utils
                .getFlag(parameterDescription.commandLineParamName(), options));
            } else {
              optionValue = Utils.getOption(
                parameterDescription.commandLineParamName(), options);
              processOpt = optionValue.length() > 0;
            }

            // grab the default/current return value so that we can determine
            // the type
            Object value = getter.invoke(target, getterArgs);
            if (value != null && processOpt) {
              if (value.getClass().isArray()
                && ((Object[]) value).length >= 0) {
                // We're interested in the actual element type...
                Class<?> elementType =
                  getter.getReturnType().getComponentType();

                // handle arrays by looking for the option multiple times
                List<String> optionValues = new ArrayList<String>();
                optionValues.add(optionValue);
                while (true) {
                  optionValue = Utils.getOption(
                    parameterDescription.commandLineParamName(), options);
                  if (optionValue.length() == 0) {
                    break;
                  }
                  optionValues.add(optionValue);
                }

                valueToSet =
                  Array.newInstance(elementType, optionValues.size());
                for (int i = 0; i < optionValues.size(); i++) {
                  Object elementObject = null;
                  if (elementType.isAssignableFrom(File.class)) {
                    elementObject = new File(optionValues.get(i));
                  } else {
                    elementObject =
                      constructOptionHandlerValue(optionValues.get(i));
                  }
                  Array.set(valueToSet, i, elementObject);
                }
              } else if (value instanceof SelectedTag) {
                Tag[] legalTags = ((SelectedTag) value).getTags();
                int tagIndex = Integer.MAX_VALUE;
                // first try and parse as an integer
                try {
                  int specifiedID = Integer.parseInt(optionValue);
                  for (int z = 0; z < legalTags.length; z++) {
                    if (legalTags[z].getID() == specifiedID) {
                      tagIndex = z;
                      break;
                    }
                  }
                } catch (NumberFormatException e) {
                  // try to match tag strings
                  for (int z = 0; z < legalTags.length; z++) {
                    if (legalTags[z].getReadable().equals(optionValue.trim())) {
                      tagIndex = z;
                      break;
                    }
                  }
                }
                if (tagIndex != Integer.MAX_VALUE) {
                  valueToSet = new SelectedTag(tagIndex, legalTags);
                } else {
                  throw new Exception("Unable to set option: '"
                    + parameterDescription.commandLineParamName()
                    + "'. This option takes a SelectedTag argument, and "
                    + "the supplied value of '" + optionValue + "' "
                    + "does not match any of the legal IDs or strings "
                    + "for it.");
                }
              } else if (value instanceof OptionHandler) {
                valueToSet = constructOptionHandlerValue(optionValue);
              } else if (value instanceof Number) {
                try {
                  if (value instanceof Integer) {
                    valueToSet = new Integer(optionValue);
                  } else if (value instanceof Long) {
                    valueToSet = new Long(optionValue);
                  } else if (value instanceof Double) {
                    valueToSet = new Double(optionValue);
                  } else if (value instanceof Float) {
                    valueToSet = new Float(optionValue);
                  }
                } catch (NumberFormatException e) {
                  throw new Exception(
                    "Option: '" + parameterDescription.commandLineParamName()
                      + "' requires a " + value.getClass().getCanonicalName()
                      + " argument");
                }
              } else if (value instanceof String) {
                valueToSet = optionValue;
              } else if (value instanceof File) {
                valueToSet = new File(optionValue);
              }

              if (valueToSet != null) {
                // set it!
                // System.err.println("Setter: " + setter.getName());
                // System.err.println("Argument: " +
                // valueToSet.getClass().getCanonicalName());
                setOption(setter, target, valueToSet);
              }
            }
          }
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  /**
   * Construct an instance of an option handler from a String specifying its
   * class name and option values
   *
   * @param optionValue a String containing the class of the option handler
   *          followed by its options
   * @return an instantiated option handling object
   * @throws Exception if a problem occurs
   */
  protected static Object constructOptionHandlerValue(String optionValue)
    throws Exception {
    String[] optHandlerSpec = Utils.splitOptions(optionValue);
    if (optHandlerSpec.length == 0) {
      throw new Exception(
        "Invalid option handler specification " + "string '" + optionValue);
    }
    String optionHandler = optHandlerSpec[0];
    optHandlerSpec[0] = "";
    Object handler = Utils.forName(null, optionHandler, optHandlerSpec);

    return handler;
  }

  /**
   * Removes an option from a given list of options.
   *
   * @param list the list to reduce
   * @param name the name of the option
   */
  public static void deleteOption(List<Option> list, String name) {

    for (Iterator<Option> iter = list.listIterator(); iter.hasNext(); ) {
      Option a = iter.next();
      if (a.name().equals(name)) {
        iter.remove();
      }
    }
  }

  /**
   * Removes an option from a given list of strings that specifies options.
   * This method is for an option that has a parameter value.
   *
   * @param list the list to reduce
   * @param name the name of the option (including hyphen)
   */
  public static void deleteOptionString(List<String> list, String name) {

    for (Iterator<String> iter = list.listIterator(); iter.hasNext(); ) {
      String a = iter.next();
      if (a.equals(name)) {
        iter.remove();
        iter.next();
        iter.remove();
      }
    }
  }

  /**
   * Removes an option from a given list of strings that specifies options.
   * This method is for an option without a parameter value (i.e., a flag).
   *
   * @param list the list to reduce
   * @param name the name of the option (including hyphen)
   */
  public static void deleteFlagString(List<String> list, String name) {

    for (Iterator<String> iter = list.listIterator(); iter.hasNext(); ) {
      String a = iter.next();
      if (a.equals(name)) {
        iter.remove();
      }
    }
  }

  /**
   * Set an option value on a target object
   *
   * @param setter the Method object for the setter method of the option to set
   * @param target the target object on which to set the option
   * @param valueToSet the value of the option to set
   * @throws InvocationTargetException if a problem occurs
   * @throws IllegalAccessException if a problem occurs
   */
  protected static void setOption(Method setter, Object target,
    Object valueToSet)
      throws InvocationTargetException, IllegalAccessException {
    Object[] setterArgs = { valueToSet };
    setter.invoke(target, setterArgs);
  }

  /**
   * Returns the option's description.
   *
   * @return the option's description
   */
  public String description() {

    return m_Description;
  }

  /**
   * Returns the option's name.
   *
   * @return the option's name
   */
  public String name() {

    return m_Name;
  }

  /**
   * Returns the option's number of arguments.
   *
   * @return the option's number of arguments
   */
  public int numArguments() {

    return m_NumArguments;
  }

  /**
   * Returns the option's synopsis.
   *
   * @return the option's synopsis
   */
  public String synopsis() {

    return m_Synopsis;
  }

  /**
   * Returns the revision string.
   *
   * @return the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 12505 $");
  }
}
