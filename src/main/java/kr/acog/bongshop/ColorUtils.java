package kr.acog.bongshop;

import net.md_5.bungee.api.ChatColor;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// https://github.com/M3II0/Spigot-Color-Utils
public class ColorUtils {

    private static Method COLOR_FROM_CHAT_COLOR;
    private static Method CHAT_COLOR_FROM_COLOR;
    private static final boolean hexSupport;
    private static final Pattern gradient = Pattern.compile("<(#[A-Fa-f0-9]{6})>(.*?)</(#[A-Fa-f0-9]{6})>");
    private static final Pattern legacyGradient = Pattern.compile("<(&[A-Fa-f0-9k-orK-OR])>(.*?)</(&[A-Fa-f0-9k-orK-OR])>");
    private static final Pattern rgb = Pattern.compile("&\\{(#[A-Fa-f0-9]{6})}");
    private static final Pattern namedTag = Pattern.compile("<([a-z_]+)>", Pattern.CASE_INSENSITIVE);
    private static final Pattern closingTag = Pattern.compile("</([a-z_]+)>", Pattern.CASE_INSENSITIVE);
    private static final Pattern hexTag = Pattern.compile("<(#[A-Fa-f0-9]{6})>");
    private static final Map<String, String> NAMED_TAGS = createNamedTags();

    static {
        try {
            COLOR_FROM_CHAT_COLOR = ChatColor.class.getDeclaredMethod("getColor");
            CHAT_COLOR_FROM_COLOR = ChatColor.class.getDeclaredMethod("of", Color.class);
        } catch (NoSuchMethodException e) {
            COLOR_FROM_CHAT_COLOR = null;
            CHAT_COLOR_FROM_COLOR = null;
        }
        hexSupport = CHAT_COLOR_FROM_COLOR != null;
    }

    public static String colorize(String text) {
        return colorize(text, '&');
    }

    public static String colorize(String text, char colorSymbol) {
        text = replaceNamedTags(text);
        text = replaceHexTags(text);

        Matcher g = gradient.matcher(text);
        Matcher l = legacyGradient.matcher(text);
        Matcher r = rgb.matcher(text);
        while (g.find()) {
            Color start = Color.decode(g.group(1));
            String between = g.group(2);
            Color end = Color.decode(g.group(3));
            if (hexSupport) text = text.replace(g.group(0), rgbGradient(between, start, end, colorSymbol));
            else text = text.replace(g.group(0), between);
        }
        while (l.find()) {
            char first = l.group(1).charAt(1);
            String between = l.group(2);
            char second = l.group(3).charAt(1);
            ChatColor firstColor = ChatColor.getByChar(first);
            ChatColor secondColor = ChatColor.getByChar(second);
            if (firstColor == null) firstColor = ChatColor.WHITE;
            if (secondColor == null) secondColor = ChatColor.WHITE;
            if (hexSupport) text = text.replace(l.group(0), rgbGradient(between, fromChatColor(firstColor), fromChatColor(secondColor), colorSymbol));
            else text = text.replace(l.group(0), between);
        }
        while (r.find()) {
            if (hexSupport) {
                ChatColor color = fromColor(Color.decode(r.group(1)));
                text = text.replace(r.group(0), color + "");
            } else {
                text = text.replace(r.group(0), "");
            }
        }
        return ChatColor.translateAlternateColorCodes(colorSymbol, text);
    }

    public static String removeColors(String text) {
        return ChatColor.stripColor(text);
    }

    public static List<Character> charactersWithoutColors(String text) {
        text = removeColors(text);
        final List<Character> result = new ArrayList<>();
        for (char var : text.toCharArray()) {
            result.add(var);
        }
        return result;
    }

    public static List<String> charactersWithColors(String text) {
        return charactersWithColors(text, '§');
    }

