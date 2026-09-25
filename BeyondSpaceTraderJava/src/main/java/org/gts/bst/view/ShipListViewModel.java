package org.gts.bst.view;

import java.util.List;


/**
 * Ship list rows: name, price text and whether the ship can be bought.
 */
public record ShipListViewModel(List<Row> rows) {
  public record Row(String name, String price, boolean buyVisible) {
  }
}
