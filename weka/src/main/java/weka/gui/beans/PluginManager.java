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
 *    PluginManager.java
 *    Copyright (C) 2011-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Class that manages a global map of plugins. The knowledge flow uses this to
 * manage plugins other than step components and perspectives. Is general
 * purpose, so can be used by other Weka components. Provides static methods for
 * registering and instantiating plugins.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 12407 $
 * @deprecated Use weka.core.PluginManager instead
 */
@Deprecated
public class PluginManager {

  /**
   * Add the supplied list of fully qualified class names to the disabled list
   * 
   * @param classnames a list of class names to add
   */
  public static synchronized void addToDisabledList(List<String> classnames) {
    weka.core.PluginManager.addToDisabledList(classnames);
  }

  /**
   * Add the supplied fully qualified class name to the list of disabled plugins
   * 
   * @param classname the fully qualified name of a class to add
   */
  public static synchronized void addToDisabledList(String classname) {
    weka.core.PluginManager.addToDisabledList(classname);
  }

  /**
   * Remove the supplied list of fully qualified class names to the disabled
   * list
   * 
   * @param classnames a list of class names to remove
   */
  public static synchronized void
    removeFromDisabledList(List<String> classnames) {
    weka.core.PluginManager.removeFromDisabledList(classnames);
  }

  /**
   * Remove the supplied fully qualified class name from the list of disabled
   * plugins
   * 
   * @param classname the fully qualified name of a class to remove
   */
  public static synchronized void removeFromDisabledList(String classname) {
    weka.core.PluginManager.removeFromDisabledList(classname);
  }

  /**
   * Returns true if the supplied fully qualified class name is in the disabled
   * list
   * 
   * @param classname the name of the class to check
   * @return true if the supplied class name is in the disabled list
   */
  public static boolean isInDisabledList(String classname) {
    return weka.core.PluginManager.isInDisabledList(classname);
  }

  /**
   * Add all key value pairs from the supplied property file
   *
   * @param propsFile the properties file to add
   * @throws Exception if a problem occurs
   */
  public static synchronized void addFromProperties(File propsFile)
    throws Exception {
    weka.core.PluginManager.addFromProperties(propsFile);
  }

  /**
   * Add all key value pairs from the supplied property file
   *
   * @param propsFile the properties file to add
   * @param maintainInsertionOrder true if the order of insertion of
   *          implementations is to be preserved (rather than sorted order)
   * @throws Exception if a problem occurs
   */
  public static synchronized void addFromProperties(File propsFile,
    boolean maintainInsertionOrder) throws Exception {
    weka.core.PluginManager
      .addFromProperties(propsFile, maintainInsertionOrder);
  }

  /**
   * Add all key value pairs from the supplied properties stream
   *
   * @param propsStream an input stream to a properties file
   * @throws Exception if a problem occurs
   */
  public static synchronized void addFromProperties(InputStream propsStream)
    throws Exception {
    weka.core.PluginManager.addFromProperties(propsStream);
  }

  /**
   * Add all key value pairs from the supplied properties stream
   *
   * @param propsStream an input stream to a properties file
   * @param maintainInsertionOrder true if the order of insertion of
   *          implementations is to be preserved (rather than sorted order)
   * @throws Exception if a problem occurs
   */
  public static synchronized void addFromProperties(InputStream propsStream,
    boolean maintainInsertionOrder) throws Exception {
    weka.core.PluginManager.addFromProperties(propsStream,
      maintainInsertionOrder);
  }

  /**
   * Add all key value pairs from the supplied properties object
   *
   * @param props a Properties object
   * @throws Exception if a problem occurs
   */
  public static synchronized void addFromProperties(Properties props)
    throws Exception {
    weka.core.PluginManager.addFromProperties(props);
  }

  /**
   * Add all key value pairs from the supplied properties object
   *
   * @param props a Properties object
   * @param maintainInsertionOrder true if the order of insertion of
   *          implementations is to be preserved (rather than sorted order)
   * @throws Exception if a problem occurs
   */
  public static synchronized void addFromProperties(Properties props,
    boolean maintainInsertionOrder) throws Exception {
    weka.core.PluginManager.addFromProperties(props, maintainInsertionOrder);
  }