    public static List<String> charactersWithColors(String text, char colorSymbol) {
        final List<String> result = new ArrayList<>();
        StringBuilder colors = new StringBuilder();
        boolean colorInput = false;
        boolean reading = false;
        for (char var : text.toCharArray()) {
            if (colorInput) {
                colors.append(var);
                colorInput = false;
            } else {
                if (var == colorSymbol) {
                    if (!reading) {
                        colors = new StringBuilder();
                    }
                    colorInput = true;
                    reading = true;
                    colors.append(var);
                } else {
                    reading = false;
                    result.add(colors.toString() + var);
                }
            }
        }
        return result;
    }

    private static String rgbGradient(String text, Color start, Color end, char colorSymbol) {
        final StringBuilder builder = new StringBuilder();
        text = ChatColor.translateAlternateColorCodes(colorSymbol, text);
        final List<String> characters = charactersWithColors(text);
        final double[] red = linear(start.getRed(), end.getRed(), characters.size());
        final double[] green = linear(start.getGreen(), end.getGreen(), characters.size());
        final double[] blue = linear(start.getBlue(), end.getBlue(), characters.size());
        if (text.length() == 1) {
            return fromColor(end) + text;
        }
        for (int i = 0; i < characters.size(); i++) {
            String currentText = characters.get(i);
            ChatColor current = fromColor(new Color((int) Math.round(red[i]), (int) Math.round(green[i]), (int) Math.round(blue[i])));
            builder.append(current).append(currentText.replace("§r", ""));
        }
        return builder.toString();
    }

    private static String replaceNamedTags(String text) {
        Matcher closingMatcher = closingTag.matcher(text);
        StringBuffer closed = new StringBuffer();
        while (closingMatcher.find()) {
            String replacement = NAMED_TAGS.containsKey(closingMatcher.group(1).toLowerCase()) ? "&r" : closingMatcher.group(0);
            closingMatcher.appendReplacement(closed, Matcher.quoteReplacement(replacement));
        }
        closingMatcher.appendTail(closed);

        Matcher namedMatcher = namedTag.matcher(closed.toString());
        StringBuffer named = new StringBuffer();
        while (namedMatcher.find()) {
            String replacement = NAMED_TAGS.get(namedMatcher.group(1).toLowerCase());
            namedMatcher.appendReplacement(named, Matcher.quoteReplacement(replacement != null ? replacement : namedMatcher.group(0)));
        }
        namedMatcher.appendTail(named);
        return named.toString();
    }

    private static String replaceHexTags(String text) {
        Matcher matcher = hexTag.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String replacement = hexSupport ? fromColor(Color.decode(matcher.group(1))).toString() : "";
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static double[] linear(double from, double to, int max) {
        final double[] res = new double[max];
        for (int i = 0; i < max; i++) {
            res[i] = from + i * ((to - from) / (max - 1));
        }
        return res;
    }

    private static Color fromChatColor(ChatColor color) {
        try {
            return (Color) COLOR_FROM_CHAT_COLOR.invoke(color);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static ChatColor fromColor(Color color) {
        try {
            return (ChatColor) CHAT_COLOR_FROM_COLOR.invoke(null, color);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, String> createNamedTags() {
        Map<String, String> tags = new HashMap<>();
        tags.put("black", "&0");
        tags.put("dark_blue", "&1");
        tags.put("dark_green", "&2");
        tags.put("dark_aqua", "&3");
        tags.put("dark_red", "&4");
        tags.put("dark_purple", "&5");
        tags.put("gold", "&6");
        tags.put("gray", "&7");
        tags.put("grey", "&7");
        tags.put("dark_gray", "&8");
        tags.put("dark_grey", "&8");
        tags.put("blue", "&9");
        tags.put("green", "&a");
        tags.put("aqua", "&b");
        tags.put("red", "&c");
        tags.put("light_purple", "&d");
        tags.put("yellow", "&e");
        tags.put("white", "&f");
        tags.put("obfuscated", "&k");
        tags.put("magic", "&k");
        tags.put("bold", "&l");
        tags.put("strikethrough", "&m");
        tags.put("underline", "&n");
        tags.put("underlined", "&n");
        tags.put("italic", "&o");
        tags.put("reset", "&r");
        return tags;
    }

}
