package de.dytanic.cloudnet.console;

import org.fusesource.jansi.Ansi;

public enum ConsoleColor {

    DEFAULT("default", '0', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.DEFAULT).boldOff().toString()),
    WHITE("white", '1', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.WHITE).bold().toString()),
    BLACK("black", '2', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLACK).bold().toString()),
    RED("red", '3', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.RED).bold().toString()),
    YELLOW("yellow", '4', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.YELLOW).bold().toString()),
    BLUE("blue", '5', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLUE).bold().toString()),
    GREEN("green", '6', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.GREEN).bold().toString()),
    PURPLE("purple", '7', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.MAGENTA).boldOff().toString()),
    ORANGE("orange", '8', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.RED).fg(Ansi.Color.YELLOW).bold().toString()),
    GRAY("gray", '9', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.WHITE).boldOff().toString()),
    DARK_RED("dark_red", 'a', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.RED).boldOff().toString()),
    DARK_GRAY("dark_gray", 'b', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLACK).bold().toString()),
    DARK_BLUE("dark_blue", 'c', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLUE).boldOff().toString()),
    AQUA("aqua", 'd', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.CYAN).bold().toString());

    private final String name, ansiCode;

    private final char index;

    ConsoleColor(String name, char index, String ansiCode) {
        this.name = name;
        this.index = index;
        this.ansiCode = ansiCode;
    }

    public static String toColouredString(char triggerChar, String text) {
        if (text == null) throw new NullPointerException();

        for (ConsoleColor consoleColour : values())
            text = text.replace(triggerChar + "" + consoleColour.index, consoleColour.ansiCode);

        return text;
    }

    @Override
    public String toString() {
        return ansiCode;
    }

    public String getName() {
        return this.name;
    }

    public String getAnsiCode() {
        return this.ansiCode;
    }

    public char getIndex() {
        return this.index;
    }
}