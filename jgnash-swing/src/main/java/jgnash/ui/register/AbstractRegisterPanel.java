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
package jgnash.ui.register;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTable;

import jgnash.engine.*;
import jgnash.message.ChannelEvent;
import jgnash.message.Message;
import jgnash.message.MessageListener;
import jgnash.message.MessageProperty;
import jgnash.ui.UIApplication;
import jgnash.ui.components.YesNoDialog;
import jgnash.ui.register.table.AbstractRegisterTableModel;
import jgnash.ui.register.table.RegisterTable;
import jgnash.ui.util.JTableUtils;
import jgnash.util.EncodeDecode;
import jgnash.util.Resource;

/**
 * Account register panels should extend this class
 *
 * @author Craig Cavanaugh
 *
 */
public abstract class AbstractRegisterPanel extends JPanel implements MessageListener, KeyListener {

    private static final String NODE_REG_POS = "/jgnash/ui/register/positions";

    private static final String NODE_REG_WIDTH = "/jgnash/ui/register/widths";

    private static final String NODE_REG_VIS = "/jgnash/ui/register/visibility";

    protected final Resource rb = Resource.get();

    private TransactionPopup popup = new TransactionPopup();

    protected abstract Account getAccount();

    protected abstract void modifyTransaction(int row);

    public abstract AbstractRegisterTableModel getTableModel();

    protected abstract void clear();

    public abstract RegisterTable getTable();

    protected abstract void updateAccountState();

    protected abstract void updateAccountInfo();

    protected void installPopupHandler() {
        getTable().addMouseListener(new MouseAdapter() {

            /* Need to look at mousePressed and mouseReleased for pop-up
             * trigger to handle different platforms.  Only mousePressed
             * is needed for a double click.
             */
            @Override
            public void mousePressed(final MouseEvent e) {
                checkForPopup(e);
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
                checkForPopup(e);
            }

            @Override
            public void mouseClicked(final MouseEvent e) {
                int row = getTable().rowAtPoint(e.getPoint());

                if (row >= 0) {
                    modifyTransaction(row);
                }
            }
        });
    }

    private void checkForPopup(final MouseEvent e) {
        /* Allow account selection with the pop-up trigger button */
        if (e.isPopupTrigger()) {
            popupAction(e);
        }
    }

