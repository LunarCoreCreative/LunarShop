package org.lunar.lunarShop;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShopGUI {

    private final Inventory inventory;

    public ShopGUI(FileConfiguration shopConfig, String shopName) {
        // Valida se a seção 'items' existe
        if (shopConfig.getConfigurationSection("items") == null) {
            throw new IllegalArgumentException("Configuração da loja '" + shopName + "' não possui a seção 'items'.");
        }

        // Define o tamanho do inventário com base nos itens da loja
        int size = 9 * ((shopConfig.getConfigurationSection("items").getKeys(false).size() / 9) + 1);
        this.inventory = Bukkit.createInventory(null, size, Component.text("Loja: " + shopName));

        // Adiciona itens à loja
        shopConfig.getConfigurationSection("items").getKeys(false).forEach(key -> {
            try {
                // Obtém informações do item do arquivo de configuração
                String materialName = shopConfig.getString("items." + key + ".material", "STONE");
                Material material = Material.valueOf(materialName.toUpperCase());
                String itemName = shopConfig.getString("items." + key + ".name", "Item");
                List<String> loreConfig = shopConfig.getStringList("items." + key + ".lore");
                double buyPrice = shopConfig.getDouble("items." + key + ".price.buy", -1);
                double sellPrice = shopConfig.getDouble("items." + key + ".price.sell", -1);
                int stock = shopConfig.getInt("items." + key + ".stock", -1); // Estoque do item

                // Obtém a moeda configurada ou usa 'lunar' como padrão
                String currency = shopConfig.getString("items." + key + ".currency", "lunar");

                // Verifica se há promoção
                boolean isPromotion = shopConfig.getBoolean("items." + key + ".promotion.enabled", false);
                double discountPercentage = shopConfig.getDouble("items." + key + ".promotion.discount_percentage", 0);
                int promotionStartDay = shopConfig.getInt("items." + key + ".promotion.start_day", -1);
                int promotionDurationDays = shopConfig.getInt("items." + key + ".promotion.duration_days_minecraft", 0);

                // Calcula o preço com desconto, se houver promoção
                double discountedPrice = buyPrice;
                if (isPromotion) {
                    discountedPrice = buyPrice - (buyPrice * discountPercentage / 100);
                }

                // Obtém os encantamentos configurados, se existirem
                Map<String, Object> enchantments = shopConfig
                        .getConfigurationSection("items." + key + ".enchantments") != null
                                ? shopConfig.getConfigurationSection("items." + key + ".enchantments").getValues(false)
                                : null;

                // Cria o item da loja
                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.displayName(Component.text("§a" + itemName));

                    // Configura a lore com preço de compra, venda e estoque
                    List<String> lore = new ArrayList<>(loreConfig);
                    if (isPromotion) {
                        lore.add("§6[Promoção Ativa]");
                        lore.add(String.format("§7Preço original: %s§b%.2f %s",
                                getCurrencySymbol(currency), buyPrice,
                                getCurrencyColor(currency) + capitalize(currency)));
                        lore.add(String.format("§7Preço promocional: %s§b%.2f %s",
                                getCurrencySymbol(currency), discountedPrice,
                                getCurrencyColor(currency) + capitalize(currency)));
                        lore.add(String.format("§7Duração: §e%d dias de Minecraft", promotionDurationDays));
                    } else if (buyPrice > 0) {
                        lore.add(String.format("§7Preço de compra: %s§b%.2f %s",
                                getCurrencySymbol(currency), buyPrice,
                                getCurrencyColor(currency) + capitalize(currency)));
                    }
                    if (sellPrice > 0) {
                        lore.add(String.format("§7Preço de venda: %s§b%.2f %s",
                                getCurrencySymbol(currency), sellPrice,
                                getCurrencyColor(currency) + capitalize(currency)));
                    }
                    if (stock >= 0) {
                        lore.add("§7Estoque: §e" + stock); // Mostra o estoque apenas na loja
                    }
                    meta.lore(lore.stream().map(Component::text).toList());

                    // Aplica encantamentos se existirem no YML
                    if (enchantments != null) {
                        enchantments.forEach((enchantName, level) -> {
                            NamespacedKey enchantKey = NamespacedKey.minecraft(enchantName.toLowerCase());
                            Enchantment enchantment = Registry.ENCHANTMENT.get(enchantKey);
                            if (enchantment != null) {
                                meta.addEnchant(enchantment, (int) level, true);
                            } else {
                                Bukkit.getLogger().warning("Encantamento inválido: " + enchantName);
                            }
                        });
                    }

                    // Esconde a descrição dos encantamentos na lore
                    if (enchantments != null && !enchantments.isEmpty()) {
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    }
                    item.setItemMeta(meta);
                }
                inventory.addItem(item);
            } catch (IllegalArgumentException e) {
                // Trata erros de configuração incorreta
                Bukkit.getLogger()
                        .warning("Erro ao carregar item '" + key + "' na loja '" + shopName + "': " + e.getMessage());
            }
        });
    }

    public Inventory getInventory() {
        return inventory;
    }

    // Função auxiliar para capitalizar o nome da moeda
    private String capitalize(String currency) {
        if (currency == null || currency.isEmpty())
            return "";
        return currency.substring(0, 1).toUpperCase() + currency.substring(1).toLowerCase();
    }

    // Função auxiliar para obter a cor da moeda
    private String getCurrencyColor(String currency) {
        switch (currency.toLowerCase()) {
            case "solar":
                return "§e"; // Amarelo para Solar
            case "lunar":
            default:
                return "§b"; // Azul para Lunar
        }
    }

    // Função auxiliar para obter o símbolo da moeda
    private String getCurrencySymbol(String currency) {
        switch (currency.toLowerCase()) {
            case "solar":
                return "§e☀ "; // Símbolo do Solar em amarelo
            case "lunar":
            default:
                return "§b₪ "; // Símbolo do Lunar em azul
        }
    }
}
