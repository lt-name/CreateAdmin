package CreateAdmin;

import CreateAdmin.utils.PlayerDataUtils;
import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.event.player.*;
import cn.nukkit.item.Item;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;

import java.io.File;
import java.util.ArrayList;

/**
 * @author 若水
 */
public class CreateAdmin extends PluginBase implements Listener {

    private Config blocks;
    private Config config;

    private ArrayList<String> banUseItems;
    private ArrayList<String> banInteractBlocks;
    private ArrayList<String> banInteractEntity;
    private final ArrayList<Player> playerOnSet = new ArrayList<>();

    public void onEnable() {
        saveDefaultConfig();

        this.config = new Config(this.getDataFolder() + "/config.yml", Config.YAML);
        this.blocks = new Config(this.getDataFolder() + "/Blocks.yml", Config.YAML);

        this.banUseItems = (ArrayList<String>) this.config.getStringList("banUseItems");
        this.banInteractBlocks = (ArrayList<String>) this.config.getStringList("banInteractBlocks");
        this.banInteractEntity = (ArrayList<String>) this.config.getStringList("banInteractEntity");

        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info(" 加载成功...");
    }

    @Override
    public void onDisable() {
        this.saveBanUseItems();
        this.saveBanInteractBlocks();
        this.saveBanInteractEntity();
        getLogger().info("已卸载");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ("创造管理".equals(command.getName()) || "CreateAdmin".equalsIgnoreCase(command.getName())) {
            if (sender instanceof Player) {
                Player player = ((Player) sender).getPlayer();
                if (player.isOp()) {
                    if (args.length > 0) {
                        switch (args[0]) {
                            case "addItem": case "禁用物品":
                                Item item = player.getInventory().getItemInHand();
                                if (item.getId() == 0) {
                                    sender.sendMessage(TextFormat.RED + "请手持要禁用的物品");
                                }else {
                                    if (this.banUseItems.contains(this.getStringID(item))) {
                                        this.banUseItems.remove(this.getStringID(item));
                                        sender.sendMessage(TextFormat.GREEN + "CreateAdmin：已解禁当前手持物品");
                                    }else {
                                        this.banUseItems.add(this.getStringID(item));
                                        sender.sendMessage(TextFormat.GREEN + "CreateAdmin：已禁用当前手持物品");
                                    }
                                    saveBanUseItems();
                                }
                                break;
                            case "addBlock": case "禁止交互":
                                this.playerOnSet.add(player);
                                sender.sendMessage(TextFormat.GREEN + "请点击要设置创造模式禁止交互的物品");
                                break;
                            default:
                                sender.sendMessage(TextFormat.YELLOW + "设置创造模式禁止使用的物品：");
                                sender.sendMessage(TextFormat.YELLOW + "/创造管理 禁用物品");
                                sender.sendMessage(TextFormat.YELLOW + "设置创造模式禁止交互的物品：");
                                sender.sendMessage(TextFormat.YELLOW + "/创造管理 禁止交互");
                                break;
                        }
                    }else {
                        sender.sendMessage(TextFormat.GREEN + "查看帮助：/创造管理 help");
                    }
                }else {
                    sender.sendMessage(TextFormat.RED + "此命令仅OP可用！");
                }
            }else {
                sender.sendMessage(TextFormat.RED + "请在游戏内执行命令！");
            }
            return true;
        }
        return false;
    }

    public String getStringID(Item item) {
        return item.getId() + ":" + item.getDamage();
    }

    public String getStringID(Block block) {
        return block.getId() + "";
    }

    public String getStringID(Entity entity) {
        return entity.getNetworkId() + "";
    }

    public void saveBanUseItems() {
        this.config.set("banUseItems", this.banUseItems);
        this.config.save();
    }

    public void saveBanInteractBlocks() {
        this.config.set("banInteractBlocks", this.banInteractBlocks);
        this.config.save();
    }

    public void saveBanInteractEntity() {
        this.config.set("banInteractEntity", this.banInteractEntity);
        this.config.save();
    }

