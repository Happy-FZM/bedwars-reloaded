package io.github.yannici.bedwars.Shop;

import io.github.yannici.bedwars.ChatWriter;
import io.github.yannici.bedwars.Main;
import io.github.yannici.bedwars.Utils;
import io.github.yannici.bedwars.Game.Game;
import io.github.yannici.bedwars.Villager.MerchantCategory;
import io.github.yannici.bedwars.Villager.VillagerTrade;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

public class NewItemShop {

	private List<MerchantCategory> categories = null;
	private MerchantCategory currentCategory = null;

	public NewItemShop(List<MerchantCategory> categories) {
		this.categories = categories;
	}

	public List<MerchantCategory> getCategories() {
		return this.categories;
	}

	public boolean hasOpenCategory() {
		return (this.currentCategory != null);
	}

	public boolean hasOpenCategory(MerchantCategory category) {
		if (this.currentCategory == null) {
			return false;
		}

		return (this.currentCategory.equals(category));
	}

	public void openCategoryInventory(Player player) {
		int size = (this.categories.size() - this.categories.size() % 9)
				+ (9 * 2);
		Inventory inventory = Bukkit.createInventory(player, size,
				Main._l("ingame.shop.name"));

		this.addCategoriesToInventory(inventory);

		ItemStack slime = new ItemStack(Material.SLIME_BALL, 1);
		ItemMeta slimeMeta = slime.getItemMeta();
		
		slimeMeta.setDisplayName(Main._l("ingame.shop.oldshop"));
		slimeMeta.setLore(new ArrayList<String>());
		slime.setItemMeta(slimeMeta);
		
		Game game = Game.getGameOfPlayer(player);
		ItemStack stack = null;
		
		if(game != null) {
		    if(game.getPlayerSettings(player).oneStackPerShift()) {
		        stack = new ItemStack(Material.BUCKET, 1);
		        ItemMeta meta = stack.getItemMeta();
		        
		        meta.setDisplayName(ChatColor.AQUA + Main._l("default.currently") + ": " + ChatColor.WHITE + Main._l("ingame.shop.onestackpershift"));
		        meta.setLore(new ArrayList<String>());
		        stack.setItemMeta(meta);
		    } else {
		        stack = new ItemStack(Material.LAVA_BUCKET, 1);
                ItemMeta meta = stack.getItemMeta();
                
                meta.setDisplayName(ChatColor.AQUA + Main._l("default.currently") + ": " + ChatColor.WHITE + Main._l("ingame.shop.fullstackpershift"));
                meta.setLore(new ArrayList<String>());
                stack.setItemMeta(meta);
		    }
		    
		    if(stack != null) {
		        inventory.setItem(size - 4, stack);
		    }
		}
		
		if(stack == null) {
		    inventory.setItem(size - 5, slime);
		} else {
		    inventory.setItem(size - 6, slime);
		}

		player.openInventory(inventory);
	}

	private void addCategoriesToInventory(Inventory inventory) {
		for (MerchantCategory category : this.categories) {
			ItemStack is = new ItemStack(category.getMaterial(), 1);
			ItemMeta im = is.getItemMeta();

			if (this.currentCategory != null) {
				if (this.currentCategory.equals(category)) {
					im.addEnchant(Enchantment.DAMAGE_ALL, 1, true);
				}
			}

			im.setDisplayName(category.getName());
			im.setLore(category.getLores());
			is.setItemMeta(im);

			inventory.addItem(is);
		}
	}

	public void handleInventoryClick(InventoryClickEvent ice, Game game,
			Player player) {
		if (!this.hasOpenCategory()) {
			if (ice.getCurrentItem().getType() == Material.SLIME_BALL) {
				this.changeToOldShop(game, player);
				return;
			}
			
			if(ice.getCurrentItem().getType() == Material.BUCKET) {
			    game.getPlayerSettings(player).setOneStackPerShift(false);
			    player.playSound(player.getLocation(), Sound.CLICK, 10.0F, 1.0F);
			    this.openCategoryInventory(player);
			    return;
			} else if(ice.getCurrentItem().getType() == Material.LAVA_BUCKET) {
			    game.getPlayerSettings(player).setOneStackPerShift(true);
			    player.playSound(player.getLocation(), Sound.CLICK, 10.0F, 1.0F);
			    this.openCategoryInventory(player);
			    return;
			}

			this.handleCategoryInventoryClick(ice, game, player);
		} else {
			this.handleBuyInventoryClick(ice, game, player);
		}
	}
	
	private void changeToOldShop(Game game, Player player) {
		game.useOldShop(player);

		player.playSound(player.getLocation(), Sound.CLICK, 10.0F, 1.0F);
		
		// open old shop
		MerchantCategory.openCategorySelection(player, game);
	}

