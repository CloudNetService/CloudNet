package de.dytanic.cloudnet.ext.cloudflare;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CloudflareGroupConfiguration {

  protected String name, sub;

  protected int priority, weight;

}