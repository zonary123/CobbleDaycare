package com.kingpixel.cobbledaycare.models;

import lombok.Data;

/**
 * @author Carlos Varas Alonso - 15/08/2025 23:17
 */
@Data
public class Position {
  double x, z;
  long lastUpdate;

  public Position(double x, double z, long lastUpdate) {
    this.x = x;
    this.z = z;
    this.lastUpdate = lastUpdate;
  }
}