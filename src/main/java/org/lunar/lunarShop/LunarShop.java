package org.lunar.lunarShop;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class LunarShop extends JavaPlugin {

    private ShopManager shopManager;

    @Override
    public void onEnable() {
        // Inicializa o plugin
        setupPlugin();
        getLogger().info("LunarShop iniciado com sucesso!");
    }

    @Override
    public void onDisable() {
        // Desativa o plugin e remove os eventos registrados
        HandlerList.unregisterAll(this);
        getLogger().info("LunarShop foi desativado.");
    }

    private void setupPlugin() {
        try {
            // Cria a pasta de dados do plugin
            if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
                getLogger().severe("Erro: Não foi possível criar a pasta de dados do plugin!");
                Bukkit.getPluginManager().disablePlugin(this);
                return;
            }

            // Verifica e cria a pasta de lojas
            File shopsFolder = new File(getDataFolder(), "shops");
            if (!shopsFolder.exists() && !shopsFolder.mkdirs()) {
                getLogger().severe("Erro: Não foi possível criar a pasta de lojas!");
                Bukkit.getPluginManager().disablePlugin(this);
                return;
            }

            // Copia YAMLs padrão para a pasta de lojas
            saveDefaultShops(shopsFolder);

            // Inicializa o ShopManager
            this.shopManager = new ShopManager(this, shopsFolder);

            // Registra o comando /shop
            PluginCommand shopCommand = getCommand("shop");
            if (shopCommand != null) {
                shopCommand.setExecutor(new ShopCommand(this));
            }

            // Registra eventos
            Bukkit.getPluginManager().registerEvents(new ShopListener(this), this);

        } catch (Exception e) {
            getLogger().severe("Erro ao configurar o LunarShop: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveDefaultShops(File shopsFolder) {
        String[] defaultShops = {"ferreiro.yml", "mercador.yml"}; // Adicione os nomes dos arquivos aqui
        for (String shopFileName : defaultShops) {
            File shopFile = new File(shopsFolder, shopFileName);
            if (!shopFile.exists()) {
                try (InputStream in = getResource("shops/" + shopFileName);
                     FileOutputStream out = new FileOutputStream(shopFile)) {
                    if (in != null) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = in.read(buffer)) > 0) {
                            out.write(buffer, 0, len);
                        }
                        getLogger().info("Arquivo padrão '" + shopFileName + "' copiado para: " + shopFile.getAbsolutePath());
                    }
                } catch (IOException e) {
                    getLogger().severe("Erro ao copiar o arquivo '" + shopFileName + "': " + e.getMessage());
                }
            }
        }
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("lunarshopreload")) {
            if (!sender.hasPermission("lunarshop.reload")) {
                sender.sendMessage("§cVocê não tem permissão para usar este comando!");
                return true;
            }

            // Lógica para recarregar o plugin
            try {
                // Limpa eventos registrados para evitar duplicação
                HandlerList.unregisterAll(this);

                // Reinicializa o plugin
                setupPlugin();

                sender.sendMessage("§aLunarShop recarregado com sucesso!");
                getLogger().info("LunarShop foi recarregado.");
            } catch (Exception e) {
                sender.sendMessage("§cErro ao recarregar o LunarShop. Verifique o console para mais detalhes.");
                getLogger().severe("Erro ao recarregar o LunarShop:");
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }
}
