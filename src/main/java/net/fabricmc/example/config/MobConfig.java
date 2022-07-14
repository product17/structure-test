package net.fabricmc.example.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

public class MobConfig {
  public List<MobDefinition> bossList = new ArrayList<>();
  public List<MobDefinition> mobList = new ArrayList<>();

  public MobDefinition getRandomMob(int level, Boolean isBoss) {
    Map<Integer, MobDefinition> weightedList = new HashMap<>();
    for (MobDefinition mobDefinition : isBoss ? bossList : mobList) {
      if (!LevelRange.matchLevel(level, mobDefinition.levelRange)) {
        // Skip if does not match levelRange
        continue;
      }

      TreeSet<Integer> weights = new TreeSet<Integer>(weightedList.keySet());
      int summedWeight = weightedList.size() > 0 ? weights.last() : 0;

      weightedList.put(summedWeight + mobDefinition.weight, mobDefinition);
    }

    TreeSet<Integer> weights = new TreeSet<Integer>(weightedList.keySet());
    Random random = new Random();
    int weightRoll = random.nextInt(weights.last()) + 1;
    for (Integer weightKey : weights) {
      if (weightRoll <= weightKey) {
        return weightedList.get(weightKey);
      }
    }

    // just return random if no weighted mobs can be selected
    return mobList.get(random.nextInt(mobList.size()));
  }
}
