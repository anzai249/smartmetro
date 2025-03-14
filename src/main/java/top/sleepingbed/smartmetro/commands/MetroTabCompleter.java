package top.sleepingbed.smartmetro.commands;

import top.sleepingbed.smartmetro.SmartMetro;
import top.sleepingbed.smartmetro.models.Station;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MetroTabCompleter implements TabCompleter {

    private final SmartMetro plugin;
    private final List<String> EMPTY_LIST = new ArrayList<>();
    private final List<String> MAIN_COMMANDS = Arrays.asList(
            "create", "delete", "list", "language", "lang", "givemachine", "setstation", "setcategory"
    );
    private final List<String> CREATE_TYPES = Arrays.asList("station", "switch");
    private final List<String> DELETE_TYPES = Arrays.asList("station", "switch");

    public MetroTabCompleter(SmartMetro plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return EMPTY_LIST;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("metro.admin")) {
            return EMPTY_LIST;
        }

        // Filter suggestions based on what the player has already typed
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            // First argument - main commands
            suggestions.addAll(MAIN_COMMANDS);
        } else if (args.length == 2) {
            // Second argument - depends on first argument
            switch (args[0].toLowerCase()) {
                case "create":
                    suggestions.addAll(CREATE_TYPES);
                    break;
                case "delete":
                    suggestions.addAll(DELETE_TYPES);
                    break;
                case "language":
                case "lang":
                    suggestions.addAll(plugin.getLocaleManager().getAvailableLocales().keySet());
                    break;
                case "givemachine":
                case "setstation":
                    // Suggest station IDs
                    suggestions.addAll(getStationIds());
                    break;
                case "setcategory":
                    // Suggest existing categories
                    suggestions.addAll(getUniqueCategories());
                    break;
            }
        } else if (args.length == 3) {
            // Third argument - depends on first and second arguments
            if (args[0].equalsIgnoreCase("create") && args[1].equalsIgnoreCase("switch")) {
                // Suggest station IDs for destination
                suggestions.addAll(getStationIds());
            } else if (args[0].equalsIgnoreCase("create") && args[1].equalsIgnoreCase("station")) {
                // For station name, we don't provide suggestions
                return EMPTY_LIST;
            }
        } else if (args.length == 4) {
            // Fourth argument - depends on previous arguments
            if (args[0].equalsIgnoreCase("create") && args[1].equalsIgnoreCase("station")) {
                // Suggest existing categories
                suggestions.addAll(getUniqueCategories());
            }
        }

        // Filter suggestions based on what the player has already typed
        String lastArg = args[args.length - 1].toLowerCase();
        return suggestions.stream()
                .filter(suggestion -> suggestion.toLowerCase().startsWith(lastArg))
                .collect(Collectors.toList());
    }

    private List<String> getStationIds() {
        return new ArrayList<>(plugin.getStationManager().getAllStations().keySet());
    }

    private List<String> getUniqueCategories() {
        List<String> categories = new ArrayList<>();
        categories.add("default"); // Always include default category
        
        for (Station station : plugin.getStationManager().getAllStations().values()) {
            String category = station.getCategory();
            if (!categories.contains(category)) {
                categories.add(category);
            }
        }
        
        return categories;
    }
}

