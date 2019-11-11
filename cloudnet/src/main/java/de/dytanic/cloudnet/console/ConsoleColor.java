package de.dytanic.cloudnet.console;

import org.fusesource.jansi.Ansi;

import java.awt.*;

public enum ConsoleColor {

    DEFAULT("reset", Color.GRAY, 'r', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.DEFAULT).boldOff().toString()),

    GREEN("green", Color.GREEN, 'a', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.GREEN).bold().toString()),
    LIGHT_BLUE("light_blue", new Color(135, 206, 250), 'b', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.CYAN).bold().toString()),
    RED("red", new Color(170, 54, 47), 'c', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.RED).bold().toString()),
    YELLOW("yellow", Color.YELLOW, 'e', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.YELLOW).bold().toString()),
    WHITE("white", Color.WHITE, 'f', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.WHITE).bold().toString()),

    BLACK("black", Color.BLACK, '0', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLACK).boldOff().toString()),
    DARK_BLUE("dark_blue", new Color(0, 0, 139), '1', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLUE).boldOff().toString()),
    DARK_GREEN("dark_green", new Color(0, 100, 0), '2', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.GREEN).boldOff().toString()),
    CYAN("cyan", Color.CYAN, '3', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.CYAN).boldOff().toString()),
    DARK_RED("dark_red", new Color(139, 0, 0), '4', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.RED).boldOff().toString()),
    PURPLE("purple", new Color(75, 0, 130), '5', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.MAGENTA).boldOff().toString()),
    ORANGE("orange", Color.ORANGE, '6', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.YELLOW).boldOff().toString()),
    DARK_GRAY("dark_gray", Color.DARK_GRAY, '8', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLACK).bold().toString()),
    GRAY("gray", Color.GRAY, '7', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.WHITE).boldOff().toString()),
    BLUE("blue", new Color(35, 61, 97), '9', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLUE).bold().toString());

    private final String name;
    private final String ansiCode;

    private final Color color;
    private final char index;

    ConsoleColor(String name, Color color, char index, String ansiCode) {
        this.name = name;
        this.color = color;
        this.index = index;
        this.ansiCode = ansiCode;
    }

    public static String toColouredString(char triggerChar, String text) {
        if (text == null) {
            throw new NullPointerException();
        }

        for (ConsoleColor consoleColour : values()) {
            text = text.replace(triggerChar + "" + consoleColour.index, consoleColour.ansiCode);
        }

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

    public Color getColor() {
        return color;
    }

    public char getIndex() {
        return this.index;
    }
}