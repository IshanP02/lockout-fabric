package me.marin.lockout.lockout.goals.attach_lead;

import me.marin.lockout.lockout.interfaces.LeashUniqueEntitiesAtOnceGoal;

public class Leash4UniqueEntitiesAtOnceGoal extends LeashUniqueEntitiesAtOnceGoal {
    public Leash4UniqueEntitiesAtOnceGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public int getAmount() {
        return 4;
    }

}
