package org.gts.bst.view;

import java.util.List;


/**
 * High scores screen: three rows, possibly empty. Rows that are not {@code filled} are
 * left with the front-end defaults.
 */
public record HighScoresViewModel(List<Row> rows) {
  public record Row(boolean filled, String name, String score, String status) {
  }
}
