package com.kingpixel.cobbledaycare.models;

import lombok.Data;

/**
 * @author Carlos Varas Alonso - 03/10/2025 3:04
 */
@Data
public class UserInfoOptions {
  private boolean notifyCreateEgg = true;
  private boolean notifyLimitEggs = true;
  private boolean notifyBanPokemon = true;
  private boolean actionBar = true;
}