    private boolean changeBlock(Block block, boolean add) {
        String s = block.getX() + ":" + block.getY() + ":" + block.getZ() + ":" + block.getLevel().getFolderName();
        ArrayList<String> list = new ArrayList<>(this.blocks.getStringList("blocks"));
        if (list.contains(s)) {
            list.remove(s);
            this.blocks.set("blocks", list);
            this.blocks.save();
            return true;
        }
        if (add) {
            list.add(s);
            this.blocks.set("blocks", list);
            this.blocks.save();
        }
        return false;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!event.isCancelled()) {
            Player player = event.getPlayer();
            Block block = event.getBlock();
            if (player != null && block != null && player.getGamemode() == 1) {
                changeBlock(block, true);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!event.isCancelled()) {
            Player player = event.getPlayer();
            Block block = event.getBlock();
            if (player != null && block != null && player.getGamemode() == 0 && changeBlock(block, false)) {
                event.setDrops(new Item[]{Item.get(0, 0)});
            }
        }
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        if (player == null || player.isOp()) {
            return;
        }
        if (event.getNewGamemode() == Player.CREATIVE) {
            PlayerDataUtils.create(player, this).saveAll().saveToFile(this);
            player.getInventory().clearAll();
            player.getUIInventory().clearAll();
            player.getEnderChestInventory().clearAll();
            player.sendMessage(TextFormat.GREEN + ">> 已切换" + TextFormat.YELLOW + "创造" + TextFormat.GREEN + "模式 生存背包已保存");
        } else if (event.getNewGamemode() == Player.SURVIVAL || event.getNewGamemode() == Player.ADVENTURE) {
            player.getInventory().clearAll();
            player.getUIInventory().clearAll();
            player.getEnderChestInventory().clearAll();
            if (new File(this.getDataFolder() + "/PlayerStatusData/" + player.getName() + ".json").exists()) {
                PlayerDataUtils.create(player, this).restoreAll();
            }
            player.sendMessage(TextFormat.GREEN + ">> 已切换" + TextFormat.YELLOW + (event.getNewGamemode() == Player.SURVIVAL ? "生存" : "冒险") + TextFormat.GREEN + "模式 生存背包已回归");
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!event.isCancelled()) {
            Player player = event.getPlayer();
            Block block = event.getBlock();
            if (player != null && block != null) {
                if (player.isOp()) {
                    if (this.playerOnSet.contains(player)) {
                        event.setCancelled();
                        this.playerOnSet.remove(player);
                        if (this.banInteractBlocks.contains(this.getStringID(block))) {
                            this.banInteractBlocks.remove(this.getStringID(block));
                            player.sendMessage(TextFormat.GREEN + "物品ID：" + block.getId() + "移除成功");
                        }else {
                            this.banInteractBlocks.add(this.getStringID(block));
                            player.sendMessage(TextFormat.GREEN + "物品ID：" + block.getId() + "添加成功");
                        }
                        this.saveBanInteractBlocks();
                    }
                }else if (player.getGamemode() == 1 && this.banInteractBlocks.contains(this.getStringID(block))) {
                    event.setCancelled();
                    player.sendMessage(TextFormat.RED + "无法在创造模式下和此物品交互！");
                }
            }
        }
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        if (!event.isCancelled()) {
            Player player = event.getPlayer();
            if (player == null || player.isOp() || player.getGamemode() != 1) {
                return;
            }
            event.setCancelled();
            player.sendMessage(TextFormat.RED + "创造模式无法丢出物品！");
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        if (!event.isCancelled()) {
            Player player = event.getPlayer();
            Item item = event.getItem();
            if (player == null || player.isOp()) {
                return;
            }
            if (item != null && this.banUseItems.contains(this.getStringID(item))) {
                event.setCancelled();
                player.sendMessage(TextFormat.RED + "无法在创造模式下使用此物品！");
            }
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!event.isCancelled()) {
            Player player = event.getPlayer();
            Entity entity = event.getEntity();
            if (player != null && entity != null) {
                if (player.isOp()) {
                    if (this.playerOnSet.contains(player)) {
                        event.setCancelled();
                        this.playerOnSet.remove(player);
                        if (this.banInteractEntity.contains(this.getStringID(entity))) {
                            this.banInteractEntity.remove(this.getStringID(entity));
                            player.sendMessage(TextFormat.GREEN + "实体：" + entity.getNetworkId() + "移除成功");
                        }else {
                            this.banInteractEntity.add(this.getStringID(entity));
                            player.sendMessage(TextFormat.GREEN + "实体：" + entity.getNetworkId() + "添加成功");
                        }
                        this.saveBanInteractEntity();
                    }
                }else if (player.getGamemode() == 1 && this.banInteractEntity.contains(this.getStringID(entity))) {
                    event.setCancelled();
                    player.sendMessage(TextFormat.RED + "无法在创造模式下和此实体交互！");
                }
            }
        }
    }

}
