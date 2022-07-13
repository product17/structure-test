package net.fabricmc.example.config;

public class LevelRange {
  public Integer min;
  public Integer max;

  public static Boolean matchLevel(int level, LevelRange range) {
    // if range not set, then always include
    if (range == null || (range.min == null && range.max == null)) {
      return true;
    }

    if (range.min != null && level < range.min) {
      return false;
    }

    if (range.max != null && level > range.max) {
      return false;
    }

    return true;
  }
}
