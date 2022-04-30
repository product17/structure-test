package net.fabricmc.example.config;

import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import net.fabricmc.example.ExampleMod;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

public class JsonLoader extends JsonDataLoader {

    public JsonLoader(Gson gson, String dataType) {
        super(gson, dataType);
        //TODO Auto-generated constructor stub
    }

    public Identifier getFabricId() {
        return new Identifier(ExampleMod.modId, "my_resources");
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> resources, ResourceManager manager, Profiler var3) {
        // TODO Auto-generated method stub
        for (JsonElement elem : resources.values()) {

            System.out.println(elem);
        }
    }
    
}
