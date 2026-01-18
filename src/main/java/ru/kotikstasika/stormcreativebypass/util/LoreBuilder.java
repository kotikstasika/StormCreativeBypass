package ru.kotikstasika.stormcreativebypass.util;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoreBuilder {
    
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern RGB_PATTERN = Pattern.compile("&x&([0-9A-Fa-f])&([0-9A-Fa-f])&([0-9A-Fa-f])&([0-9A-Fa-f])&([0-9A-Fa-f])&([0-9A-Fa-f])");
    
    public static String translateColorCodes(String text) {
        if (text == null) return "";
        
        try {
            text = translateHexColors(text);
            text = translateRgbColors(text);
        } catch (Exception e) {
        }
        
        text = ChatColor.translateAlternateColorCodes('&', text);
        
        return text;
    }
    
    private static String translateHexColors(String text) {
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String hex = matcher.group(1);
            try {
                String replacement = convertHexToColorCode(hex);
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
            } catch (Exception e) {
                matcher.appendReplacement(buffer, matcher.group(0));
            }
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
    
    private static String translateRgbColors(String text) {
        Matcher matcher = RGB_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            try {
                String r = matcher.group(1);
                String g = matcher.group(2);
                String b = matcher.group(3);
                String hex = r + g + b + matcher.group(4) + matcher.group(5) + matcher.group(6);
                String replacement = convertHexToColorCode(hex);
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
            } catch (Exception e) {
                matcher.appendReplacement(buffer, matcher.group(0));
            }
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
    
    private static String convertHexToColorCode(String hex) {
        try {
            if (hex.length() != 6) return "";
            
            java.awt.Color color = java.awt.Color.decode("#" + hex);
            int r = color.getRed();
            int g = color.getGreen();
            int b = color.getBlue();
            
            String rHex = String.format("%02x", r);
            String gHex = String.format("%02x", g);
            String bHex = String.format("%02x", b);
            
            return String.format("§x§%c§%c§%c§%c§%c§%c",
                    rHex.charAt(0), rHex.charAt(1),
                    gHex.charAt(0), gHex.charAt(1),
                    bHex.charAt(0), bHex.charAt(1));
        } catch (Exception e) {
            return "";
        }
    }
    
    public static List<String> buildLore(List<String> template, String owner, String lastPlayer) {
        List<String> lore = new ArrayList<>();
        
        for (String line : template) {
            String formatted = line.replace("{creative}", "Креатив")
                    .replace("{player}", owner);
            lore.add(translateColorCodes(formatted));
        }
        
        if (lastPlayer != null && !lastPlayer.equals(owner)) {
            lore.add(translateColorCodes("&7&f &b&l— &fПоследний игрок: &6" + lastPlayer));
        }
        
        return lore;
    }
}


