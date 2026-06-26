package me.marin.lockout.lockout.interfaces;

import net.minecraft.world.entity.player.Player;

public abstract class OpponentObtainsItemGoal extends ObtainAllItemsGoal {

    public OpponentObtainsItemGoal(String id, String data) {
        super(id, data);
    }

    public abstract String getMessage(Player player);

}
