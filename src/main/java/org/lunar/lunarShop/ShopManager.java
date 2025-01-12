package org.lunar.lunarShop;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.potion.PotionEffectType;
import org.lunar.lunarEconomy.LunarEconomyAPI;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ShopManager {

    private final LunarShop plugin; // Referência ao plugin principal
    private final File shopsFolder;
    private final Map<String, FileConfiguration> shops = new HashMap<>();
    private final LunarEconomyAPI lunarEconomyAPI; // Instância da API do LunarEconomy

    public ShopManager(LunarShop plugin, File shopsFolder) {
        this.plugin = plugin;
        this.shopsFolder = shopsFolder;

        // Inicializa a API do LunarEconomy
        this.lunarEconomyAPI = LunarEconomyAPI.getInstance();
        if (this.lunarEconomyAPI == null) {
            plugin.getLogger().severe("Erro: API do LunarEconomy não foi encontrada!");
            throw new IllegalStateException("API do LunarEconomy não está disponível.");
        }

        // Carrega as lojas
        loadShops();
    }

    public void reloadShops() {
        shops.clear(); // Limpa as lojas existentes
        loadShops(); // Recarrega as lojas a partir dos arquivos
        plugin.getLogger().info("Lojas recarregadas com sucesso!");
    }

    public void saveShopConfig(FileConfiguration shopConfig, String shopName) {
        try {
            File shopFile = new File(shopsFolder, shopName + ".yml");
            shopConfig.save(shopFile);
            plugin.getLogger().info("Loja '" + shopName + "' salva com sucesso.");
        } catch (IOException e) {
            plugin.getLogger().severe("Erro ao salvar a loja '" + shopName + "': " + e.getMessage());
        }
    }

    private void loadShops() {
        File[] shopFiles = shopsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (shopFiles == null || shopFiles.length == 0) {
            plugin.getLogger().info("Nenhuma loja encontrada na pasta 'shops'.");
            return;
        }

        for (File shopFile : shopFiles) {
            FileConfiguration shopConfig = YamlConfiguration.loadConfiguration(shopFile);

            // Validações adicionais para poções
            ConfigurationSection itemsSection = shopConfig.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String key : itemsSection.getKeys(false)) {
                    String materialName = shopConfig.getString("items." + key + ".material", "STONE").toUpperCase();
                    Material material = Material.matchMaterial(materialName);

                    if (material == Material.POTION || material == Material.SPLASH_POTION
                            || material == Material.LINGERING_POTION) {
                        String effectType = shopConfig.getString("items." + key + ".potion_effect.type", "SPEED");
                        int duration = shopConfig.getInt("items." + key + ".potion_effect.duration", 600);
                        int amplifier = shopConfig.getInt("items." + key + ".potion_effect.amplifier", 1);

                        PotionEffectType potionEffectType = PotionEffectType.getByName(effectType.toUpperCase());
                        if (potionEffectType == null) {
                            plugin.getLogger().warning("Tipo de efeito inválido para poção: " + effectType
                                    + " na loja: " + shopFile.getName());
                            continue;
                        }

                        shopConfig.set("items." + key + ".potion_effect.parsed_type", potionEffectType.getName());
                    }
                }
            }

            shops.put(shopFile.getName().replace(".yml", ""), shopConfig);
            plugin.getLogger().info("Loja carregada: " + shopFile.getName());
        }
    }

    public FileConfiguration getShop(String shopName) {
        return shops.get(shopName);
    }

    public LunarEconomyAPI getLunarEconomyAPI() {
        return lunarEconomyAPI;
    }
}
