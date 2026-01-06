package me.marin.lockout.lockout.goals.attach_lead;

import me.marin.lockout.lockout.interfaces.LeashUniqueEntitiesAtOnceGoal;

public class Leash8UniqueEntitiesAtOnceGoal extends LeashUniqueEntitiesAtOnceGoal {
    public Leash8UniqueEntitiesAtOnceGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public int getAmount() {
        return 8;
    }

}
