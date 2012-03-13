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
package jgnash.ui.actions;

import jgnash.ui.report.compiled.PayeePieChart;
import jgnash.ui.util.builder.Action;

import java.awt.event.ActionEvent;

/**
 * UI Action to open the new file dialog
 *
 * @author Craig Cavanaugh
 * @version $Id: NewFileAction.java 2540 2011-01-16 21:00:19Z ccavanaugh $
 */
@Action("report-payeepiechart-command")
public class PayeePieChartAction extends AbstractEnabledAction {

    private static final long serialVersionUID = 0L;

    @Override
    public void actionPerformed(final ActionEvent e) {
       PayeePieChart.show();
    }
}