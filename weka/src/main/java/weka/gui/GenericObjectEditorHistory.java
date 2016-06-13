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
 * ObjectHistory.java
 * Copyright (C) 2009-2012 University of Waikato, Hamilton, New Zealand
 */

package weka.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.EventObject;
import java.util.Vector;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import weka.core.SerializedObject;
import weka.core.Utils;

/**
 * A helper class for maintaining a history of objects selected in the GOE.
 * 
 * @author fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 10216 $
 */
public class GenericObjectEditorHistory implements Serializable {

  /** for serialization. */
  private static final long serialVersionUID = -1255734638729633595L;

  /**
   * Event that gets sent when a history item gets selected.
   * 
   * @author fracpete (fracpete at waikato dot ac dot nz)
   * @version $Revision: 10216 $
   */
  public static class HistorySelectionEvent extends EventObject {

    /** for serialization. */
    private static final long serialVersionUID = 45824542929908105L;

    /** the selected favorite. */
    protected Object m_HistoryItem;

    /**
     * Initializes the event.
     * 
     * @param source the object that triggered the event
     * @param historyItem the selected history item
     */
    public HistorySelectionEvent(Object source, Object historyItem) {
      super(source);

      m_HistoryItem = historyItem;
    }

    /**
     * Returns the selected history item.
     * 
     * @return the history item
     */
    public Object getHistoryItem() {
      return m_HistoryItem;
    }
  }

  /**
   * Interface for classes that listen to selections of history items.
   * 
   * @author fracpete (fracpete at waikato dot ac dot nz)
   * @version $Revision: 10216 $
   */
  public static interface HistorySelectionListener {

    /**
     * Gets called when a history item gets selected.
     * 
     * @param e the event
     */
    public void historySelected(HistorySelectionEvent e);
  }

  /** the maximum entries in the history. */
  public final static int MAX_HISTORY_COUNT = 10;

  /** the maximum length of a caption in the history. */
  public final static int MAX_HISTORY_LENGTH = 200;

  /** the menu max line length. */
  public final static int MAX_LINE_LENGTH = 80;

  /** the history of objects. */
  protected Vector<Object> m_History;

  /**
   * Initializes the history.
   */
  public GenericObjectEditorHistory() {
    super();

    initialize();
  }

  /**
   * Initializes members.
   */
  protected void initialize() {
    m_History = new Vector<Object>();
  }

  /**
   * Clears the history.
   */
  public synchronized void clear() {
    m_History.clear();
  }

  /**
   * Adds the object to the history.
   * 
   * @param obj the object to add
   */
  public synchronized void add(Object obj) {
    obj = copy(obj);

    if (m_History.contains(obj)) {
      m_History.remove(obj);
    }
    m_History.insertElementAt(obj, 0);

    while (m_History.size() > MAX_HISTORY_COUNT) {
      m_History.remove(m_History.size() - 1);
    }
  }

  /**
   * Returns the number of entries in the history.
   * 
   * @return the size of the history
   */
  public synchronized int size() {
    return m_History.size();
  }

  /**
   * Returns the current history.
   * 
   * @return the history
   */
  public synchronized Vector<Object> getHistory() {
    return m_History;
  }

  /**
   * Creates a copy of the object.
   * 
   * @param obj the object to copy
   */
  protected Object copy(Object obj) {
    SerializedObject so;
    Object result;

    try {
      so = new SerializedObject(obj);
      result = so.getObject();
    } catch (Exception e) {
      result = null;
      e.printStackTrace();
    }

    return result;
  }

  /**
   * Generates an HTML caption for the an entry in the history menu.
   * 
   * @param obj the object to create the caption for
   * @return the generated HTML captiopn
   */
  protected String generateMenuItemCaption(Object obj) {
    StringBuffer result;
    String cmd;
    String[] lines;
    int i;

    result = new StringBuffer();

    cmd = Utils.toCommandLine(obj);
    if (cmd.length() > MAX_HISTORY_LENGTH) {
      cmd = cmd.substring(0, MAX_HISTORY_LENGTH) + "...";
    }

    lines = Utils.breakUp(cmd, MAX_LINE_LENGTH);
    result.append("<html>");
    for (i = 0; i < lines.length; i++) {
      if (i > 0) {
        result.append("<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
      }
      result.append(lines[i].trim());
    }
    result.append("</html>");

    return result.toString();
  }

  /**
   * Adds a menu item with the history to the popup menu.
   * 
   * @param menu the menu to add the history to
   * @param current the current object
   * @param listener the listener to attach to the menu items' ActionListener
   */
  public void customizePopupMenu(JPopupMenu menu, Object current,
    HistorySelectionListener listener) {
    JMenu submenu;
    JMenuItem item;
    int i;

    if (m_History.size() == 0) {
      return;
    }

    submenu = new JMenu("History");
    menu.addSeparator();
    menu.add(submenu);

    // clear history
    item = new JMenuItem("Clear history");
    item.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        m_History.clear();
      }
    });
    submenu.add(item);

    // current history
    final HistorySelectionListener fListener = listener;
    for (i = 0; i < m_History.size(); i++) {
      if (i == 0) {
        submenu.addSeparator();
      }
      final Object history = m_History.get(i);
      item = new JMenuItem(generateMenuItemCaption(history));
      item.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          fListener.historySelected(new HistorySelectionEvent(fListener,
            history));
        }
      });
      submenu.add(item);
    }
  }
}
