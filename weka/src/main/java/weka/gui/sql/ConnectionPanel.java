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
 * ConnectionPanel.java
 * Copyright (C) 2005-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.sql;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Iterator;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import weka.gui.ComponentHelper;
import weka.gui.DatabaseConnectionDialog;
import weka.gui.ExtensionFileFilter;
import weka.gui.ListSelectorDialog;
import weka.gui.sql.event.ConnectionEvent;
import weka.gui.sql.event.ConnectionListener;
import weka.gui.sql.event.HistoryChangedEvent;
import weka.gui.sql.event.HistoryChangedListener;

/**
 * Enables the user to insert a database URL, plus user/password to connect to
 * this database.
 * 
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 10222 $
 */
public class ConnectionPanel extends JPanel implements CaretListener {

  /** for serialization. */
  static final long serialVersionUID = 3499317023969723490L;

  /** the name of the history. */
  public final static String HISTORY_NAME = "connection";

  /** the parent frame. */
  protected JFrame m_Parent = null;

  /** the databae connection dialog. */
  protected DatabaseConnectionDialog m_DbDialog;

  /** the URL to use. */
  protected String m_URL = "";

  /** the user to use for connecting to the DB. */
  protected String m_User = "";

  /** the password to use for connecting to the DB. */
  protected String m_Password = "";

  /** the label for the URL. */
  protected JLabel m_LabelURL = new JLabel("URL ");

  /** the textfield for the URL. */
  protected JTextField m_TextURL = new JTextField(40);

  /** the button for the DB-Dialog. */
  protected JButton m_ButtonDatabase = new JButton(
    ComponentHelper.getImageIcon("user.png"));

  /** the button for connecting to the database. */
  protected JButton m_ButtonConnect = new JButton(
    ComponentHelper.getImageIcon("connect.png"));

  /** the button for the history. */
  protected JButton m_ButtonHistory = new JButton(
    ComponentHelper.getImageIcon("history.png"));

  /** the button for the setup. */
  protected JButton m_ButtonSetup = new JButton(
    ComponentHelper.getImageIcon("properties.gif"));

  /** the connection listeners. */
  protected HashSet<ConnectionListener> m_ConnectionListeners;

  /** the history listeners. */
  protected HashSet<HistoryChangedListener> m_HistoryChangedListeners;

  /** for connecting to the database. */
  protected DbUtils m_DbUtils;

  /** the history of connections. */
  protected DefaultListModel m_History = new DefaultListModel();

  /** the file chooser for the setup files. */
  protected JFileChooser m_SetupFileChooser;

  /**
   * initializes the panel.
   * 
   * @param parent the parent of this panel
   */
  public ConnectionPanel(JFrame parent) {
    super();

    m_Parent = parent;
    m_ConnectionListeners = new HashSet<ConnectionListener>();
    m_HistoryChangedListeners = new HashSet<HistoryChangedListener>();
    m_SetupFileChooser = new JFileChooser();
    m_SetupFileChooser.setDialogTitle("Switch database setup");
    m_SetupFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    m_SetupFileChooser.setMultiSelectionEnabled(false);
    m_SetupFileChooser.setAcceptAllFileFilterUsed(true);
    ExtensionFileFilter filter = new ExtensionFileFilter(".props",
      "Properties file");
    m_SetupFileChooser.addChoosableFileFilter(filter);
    m_SetupFileChooser.setFileFilter(filter);

    try {
      m_DbUtils = new DbUtils();
      m_URL = m_DbUtils.getDatabaseURL();
      m_User = m_DbUtils.getUsername();
      m_Password = m_DbUtils.getPassword();
    } catch (Exception e) {
      e.printStackTrace();
      m_URL = "";
      m_User = "";
      m_Password = "";
    }

    createPanel();
  }

