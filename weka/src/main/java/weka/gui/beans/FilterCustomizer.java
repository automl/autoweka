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
 *    FilterCustomizer.java
 *    Copyright (C) 2002-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

import weka.gui.GenericObjectEditor;
import weka.gui.PropertySheetPanel;

/**
 * GUI customizer for the filter bean
 * 
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 9467 $
 */
public class FilterCustomizer extends JPanel implements BeanCustomizer,
    CustomizerCloseRequester {

  /** for serialization */
  private static final long serialVersionUID = 2049895469240109738L;

  static {
    GenericObjectEditor.registerEditors();
  }

  private final PropertyChangeSupport m_pcSupport = new PropertyChangeSupport(
      this);

  private weka.gui.beans.Filter m_filter;
  /*
   * private GenericObjectEditor m_filterEditor = new GenericObjectEditor(true);
   */

  /** Backup if user presses cancel */
  private weka.filters.Filter m_backup;

  private final PropertySheetPanel m_filterEditor = new PropertySheetPanel();

  private Window m_parentWindow;

  private ModifyListener m_modifyListener;

  public FilterCustomizer() {
    m_filterEditor
        .setBorder(BorderFactory.createTitledBorder("Filter options"));

    setLayout(new BorderLayout());
    add(m_filterEditor, BorderLayout.CENTER);

    JPanel butHolder = new JPanel();
    butHolder.setLayout(new GridLayout(1, 2));
    JButton OKBut = new JButton("OK");
    OKBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        // Tell the editor that we are closing under an OK condition
        // so that it can pass on the message to any customizer that
        // might be in use
        m_filterEditor.closingOK();

        if (m_modifyListener != null) {
          m_modifyListener.setModifiedStatus(FilterCustomizer.this, true);
        }

        m_parentWindow.dispose();
      }
    });

    JButton CancelBut = new JButton("Cancel");
    CancelBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        // Tell the editor that we are closing under a CANCEL condition
        // so that it can pass on the message to any customizer that
        // might be in use
        m_filterEditor.closingCancel();

        // cancel requested, so revert to backup and then
        // close the dialog
        if (m_backup != null) {
          m_filter.setFilter(m_backup);
        }

        if (m_modifyListener != null) {
          m_modifyListener.setModifiedStatus(FilterCustomizer.this, false);
        }
        m_parentWindow.dispose();
      }
    });

    butHolder.add(OKBut);
    butHolder.add(CancelBut);
    add(butHolder, BorderLayout.SOUTH);
  }

  /**
   * Set the filter bean to be edited
   * 
   * @param object a Filter bean
   */
  @Override
  public void setObject(Object object) {
    m_filter = (weka.gui.beans.Filter) object;
    try {
      m_backup = (weka.filters.Filter) GenericObjectEditor.makeCopy(m_filter
          .getFilter());
    } catch (Exception ex) {
      // ignore
    }
    m_filterEditor.setTarget(m_filter.getFilter());
  }

  /**
   * Add a property change listener
   * 
   * @param pcl a <code>PropertyChangeListener</code> value
   */
  @Override
  public void addPropertyChangeListener(PropertyChangeListener pcl) {
    m_pcSupport.addPropertyChangeListener(pcl);
  }

  /**
   * Remove a property change listener
   * 
   * @param pcl a <code>PropertyChangeListener</code> value
   */
  @Override
  public void removePropertyChangeListener(PropertyChangeListener pcl) {
    m_pcSupport.removePropertyChangeListener(pcl);
  }

  @Override
  public void setParentWindow(Window parent) {
    m_parentWindow = parent;
  }

  @Override
  public void setModifiedListener(ModifyListener l) {
    m_modifyListener = l;
  }
}
