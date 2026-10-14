/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.view;

import java.util.List;
import org.gts.bst.ports.DialogResult;


/**
 * Shows an alert and returns the result of the button the player pressed. The
 * Lanterna front-end implements it over its GUI; tests over a fake.
 */
@FunctionalInterface
public interface AlertDialogHost {
  DialogResult show(String title, String message, List<AlertButton> buttons);
}
