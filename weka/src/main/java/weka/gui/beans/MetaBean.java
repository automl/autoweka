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
 *    MetaBean.java
 *    Copyright (C) 2005-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.BeanInfo;
import java.beans.EventSetDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;

import weka.gui.Logger;

/**
 * A meta bean that encapsulates several other regular beans, useful for
 * grouping large KnowledgeFlows.
 * 
 * 
 * @author Mark Hall (mhall at cs dot waikato dot ac dot nz)
 * @version $Revision: 10221 $
 */
public class MetaBean extends JPanel implements BeanCommon, Visible,
  EventConstraints, Serializable, UserRequestAcceptor, Startable {

  /** for serialization */
  private static final long serialVersionUID = -6582768902038027077L;

  protected BeanVisual m_visual = new BeanVisual("Group", BeanVisual.ICON_PATH
    + "DiamondPlain.gif", BeanVisual.ICON_PATH + "DiamondPlain.gif");

  private transient Logger m_log = null;
  private transient JWindow m_previewWindow = null;
  private transient javax.swing.Timer m_previewTimer = null;

  protected Vector<Object> m_subFlow = new Vector<Object>();
  protected Vector<Object> m_inputs = new Vector<Object>();
  protected Vector<Object> m_outputs = new Vector<Object>();

  // the internal connections for the grouping
  protected Vector<BeanConnection> m_associatedConnections = new Vector<BeanConnection>();

  // Holds a preview image of the encapsulated sub-flow
  protected ImageIcon m_subFlowPreview = null;

  // offset from where originally grouped if the meta bean
  // dropped onto the canvas from the user toolbar
  protected int m_xCreate = 0;
  protected int m_yCreate = 0;
  protected int m_xDrop = 0;
  protected int m_yDrop = 0;

  public MetaBean() {
    setLayout(new BorderLayout());
    add(m_visual, BorderLayout.CENTER);
  }

  /**
   * Set a custom (descriptive) name for this bean
   * 
   * @param name the name to use
   */
  @Override
  public void setCustomName(String name) {
    m_visual.setText(name);
  }

  /**
   * Get the custom (descriptive) name for this bean (if one has been set)
   * 
   * @return the custom name (or the default name)
   */
  @Override
  public String getCustomName() {
    return m_visual.getText();
  }

  public void setAssociatedConnections(Vector<BeanConnection> ac) {
    m_associatedConnections = ac;
  }

  public Vector<BeanConnection> getAssociatedConnections() {
    return m_associatedConnections;
  }

  public void setSubFlow(Vector<Object> sub) {
    m_subFlow = sub;
  }

  public Vector<Object> getSubFlow() {
    return m_subFlow;
  }

  public void setInputs(Vector<Object> inputs) {
    m_inputs = inputs;
  }

  public Vector<Object> getInputs() {
    return m_inputs;
  }

  public void setOutputs(Vector<Object> outputs) {
    m_outputs = outputs;
  }

  public Vector<Object> getOutputs() {
    return m_outputs;
  }

  private Vector<Object> getBeans(Vector<Object> beans, int type) {
    Vector<Object> comps = new Vector<Object>();
    for (int i = 0; i < beans.size(); i++) {
      BeanInstance temp = (BeanInstance) beans.elementAt(i);
      // need to check for sub MetaBean!
      if (temp.getBean() instanceof MetaBean) {
        switch (type) {
        case 0:
          comps.addAll(((MetaBean) temp.getBean()).getBeansInSubFlow());
          break;
        case 1:
          comps.addAll(((MetaBean) temp.getBean()).getBeansInInputs());
          break;
        case 2:
          comps.addAll(((MetaBean) temp.getBean()).getBeansInOutputs());
          break;
        }
      } else {
        comps.add(temp);
      }
    }
    return comps;
  }

  private boolean beanSetContains(Vector<Object> set, BeanInstance toCheck) {
    boolean ok = false;

    for (int i = 0; i < set.size(); i++) {
      BeanInstance temp = (BeanInstance) set.elementAt(i);
      if (toCheck == temp) {
        ok = true;
        break;
      }
    }
    return ok;
  }

  public boolean subFlowContains(BeanInstance toCheck) {
    return beanSetContains(m_subFlow, toCheck);
  }

  public boolean inputsContains(BeanInstance toCheck) {
    return beanSetContains(m_inputs, toCheck);
  }

  public boolean outputsContains(BeanInstance toCheck) {
    return beanSetContains(m_outputs, toCheck);
  }

  /**
   * Return all the beans in the sub flow
   * 
   * @return a Vector of all the beans in the sub flow
   */
  public Vector<Object> getBeansInSubFlow() {
    return getBeans(m_subFlow, 0);
  }

  /**
   * Return all the beans in the inputs
   * 
   * @return a Vector of all the beans in the inputs
   */
  public Vector<Object> getBeansInInputs() {
    return getBeans(m_inputs, 1);
  }

  /**
   * Return all the beans in the outputs
   * 
   * @return a Vector of all the beans in the outputs
   */
  public Vector<Object> getBeansInOutputs() {
    return getBeans(m_outputs, 2);
  }

  private Vector<BeanInfo> getBeanInfos(Vector<Object> beans, int type) {
    Vector<BeanInfo> infos = new Vector<BeanInfo>();
    for (int i = 0; i < beans.size(); i++) {
      BeanInstance temp = (BeanInstance) beans.elementAt(i);
      if (temp.getBean() instanceof MetaBean) {
        switch (type) {
        case 0:
          infos.addAll(((MetaBean) temp.getBean()).getBeanInfoSubFlow());
          break;
        case 1:
          infos.addAll(((MetaBean) temp.getBean()).getBeanInfoInputs());
          break;
        case 2:
          infos.addAll(((MetaBean) temp.getBean()).getBeanInfoOutputs());
        }
      } else {
        try {
          infos.add(Introspector.getBeanInfo(temp.getBean().getClass()));
        } catch (IntrospectionException ex) {
          ex.printStackTrace();
        }
      }
    }
    return infos;
  }

  public Vector<BeanInfo> getBeanInfoSubFlow() {
    return getBeanInfos(m_subFlow, 0);
  }

  public Vector<BeanInfo> getBeanInfoInputs() {
    return getBeanInfos(m_inputs, 1);
  }

  public Vector<BeanInfo> getBeanInfoOutputs() {
    return getBeanInfos(m_outputs, 2);
  }

  // stores the original position of the beans
  // when this group is created. Used
  // to restore their locations if the group is ungrouped.
  private Vector<Point> m_originalCoords;

  /**
   * returns the vector containing the original coordinates (instances of class
   * Point) for the inputs
   * 
   * @return the containing the coord Points of the original inputs
   */
  public Vector<Point> getOriginalCoords() {
    return m_originalCoords;
  }

  /**
   * sets the vector containing the original coordinates (instances of class
   * Point) for the inputs
   * 
   * @param value the vector containing the points of the coords of the original
   *          inputs
   */
  public void setOriginalCoords(Vector<Point> value) {
    m_originalCoords = value;
  }

  /**
   * Move coords of all inputs and outputs of this meta bean to the coords of
   * the supplied BeanInstance. Typically the supplied BeanInstance is the
   * BeanInstance that encapsulates this meta bean; the result in this case is
   * that all inputs and outputs are shifted so that their coords coincide with
   * the meta bean and all connections to them appear (visually) to go to/from
   * the meta bean.
   * 
   * @param toShiftTo the BeanInstance whos coordinates will be used.
   * @param save true if coordinates are to be saved.
   */
  public void shiftBeans(BeanInstance toShiftTo, boolean save) {
    if (save) {
      m_originalCoords = new Vector<Point>();
    }
    int targetX = toShiftTo.getX();
    int targetY = toShiftTo.getY();

    for (int i = 0; i < m_subFlow.size(); i++) {
      BeanInstance temp = (BeanInstance) m_subFlow.elementAt(i);
      if (save) {
        // save offsets from this point
        Point p = new Point(temp.getX() - targetX, temp.getY() - targetY);
        m_originalCoords.add(p);
      }
      temp.setX(targetX);
      temp.setY(targetY);
    }
  }

  public void restoreBeans(int x, int y) {
    for (int i = 0; i < m_subFlow.size(); i++) {
      BeanInstance temp = (BeanInstance) m_subFlow.elementAt(i);
      Point p = m_originalCoords.elementAt(i);
      JComponent c = (JComponent) temp.getBean();
      c.getPreferredSize();
      temp.setX(x + (int) p.getX());// + (m_xDrop - m_xCreate));
      temp.setY(y + (int) p.getY());// + (m_yDrop - m_yCreate));
    }
  }

  /**
   * Returns true, if at the current time, the event described by the supplied
   * event descriptor could be generated.
   * 
   * @param esd an <code>EventSetDescriptor</code> value
   * @return a <code>boolean</code> value
   */
  public boolean eventGeneratable(EventSetDescriptor esd) {
    String eventName = esd.getName();
    return eventGeneratable(eventName);
  }

  /**
   * Returns true, if at the current time, the named event could be generated.
   * Assumes that the supplied event name is an event that could be generated by
   * this bean
   * 
   * @param eventName the name of the event in question
   * @return true if the named event could be generated at this point in time
   */
  @Override
  public boolean eventGeneratable(String eventName) {
    for (int i = 0; i < m_subFlow.size(); i++) {
      BeanInstance output = (BeanInstance) m_subFlow.elementAt(i);
      if (output.getBean() instanceof EventConstraints) {
        if (((EventConstraints) output.getBean()).eventGeneratable(eventName)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns true if, at this time, the object will accept a connection with
   * respect to the supplied EventSetDescriptor
   * 
   * @param esd the EventSetDescriptor
   * @return true if the object will accept a connection
   */
  @Override
  public boolean connectionAllowed(EventSetDescriptor esd) {
    Vector<BeanInstance> targets = getSuitableTargets(esd);
    for (int i = 0; i < targets.size(); i++) {
      BeanInstance input = targets.elementAt(i);
      if (input.getBean() instanceof BeanCommon) {
        // if (((BeanCommon)input.getBean()).connectionAllowed(esd.getName())) {
        if (((BeanCommon) input.getBean()).connectionAllowed(esd)) {
          return true;
        }
      } else {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean connectionAllowed(String eventName) {
    return false;
  }

  /**
   * Notify this object that it has been registered as a listener with a source
   * with respect to the named event. This is just a dummy method in this class
   * to satisfy the interface. Specific code in BeanConnection takes care of
   * this method for MetaBeans
   * 
   * @param eventName the event
   * @param source the source with which this object has been registered as a
   *          listener
   */
  @Override
  public synchronized void connectionNotification(String eventName,
    Object source) {
  }

  /**
   * Notify this object that it has been deregistered as a listener with a
   * source with respect to the supplied event name. This is just a dummy method
   * in this class to satisfy the interface. Specific code in BeanConnection
   * takes care of this method for MetaBeans
   * 
   * @param eventName the event
   * @param source the source with which this object has been registered as a
   *          listener
   */
  @Override
  public synchronized void disconnectionNotification(String eventName,
    Object source) {

  }

  /**
   * Stop all encapsulated beans
   */
  @Override
  public void stop() {
    for (int i = 0; i < m_subFlow.size(); i++) {
      Object temp = m_subFlow.elementAt(i);
      if (temp instanceof BeanCommon) {
        ((BeanCommon) temp).stop();
      }
    }
  }

  /**
   * Returns true if. at this time, the bean is busy with some (i.e. perhaps a
   * worker thread is performing some calculation).
   * 
   * @return true if the bean is busy.
   */
  @Override
  public boolean isBusy() {
    boolean result = false;
    for (int i = 0; i < m_subFlow.size(); i++) {
      Object temp = m_subFlow.elementAt(i);
      if (temp instanceof BeanCommon) {
        if (((BeanCommon) temp).isBusy()) {
          result = true;
          break;
        }
      }
    }
    return result;
  }

  /**
   * Sets the visual appearance of this wrapper bean
   * 
   * @param newVisual a <code>BeanVisual</code> value
   */
  @Override
  public void setVisual(BeanVisual newVisual) {
    m_visual = newVisual;
  }

  /**
   * Gets the visual appearance of this wrapper bean
   */
  @Override
  public BeanVisual getVisual() {
    return m_visual;
  }

  /**
   * Use the default visual appearance for this bean
   */
  @Override
  public void useDefaultVisual() {
    m_visual.loadIcons(BeanVisual.ICON_PATH + "DiamondPlain.gif",
      BeanVisual.ICON_PATH + "DiamondPlain.gif");
  }

  @Override
  public String getStartMessage() {
    String message = "Start loading";
    for (int i = 0; i < m_subFlow.size(); i++) {
      BeanInstance temp = (BeanInstance) m_subFlow.elementAt(i);
      if (temp.getBean() instanceof Startable) {
        String s = ((Startable) temp.getBean()).getStartMessage();
        if (s.startsWith("$")) {
          message = "$" + message;
          break;
        }
      }
    }

    return message;
  }

  @Override
  public void start() {
    TreeMap<Integer, Startable> startables = new TreeMap<Integer, Startable>();
    for (int i = 0; i < m_subFlow.size(); i++) {
      BeanInstance temp = (BeanInstance) m_subFlow.elementAt(i);
      if (temp.getBean() instanceof Startable) {
        Startable s = (Startable) temp.getBean();
        String beanName = s.getClass().getName();
        String customName = beanName;

        boolean ok = false;
        Integer position = null;
        boolean launch = true;
        // String beanName = s.getClass().getName();
        if (s instanceof BeanCommon) {
          customName = ((BeanCommon) s).getCustomName();
          beanName = customName;
          // see if we have a parseable integer at the start of the name
          if (customName.indexOf(':') > 0) {
            if (customName.substring(0, customName.indexOf(':'))
              .startsWith("!")) {
              launch = false;
            } else {
              String startPos = customName
                .substring(0, customName.indexOf(':'));

              try {
                position = new Integer(startPos);
                ok = true;
              } catch (NumberFormatException n) {
              }
            }
          }
        }

        if (!ok && launch) {
          if (startables.size() == 0) {
            position = new Integer(0);
          } else {
            int newPos = startables.lastKey().intValue();
            newPos++;
            position = new Integer(newPos);
          }
        }

        if (s.getStartMessage().charAt(0) != '$') {
          if (launch) {
            if (m_log != null) {
              m_log.logMessage(statusMessagePrefix() + "adding start point "
                + beanName + " to the execution list (position " + position
                + ")");
            }
            startables.put(position, s);
          }
        }
      }
    }

    if (startables.size() > 0) {
      if (m_log != null) {
        m_log.logMessage(statusMessagePrefix() + "Starting "
          + startables.size() + " sub-flow start points sequentially.");
      }
      Set<Integer> s = startables.keySet();
      for (Integer i : s) {
        try {
          Startable startPoint = startables.get(i);
          String bN = startPoint.getClass().getName();
          if (startPoint instanceof BeanCommon) {
            bN = ((BeanCommon) startPoint).getCustomName();
          }
          if (m_log != null) {
            m_log.statusMessage(statusMessagePrefix()
              + "Starting sub-flow start point: " + bN);
            m_log.logMessage(statusMessagePrefix()
              + "Starting sub-flow start point: " + bN);
          }
          startPoint.start();
          Thread.sleep(500);
          while (isBusy()) {
            Thread.sleep(2000);
          }
        } catch (Exception ex) {
          if (m_log != null) {
            m_log.logMessage(statusMessagePrefix()
              + "A problem occurred when launching start points in sub-flow: "
              + ex.getMessage());
          }
          stop();
          if (m_log != null) {
            m_log.statusMessage(statusMessagePrefix()
              + "ERROR (see log for details)");
          }
        }
      }
      if (m_log != null) {
        m_log.statusMessage(statusMessagePrefix() + "Finished.");
      }
    }
  }

  /**
   * Return an enumeration of requests that can be made by the user
   * 
   * @return an <code>Enumeration</code> value
   */
  @Override
  public Enumeration<String> enumerateRequests() {
    Vector<String> newVector = new Vector<String>();
    if (m_subFlowPreview != null) {
      String text = "Show preview";
      if (m_previewWindow != null) {
        text = "$" + text;
      }
      newVector.addElement(text);
    }
    for (int i = 0; i < m_subFlow.size(); i++) {
      BeanInstance temp = (BeanInstance) m_subFlow.elementAt(i);
      if (temp.getBean() instanceof UserRequestAcceptor) {
        String prefix = "";
        if ((temp.getBean() instanceof BeanCommon)) {
          prefix = ((BeanCommon) temp.getBean()).getCustomName();
        } else {
          prefix = temp.getBean().getClass().getName();
          prefix = prefix.substring(prefix.lastIndexOf('.') + 1,
            prefix.length());
        }
        prefix = "" + (i + 1) + ": (" + prefix + ")";
        Enumeration<String> en = ((UserRequestAcceptor) temp.getBean())
          .enumerateRequests();
        while (en.hasMoreElements()) {
          String req = en.nextElement();
          if (req.charAt(0) == '$') {
            prefix = '$' + prefix;
            req = req.substring(1, req.length());
          }

          if (req.charAt(0) == '?') {
            prefix = '?' + prefix;
            req = req.substring(1, req.length());
          }
          newVector.add(prefix + " " + req);
        }
      } else if (temp.getBean() instanceof Startable) {
        String prefix = "";
        if ((temp.getBean() instanceof BeanCommon)) {
          prefix = ((BeanCommon) temp.getBean()).getCustomName();
        } else {
          prefix = temp.getBean().getClass().getName();
          prefix = prefix.substring(prefix.lastIndexOf('.') + 1,
            prefix.length());
        }
        prefix = "" + (i + 1) + ": (" + prefix + ")";
        String startMessage = ((Startable) temp.getBean()).getStartMessage();
        if (startMessage.charAt(0) == '$') {
          prefix = '$' + prefix;
          startMessage = startMessage.substring(1, startMessage.length());
        }
        newVector.add(prefix + " " + startMessage);
      }
    }

    return newVector.elements();
  }

  public void setSubFlowPreview(ImageIcon sfp) {
    m_subFlowPreview = sfp;
  }

  private void showPreview() {
    if (m_previewWindow == null) {

      JLabel jl = new JLabel(m_subFlowPreview);
      // Dimension d = jl.getPreferredSize();
      jl.setLocation(0, 0);
      m_previewWindow = new JWindow();
      // popup.getContentPane().setLayout(null);
      m_previewWindow.getContentPane().add(jl);
      m_previewWindow.validate();
      m_previewWindow.setSize(m_subFlowPreview.getIconWidth(),
        m_subFlowPreview.getIconHeight());

      m_previewWindow.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          m_previewWindow.dispose();
          m_previewWindow = null;
        }
      });

      m_previewWindow.setLocation(getParent().getLocationOnScreen().x + getX()
        + getWidth() / 2 - m_subFlowPreview.getIconWidth() / 2,
        getParent().getLocationOnScreen().y + getY() + getHeight() / 2
          - m_subFlowPreview.getIconHeight() / 2);
      // popup.pack();
      m_previewWindow.setVisible(true);
      m_previewTimer = new javax.swing.Timer(8000,
        new java.awt.event.ActionListener() {
          @Override
          public void actionPerformed(java.awt.event.ActionEvent e) {
            if (m_previewWindow != null) {
              m_previewWindow.dispose();
              m_previewWindow = null;
              m_previewTimer = null;
            }
          }
        });
      m_previewTimer.setRepeats(false);
      m_previewTimer.start();
    }
  }

  /**
   * Perform a particular request
   * 
   * @param request the request to perform
   * @exception IllegalArgumentException if an error occurs
   */
  @Override
  public void performRequest(String request) {
    if (request.compareTo("Show preview") == 0) {
      showPreview();
      return;
    }
    // first grab the index if any
    if (request.indexOf(":") < 0) {
      return;
    }
    String tempI = request.substring(0, request.indexOf(':'));
    int index = Integer.parseInt(tempI);
    index--;
    String req = request.substring(request.indexOf(')') + 1, request.length())
      .trim();

    Object target = ((BeanInstance) m_subFlow.elementAt(index)).getBean();
    if (target instanceof Startable
      && req.equals(((Startable) target).getStartMessage())) {
      try {
        ((Startable) target).start();
      } catch (Exception ex) {
        if (m_log != null) {
          String compName = (target instanceof BeanCommon) ? ((BeanCommon) target)
            .getCustomName() : "";
          m_log.logMessage("Problem starting subcomponent " + compName);
        }
      }
    } else {
      ((UserRequestAcceptor) target).performRequest(req);
    }
  }

  /**
   * Set a logger
   * 
   * @param logger a <code>Logger</code> value
   */
  @Override
  public void setLog(Logger logger) {
    m_log = logger;
  }

  public void removePropertyChangeListenersSubFlow(PropertyChangeListener pcl) {
    for (int i = 0; i < m_subFlow.size(); i++) {
      BeanInstance temp = (BeanInstance) m_subFlow.elementAt(i);
      if (temp.getBean() instanceof Visible) {
        ((Visible) (temp.getBean())).getVisual().removePropertyChangeListener(
          pcl);
      }
      if (temp.getBean() instanceof MetaBean) {
        ((MetaBean) temp.getBean()).removePropertyChangeListenersSubFlow(pcl);
      }
    }
  }

  public void addPropertyChangeListenersSubFlow(PropertyChangeListener pcl) {
    for (int i = 0; i < m_subFlow.size(); i++) {
      BeanInstance temp = (BeanInstance) m_subFlow.elementAt(i);
      if (temp.getBean() instanceof Visible) {
        ((Visible) (temp.getBean())).getVisual().addPropertyChangeListener(pcl);
      }
      if (temp.getBean() instanceof MetaBean) {
        ((MetaBean) temp.getBean()).addPropertyChangeListenersSubFlow(pcl);
      }
    }
  }

  /**
   * Checks to see if any of the inputs to this group implements the supplied
   * listener class
   * 
   * @param listenerClass the listener to check for
   */
  public boolean canAcceptConnection(Class<?> listenerClass) {
    for (int i = 0; i < m_inputs.size(); i++) {
      BeanInstance input = (BeanInstance) m_inputs.elementAt(i);
      if (listenerClass.isInstance(input.getBean())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Return a list of input beans capable of receiving the supplied event
   * 
   * @param esd the event in question
   * @return a vector of beans capable of handling the event
   */
  public Vector<BeanInstance> getSuitableTargets(EventSetDescriptor esd) {
    Class<?> listenerClass = esd.getListenerType(); // class of the listener
    Vector<BeanInstance> targets = new Vector<BeanInstance>();
    for (int i = 0; i < m_inputs.size(); i++) {
      BeanInstance input = (BeanInstance) m_inputs.elementAt(i);
      if (listenerClass.isInstance(input.getBean())) {
        targets.add(input);
      }
    }
    return targets;
  }

  private String statusMessagePrefix() {
    return getCustomName() + "$" + MetaBean.this.hashCode() + "|";
  }
}
