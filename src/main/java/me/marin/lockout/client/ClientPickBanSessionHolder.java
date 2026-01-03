package me.marin.lockout.client;

import me.marin.lockout.network.UpdatePickBanSessionPayload;

/**
 * Client-side holder for active pick/ban session state
 */
public class ClientPickBanSessionHolder {
    private static UpdatePickBanSessionPayload activeSession = null;
    
    public static void setActiveSession(UpdatePickBanSessionPayload session) {
        activeSession = session;
    }
    
    public static UpdatePickBanSessionPayload getActiveSession() {
        return activeSession;
    }
    
    public static void clearSession() {
        activeSession = null;
    }
    
    public static boolean hasActiveSession() {
        return activeSession != null;
    }
}
