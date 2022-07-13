package net.fabricmc.example.config;

import java.util.ArrayList;
import java.util.List;

public class MobDefinition {
  public Float damageMultiplier;
  public Float healthMultiplier;
  public LevelRange levelRange;
  public List<LootTier> lootTables = new ArrayList<>();
  public String mobType;
  public Integer weight;
  public Float xpMultiplier;
}
