package org.gts.bst.view;

import java.util.List;


/**
 * Everything the personnel screen displays, already formatted for the front-end.
 */
public record PersonnelViewModel(
    List<String> crewEntries,
    boolean crewVisible,
    String crewEmptyText,
    List<String> forHireEntries,
    boolean forHireVisible,
    String forHireEmptyText,
    PersonnelInfo info) {
}
