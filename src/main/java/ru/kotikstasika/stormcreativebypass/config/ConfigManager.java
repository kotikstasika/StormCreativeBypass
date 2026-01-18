package ru.kotikstasika.stormcreativebypass.config;

import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import ru.kotikstasika.stormcreativebypass.StormCreativeBypass;
import ru.kotikstasika.stormcreativebypass.model.AuditProfile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class ConfigManager {

    private List<String> itemLoreTemplate;
    private String creativePlaceholder;
    private String playerPlaceholder;

    private String bypassPermission;
    private String removeBypassPermission;
    private String commandBypassPermission;
    private String creativeItemCommandBypassPermission;

    private boolean enableAuditLog;
    private boolean enableConsoleLogging;
    private boolean enableTelegramAlerts;
    private String telegramBotToken;
    private String telegramChatId;

    private boolean enableNearbyPlayersLogging;
    private double nearbyRadius;
    private boolean removeCreativeItemsOnDeath;

    private List<String> blockingCommands;
    private String blockingMessage;

    private List<String> creativeItemBlockingCommands;
    private String creativeItemBlockingMessage;

    private Map<String, AuditProfile> auditProfiles;
    private Set<String> monitoredPlayersSet;

    public void load() {
        StormCreativeBypass plugin = StormCreativeBypass.getInstance();
        plugin.reloadConfig();

        itemLoreTemplate = plugin.getConfig().getStringList("item-added-lore").stream().map(line -> ChatColor.translateAlternateColorCodes('&', line)).collect(Collectors.toList());

        creativePlaceholder = plugin.getConfig().getString("placeholders.creative", "{creative}");
        playerPlaceholder = plugin.getConfig().getString("placeholders.player", "{player}");

        bypassPermission = plugin.getConfig().getString("permissions.bypass", "stormcreativebypass.bypass");
        removeBypassPermission = plugin.getConfig().getString("permissions.remove-bypass", "stormcreativebypass.remove.bypass");
        commandBypassPermission = plugin.getConfig().getString("permissions.command-bypass", "stormcreativebypass.command.bypass");
        creativeItemCommandBypassPermission = plugin.getConfig().getString("permissions.creative-item-command-bypass", "stormcreativebypass.creativeitem.command.bypass");

        enableAuditLog = plugin.getConfig().getBoolean("audit.enabled", true);
        enableConsoleLogging = plugin.getConfig().getBoolean("audit.console.enabled", true);
        enableTelegramAlerts = plugin.getConfig().getBoolean("audit.telegram.enabled", false);
        telegramBotToken = plugin.getConfig().getString("audit.telegram.bot-token", "");
        telegramChatId = plugin.getConfig().getString("audit.telegram.chat-id", "");

        enableNearbyPlayersLogging = plugin.getConfig().getBoolean("audit.nearby-players.enabled", false);
        nearbyRadius = plugin.getConfig().getDouble("audit.nearby-players.radius", 10.0);
        removeCreativeItemsOnDeath = plugin.getConfig().getBoolean("audit.remove-creative-items-on-death", false);

        auditProfiles = new HashMap<>();
        monitoredPlayersSet = new java.util.HashSet<>();

        if (plugin.getConfig().contains("audit.profiles")) {
            ConfigurationSection profilesSection = plugin.getConfig().getConfigurationSection("audit.profiles");
            if (profilesSection != null) {
                for (String profileName : profilesSection.getKeys(false)) {
                    String path = "audit.profiles." + profileName;
                    AuditProfile profile = new AuditProfile();
                    profile.setPlayers(plugin.getConfig().getStringList(path + ".players"));
                    profile.setLogGameMode(plugin.getConfig().getBoolean(path + ".log-types.game-mode", true));
                    profile.setLogItemTake(plugin.getConfig().getBoolean(path + ".log-types.item-take", true));
                    profile.setLogItemDrop(plugin.getConfig().getBoolean(path + ".log-types.item-drop", true));
                    profile.setLogBlockPlace(plugin.getConfig().getBoolean(path + ".log-types.block-place", true));
                    profile.setLogItemPickup(plugin.getConfig().getBoolean(path + ".log-types.item-pickup", true));
                    profile.setLogContainerItems(plugin.getConfig().getBoolean(path + ".log-types.container-items", true));
                    profile.setLogInteract(plugin.getConfig().getBoolean(path + ".log-types.interact", true));
                    profile.setRemoveCreativeItemsOnDeath(plugin.getConfig().getBoolean(path + ".remove-creative-items-on-death", false));

                    auditProfiles.put(profileName, profile);
                    monitoredPlayersSet.addAll(profile.getPlayers());
                }
            }
        }

        blockingCommands = plugin.getConfig().getStringList("blocking-commands");
        blockingMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("blocking-message", "§cВы не можете использовать эту команду, пока находитесь в креативе!"));

        creativeItemBlockingCommands = plugin.getConfig().getStringList("creative-item-blocking-commands");
        creativeItemBlockingMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("creative-item-blocking-message", "§cВы не можете использовать эту команду, пока в вашем инвентаре есть креативные предметы!"));
    }
}

