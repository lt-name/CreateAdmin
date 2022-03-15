package CreateAdmin.utils;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.item.Item;
import cn.nukkit.level.Position;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.utils.Config;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

/**
 * 玩家数据工具类
 *
 * @author LT_Name
 */
@SuppressWarnings("unused")
public class PlayerDataUtils {

    private PlayerDataUtils() {
        throw new RuntimeException("error");
    }

    public static PlayerData create(@NotNull Player player) {
        return new PlayerData(player);
    }

    public static PlayerData create(@NotNull Player player, @NotNull Plugin plugin) {
        return create(player, new File(plugin.getDataFolder() + "/PlayerStatusData/" + player.getName() + ".json"));
    }

    public static PlayerData create(@NotNull Player player, @NotNull File file) {
        return create(player, new Config(file, Config.JSON));
    }

    public static PlayerData create(@NotNull Player player, @NotNull Config config) {
        return new PlayerData(player, config);
    }

    /**
     * 背包内容转换为 适合保存的LinkedHashMap
     *
     * @param inventoryContents 背包内容
     * @return LinkedHashMap
     */
    public static LinkedHashMap<String, List<?>> inventoryToLinkedHashMap(@NotNull Map<Integer, Item> inventoryContents) {
        LinkedHashMap<String, List<?>> linkedHashMap = new LinkedHashMap<>();
        for (int i = 0; i < inventoryContents.size(); i++) {
            Item item = inventoryContents.get(i);

            LinkedList<String> list = new LinkedList<>();
            if (item != null) {
                list.add(item.getId() + ":" + item.getDamage());
                list.add(String.valueOf(item.getCount()));
                list.add(bytesToBase64(item.getCompoundTag()));
            }

            linkedHashMap.put(i + "", list);
        }
        return linkedHashMap;
    }

    /**
     * LinkedHashMap 转换为可以直接使用的背包内容
     *
     * @param inventory 物品Map
     * @return 背包内容
     */
    public static Map<Integer, Item> linkedHashMapToInventory(Map<String, List<?>> inventory) {
        if (inventory == null || inventory.isEmpty()) {
            return new HashMap<>();
        }
        LinkedHashMap<Integer, Item> map = new LinkedHashMap<>();
        for (Map.Entry<String, List<?>> entry : inventory.entrySet()) {
            List<String> list = null;
            try {
                list = (List<String>) entry.getValue();
            } catch (Exception ignored) {

            }
            if (list == null || list.isEmpty()) {
                continue;
            }
            Item item = Item.fromString(list.get(0));
            item.setCount(Integer.parseInt(list.get(1)));
            if (!"not".equals(String.valueOf(list.get(2)))) {
                item.setNamedTag(Item.parseCompoundTag(base64ToBytes(list.get(2))));
            }
            map.put(Integer.parseInt(entry.getKey()), item);
        }
        return map;
    }

    /**
     * 字节数组转base64
     *
     * @param src 字节数组
     * @return base64字符串
     */
    public static String bytesToBase64(byte[] src) {
        if (src == null || src.length <= 0) {
            return "not";
        }
        return Base64.getEncoder().encodeToString(src);
    }

    /**
     * base64转字节数组
     *
     * @param hexString base64
     * @return 字节数组
     */
    public static byte[] base64ToBytes(String hexString) {
        if (hexString == null || "".equals(hexString)) {
            return null;
        }
        return Base64.getDecoder().decode(hexString);
    }

    /**
     * Position转为保存用Map
     *
     * @param position 位置
     * @return Map
     */
    public static Map<String, Object> positionToMap(Position position) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();

        map.put("x", position.x);
        map.put("y", position.y);
        map.put("z", position.z);
        map.put("level", position.level.getFolderName());