	private void handleCategoryInventoryClick(InventoryClickEvent ice,
			Game game, Player player) {
	    
	    int sizeCategories = (this.categories.size() - this.categories.size() % 9) + 9;
	    if(ice.getRawSlot() >= sizeCategories) {
	        ice.setCancelled(false);
	        return;
	    }
	    
		MerchantCategory clickedCategory = this.getCategoryByMaterial(ice
				.getCurrentItem().getType());
		if (clickedCategory == null) {
		    ice.setCancelled(false);
			return;
		}

		this.openBuyInventory(clickedCategory, player, game);
	}

	private void openBuyInventory(MerchantCategory category, Player player,
			Game game) {
		List<VillagerTrade> offers = category.getOffers();
		int sizeCategories = (this.categories.size() - this.categories.size() % 9) + 9;
		int sizeItems = (offers.size() - offers.size() % 9) + 9;
		int totalSize = sizeCategories + sizeItems;
		
		player.playSound(player.getLocation(), Sound.CLICK, 10.0F, 1.0F);
		
		this.currentCategory = category;
		Inventory buyInventory = Bukkit.createInventory(player, totalSize,
				Main._l("ingame.shop.name"));
		this.addCategoriesToInventory(buyInventory);

		for (int i = 0; i < offers.size(); i++) {
			VillagerTrade trade = offers.get(i);
			int slot = sizeCategories + i;
			ItemStack tradeStack = this.toItemStack(trade, player, game);

			buyInventory.setItem(slot, tradeStack);
		}

		player.openInventory(buyInventory);
	}

