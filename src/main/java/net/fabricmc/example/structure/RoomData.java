package net.fabricmc.example.structure;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.math.BlockPos;

public class RoomData {
  public Boolean isBossRoom = false;
  public List<BlockPos> chestPositions = new ArrayList<>();
  public List<BlockPos> mobPositions = new ArrayList<>();
  // entityPositions? for traveling merchants or quest NPCs?
}
