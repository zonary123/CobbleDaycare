package com.kingpixel.cobbledaycare.models.mecanics;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Carlos Varas Alonso - 31/01/2025 0:54
 */
@Getter
@Setter
@ToString
public class OptionsMecanics {
  private boolean masuda;
  private boolean doubleDitto;
  private int defaultIvsTransfer;
  private int DestinyKnotIvsTransfer;
  private int maxIvsRandom;
  private float percentageEverStone;
  private float percentageDestinyKnot;
  private float percentagePowerItem;

  public OptionsMecanics() {
    this.defaultIvsTransfer = 3;
    this.DestinyKnotIvsTransfer = 5;
    this.maxIvsRandom = 31;
    this.percentageEverStone = 70f;
    this.percentageDestinyKnot = 100f;
    this.percentagePowerItem = 100f;
  }
}
