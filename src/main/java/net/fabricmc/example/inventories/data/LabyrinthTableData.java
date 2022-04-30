package net.fabricmc.example.inventories.data;

import java.util.ArrayList;

public class LabyrinthTableData {
    public String eventKey; // pos of the block to emit to all other open inventories for this block
    public ArrayList<LabyrinthChoice> choices;
}