  /**
   * Add a resource.
   * 
   * @param resourceGroupID the ID of the group under which the resource should
   *          be stored
   * @param resourceDescription the description/ID of the resource
   * @param resourcePath the path to the resource
   */
  public static synchronized void addPluginResource(String resourceGroupID,
    String resourceDescription, String resourcePath) {
    weka.core.PluginManager.addPlugin(resourceGroupID, resourceDescription,
      resourcePath);
  }

  /**
   * Get an input stream for a named resource under a given resource group ID.
   * 
   * @param resourceGroupID the group ID that the resource falls under
   * @param resourceDescription the description/ID of the resource
   * @return an InputStream for the resource
   * @throws IOException if the group ID or resource description/ID are not
   *           known to the PluginManager, or a problem occurs while trying to
   *           open an input stream
   */
  public static InputStream getPluginResourceAsStream(String resourceGroupID,
    String resourceDescription) throws IOException {
    return weka.core.PluginManager.getPluginResourceAsStream(resourceGroupID,
      resourceDescription);
  }

  /**
   * Get the number of resources available under a given resource group ID.
   *
   * @param resourceGroupID the group ID of the resources
   * @return the number of resources registered under the supplied group ID
   */
  public static int numResourcesForWithGroupID(String resourceGroupID) {
    return weka.core.PluginManager.numResourcesForWithGroupID(resourceGroupID);
  }

  /**
   * Get a map of resources (description,path) registered under a given resource
   * group ID.
   *
   * @param resourceGroupID the group ID of the resources to get
   * @return a map of resources registered under the supplied group ID, or null
   *         if the resourceGroupID is not known to the plugin manager
   */
  public static Map<String, String> getResourcesWithGroupID(
    String resourceGroupID) {
    return weka.core.PluginManager.getResourcesWithGroupID(resourceGroupID);
  }

  /**
   * Get a set of names of plugins that implement the supplied interface.
   * 
   * @param interfaceName the fully qualified name of the interface to list
   *          plugins for
   * 
   * @return a set of names of plugins
   */
  public static Set<String> getPluginNamesOfType(String interfaceName) {
    return weka.core.PluginManager.getPluginNamesOfType(interfaceName);
  }

  /**
   * Add a plugin.
   *
   * @param interfaceName the fully qualified interface name that the plugin
   *          implements
   *
   * @param name the name/short description of the plugin
   * @param concreteType the fully qualified class name of the actual concrete
   *          implementation
   */
  public static void addPlugin(String interfaceName, String name,
    String concreteType) {
    weka.core.PluginManager.addPlugin(interfaceName, name, concreteType);
  }

  /**
   * Add a plugin.
   *
   * @param interfaceName the fully qualified interface name that the plugin
   *          implements
   *
   * @param name the name/short description of the plugin
   * @param concreteType the fully qualified class name of the actual concrete
   *          implementation
   * @param maintainInsertionOrder true if the order of insertion of
   *          implementations is to be preserved (rather than sorted order)
   */
  public static void addPlugin(String interfaceName, String name,
    String concreteType, boolean maintainInsertionOrder) {
    weka.core.PluginManager.addPlugin(interfaceName, name, concreteType,
      maintainInsertionOrder);
  }

  /**
   * Remove plugins of a specific type.
   * 
   * @param interfaceName the fully qualified interface name that the plugins to
   *          be remove implement
   * @param names a list of named plugins to remove
   */
  public static void removePlugins(String interfaceName, List<String> names) {
    for (String name : names) {
      weka.core.PluginManager.removePlugins(interfaceName, names);
    }
  }

  /**
   * Remove a plugin.
   * 
   * @param interfaceName the fully qualified interface name that the plugin
   *          implements
   * 
   * @param name the name/short description of the plugin
   */
  public static void removePlugin(String interfaceName, String name) {
    weka.core.PluginManager.removePlugin(interfaceName, name);
  }

  /**
   * Get an instance of a concrete implementation of a plugin type
   * 
   * @param interfaceType the fully qualified interface name of the plugin type
   * @param name the name/short description of the plugin to get
   * @return the concrete plugin or null if the plugin is disabled
   * @throws Exception if the plugin can't be found or instantiated
   */
  public static Object getPluginInstance(String interfaceType, String name)
    throws Exception {
    return weka.core.PluginManager.getPluginInstance(interfaceType, name);
  }
}
