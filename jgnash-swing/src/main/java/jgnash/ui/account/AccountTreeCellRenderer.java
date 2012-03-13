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
package jgnash.ui.account;

import java.util.HashSet;
import java.util.Set;

import javax.swing.tree.TreeCellRenderer;

import jgnash.engine.Account;

/**
 * This extends the DefaultTreeCellRenderer to make place holder accounts and any specified accounts appear disabled.
 * 
 * @author Craig Cavanaugh
 * @version $Id: AccountTreeCellRenderer.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
class AccountTreeCellRenderer extends AbstractAccountEnabledTreeCellRenderer {

    public AccountTreeCellRenderer(final TreeCellRenderer delegate) {
        super(delegate);
    }

    private Set<Account> disabledAccount = new HashSet<Account>();

    private boolean isPlaceHolderEnabled = true;

    @Override
    public boolean isAccountEnabled(final Account a) {

        if (disabledAccount.contains(a)) {
            return false;
        }

        return isPlaceHolderEnabled || !a.isPlaceHolder();
    }

    void addDisabledAccount(final Account account) {
        disabledAccount.add(account);
    }

    public void setPlaceHoldersEnabled(final boolean enabled) {
        isPlaceHolderEnabled = enabled;
    }
}