package com.kingpixel.cobbledaycare.models;

import lombok.Data;

/**
 * @author Carlos Varas Alonso - 03/10/2025 3:04
 */
@Data
public class UserInfoOptions {
  private boolean notifyCreateEgg;
  private boolean notifyBanPokemon;
  private boolean actionBar;

  public UserInfoOptions() {
    notifyCreateEgg = true;
    notifyBanPokemon = true;
    actionBar = true;
  }

}
