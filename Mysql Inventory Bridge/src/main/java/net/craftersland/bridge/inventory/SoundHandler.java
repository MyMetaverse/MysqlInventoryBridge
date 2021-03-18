package net.craftersland.bridge.inventory;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundHandler {
	
	private final Main pd;
	
	public SoundHandler(Main pd) {
		this.pd = pd;
	}
	
	public void sendPlingSound(Player p) {
		if (!pd.getConfigHandler().getBoolean("General.disableSounds")) {
			if (Main.is13Server) {
				p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 3F, 3F);
			} else if (Main.is19Server) {
				p.playSound(p.getLocation(), Sound.valueOf("BLOCK_NOTE_PLING"), 3F, 3F);
			} else {
				p.playSound(p.getLocation(), Sound.valueOf("NOTE_PLING"), 3F, 3F);
			}
		}
	}
	
	public void sendLevelUpSound(Player p) {
		if (!pd.getConfigHandler().getBoolean("General.disableSounds")) {
			if (Main.is19Server) {
				p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1F, 1F);
			} else {
				p.playSound(p.getLocation(), Sound.valueOf("LEVEL_UP"), 1F, 1F);
			}
		}
	}
	
	public void sendArrowHit(Player p) {
		if (!pd.getConfigHandler().getBoolean("General.disableSounds")) {
			if (Main.is19Server) {
				p.playSound(p.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 3F, 3F);
			} else {
				p.playSound(p.getLocation(), Sound.valueOf("SUCCESSFUL_HIT"), 3F, 3F);
			}
		}
	}

}
