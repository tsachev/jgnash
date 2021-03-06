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
package jgnash.ui.components;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;

import jgnash.ui.UIApplication;
import jgnash.util.Resource;

/**
 * A simple dialog with a close button
 *
 * @author Craig Cavanaugh
 *
 */
public class GenericCloseDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = 1L;

    private final Resource rb = Resource.get();

    private JButton closeButton;

    private JComponent component;

    public GenericCloseDialog(final Window parent, final JComponent panel, final String title) {
        super(parent);
        setTitle(title);
        setModal(true);
        setIconImage(Resource.getImage("/jgnash/resource/gnome-money.png"));
        this.component = panel;
        layoutMainPanel();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(parent);
    }

    public GenericCloseDialog(final JComponent panel, final String title) {
        this(UIApplication.getFrame(), panel, title);
    }

    JComponent getComponent() {
        return component;
    }

    private void layoutMainPanel() {
        FormLayout layout = new FormLayout("fill:p:g", "f:p:g, $ugap, f:p");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        closeButton = new JButton(rb.getString("Button.Close"));

        builder.append(component);
        builder.nextLine();
        builder.nextLine();
        builder.append(ButtonBarFactory.buildCloseBar(closeButton));

        getContentPane().add(builder.getPanel());
        pack();

        closeButton.addActionListener(this);
    }

    /**
     * Invoked when an action occurs.
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == closeButton) {
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        }
    }
}