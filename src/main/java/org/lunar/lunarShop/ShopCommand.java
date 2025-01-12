package org.lunar.lunarShop;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;

public class ShopCommand implements CommandExecutor {

    private final LunarShop plugin;

    public ShopCommand(LunarShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cApenas jogadores podem usar este comando.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 2 || !args[0].equalsIgnoreCase("abrir")) {
            player.sendMessage("§cUso correto: /shop abrir <nome_da_loja>");
            return true;
        }

        String shopName = args[1];
        FileConfiguration shopConfig = plugin.getShopManager().getShop(shopName);

        if (shopConfig == null) {
            player.sendMessage("§cLoja '" + shopName + "' não encontrada.");
            return true;
        }

        // Abre a GUI da loja
        ShopGUI shopGUI = new ShopGUI(shopConfig, shopName);
        player.openInventory(shopGUI.getInventory());

        return true;
    }
}
