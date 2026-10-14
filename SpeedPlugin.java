package de.speed;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SpeedPlugin extends JavaPlugin implements TabExecutor {

    private static final float DEFAULT_WALK = 0.2f;
    private static final float DEFAULT_FLY = 0.1f;

    @Override
    public void onEnable() {
        var cmd = getCommand("speed");
        if (cmd == null) {
            getLogger().severe("Befehl 'speed' fehlt in der plugin.yml!");
            return;
        }
        cmd.setExecutor(this);
        cmd.setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1 || args.length > 2) {
            send(sender, "Benutzung: /" + label + " <0-10|reset> [Spieler]", NamedTextColor.YELLOW);
            return true;
        }

        // Ziel bestimmen
        Player target;
        if (args.length == 2) {
            if (!sender.hasPermission("speedplugin.others")) {
                send(sender, "Du darfst die Geschwindigkeit anderer nicht ändern.", NamedTextColor.RED);
                return true;
            }
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                send(sender, "Spieler '" + args[1] + "' ist nicht online.", NamedTextColor.RED);
                return true;
            }
        } else {
            if (!(sender instanceof Player p)) {
                send(sender, "Von der Konsole aus musst du einen Spieler angeben.", NamedTextColor.RED);
                return true;
            }
            target = p;
        }

        // Zurücksetzen
        if (args[0].equalsIgnoreCase("reset")) {
            target.setWalkSpeed(DEFAULT_WALK);
            target.setFlySpeed(DEFAULT_FLY);
            send(sender, "Geschwindigkeit von " + target.getName() + " zurückgesetzt.", NamedTextColor.GREEN);
            if (target != sender) send(target, "Deine Geschwindigkeit wurde zurückgesetzt.", NamedTextColor.GREEN);
            return true;
        }

        // Wert lesen (Komma oder Punkt erlaubt)
        double value;
        try {
            value = Double.parseDouble(args[0].replace(',', '.'));
        } catch (NumberFormatException e) {
            send(sender, "'" + args[0] + "' ist keine Zahl. Erlaubt: 0 bis 10.", NamedTextColor.RED);
            return true;
        }
        if (Double.isNaN(value) || value < 0 || value > 10) {
            send(sender, "Der Wert muss zwischen 0 und 10 liegen.", NamedTextColor.RED);
            return true;
        }

        // 0..10 -> 0.0..1.0 (Minecraft erlaubt maximal 1.0)
        float speed = (float) (value / 10.0);
        boolean flying = target.isFlying();
        if (flying) {
            target.setFlySpeed(speed);
        } else {
            target.setWalkSpeed(speed);
        }

        String art = flying ? "Fluggeschwindigkeit" : "Laufgeschwindigkeit";
        String zahl = String.format(Locale.US, "%.1f", value);
        send(sender, art + " von " + target.getName() + " auf " + zahl + " gesetzt.", NamedTextColor.GREEN);
        if (target != sender) send(target, "Deine " + art + " wurde auf " + zahl + " gesetzt.", NamedTextColor.GREEN);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (int i = 0; i <= 10; i++) out.add(String.valueOf(i));
            out.add("reset");
            out.removeIf(s -> !s.startsWith(args[0].toLowerCase(Locale.ROOT)));
        } else if (args.length == 2 && sender.hasPermission("speedplugin.others")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(p.getName());
                }
            }
        }
        return out;
    }

    private static void send(CommandSender to, String text, NamedTextColor color) {
        to.sendMessage(Component.text(text, color));
    }
}
