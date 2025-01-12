# LunarShop

LunarShop é um plugin avançado de lojas para servidores Minecraft. Ele oferece suporte a moedas customizadas (**lunar** e **solar**), promoções configuráveis, itens encantados, poções, e diversas opções para personalização de cada item.

---

## 📋 Funcionalidades

- **Sistema de Economia**:
  - Suporte às moedas **lunar** e **solar**.
  - Integração com o **[LunarEconomy](https://github.com/seu-repositorio/LunarEconomy)**.
- **Itens Personalizados**:
  - Suporte a **itens únicos**, **não únicos**, **poções**, e **itens encantados**.
  - Configuração de estoque e controle de venda.
  - Suporte completo para livros encantados e poções customizadas.
- **Promoções Configuráveis**:
  - Desconto em porcentagem.
  - Duração configurável em dias do Minecraft.
- **Sistema Completo de Lores**:
  - Personalize a descrição dos itens para deixar sua loja mais imersiva.

---

## 🚀 Instalação

### Pré-requisitos
- **Servidor Minecraft**:
  - Compatível com **Spigot**, **Paper** ou forks similares.
- **Java**: Versão 8 ou superior.
- **Dependências**:
  - **[LunarEconomy](https://github.com/seu-repositorio/LunarEconomy)** (API de economia para suporte às moedas **lunar** e **solar**).

### Passos
1. Baixe o **LunarShop** e **LunarEconomy**.
2. Coloque ambos os arquivos `.jar` na pasta `plugins` do seu servidor.
3. Reinicie o servidor ou use `/reload`.

---

## ⚙️ Configuração

Os arquivos de configuração das lojas são armazenados em `plugins/LunarShop/shops/`. Cada loja tem seu próprio arquivo **YAML**, onde você pode configurar itens, preços, encantamentos, promoções, poções e mais.

### 📂 Estrutura Geral

```yaml
items:
  <item_id>:
    name: "Nome do Item"
    material: "TIPO_DO_ITEM"
    lore:
      - "Linha 1 da descrição"
      - "Linha 2 da descrição"
    price:
      buy: <preço de compra>
      sell: <preço de venda>
    stock: <estoque disponível>
    unique: <true ou false>
    sellable: <true ou false>
    currency: "<lunar ou solar>"
    enchantments:
      <encantamento>: <nível>
    potion_effect:
      type: <efeito_da_poção>
      duration: <duração_em_ticks>
      amplifier: <nível>
    promotion:
      enabled: <true ou false>
      discount_percentage: <porcentagem>
      start_day: <dia de início no Minecraft>
      duration_days_minecraft: <duração em dias>
```

### 🔍 Explicação dos Campos

| Campo                     | Descrição                                                                                 |
|---------------------------|-------------------------------------------------------------------------------------------|
| **name**                  | Nome do item exibido na loja.                                                            |
| **material**              | Tipo do item (ex.: `STONE`, `DIAMOND_SWORD`, `ENCHANTED_BOOK`, `POTION`).                 |
| **lore**                  | Lista de descrições exibidas no item.                                                    |
| **price.buy**             | Preço para comprar o item.                                                               |
| **price.sell**            | Preço para vender o item.                                                                |
| **stock**                 | Quantidade disponível no estoque da loja.                                                |
| **unique**                | Se `true`, o item é único e só pode ser identificado pelo nome e lore configurados.       |
| **sellable**              | Define se o item pode ser vendido pelo jogador.                                           |
| **currency**              | Moeda usada para o item (`lunar` ou `solar`).                                            |
| **enchantments**          | Encantamentos aplicados ao item (ex.: `sharpness: 5`, `unbreaking: 3`).                   |
| **potion_effect.type**    | Tipo do efeito da poção (ex.: `SPEED`, `HEAL`, `STRENGTH`).                               |
| **potion_effect.duration**| Duração do efeito da poção em ticks (1 segundo = 20 ticks).                               |
| **potion_effect.amplifier** | Nível do efeito da poção (`0` para nível I, `1` para nível II, etc.).                    |
| **promotion.enabled**     | Define se o item está em promoção.                                                       |
| **promotion.discount_percentage** | Porcentagem de desconto aplicada no preço de compra.                              |
| **promotion.start_day**   | Dia do Minecraft em que a promoção começa.                                               |
| **promotion.duration_days_minecraft** | Duração da promoção em dias do Minecraft.                                    |

---

## 🛒 Exemplos de Configuração de Itens

### Item Simples
```yaml
items:
  iron_axe:
    material: IRON_AXE
    name: Machado de Ferro
    lore:
      - Um machado muito afiado
      - Use para lenhar com destreza!
    price:
      buy: 40
      sell: 25
    stock: 10
    unique: false
    sellable: true
    currency: solar
```

### Poção Personalizada
```yaml
items:
  speed_potion:
    material: POTION
    name: Poção de Velocidade
    lore:
      - §aAumenta sua velocidade por 30 segundos.
    price:
      buy: 20
      sell: 10
    stock: 50
    unique: false
    sellable: true
    currency: lunar
    potion_effect:
      type: SPEED
      duration: 600
      amplifier: 1
```

### Item Encantado
```yaml
items:
  enchanted_sword:
    material: DIAMOND_SWORD
    name: §bEspada dos Deuses
    lore:
      - §fUma espada lendária forjada pelos deuses.
      - §7Capaz de cortar até o destino.
      - §6[Encantamentos]
      - §7Afiação V
      - §7Inquebrável III
      - §7Aspecto Flamejante II
    price:
      buy: 1500
      sell: 750
    stock: 9
    unique: true
    sellable: true
    currency: lunar
    enchantments:
      sharpness: 5
      unbreaking: 3
      fire_aspect: 2
    promotion:
      enabled: true
      discount_percentage: 100
      duration_days_minecraft: 7
```

---

## ⚙️ Comandos

### Principais Comandos

- **Abrir Loja**:
  ```
  /lunarshop open <nome_da_loja>
  ```
  Exemplo: `/lunarshop open loja_principal`

- **Recarregar Configurações**:
  ```
  /lunarshop reload
  ```

---

## 🌈 Documentação Técnica

### Economia Integrada

O LunarShop utiliza a API de economia do **LunarEconomy** para gerenciar saldos e transações:

- `getBalance(playerUUID, "lunar" | "solar")`: Obtém o saldo do jogador.
- `removeBalance(playerUUID, "lunar" | "solar", amount)`: Deduz saldo do jogador.
- `addBalance(playerUUID, "lunar" | "solar", amount)`: Adiciona saldo ao jogador.

---

## 🌟 Créditos e Contribuições

- **Desenvolvedor Principal**: EthanNoward! 🎉  
- **API de Economia**: [LunarEconomy](https://github.com/seu-repositorio/LunarEconomy)  
- **Comunidade Minecraft**: Por sempre ajudar a construir sistemas mais incríveis!

---