  /**
   * builds the panel with all its components.
   */
  protected void createPanel() {
    JPanel panel;
    JPanel panel2;

    setLayout(new BorderLayout());
    panel2 = new JPanel(new FlowLayout());
    add(panel2, BorderLayout.WEST);

    // label
    m_LabelURL.setLabelFor(m_ButtonDatabase);
    m_LabelURL.setDisplayedMnemonic('U');
    panel2.add(m_LabelURL);

    // editfield
    m_TextURL.setText(m_URL);
    m_TextURL.addCaretListener(this);
    panel2.add(m_TextURL);

    // buttons
    panel = new JPanel(new FlowLayout());
    panel2.add(panel);

    m_ButtonDatabase.setToolTipText("Set user and password");
    m_ButtonDatabase.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        showDialog();
      }
    });
    panel.add(m_ButtonDatabase);

    m_ButtonConnect.setToolTipText("Connect to the database");
    m_ButtonConnect.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        connect();
      }
    });
    panel.add(m_ButtonConnect);

    m_ButtonHistory.setToolTipText("Select a previously used connection");
    m_ButtonHistory.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        showHistory();
      }
    });
    panel.add(m_ButtonHistory);

    m_ButtonSetup.setToolTipText("Switch database setup");
    m_ButtonSetup.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        switchSetup();
      }
    });
    panel.add(m_ButtonSetup);

    setButtons();
  }

  /**
   * sets the buttons according to the connected-state.
   */
  protected void setButtons() {
    boolean isEmpty;

    isEmpty = m_TextURL.getText().equals("");

    m_ButtonConnect.setEnabled(!isEmpty);
    m_ButtonDatabase.setEnabled(!isEmpty);
    m_ButtonHistory.setEnabled(m_History.size() > 0);
    m_ButtonSetup.setEnabled(true);
  }

  /**
   * sets the parameters back to standard.
   */
  public void clear() {
    setURL(m_DbUtils.getDatabaseURL());
    setUser(m_DbUtils.getUsername());
    setPassword(m_DbUtils.getPassword());
  }

  /**
   * sets the focus in a designated control.
   */
  public void setFocus() {
    m_TextURL.requestFocus();
  }

  /**
   * sets the URL.
   * 
   * @param url the new value of the URL
   */
  public void setURL(String url) {
    m_URL = url;
    m_TextURL.setText(url);
  }

  /**
   * returns the current URL.
   * 
   * @return the current URL
   */
  public String getURL() {
    m_URL = m_TextURL.getText();
    return m_URL;
  }

  /**
   * sets the User.
   * 
   * @param user the new value of the User
   */
  public void setUser(String user) {
    m_User = user;
  }

  /**
   * returns the current User.
   * 
   * @return the current user
   */
  public String getUser() {
    return m_User;
  }

  /**
   * sets the Password.
   * 
   * @param pw the new value of the Password
   */
  public void setPassword(String pw) {
    m_Password = pw;
  }

  /**
   * returns the current Password.
   * 
   * @return the current password
   */
  public String getPassword() {
    return m_Password;
  }

  /**
   * adds the given string to the history (removes duplicates).
   * 
   * @param s the string to add
   */
  protected void addHistory(String s) {
    if (s.equals("")) {
      return;
    }

    // no duplicates!
    if (m_History.contains(s)) {
      m_History.removeElement(s);
    }

    m_History.add(0, s);

    // send notification
    notifyHistoryChangedListeners();
  }

  /**
   * sets the local history to the given one.
   * 
   * @param history the history to use
   */
  public void setHistory(DefaultListModel history) {
    int i;

    m_History.clear();
    for (i = 0; i < history.size(); i++) {
      m_History.addElement(history.get(i));
    }

    setButtons();
  }

  /**
   * returns the history.
   * 
   * @return the current history
   */
  public DefaultListModel getHistory() {
    return m_History;
  }

  /**
   * displays the database dialog.
   */
  protected void showDialog() {
    m_DbDialog = new DatabaseConnectionDialog(m_Parent, getURL(), getUser(),
      false);
    m_DbDialog.setVisible(true);
    if (m_DbDialog.getReturnValue() == JOptionPane.OK_OPTION) {
      setURL(m_DbDialog.getURL());
      setUser(m_DbDialog.getUsername());
      setPassword(m_DbDialog.getPassword());
    }

    setButtons();
  }

  /**
   * connects to the database, notifies the listeners.
   */
  protected void connect() {
    // disconnect if still connected
    if (m_DbUtils.isConnected()) {
      try {
        m_DbUtils.disconnectFromDatabase();
        notifyConnectionListeners(ConnectionEvent.DISCONNECT);
      } catch (Exception e) {
        e.printStackTrace();
        notifyConnectionListeners(ConnectionEvent.DISCONNECT, e);
      }
    }

    // connect
    try {
      m_DbUtils.setDatabaseURL(getURL());
      m_DbUtils.setUsername(getUser());
      m_DbUtils.setPassword(getPassword());
      m_DbUtils.connectToDatabase();
      notifyConnectionListeners(ConnectionEvent.CONNECT);
      // add to history
      addHistory(getUser() + "@" + getURL());
    } catch (Exception e) {
      e.printStackTrace();
      notifyConnectionListeners(ConnectionEvent.CONNECT, e);
    }

    setButtons();
  }

  /**
   * displays the query history.
   */
  public void showHistory() {
    JList list;
    ListSelectorDialog dialog;
    String tmpStr;

    list = new JList(m_History);
    dialog = new ListSelectorDialog(m_Parent, list);

    if (dialog.showDialog() == ListSelectorDialog.APPROVE_OPTION) {
      if (list.getSelectedValue() != null) {
        tmpStr = list.getSelectedValue().toString();
        if (tmpStr.indexOf("@") > -1) {
          setUser(tmpStr.substring(0, tmpStr.indexOf("@")));
          setURL(tmpStr.substring(tmpStr.indexOf("@") + 1));
          showDialog();
        } else {
          setUser("");
          setURL(tmpStr);
        }
      }
    }

    setButtons();
  }

  /**
   * Lets the user select a props file for changing the database connection
   * parameters.
   */
  public void switchSetup() {
    int retVal;

    retVal = m_SetupFileChooser.showOpenDialog(this);
    if (retVal != JFileChooser.APPROVE_OPTION) {
      return;
    }

    m_DbUtils.initialize(m_SetupFileChooser.getSelectedFile());

    m_URL = m_DbUtils.getDatabaseURL();
    m_User = m_DbUtils.getUsername();
    m_Password = m_DbUtils.getPassword();

    m_TextURL.setText(m_URL);
  }

  /**
   * adds the given listener to the list of listeners.
   * 
   * @param l the listener to add to the list
   */
  public void addConnectionListener(ConnectionListener l) {
    m_ConnectionListeners.add(l);
  }

  /**
   * removes the given listener from the list of listeners.
   * 
   * @param l the listener to remove
   */
  public void removeConnectionListener(ConnectionListener l) {
    m_ConnectionListeners.remove(l);
  }

  /**
   * notifies the connection listeners of the event.
   * 
   * @param type the type of the action, CONNECT or DISCONNECT
   */
  protected void notifyConnectionListeners(int type) {
    notifyConnectionListeners(type, null);
  }

  /**
   * notifies the connection listeners of the event.
   * 
   * @param type the type of the action, CONNECT or DISCONNECT
   * @param ex an optional exception that happened (indicates failure!)
   */
  protected void notifyConnectionListeners(int type, Exception ex) {
    Iterator<ConnectionListener> iter;
    ConnectionListener l;

    iter = m_ConnectionListeners.iterator();
    while (iter.hasNext()) {
      l = iter.next();
      l.connectionChange(new ConnectionEvent(this, type, m_DbUtils, ex));
    }
  }

  /**
   * adds the given listener to the list of listeners.
   * 
   * @param l the listener to add to the list
   */
  public void addHistoryChangedListener(HistoryChangedListener l) {
    m_HistoryChangedListeners.add(l);
  }

  /**
   * removes the given listener from the list of listeners.
   * 
   * @param l the listener to remove
   */
  public void removeHistoryChangedListener(HistoryChangedListener l) {
    m_HistoryChangedListeners.remove(l);
  }

  /**
   * notifies the history listeners of the event.
   */
  protected void notifyHistoryChangedListeners() {
    Iterator<HistoryChangedListener> iter;
    HistoryChangedListener l;

    iter = m_HistoryChangedListeners.iterator();
    while (iter.hasNext()) {
      l = iter.next();
      l.historyChanged(new HistoryChangedEvent(this, HISTORY_NAME, getHistory()));
    }
  }

  /**
   * Called when the caret position is updated.
   * 
   * @param event the event to process
   */
  @Override
  public void caretUpdate(CaretEvent event) {
    setButtons();
  }
}
