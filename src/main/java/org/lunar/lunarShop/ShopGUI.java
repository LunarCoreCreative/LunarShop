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
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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
                String materialName = shopConfig.getString("items." + key + ".material", "STONE");
                Material material = Material.valueOf(materialName.toUpperCase());
                String itemName = shopConfig.getString("items." + key + ".name", "Item");
                List<String> loreConfig = shopConfig.getStringList("items." + key + ".lore");
                double buyPrice = shopConfig.getDouble("items." + key + ".price.buy", -1);
                double sellPrice = shopConfig.getDouble("items." + key + ".price.sell", -1);
                int stock = shopConfig.getInt("items." + key + ".stock", -1);
                String currency = shopConfig.getString("items." + key + ".currency", "lunar");

                boolean isPromotion = shopConfig.getBoolean("items." + key + ".promotion.enabled", false);
                double discountPercentage = shopConfig.getDouble("items." + key + ".promotion.discount_percentage", 0);
                double discountedPrice = isPromotion ? buyPrice - (buyPrice * discountPercentage / 100) : buyPrice;

                Map<String, Object> enchantments = shopConfig
                        .getConfigurationSection("items." + key + ".enchantments") != null
                                ? shopConfig.getConfigurationSection("items." + key + ".enchantments").getValues(false)
                                : null;

                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();

                if (meta != null) {
                    meta.displayName(Component.text("§a" + itemName));
                    List<String> lore = new ArrayList<>(loreConfig);

                    // Adiciona informações de promoção
                    if (isPromotion) {
                        lore.add("§6[Promoção Ativa]");
                        lore.add(String.format("§7Preço original: %s§b%.2f %s",
                                getCurrencySymbol(currency), buyPrice,
                                getCurrencyColor(currency) + capitalize(currency)));
                        lore.add(String.format("§7Preço promocional: %s§b%.2f %s",
                                getCurrencySymbol(currency), discountedPrice,
                                getCurrencyColor(currency) + capitalize(currency)));
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
                        lore.add("§7Estoque: §e" + stock);
                    }

                    // Configuração adicional para poções
                    if (meta instanceof PotionMeta potionMeta) {
                        String effectType = shopConfig.getString("items." + key + ".potion_effect.type", "SPEED");
                        int duration = shopConfig.getInt("items." + key + ".potion_effect.duration", 600);
                        int amplifier = shopConfig.getInt("items." + key + ".potion_effect.amplifier", 1);

                        PotionEffectType potionEffectType = PotionEffectType.getByName(effectType.toUpperCase());
                        if (potionEffectType != null) {
                            lore.add("§7Efeitos da Poção:");
                            lore.add("§a" + potionEffectType.getName() + " §7Nível: §e" + amplifier);
                            lore.add("§7Duração: §e" + (duration / 20) + "s");

                            potionMeta.addCustomEffect(new PotionEffect(potionEffectType, duration, amplifier - 1),
                                    true);
                        } else {
                            Bukkit.getLogger().warning("Tipo de efeito inválido: " + effectType);
                        }
                    }

                    meta.lore(lore.stream().map(Component::text).toList());

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
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    }

                    item.setItemMeta(meta);
                }
                inventory.addItem(item);
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger()
                        .warning("Erro ao carregar item '" + key + "' na loja '" + shopName + "': " + e.getMessage());
            }
        });
    }

    public Inventory getInventory() {
        return inventory;
    }

    private String capitalize(String currency) {
        if (currency == null || currency.isEmpty())
            return "";
        return currency.substring(0, 1).toUpperCase() + currency.substring(1).toLowerCase();
    }

    private String getCurrencyColor(String currency) {
        return switch (currency.toLowerCase()) {
            case "solar" -> "§e"; // Amarelo para Solar
            default -> "§b"; // Azul para Lunar
        };
    }

    private String getCurrencySymbol(String currency) {
        return switch (currency.toLowerCase()) {
            case "solar" -> "§e☀ "; // Símbolo do Solar
            default -> "§b₪ "; // Símbolo do Lunar
        };
    }
}
