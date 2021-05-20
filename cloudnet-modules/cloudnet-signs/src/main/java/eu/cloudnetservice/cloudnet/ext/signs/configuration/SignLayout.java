package eu.cloudnetservice.cloudnet.ext.signs.configuration;

import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
@EqualsAndHashCode
public class SignLayout implements SerializableObject {

    protected String[] lines;
    protected String blockMaterial;
    protected int blockSubId;

    public SignLayout(String[] lines, String blockMaterial, int blockSubId) {
        this.lines = lines;
        this.blockMaterial = blockMaterial;
        this.blockSubId = blockSubId;
    }

    public String[] getLines() {
        return lines;
    }

    public void setLines(String[] lines) {
        this.lines = lines;
    }

    public String getBlockMaterial() {
        return blockMaterial;
    }

    public void setBlockMaterial(String blockMaterial) {
        this.blockMaterial = blockMaterial;
    }

    public int getBlockSubId() {
        return blockSubId;
    }

    public void setBlockSubId(int blockSubId) {
        this.blockSubId = blockSubId;
    }

    @Override
    public void write(@NotNull ProtocolBuffer buffer) {
        buffer.writeStringArray(this.lines);
        buffer.writeOptionalString(this.blockMaterial);
        buffer.writeInt(this.blockSubId);
    }

    @Override
    public void read(@NotNull ProtocolBuffer buffer) {
        this.lines = buffer.readStringArray();
        this.blockMaterial = buffer.readOptionalString();
        this.blockSubId = buffer.readInt();
    }
}
