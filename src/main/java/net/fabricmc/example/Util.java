package net.fabricmc.example;

import net.minecraft.util.Identifier;

public class Util {
  public static Identifier id(String name) {
		return new Identifier(ExampleMod.modId, name);
	}
}
