package com.narrowtux.DropChest;

import java.util.ArrayList;
import java.util.HashMap;
import org.bukkit.ChatColor;
import java.util.TimerTask;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.StorageMinecart;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.ContainerBlock;
import org.bukkit.inventory.Inventory;

import java.util.List;

import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftItem;
import org.bukkit.craftbukkit.entity.CraftMinecart;
import org.bukkit.craftbukkit.entity.CraftStorageMinecart;

import net.minecraft.server.EntityItem;
import net.minecraft.server.EntityMinecart;

import org.bukkit.inventory.ItemStack;

import com.narrowtux.DropChest.DropChest;


public class EntityWatcher extends TimerTask {
	private DropChest plugin;
	private List<DropChestItem> chestsToBeRemoved;
	public EntityWatcher(DropChest plugin) {
		this.plugin = plugin;
		chestsToBeRemoved = new ArrayList<DropChestItem>();
	}

	@Override
	public void run() {
		try{
			for(World w : plugin.getServer().getWorlds()){
				for(Entity e : w.getEntities())
				{
					if(e.getClass().getName().contains("CraftItem"))
					{
						CraftItem item = (CraftItem)e;
						EntityItem eitem = (EntityItem)item.getHandle();
						int type = eitem.a.id;
						int count = eitem.a.count;
						short damage = (short)eitem.a.damage;
						ItemStack stack = new ItemStack(type, count, damage);
						for(DropChestItem dci : plugin.getChests()){
							Block block = dci.getBlock();
							if(!DropChestItem.acceptsBlockType(block.getType())){
								if(chestsToBeRemoved.contains(dci)){
									plugin.getChests().remove(dci);
									chestsToBeRemoved.remove(dci);
									plugin.getServer().broadcastMessage(ChatColor.RED.toString()+"A DropChest was broken and has been removed.");
								} else {
									chestsToBeRemoved.add(dci);
								}
							} else if(chestsToBeRemoved.contains(dci)){
								chestsToBeRemoved.remove(dci);
							}
							if(plugin.isNear(dci.getBlock().getLocation(), e.getLocation(), dci.getRadius())&&!chestsToBeRemoved.contains(dci))
							{
								HashMap<Integer, ItemStack> ret = dci.addItem(stack);
								boolean allin = false;
								if(ret.size()==0){
									eitem.q();
									allin = true;
								}
								else {
									for(ItemStack s : ret.values()){
										eitem.a.count = s.getAmount();
									}
								}
								if(dci.isFull())
									dci.warnFull();
								else if(dci.getPercentFull()>=0.8)
									dci.warnNearlyFull();
								if(allin){
									break;
								}
							}
						}
					} else if(e instanceof Minecart)
					{
						EntityMinecart minecart = (EntityMinecart)((CraftMinecart)e).getHandle();
						/*
						 * EntityMinecart.d means the type of the minecart
						 * d == 0 means a normal minecart
						 * d == 1 means a storage minecart
						 * d == 2 means a powered minecart
						 */
						if(minecart.d == 1){
							CraftStorageMinecart storage = new CraftStorageMinecart((CraftServer)plugin.getServer(), minecart);
							Inventory inv = storage.getInventory();
							for(DropChestItem dci : plugin.getChests()){
								if(plugin.isNear(dci.getBlock().getLocation(), storage.getLocation(), 2))
								{
									ContainerBlock chest = dci.getChest();
									Inventory chinv = chest.getInventory();
									Inventory miinv = storage.getInventory();
									if(dci.getMinecartAction()==DropChestMinecartAction.PUSH_TO_MINECART){
										for(int i = 0; i<chinv.getSize();i++){
											ItemStack items = chinv.getItem(i);
											if(items.getAmount()!=0){
												HashMap<Integer,ItemStack> hash = miinv.addItem(items);
												if(hash.size()!=0){
													ItemStack ret=hash.get(0);
													items.setAmount(items.getAmount()-ret.getAmount());
												}
												chinv.remove(items);
											}
										}
									} else if(dci.getMinecartAction() == DropChestMinecartAction.PULL_FROM_MINECART){
										for(int i = 0; i<miinv.getSize();i++){
											ItemStack items = miinv.getItem(i);
											if(items.getAmount()!=0){
												HashMap<Integer,ItemStack> hash = dci.addItem(items);
												if(hash.size()!=0){
													ItemStack ret=hash.get(0);
													items.setAmount(items.getAmount()-ret.getAmount());
												}
												miinv.remove(items);
											}
										}
									}
								}
							}
						}

					}
				}
			}
		} catch(Exception e){
			System.out.println("Warning! An error occured!");
			e.printStackTrace();
		}
	}
}
