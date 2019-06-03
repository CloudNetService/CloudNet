package de.dytanic.cloudnet.ext.signs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignLayout {

    protected String[] lines;

    protected String blockType;

    protected int subId;

}