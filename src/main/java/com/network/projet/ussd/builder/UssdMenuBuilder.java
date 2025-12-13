package com.network.projet.ussd.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builder for constructing USSD menus
 */
public class UssdMenuBuilder {
    private String menuId;
    private String title;
    private List<MenuItem> menuItems;
    private String previousMenuId;
    private boolean isTerminal;

    public UssdMenuBuilder() {
        this.menuItems = new ArrayList<>();
        this.isTerminal = false;
    }

    public UssdMenuBuilder menuId(String menuId) {
        this.menuId = menuId;
        return this;
    }

    public UssdMenuBuilder title(String title) {
        this.title = title;
        return this;
    }

    public UssdMenuBuilder addMenuItem(String option, String text, String nextMenuId) {
        menuItems.add(new MenuItem(option, text, nextMenuId));
        return this;
    }

    public UssdMenuBuilder addMenuItem(MenuItem item) {
        menuItems.add(item);
        return this;
    }

    public UssdMenuBuilder previousMenuId(String previousMenuId) {
        this.previousMenuId = previousMenuId;
        return this;
    }

    public UssdMenuBuilder terminal(boolean isTerminal) {
        this.isTerminal = isTerminal;
        return this;
    }

    public UssdMenu build() {
        return new UssdMenu(menuId, title, menuItems, previousMenuId, isTerminal);
    }

    /**
     * Inner class representing a menu item
     */
    public static class MenuItem {
        private String option;
        private String text;
        private String nextMenuId;

        public MenuItem(String option, String text, String nextMenuId) {
            this.option = option;
            this.text = text;
            this.nextMenuId = nextMenuId;
        }

        public String getOption() {
            return option;
        }

        public String getText() {
            return text;
        }

        public String getNextMenuId() {
            return nextMenuId;
        }
    }

    /**
     * Inner class representing the built USSD menu
     */
    public static class UssdMenu {
        private String menuId;
        private String title;
        private List<MenuItem> menuItems;
        private String previousMenuId;
        private boolean isTerminal;

        public UssdMenu(String menuId, String title, List<MenuItem> menuItems,
                       String previousMenuId, boolean isTerminal) {
            this.menuId = menuId;
            this.title = title;
            this.menuItems = new ArrayList<>(menuItems);
            this.previousMenuId = previousMenuId;
            this.isTerminal = isTerminal;
        }

        public String getMenuId() {
            return menuId;
        }

        public String getTitle() {
            return title;
        }

        public List<MenuItem> getMenuItems() {
            return Collections.unmodifiableList(menuItems);
        }

        public String getPreviousMenuId() {
            return previousMenuId;
        }

        public boolean isTerminal() {
            return isTerminal;
        }

        @Override
        public String toString() {
            return "UssdMenu{" +
                    "menuId='" + menuId + '\'' +
                    ", title='" + title + '\'' +
                    ", itemCount=" + menuItems.size() +
                    ", isTerminal=" + isTerminal +
                    '}';
        }
    }
}