	private ItemStack toItemStack(VillagerTrade trade, Player player, Game game) {
		ItemStack tradeStack = trade.getRewardItem().clone();
		Method colorable = Utils.getColorableMethod(tradeStack.getType());
		ItemMeta meta = tradeStack.getItemMeta();
		ItemStack item1 = trade.getItem1();
		ItemStack item2 = trade.getItem2();

		if (colorable != null) {
			colorable.setAccessible(true);
			try {
				colorable.invoke(meta,
						new Object[] { Game.getPlayerTeam(player, game)
								.getColor().getColor() });
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		List<String> lores = meta.getLore();
		if (lores == null) {
			lores = new ArrayList<String>();
		}

		lores.add(ChatColor.WHITE + String.valueOf(item1.getAmount()) + " "
				+ item1.getItemMeta().getDisplayName());
		if (item2 != null) {
			lores.add(ChatColor.WHITE + String.valueOf(item2.getAmount()) + " "
					+ item2.getItemMeta().getDisplayName());
		}

		meta.setLore(lores);

		tradeStack.setItemMeta(meta);
		return tradeStack;
	}

	private void handleBuyInventoryClick(InventoryClickEvent ice, Game game,
			Player player) {
		int sizeCategories = (this.categories.size() - this.categories.size() % 9) + 9;
		List<VillagerTrade> offers = this.currentCategory.getOffers();
		int sizeItems = (offers.size() - offers.size() % 9) + 9;
		int totalSize = sizeCategories + sizeItems;
		ItemStack item = ice.getCurrentItem();
		boolean cancel = false;
		int bought = 0;
		boolean oneStackPerShift = game.getPlayerSettings(player).oneStackPerShift();

		if (this.currentCategory == null) {
			player.closeInventory();
			return;
		}

		if (ice.getRawSlot() < sizeCategories) {
			// is category click
			ice.setCancelled(true);
			
			if (item.getType().equals(this.currentCategory.getMaterial())) {
				// back to default category view
				this.currentCategory = null;
				this.openCategoryInventory(player);
			} else {
				// open the clicked buy inventory
				this.handleCategoryInventoryClick(ice, game, player);
			}
		} else if(ice.getRawSlot() < totalSize) {
			// its a buying item
			ice.setCancelled(true);
			
			MerchantCategory category = this.currentCategory;
			VillagerTrade trade = this.getTradingItem(category,
					ice.getCurrentItem(), game, player);

			if (trade == null) {
				player.closeInventory();
				return;
			}
			
			player.playSound(player.getLocation(), Sound.ITEM_PICKUP, 10.0F, 1.0F);

			// enough ressources?
			if (!this.hasEnoughRessource(player, trade)) {
				player.sendMessage(ChatWriter.pluginMessage(ChatColor.RED
						+ Main._l("errors.notenoughress")));
				return;
			}

			if (ice.isShiftClick()) {
				while (this.hasEnoughRessource(player, trade) && !cancel) {
					cancel = !this.buyItem(item, trade, player);
					if(!cancel && oneStackPerShift) {
				        bought = bought+item.getAmount();
				        cancel = ((bought + item.getAmount()) > 64);
					}
				}
				
				bought = 0;
			} else {
				this.buyItem(item, trade, player);
			}
		} else {
			if(ice.isShiftClick()) {
				ice.setCancelled(true);
			} else {
				ice.setCancelled(false);
			}
			
			return;
		}
	}

	@SuppressWarnings("unchecked")
	private boolean buyItem(ItemStack item, VillagerTrade trade, Player player) {
		PlayerInventory inventory = player.getInventory();
		boolean success = true;

		int item1ToPay = trade.getItem1().getAmount();
		Iterator<?> stackIterator = inventory.all(trade.getItem1().getType())
				.entrySet().iterator();
		
		int firstItem1 = inventory.first(trade.getItem1());
		if(firstItem1 > -1) {
			inventory.clear(firstItem1);
		} else {
			// pay
			while (stackIterator.hasNext()) {
				Entry<Integer, ? extends ItemStack> entry = (Entry<Integer, ? extends ItemStack>) stackIterator
						.next();
				ItemStack stack = (ItemStack) entry.getValue();

				int endAmount = stack.getAmount() - item1ToPay;
				if (endAmount < 0) {
					endAmount = 0;
				}

				item1ToPay = item1ToPay - stack.getAmount();
				stack.setAmount(endAmount);
				inventory.setItem(entry.getKey(), stack);

				if (item1ToPay <= 0) {
					break;
				}
			}
		}

		if (trade.getItem2() != null) {
			int item2ToPay = trade.getItem2().getAmount();
			stackIterator = inventory.all(trade.getItem2().getType())
					.entrySet().iterator();
			
			int firstItem2 = inventory.first(trade.getItem2());
			if(firstItem2 > -1) {
				inventory.clear(firstItem2);
			} else {
				// pay item2
				while (stackIterator.hasNext()) {
					Entry<Integer, ? extends ItemStack> entry = (Entry<Integer, ? extends ItemStack>) stackIterator
							.next();
					ItemStack stack = (ItemStack) entry.getValue();

					int endAmount = stack.getAmount() - item2ToPay;
					if (endAmount < 0) {
						endAmount = 0;
					}

					item2ToPay = item2ToPay - stack.getAmount();
					stack.setAmount(endAmount);
					inventory.setItem(entry.getKey(), stack);

					if (item2ToPay <= 0) {
						break;
					}
				}
			}
		}

		// cloning stack
		ItemStack addingItem = item.clone();
		ItemMeta itemMeta = addingItem.getItemMeta();
		List<String> lore = itemMeta.getLore();
		
		lore.remove(lore.size()-1);
		itemMeta.setLore(lore);
		addingItem.setItemMeta(itemMeta);
		
		HashMap<Integer, ItemStack> notStored = inventory.addItem(addingItem);
		if(notStored.size() > 0) {
			ItemStack notAddedItem = notStored.get(0);
			int removingAmount = addingItem.getAmount() - notAddedItem.getAmount();
			addingItem.setAmount(removingAmount);
			inventory.removeItem(addingItem);
			
			// restore
			inventory.addItem(trade.getItem1());
			if(trade.getItem2() != null) {
				inventory.addItem(trade.getItem2());
			}
			
			success = false;
		}

		player.updateInventory();
		return success;
	}

	private boolean hasEnoughRessource(Player player, VillagerTrade trade) {
		ItemStack item1 = trade.getItem1();
		ItemStack item2 = trade.getItem2();
		PlayerInventory inventory = player.getInventory();

		if (item2 != null) {
			if (!inventory.contains(item1.getType(), item1.getAmount())
					|| !inventory.contains(item2.getType(), item2.getAmount())) {
				return false;
			}
		} else {
			if (!inventory.contains(item1.getType(), item1.getAmount())) {
				return false;
			}
		}

		return true;
	}

	private VillagerTrade getTradingItem(MerchantCategory category,
			ItemStack stack, Game game, Player player) {
		for (VillagerTrade trade : category.getOffers()) {
			ItemStack iStack = this.toItemStack(trade, player, game);
			if (iStack.equals(stack)) {
				return trade;
			} else if(iStack.getType() == Material.ENDER_CHEST && stack.getType() == Material.ENDER_CHEST) {
			    return trade;
			}
		}

		return null;
	}

	private MerchantCategory getCategoryByMaterial(Material material) {
		for (MerchantCategory category : this.categories) {
			if (category.getMaterial() == material) {
				return category;
			}
		}

		return null;
	}

	public void setCurrentCategory(MerchantCategory category) {
		this.currentCategory = category;
	}
}
