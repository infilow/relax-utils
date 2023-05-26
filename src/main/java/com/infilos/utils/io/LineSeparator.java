package com.infilos.utils.io;

public enum LineSeparator {
    /**
     * Line separator for Unix systems (<tt>\n</tt>).
     */
    Unix("\n"),
    /**
     * Line separator for Windows systems (<tt>\r\n</tt>).
     */
    Windows("\r\n"),
    /**
     * Line separator for Macintosh systems (<tt>\r</tt>).
     */
    Macintosh("\r\n"),
    /**
     * Line separator for the Web (<tt>\n</tt>).
     */
    Web("\n");

    private final String value;

    LineSeparator(String lineSeparator) {
        this.value = lineSeparator;
    }

    public String getValue() {
        return this.value;
    }
}
