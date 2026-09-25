package org.gts.bst.view;


/**
 * Text of the quests screen, already formatted. {@code hasQuests} tells the front-end
 * whether the text describes open quests (and can therefore contain system links).
 */
public record QuestsViewModel(String text, boolean hasQuests) {
}