    private void popupAction(final MouseEvent e) {
        int row = getTable().rowAtPoint(e.getPoint());
        if (row >= 0) {
            getTable().getSelectionModel().setSelectionInterval(row, row);
            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    /**
     * Return the column positions of the register table
     *
     * @return Returns a string representing the column order
     */
    private String getColumnPositions() {
        return JTableUtils.getColumnOrder(getTable());
    }

    /**
     * Sets the column positions of the register table
     *
     * @param positions A string representing the column order
     */
    private void setColumnPositions(final String positions) {
        JTableUtils.setColumnOrder(getTable(), positions);
    }

    private String getColumnWidths() {
        return JTableUtils.getColumnWidths(getTable());
    }

    private void setColumnWidths(final String widths) {
        JTableUtils.setColumnWidths(getTable(), widths);
    }

    protected boolean restoreColumnLayout() {
        boolean result = false;

        Preferences pwidth = Preferences.userRoot().node(NODE_REG_WIDTH);
        Preferences ppos = Preferences.userRoot().node(NODE_REG_POS);
        Preferences pvis = Preferences.userRoot().node(NODE_REG_VIS);

        String id = getAccount().getUuid();

        String colVis = pvis.get(id, null);

        if (colVis != null) {
            /* Be sure to set column visibility prior to setting position, otherwise
             * column count will not match and position restoration will fail */

            getTableModel().setColumnVisibility(EncodeDecode.decodeBooleanArray(colVis));
            setColumnPositions(ppos.get(id, null)); // restore column positions
            setColumnWidths(pwidth.get(id, null)); // restore column widths

            result = true;
        }

        return result;
    }

    protected void saveColumnLayout() {
        Preferences pwidth = Preferences.userRoot().node(NODE_REG_WIDTH);
        Preferences ppos = Preferences.userRoot().node(NODE_REG_POS);
        Preferences pvis = Preferences.userRoot().node(NODE_REG_VIS);

        String id = getAccount().getUuid();

        ppos.put(id, getColumnPositions());
        pwidth.put(id, getColumnWidths());
        pvis.put(id, EncodeDecode.encodeBooleanArray(getTableModel().getColumnVisibility()));
    }

    private boolean confirmTransactionRemoval(final int count) {
        String message = count == 1 ? rb.getString("Message.ConfirmTransDelete") : rb.getString("Message.ConfirmMultipleTransDelete");

        return YesNoDialog.showYesNoDialog(UIApplication.getFrame(), new JLabel(rb.getString(message)), rb.getString("Title.Confirm"));
    }

    protected void deleteAction() {
        Transaction trans[] = getSelectedTransactions();

        if (RegisterFactory.isConfirmTransactionDeleteEnabled()) {
            if (!confirmTransactionRemoval(trans.length)) {
                return;
            }
        }

        // walk through the array and delete each transaction
        for (Transaction tran : trans) {
            EngineFactory.getEngine(EngineFactory.DEFAULT).removeTransaction(tran);
        }
    }

    protected void duplicateAction() {
        Transaction trans[] = getSelectedTransactions();

        // walk through the array and duplicate each transaction
        for (Transaction tran : trans) {
            final DuplicateTransactionDialog d = DuplicateTransactionDialog.showDialog(getAccount(), tran);

            if (d.getTransaction() != null) {
                EventQueue.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        clear();
                        setSelectedTransaction(d.getTransaction());
                    }
                });

            }
        }
    }

    /**
     * Displays a register dialog for the opposite account and places the opposite side of the transaction in edit mode.
     */
    void jumpAction() {
        Transaction t = getSelectedTransaction();

        if (t != null) {
            if (t.getTransactionType() == TransactionType.DOUBLEENTRY) {
                Set<Account> set = t.getAccounts();

                for (Account a : set) {
                    if (!getAccount().equals(a)) {
                        RegisterFrame.showDialog(a, t);
                    }
                }
            } else if (t.getTransactionType() == TransactionType.SPLITENTRY) {
                Account common = t.getCommonAccount();

                if (!getAccount().equals(common)) {
                    RegisterFrame.showDialog(common, t);
                }
            } else if (t instanceof InvestmentTransaction) {
                Account invest = ((InvestmentTransaction) t).getInvestmentAccount();

                if (!getAccount().equals(invest)) {
                    RegisterFrame.showDialog(invest, t);
                }
            }
        }
    }

    void reconcileAction(final ReconciledState reconciled) {
        Transaction t = getSelectedTransaction();
        if (t != null) {
            EngineFactory.getEngine(EngineFactory.DEFAULT).setTransactionReconciled(t, getAccount(), reconciled);
        }
    }

    private Transaction getSelectedTransaction() {
        int index = getTable().getSelectedRow();

        if (index >= 0) {
            return getTableModel().getTransactionAt(index);
        }
        return null;
    }

    private Transaction[] getSelectedTransactions() {
        int rows[] = getTable().getSelectedRows();

        // create an array of transactions
        Transaction trans[] = new Transaction[rows.length];
        for (int i = 0; i < rows.length; i++) {
            trans[i] = getTableModel().getTransactionAt(rows[i]);
        }
        return trans;
    }

    public void setSelectedTransaction(final Transaction t) {
        int row = getTableModel().indexOf(t);

        if (row >= 0) {
            setSelectedRow(row);
        }
    }

    public void setSelectedRow(final int row) {
        if (row >= 0) {
            JTable table = getTable();
            table.scrollRectToVisible(table.getCellRect(row, 0, true));
            table.getSelectionModel().setSelectionInterval(row, row);
        }
    }

    protected CommodityNode getAccountCurrencyNode() {
        return getAccount().getCurrencyNode();
    }

    protected String getAccountPath() {
        return getAccount().getPathName();
    }

    /**        \
     * Watch for arrow keys and wrap the table if at the top or bottom
     *
     * @param e key event
     * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
     */
    @Override
    public void keyPressed(final KeyEvent e) {
        int keyCode = e.getKeyCode();

        if (keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_UP) {
            JTable table = getTable();
            int row = table.getSelectedRow();

            if (keyCode == KeyEvent.VK_DOWN && row == table.getRowCount() - 1) {
                row = 0;
                setSelectedRow(row);
                e.consume();
            } else if (keyCode == KeyEvent.VK_UP && row == 0) {
                row = getTable().getRowCount() - 1;
                setSelectedRow(row);
                e.consume();
            }
        }
    }

    /**
     * @param e key event
     * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
     */
    @Override
    public void keyReleased(final KeyEvent e) {
        int keyCode = e.getKeyCode();

        if (keyCode == KeyEvent.VK_DELETE) {
            deleteAction();
        } else if (keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_UP) {
            JTable table = getTable();
            int row = table.getSelectedRow();

            if (keyCode == KeyEvent.VK_DOWN && row > -1 && row < table.getRowCount()) {
                modifyTransaction(row);
            } else if (keyCode == KeyEvent.VK_UP && row >= 0) {
                modifyTransaction(row);
            }
        }
    }

    /**
     * @param e key event
     * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
     */
    @Override
    public void keyTyped(final KeyEvent e) {
        // empty implementation
    }

    @Override
    public void messagePosted(final Message event) {

        final Account account = getAccount();

        EventQueue.invokeLater(new Runnable() { // must update on EDT or a deadlock can occur

            @Override
            public void run() {

                Account a = (Account) event.getObject(MessageProperty.ACCOUNT);

                if (account.equals(a)) {
                    switch (event.getEvent()) {
                        case ACCOUNT_MODIFY:
                            updateAccountState();
                            updateAccountInfo();
                            break;
                        case TRANSACTION_ADD:
                            Transaction t = (Transaction) event.getObject(MessageProperty.TRANSACTION);

                            int index = account.indexOf(t);
                            if (index == account.getTransactionCount() - 1) {
                                autoScroll();
                            }

                            updateAccountInfo();
                            break;
                        case TRANSACTION_REMOVE:
                            updateAccountInfo();
                            break;
                        default:
                            break;
                    }
                }

                if (event.getEvent() == ChannelEvent.COMMODITY_HISTORY_ADD || event.getEvent() == ChannelEvent.COMMODITY_HISTORY_REMOVE) {

                    SecurityNode node = (SecurityNode) event.getObject(MessageProperty.COMMODITY);

                    if (account.containsSecurity(node)) {
                        updateAccountInfo();
                    }
                }
            }
        });
    }

    /**
     * Must place at the end of the event list for this to work
     */
    private void autoScroll() {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {

                final JTable table = getTable();

                Rectangle cell = table.getCellRect(table.getRowCount() - 1, 0, true);
                table.scrollRectToVisible(cell);
            }
        });
    }

    protected class TransactionPopup extends JPopupMenu implements ActionListener {

        private JMenuItem duplicate;

        private JMenuItem delete;

        private JRadioButtonMenuItem reconciled;

        private JRadioButtonMenuItem unreconciled;

        private JMenuItem jump;

        public TransactionPopup() {

            JMenu markSub = new JMenu(rb.getString("Menu.MarkAs.Name"));

            reconciled = new JRadioButtonMenuItem(rb.getString("Menu.Reconciled.Name"));
            reconciled.addActionListener(this);

            unreconciled = new JRadioButtonMenuItem(rb.getString("Menu.Unreconciled.Name"));
            unreconciled.addActionListener(this);

            ButtonGroup g = new ButtonGroup();
            g.add(reconciled);
            g.add(unreconciled);

            markSub.add(reconciled);
            markSub.add(unreconciled);

            add(markSub);

            addSeparator();

            duplicate = new JMenuItem(rb.getString("Menu.Duplicate.Name"));
            duplicate.addActionListener(this);
            add(duplicate);

            jump = new JMenuItem(rb.getString("Menu.Jump.Name"));
            jump.addActionListener(this);
            add(jump);

            addSeparator();

            delete = new JMenuItem(rb.getString("Menu.Delete.Name"));
            delete.addActionListener(this);
            add(delete);
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            if (e.getSource() == delete) {
                deleteAction();
            } else if (e.getSource() == duplicate) {
                duplicateAction();
            } else if (e.getSource() == jump) {
                jumpAction();
            } else if (e.getSource() == reconciled) {
                reconcileAction(ReconciledState.RECONCILED);
            } else if (e.getSource() == unreconciled) {
                reconcileAction(ReconciledState.NOT_RECONCILED);
            }
        }

        @Override
        public void show(final Component invoker, final int x, final int y) {
            Transaction t = getSelectedTransaction();

            if (t != null) {
                if (t.getReconciled(getAccount()) == ReconciledState.RECONCILED) {
                    reconciled.setSelected(true);
                } else {
                    unreconciled.setSelected(true);
                }
            }
            super.show(invoker, x, y);
        }
    }
}
