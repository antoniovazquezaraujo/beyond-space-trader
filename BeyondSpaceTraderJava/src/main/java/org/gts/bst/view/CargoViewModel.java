package org.gts.bst.view;

import java.util.List;


/**
 * Cargo table of the main window, one row per trade item. When no game is loaded the
 * rows are {@link CargoRowViewModel#empty()}.
 */
public record CargoViewModel(List<CargoRowViewModel> rows) {
}
