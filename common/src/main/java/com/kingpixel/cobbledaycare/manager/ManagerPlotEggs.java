package com.kingpixel.cobbleutils.features.breeding.manager;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.features.breeding.models.PlotBreeding;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Data;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Author: Carlos Varas Alonso - 02/08/2024 14:09
 */
@Data
public class ManagerPlotEggs {


  public ManagerPlotEggs() {
    File dataFolder = Utils.getAbsolutePath(CobbleUtils.PATH_BREED_DATA);
    if (!dataFolder.exists()) {
      dataFolder.mkdirs();
    }
  }

  public static List<PlotBreeding> createPlots() {
    List<PlotBreeding> plots = new ArrayList<>();
    for (int i = 0; i < CobbleUtils.breedconfig.getPlotSlots().size(); i++) {
      plots.add(new PlotBreeding());
    }
    return plots;
  }


}
