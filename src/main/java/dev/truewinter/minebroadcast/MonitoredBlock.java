package dev.truewinter.minebroadcast;

import org.bukkit.Material;

/**
 * @author TrueWinter
 */
public class MonitoredBlock {
    private Material material;
    private String color;
    private String name;
    private boolean counting;
    private int alertAfter;

    public MonitoredBlock(Material material, String color, String name, boolean counting, int alertAfter) {
        this.material = material;
        this.color = color;
        this.name = name;
        this.counting = counting;
        this.alertAfter = alertAfter;
    }

    public Material getMaterial() {
        return material;
    }

    public String getColor() {
        return color;
    }

    public String getName() {
        return name;
    }

    public boolean countingIsEnabled() {
        return counting;
    }

    public int getAlertAfter() {
        return alertAfter;
    }
}