        return map;
    }

    /**
     * 保存用Map转为Position
     *
     * @param map 保存用Map
     * @return Position
     */
    public static Position mapToPosition(Map<String, Object> map) {
        Position position = new Position(
                (Double) map.getOrDefault("x", 0.0D),
                (Double) map.getOrDefault("y", 0.0D),
                (Double) map.getOrDefault("z", 0.0D),
                Server.getInstance().getLevelByName((String) map.getOrDefault("level", "world")));
        return position.isValid() ? position : null;
    }

    public static class PlayerData {

        private final Player player;

        private Map<Integer, Item> inventoryContents = null;
        private Map<Integer, Item> offhandInventoryContents = null;
        private Map<Integer, Item> enderChestContents = null;

        private PlayerData(@NotNull Player player) {
            this.player = player;
        }

        private PlayerData(@NotNull Player player, @NotNull Config config) {
            this(player);

            if (config.exists("inventoryContents")) {
                this.inventoryContents = linkedHashMapToInventory(config.get("inventoryContents", new HashMap<>()));
            }
            if (config.exists("offhandInventoryContents")) {
                this.offhandInventoryContents = linkedHashMapToInventory(config.get("offhandInventoryContents", new HashMap<>()));
            }
            if (config.exists("enderChestContents")) {
                this.enderChestContents = linkedHashMapToInventory(config.get("enderChestContents", new HashMap<>()));
            }
        }

        /**
         * 保存所有数据
         *
         * @return PlayerData实例
         */
        public PlayerData saveAll() {
            this.saveInventory();
            this.saveEnderChestInventory();

            return this;
        }

        /**
         * 还原所有已保存的数据
         *
         * @return PlayerData实例
         */
        public PlayerData restoreAll() {
            this.restoreInventory();
            this.restoreEnderChestInventory();

            return this;
        }

        /**
         * 保存玩家背包内容
         *
         * @return PlayerData实例
         */
        public PlayerData saveInventory() {
            this.inventoryContents = this.player.getInventory().getContents();
            this.offhandInventoryContents = this.player.getOffhandInventory().getContents();

            return this;
        }

        /**
         * 还原玩家背包内容
         *
         * @return PlayerData实例
         */
        public PlayerData restoreInventory() {
            if (this.inventoryContents != null) {
                this.player.getInventory().setContents(this.inventoryContents);
            }
            if (this.offhandInventoryContents != null) {
                this.player.getOffhandInventory().setContents(this.offhandInventoryContents);
            }

            return this;
        }

        /**
         * 保存玩家末影箱内容
         *
         * @return PlayerData实例
         */
        public PlayerData saveEnderChestInventory() {
            this.enderChestContents = this.player.getEnderChestInventory().getContents();

            return this;
        }

        /**
         * 还原玩家末影箱内容
         *
         * @return PlayerData实例
         */
        public PlayerData restoreEnderChestInventory() {
            if (this.enderChestContents != null) {
                this.player.getEnderChestInventory().setContents(this.enderChestContents);
            }

            return this;
        }

        /**
         * 保存到文件
         *
         * @param plugin 插件
         * @return PlayerData实例
         */
        public PlayerData saveToFile(Plugin plugin) {
            return this.saveToFile(new File(plugin.getDataFolder() + "/PlayerStatusData/" + this.player.getName() + ".json"));
        }

        /**
         * 保存到文件
         *
         * @param file 文件
         * @return PlayerData实例
         */
        public PlayerData saveToFile(File file) {
            return this.saveToFile(new Config(file, Config.JSON));
        }

        /**
         * 保存到文件
         *
         * @param config 配置文件
         * @return PlayerData实例
         */
        public PlayerData saveToFile(Config config) {
            if (this.inventoryContents != null) {
                config.set("inventoryContents", inventoryToLinkedHashMap(this.inventoryContents));
            }
            if (this.offhandInventoryContents != null) {
                config.set("offhandInventoryContents", inventoryToLinkedHashMap(this.offhandInventoryContents));
            }
            if (this.enderChestContents != null) {
                config.set("enderChestContents", inventoryToLinkedHashMap(this.enderChestContents));
            }

            config.save();

            return this;
        }

    }

}
