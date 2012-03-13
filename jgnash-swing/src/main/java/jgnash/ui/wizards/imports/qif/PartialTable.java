/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2012 Craig Cavanaugh
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.ui.wizards.imports.qif;

import java.math.BigDecimal;
import java.text.DateFormat;

import javax.swing.DefaultCellEditor;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;

import jgnash.engine.Account;
import jgnash.imports.qif.QifAccount;
import jgnash.imports.qif.QifTransaction;
import jgnash.ui.components.AccountListComboBox;
import jgnash.ui.components.FormattedJTable;
import jgnash.util.DateUtils;
import jgnash.util.Resource;

/**
 * @author Craig Cavanaugh
 * @version $Id: PartialTable.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
class PartialTable extends FormattedJTable {

    private Resource rb = Resource.get();

    private QifAccount qAccount;

    private Model model;

    private TableCellEditor accEditor = new DefaultCellEditor(new AccountListComboBox());

    public PartialTable(final QifAccount qAccount) {
        super();
        this.qAccount = qAccount;
        model = new Model();
        setModel(model);
    }

    public void deleteSelected() {
        int row = getSelectedRow();
        qAccount.items.remove(row);
        model.fireTableRowsDeleted(row, row);
    }

    @Override
    public TableCellEditor getCellEditor(final int row, final int column) {
        if (column == 2) {
            return accEditor;
        }
        return super.getCellEditor(row, column);
    }

    public void fireTableDataChanged() {
        model.fireTableDataChanged();
    }

    class Model extends AbstractTableModel {

        private static final long serialVersionUID = 7054758354758853979L;

        private String[] cNames = { rb.getString("Column.Date"), rb.getString("Column.Payee"),
                        rb.getString("Column.Account"), rb.getString("Column.Amount") };

        private Class<?>[] cClass = { String.class, String.class, String.class, BigDecimal.class };

        private DateFormat dateFormatter = DateUtils.getShortDateFormat();

        @Override
        public String getColumnName(final int column) {
            return cNames[column];
        }

        @Override
        public Class<?> getColumnClass(final int column) {
            return cClass[column];
        }

        @Override
        public boolean isCellEditable(final int rowIndex, final int columnIndex) {
            return columnIndex == 2;
        }

        /**
         * Returns the number of columns in the model. A <code>JTable</code> uses this method to determine how many
         * columns it should create and display by default.
         * 
         * @return the number of columns in the model
         * @see #getRowCount
         */
        @Override
        public int getColumnCount() {
            return 4;
        }

        /**
         * Returns the number of rows in the model. A <code>JTable</code> uses this method to determine how many rows it
         * should display. This method should be quick, as it is called frequently during rendering.
         * 
         * @return the number of rows in the model
         * @see #getColumnCount
         */
        @Override
        public int getRowCount() {
            return qAccount.numItems();
        }

        /**
         * Returns the value for the cell at <code>columnIndex</code> and <code>rowIndex</code>.
         * 
         * @param rowIndex the row whose value is to be queried
         * @param columnIndex the column whose value is to be queried
         * @return the value Object at the specified cell
         */
        @Override
        public Object getValueAt(final int rowIndex, final int columnIndex) {
            QifTransaction qt = qAccount.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return dateFormatter.format(qt.date);
                case 1:
                    return qt.payee;
                case 2:
                    return qt.category;
                case 3:
                    return qt.amount;
                default:
                    return null;
            }
        }

        @Override
        public void setValueAt(final Object value, final int rowIndex, final int columnIndex) {
            if (columnIndex == 2 && value != null) {
                QifTransaction qt = qAccount.get(rowIndex);
                qt.category = ((Account) value).getPathName();
                qt._category = (Account) value;
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }
    }
}