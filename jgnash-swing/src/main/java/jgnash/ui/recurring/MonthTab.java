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
package jgnash.ui.recurring;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import jgnash.engine.recurring.MonthlyReminder;
import jgnash.engine.recurring.Reminder;
import jgnash.ui.components.DatePanel;
import jgnash.util.Resource;

/**
 * Monthly reminder tab.
 *
 * @author Craig Cavanaugh
 *
 */
public class MonthTab extends JPanel implements RecurringTab, ActionListener {

    private JRadioButton noEndButton;

    private JRadioButton endButton;

    private JSpinner numberSpinner;

    private DatePanel endDateField;

    private JComboBox<String> typeComboBox;

    private final Resource rb = Resource.get();

    private final ButtonGroup group = new ButtonGroup();

    private Reminder reminder = new MonthlyReminder();

    /**
     * Creates new form DayTab
     */
    public MonthTab() {
        layoutMainPanel();

        noEndButton.addActionListener(this);
        endButton.addActionListener(this);

        noEndButton.setSelected(true);
        updateForm();
    }

    private void layoutMainPanel() {
        FormLayout layout = new FormLayout("right:p, $lcgap, f:p, 2dlu, max(48dlu;min), 2dlu, f:p, 10dlu, right:p, 4dlu, max(48dlu;min)", "f:p, $lgap, f:p, $lgap, f:p");
        layout.setRowGroups(new int[][]{{1, 3, 5}});
        setLayout(layout);
        setBorder(Borders.DIALOG_BORDER);

        CellConstraints cc = new CellConstraints();

        noEndButton = new JRadioButton(rb.getString("Button.NoEndDate"));
        endButton = new JRadioButton();
        endDateField = new DatePanel();

        group.add(noEndButton);
        group.add(endButton);

        numberSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 24, 1));
        typeComboBox = new JComboBox<>();

        typeComboBox.setModel(new DefaultComboBoxModel<>(new String[]{rb.getString("Column.Date"), rb.getString("Column.Day")}));

        add(new JLabel(rb.getString("Label.Every")), cc.xy(1, 1));
        add(numberSpinner, cc.xywh(3, 1, 3, 1));
        add(new JLabel(rb.getString("Tab.Month")), cc.xy(7, 1));

        add(new JLabel(rb.getString("Label.By")), cc.xy(9, 1));
        add(typeComboBox, cc.xy(11, 1));

        add(new JLabel(rb.getString("Label.EndOn")), cc.xy(1, 3));
        add(noEndButton, cc.xyw(3, 3, 5));

        add(endButton, cc.xy(3, 5));
        add(endDateField, cc.xy(5, 5));

    }

    private void updateForm() {
        endDateField.setEnabled(endButton.isSelected());
    }

    /**
     * @param e action event
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == noEndButton || e.getSource() == endButton) {
            updateForm();
        }
    }

    /**
     * @see jgnash.ui.recurring.RecurringTab#getReminder()
     */
    @Override
    public Reminder getReminder() {
        int inc = ((Number) numberSpinner.getValue()).intValue();
        Date endDate = null;

        if (endButton.isSelected()) {
            endDate = endDateField.getDate();
        }

        reminder.setIncrement(inc);
        reminder.setEndDate(endDate);
        ((MonthlyReminder) reminder).setType(typeComboBox.getSelectedIndex());

        return reminder;
    }

    /**
     * @see jgnash.ui.recurring.RecurringTab#setReminder(jgnash.engine.recurring.Reminder)
     */
    @Override
    public void setReminder(final Reminder reminder) {
        assert reminder instanceof MonthlyReminder;

        this.reminder = reminder;

        numberSpinner.setValue(reminder.getIncrement());
        typeComboBox.setSelectedIndex(((MonthlyReminder) reminder).getType());

        if (reminder.getEndDate() != null) {
            endDateField.setDate(reminder.getEndDate());
            endButton.setSelected(true);
        }
        updateForm();
    }
}
