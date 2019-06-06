package de.dytanic.cloudnet.ext.bridge;

public class WorldPosition {

    protected double x, y, z, yaw, pitch;

    protected String world;

    public WorldPosition(double x, double y, double z, double yaw, double pitch, String world) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.world = world;
    }

    public WorldPosition() {
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public double getYaw() {
        return this.yaw;
    }

    public double getPitch() {
        return this.pitch;
    }

    public String getWorld() {
        return this.world;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public void setYaw(double yaw) {
        this.yaw = yaw;
    }

    public void setPitch(double pitch) {
        this.pitch = pitch;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof WorldPosition)) return false;
        final WorldPosition other = (WorldPosition) o;
        if (!other.canEqual((Object) this)) return false;
        if (Double.compare(this.getX(), other.getX()) != 0) return false;
        if (Double.compare(this.getY(), other.getY()) != 0) return false;
        if (Double.compare(this.getZ(), other.getZ()) != 0) return false;
        if (Double.compare(this.getYaw(), other.getYaw()) != 0) return false;
        if (Double.compare(this.getPitch(), other.getPitch()) != 0) return false;
        final Object this$world = this.getWorld();
        final Object other$world = other.getWorld();
        if (this$world == null ? other$world != null : !this$world.equals(other$world)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof WorldPosition;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $x = Double.doubleToLongBits(this.getX());
        result = result * PRIME + (int) ($x >>> 32 ^ $x);
        final long $y = Double.doubleToLongBits(this.getY());
        result = result * PRIME + (int) ($y >>> 32 ^ $y);
        final long $z = Double.doubleToLongBits(this.getZ());
        result = result * PRIME + (int) ($z >>> 32 ^ $z);
        final long $yaw = Double.doubleToLongBits(this.getYaw());
        result = result * PRIME + (int) ($yaw >>> 32 ^ $yaw);
        final long $pitch = Double.doubleToLongBits(this.getPitch());
        result = result * PRIME + (int) ($pitch >>> 32 ^ $pitch);
        final Object $world = this.getWorld();
        result = result * PRIME + ($world == null ? 43 : $world.hashCode());
        return result;
    }

    public String toString() {
        return "WorldPosition(x=" + this.getX() + ", y=" + this.getY() + ", z=" + this.getZ() + ", yaw=" + this.getYaw() + ", pitch=" + this.getPitch() + ", world=" + this.getWorld() + ")";
    }
}