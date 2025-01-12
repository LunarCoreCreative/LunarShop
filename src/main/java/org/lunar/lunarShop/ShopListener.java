package org.lunar.lunarShop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class ShopListener implements Listener {

    private final LunarShop plugin;

    public ShopListener(LunarShop plugin) {
        this.plugin = plugin;
    }

    private String getCurrencyColor(String currency) {
        switch (currency.toLowerCase()) {
            case "solar":
                return "§e"; // Amarelo para Solar
            case "lunar":
            default:
                return "§b"; // Azul para Lunar
        }
    }

    private String getCurrencySymbol(String currency) {
        switch (currency.toLowerCase()) {
            case "solar":
                return "§e☀ "; // Símbolo do Solar
            case "lunar":
            default:
                return "§b₪ "; // Símbolo do Lunar
        }
    }

    private String capitalize(String currency) {
        if (currency == null || currency.isEmpty())
            return "";
        return currency.substring(0, 1).toUpperCase() + currency.substring(1).toLowerCase();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Component titleComponent = event.getView().title();
        String inventoryTitle = PlainTextComponentSerializer.plainText().serialize(titleComponent);

        if (inventoryTitle.startsWith("Loja: ")) {
            event.setCancelled(true); // Previne que o jogador pegue itens da GUI

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR)
                return;

            Player player = (Player) event.getWhoClicked();
            String shopName = inventoryTitle.replace("Loja: ", "");

            // Carrega a configuração da loja
            FileConfiguration shopConfig = plugin.getShopManager().getShop(shopName);
            if (shopConfig == null || shopConfig.getConfigurationSection("items") == null) {
                player.sendMessage("§cErro: Loja não encontrada ou inválida.");
                return;
            }

            Optional.ofNullable(shopConfig.getConfigurationSection("items"))
                    .ifPresent(itemsSection -> {
                        for (String key : itemsSection.getKeys(false)) {
                            String materialName = Optional
                                    .ofNullable(shopConfig.getString("items." + key + ".material"))
                                    .orElse("STONE");
                            Material material = Material.matchMaterial(materialName.toUpperCase());
                            if (material == null) {
                                plugin.getLogger().warning("Material inválido encontrado na loja: " + materialName);
                                continue;
                            }

                            String name = shopConfig.getString("items." + key + ".name", "");
                            List<String> loreConfig = shopConfig.getStringList("items." + key + ".lore");
                            int buyPrice = shopConfig.getInt("items." + key + ".price.buy", -1);
                            int sellPrice = shopConfig.getInt("items." + key + ".price.sell", -1);
                            int stock = shopConfig.getInt("items." + key + ".stock", -1);
                            boolean isUnique = shopConfig.getBoolean("items." + key + ".unique", false);
                            boolean sellable = shopConfig.getBoolean("items." + key + ".sellable", true);

                            // Verifica os encantamentos configurados
                            ConfigurationSection enchantSection = shopConfig
                                    .getConfigurationSection("items." + key + ".enchantments");
                            Map<Enchantment, Integer> expectedEnchants = new HashMap<>();
                            if (enchantSection != null) {
                                for (String enchantKey : enchantSection.getKeys(false)) {
                                    try {
                                        NamespacedKey keyNamespace = NamespacedKey.minecraft(enchantKey.toLowerCase());
                                        Enchantment enchantment = Registry.ENCHANTMENT.get(keyNamespace);
                                        if (enchantment != null) {
                                            int level = enchantSection.getInt(enchantKey, 1);
                                            expectedEnchants.put(enchantment, level);
                                        } else {
                                            plugin.getLogger().warning("Encantamento inválido: " + enchantKey);
                                        }
                                    } catch (IllegalArgumentException e) {
                                        plugin.getLogger().warning("Chave inválida para encantamento: " + enchantKey);
                                    }
                                }
                            }

                            // Verifica se o item corresponde ao clicado
                            if (clickedItem.getType() == material) {
                                boolean isShiftClick = event.isShiftClick(); // Detecta Shift
                                if (event.isLeftClick() && buyPrice > 0) {
                                    processBuy(player, shopConfig, key, buyPrice, stock, name, loreConfig, isUnique,
                                            expectedEnchants, shopName, event, isShiftClick);
                                } else if (event.isRightClick() && sellable && sellPrice > 0) {
                                    processSell(player, shopConfig, key, sellPrice, stock, name, loreConfig, isUnique,
                                            expectedEnchants, shopName, event, isShiftClick);
                                } else if (event.isRightClick() && !sellable) {
                                    player.sendMessage("§cEste item não pode ser vendido!");
                                }
                                return; // Interrompe o loop após processar o item clicado
                            }
                        }
                    });
        }
    }

    private void processBuy(Player player, FileConfiguration shopConfig, String key, double price, int stock,
            String itemName, List<String> loreConfig, boolean isUnique, Map<Enchantment, Integer> expectedEnchants,
            String shopName, InventoryClickEvent event, boolean isShiftClick) {

        // Determina a quantidade a ser comprada (1 ou até 64 dependendo do Shift)
        int quantityToBuy = isShiftClick ? Math.min(stock, 64) : 1;
        double finalPrice = getPromotionalPrice(shopConfig, key, price) * quantityToBuy; // Aplica o desconto, se houver
                                                                                         // promoção

        if (stock == 0) {
            player.sendMessage("§cEste item está fora de estoque!");
            return;
        }

        // Obtém a moeda diretamente da configuração do item
        String currency = shopConfig.getString("items." + key + ".currency", "lunar").toLowerCase(); // Default: lunar

        // Verifica se a moeda configurada é válida
        if (!currency.equals("lunar") && !currency.equals("solar")) {
            player.sendMessage("§cErro: Moeda inválida configurada para este item.");
            return;
        }

        // Obtém o saldo do jogador com base na moeda do item
        double balance = plugin.getShopManager().getLunarEconomyAPI().getBalance(player.getUniqueId(), currency);

        // Verifica se o jogador tem saldo suficiente para a compra
        if (balance < finalPrice) {
            player.sendMessage(
                    String.format("§cVocê não tem saldo suficiente para comprar este item! Faltam %s%.2f %s§c.",
                            getCurrencySymbol(currency), finalPrice - balance,
                            getCurrencyColor(currency) + capitalize(currency)));
            return;
        }

        // Deduz o saldo do jogador
        plugin.getShopManager().getLunarEconomyAPI().removeBalance(player.getUniqueId(), currency, finalPrice);

        // Cria o item a ser entregue
        Material material = Material.matchMaterial(shopConfig.getString("items." + key + ".material").toUpperCase());
        if (material == null) {
            player.sendMessage("§cErro ao comprar: material inválido.");
            return;
        }

        ItemStack itemToGive = new ItemStack(material, quantityToBuy);
        if (material == Material.ENCHANTED_BOOK) {
            // Aplica encantamentos ao livro encantado
            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) itemToGive.getItemMeta();
            if (meta != null) {
                for (Map.Entry<Enchantment, Integer> entry : expectedEnchants.entrySet()) {
                    meta.addStoredEnchant(entry.getKey(), entry.getValue(), true);
                }
                meta.displayName(Component.text("§a" + itemName));
                meta.lore(loreConfig.stream()
                        .filter(line -> !line.startsWith("§7Estoque:")) // Remove a linha de estoque
                        .map(Component::text)
                        .toList());
                itemToGive.setItemMeta(meta);
            }
        } else {
            // Aplica encantamentos a itens que não sejam livros
            ItemMeta meta = itemToGive.getItemMeta();
            if (meta != null) {
                if (isUnique) {
                    meta.displayName(Component.text("§a" + itemName));
                    meta.lore(loreConfig.stream()
                            .filter(line -> !line.startsWith("§7Estoque:")) // Remove a linha de estoque
                            .map(Component::text)
                            .toList());
                }
                for (Map.Entry<Enchantment, Integer> entry : expectedEnchants.entrySet()) {
                    meta.addEnchant(entry.getKey(), entry.getValue(), true);
                }
                itemToGive.setItemMeta(meta);
            }
        }

        player.getInventory().addItem(itemToGive);

        // Atualiza o estoque
        shopConfig.set("items." + key + ".stock", stock - quantityToBuy);
        plugin.getShopManager().saveShopConfig(shopConfig, shopName);

        // Atualiza a loja
        updateShopInventory(event.getInventory(), shopConfig);

        player.sendMessage(String.format("§aVocê comprou %dx %s por %s%.2f %s!",
                quantityToBuy, material.name(), getCurrencySymbol(currency), finalPrice,
                getCurrencyColor(currency) + capitalize(currency)));
    }

    private double getPromotionalPrice(FileConfiguration shopConfig, String key, double originalPrice) {
        // Obtém a seção de promoção do item
        ConfigurationSection promotionSection = shopConfig.getConfigurationSection("items." + key + ".promotion");

        if (promotionSection == null || !promotionSection.getBoolean("enabled", false)) {
            return originalPrice; // Sem promoção
        }

        int durationDays = promotionSection.getInt("duration_days_minecraft", 0);
        int startDay = promotionSection.getInt("start_day", -1);

        // Obtém o dia atual do Minecraft
        int currentDay = (int) (plugin.getServer().getWorld("world").getFullTime() / 24000);

        // Verifica se a promoção expirou
        if (startDay != -1 && currentDay > startDay + durationDays) {
            // Marca a promoção como desativada
            promotionSection.set("enabled", false);
            plugin.getShopManager().saveShopConfig(shopConfig, shopConfig.getString("name"));
            return originalPrice; // Retorna o preço original após o término da promoção
        }

        // Aplica o desconto configurado
        double discountPercentage = promotionSection.getDouble("discount_percentage", 0);
        return originalPrice - (originalPrice * discountPercentage / 100);
    }

    private void processSell(Player player, FileConfiguration shopConfig, String key, double sellPrice, int stock,
            String expectedName, List<String> expectedLore, boolean isUnique,
            Map<Enchantment, Integer> expectedEnchants, String shopName, InventoryClickEvent event,
            boolean isShiftClick) {

        boolean itemSold = false;

        // Obtém a moeda usada no item (lunar ou solar)
        String currency = shopConfig.getString("items." + key + ".currency", "lunar"); // Default: lunar

        // Itera por todo o inventário do jogador
        for (ItemStack itemStack : player.getInventory().getContents()) {
            if (itemStack == null || itemStack.getType() != Material.matchMaterial(
                    Optional.ofNullable(shopConfig.getString("items." + key + ".material"))
                            .map(String::toUpperCase)
                            .orElse("STONE"))) {
                continue; // Pula itens que não correspondem ao material
            }

            int quantityToSell = isShiftClick ? Math.min(itemStack.getAmount(), 64) : 1;
            double totalSellPrice = sellPrice * quantityToSell;

            // Valida itens únicos
            if (isUnique) {
                ItemMeta meta = itemStack.getItemMeta();
                if (meta != null) {
                    String itemName = meta.hasDisplayName()
                            ? Optional.ofNullable(meta.displayName())
                                    .map(display -> PlainTextComponentSerializer.plainText().serialize(display))
                                    .orElse("")
                            : "";
                    if (!itemName.equals("§a" + expectedName)) {
                        continue; // Pula itens cujo nome não corresponde
                    }

                    List<String> handItemLore = Optional.ofNullable(meta.lore())
                            .map(lore -> lore.stream()
                                    .map(line -> PlainTextComponentSerializer.plainText().serialize(line))
                                    .filter(line -> !line.startsWith("§7Estoque:")) // Ignora a linha de estoque
                                    .collect(Collectors.toList()))
                            .orElse(new ArrayList<>());

                    if (!handItemLore.equals(expectedLore)) {
                        continue; // Pula itens cuja lore não corresponde
                    }
                }
            }

            // Valida os encantamentos
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                for (Map.Entry<Enchantment, Integer> entry : expectedEnchants.entrySet()) {
                    if (!meta.hasEnchant(entry.getKey()) || meta.getEnchantLevel(entry.getKey()) != entry.getValue()) {
                        player.sendMessage("§cOs encantamentos do item não correspondem ao esperado!");
                        return;
                    }
                }
            }

            // Incrementa o estoque da loja
            shopConfig.set("items." + key + ".stock", stock + quantityToSell);
            plugin.getShopManager().saveShopConfig(shopConfig, shopName);

            // Atualiza a loja
            updateShopInventory(event.getInventory(), shopConfig);

            // Adiciona saldo na moeda configurada e remove o item do inventário
            plugin.getShopManager().getLunarEconomyAPI().addBalance(player.getUniqueId(), currency, totalSellPrice);
            itemStack.setAmount(itemStack.getAmount() - quantityToSell);

            player.sendMessage(String.format("§aVocê vendeu %dx %s por %s§b%.2f %s!", quantityToSell,
                    itemStack.getType(), getCurrencySymbol(currency), totalSellPrice,
                    getCurrencyColor(currency) + capitalize(currency)));
            itemSold = true;
            break; // Vendeu um item, não precisamos continuar
        }

        if (!itemSold) {
            player.sendMessage("§cVocê não possui o item necessário para vender!");
        }
    }

    private void updateShopInventory(Inventory inventory, FileConfiguration shopConfig) {
        inventory.clear();
        Optional.ofNullable(shopConfig.getConfigurationSection("items"))
                .ifPresent(itemsSection -> itemsSection.getKeys(false).forEach(key -> {
                    String materialName = Optional.ofNullable(shopConfig.getString("items." + key + ".material"))
                            .orElse("STONE");
                    Material material = Material.matchMaterial(materialName.toUpperCase());
                    if (material == null) {
                        plugin.getLogger().warning("Material inválido encontrado na loja: " + materialName);
                        return;
                    }

                    String itemName = shopConfig.getString("items." + key + ".name", "Item");
                    double buyPrice = shopConfig.getDouble("items." + key + ".price.buy", -1);
                    double sellPrice = shopConfig.getDouble("items." + key + ".price.sell", -1);
                    int stock = shopConfig.getInt("items." + key + ".stock", -1);
                    String currency = shopConfig.getString("items." + key + ".currency", "lunar"); // Default: lunar

                    // Verifica se o item está em promoção
                    boolean isOnSale = shopConfig.getBoolean("items." + key + ".sale.active", false);
                    double saleDiscount = shopConfig.getDouble("items." + key + ".sale.discount", 0);
                    long saleEnd = shopConfig.getLong("items." + key + ".sale.end", 0);

                    // Calcula o preço com desconto, se a promoção estiver ativa
                    double discountedPrice = buyPrice;
                    if (isOnSale && saleDiscount > 0) {
                        discountedPrice = buyPrice - (buyPrice * (saleDiscount / 100.0));

                        // Verifica se a promoção ainda é válida
                        long currentTime = plugin.getServer().getWorld("world").getFullTime();
                        if (currentTime > saleEnd) {
                            isOnSale = false;
                            shopConfig.set("items." + key + ".sale.active", false);
                            plugin.getShopManager().saveShopConfig(shopConfig, "items");
                        }
                    }

                    List<String> loreConfig = new ArrayList<>(shopConfig.getStringList("items." + key + ".lore"));

                    ItemStack item = new ItemStack(material);
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.displayName(Component.text("§a" + itemName));

                        // Cria a lore dinâmica para a interface da loja
                        List<String> lore = new ArrayList<>(loreConfig);

                        // Adiciona informações de promoção
                        if (isOnSale && saleDiscount > 0) {
                            lore.add("§6Promoção Ativa!");
                            lore.add(String.format("§7Preço original: %s§b%.2f %s",
                                    getCurrencySymbol(currency), buyPrice,
                                    getCurrencyColor(currency) + capitalize(currency)));
                            lore.add(String.format("§7Preço com desconto: %s§b%.2f %s (-%.1f%%)",
                                    getCurrencySymbol(currency), discountedPrice,
                                    getCurrencyColor(currency) + capitalize(currency), saleDiscount));

                            long remainingDays = (saleEnd - plugin.getServer().getWorld("world").getFullTime()) / 24000;
                            if (remainingDays > 0) {
                                lore.add(String.format("§7Duração restante: §e%d dias no Minecraft", remainingDays));
                            } else {
                                lore.add("§cPromoção expirada!");
                            }
                        } else if (buyPrice > 0) {
                            lore.add(String.format("§7Preço de compra: %s§b%.2f %s",
                                    getCurrencySymbol(currency), buyPrice,
                                    getCurrencyColor(currency) + capitalize(currency)));
                        }

                        if (sellPrice > 0)
                            lore.add(String.format("§7Preço de venda: %s§b%.2f %s",
                                    getCurrencySymbol(currency), sellPrice,
                                    getCurrencyColor(currency) + capitalize(currency)));

                        if (stock >= 0)
                            lore.add("§7Estoque: §e" + stock);

                        // Adiciona instruções da loja
                        lore.add("");
                        lore.add("§6Clique esquerdo§7: Comprar 1 unidade");
                        lore.add("§6Shift + Clique esquerdo§7: Comprar 64 unidades");
                        lore.add("§6Clique direito§7: Vender 1 unidade");
                        lore.add("§6Shift + Clique direito§7: Vender 64 unidades");

                        meta.lore(lore.stream().map(Component::text).toList());
                        item.setItemMeta(meta);
                    }
                    inventory.addItem(item);
                }));
    }

}
