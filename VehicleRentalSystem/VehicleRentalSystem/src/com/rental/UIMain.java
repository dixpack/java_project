package com.rental;

import com.rental.gui.RentalSystemGUI;
import javax.swing.SwingUtilities;

/**
 * Entry point to launch the Vehicle Rental System with Graphical User Interface (GUI).
 */
public class UIMain {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            RentalSystemGUI gui = new RentalSystemGUI();
            gui.setVisible(true);
        });
    }
}
