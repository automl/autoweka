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
 * ResultSetTableCellRenderer.java
 * Copyright (C) 2005-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.sql;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Handles the background colors for missing values differently than the
 * DefaultTableCellRenderer.
 * 
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 10222 $
 */
public class ResultSetTableCellRenderer extends DefaultTableCellRenderer {

  /** for serialization */
  private static final long serialVersionUID = -8106963669703497351L;

  // the color for missing values
  private final Color missingColor;
  private final Color missingColorSelected;

  /**
   * initializes the Renderer with a standard color
   */
  public ResultSetTableCellRenderer() {
    this(new Color(223, 223, 223), new Color(192, 192, 192));
  }

  /**
   * initializes the Renderer with the given colors
   */
  public ResultSetTableCellRenderer(Color missingColor,
    Color missingColorSelected) {

    super();

    this.missingColor = missingColor;
    this.missingColorSelected = missingColorSelected;
  }

  /**
   * Returns the default table cell renderer. stuff for the header is taken from
   * <a href="http://www.chka.de/swing/table/faq.html">here</a>
   */
  @Override
  public Component getTableCellRendererComponent(JTable table, Object value,
    boolean isSelected, boolean hasFocus, int row, int column) {

    ResultSetTableModel model;
    Component result;
    // boolean found; NOT USED

    result = super.getTableCellRendererComponent(table, value, isSelected,
      hasFocus, row, column);

    if (table.getModel() instanceof ResultSetTableModel) {
      model = (ResultSetTableModel) table.getModel();
      // normal cell
      if (row >= 0) {
        if (model.isNullAt(row, column)) {
          setToolTipText("NULL");
          if (isSelected) {
            result.setBackground(missingColorSelected);
          } else {
            result.setBackground(missingColor);
          }
        } else {
          setToolTipText(null);
          if (isSelected) {
            result.setBackground(table.getSelectionBackground());
          } else {
            result.setBackground(Color.WHITE);
          }
        }

        // alignment
        if (model.isNumericAt(column)) {
          setHorizontalAlignment(SwingConstants.RIGHT);
        } else {
          setHorizontalAlignment(SwingConstants.LEFT);
        }
      }
      // header
      else {
        setBorder(UIManager.getBorder("TableHeader.cellBorder"));
        setHorizontalAlignment(SwingConstants.CENTER);
        if (table.getColumnModel().getSelectionModel().isSelectedIndex(column)) {
          result.setBackground(UIManager.getColor("TableHeader.background")
            .darker());
        } else {
          result.setBackground(UIManager.getColor("TableHeader.background"));
        }
      }
    }

    return result;
  }
}
