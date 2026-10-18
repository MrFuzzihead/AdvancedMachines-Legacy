package atomicstryker.ic2.advancedmachines;

import java.util.ArrayList;

import ic2.core.block.invslot.InvSlotOutput;

public interface IAdvancedMachine {

    String printFormattedData();

    int getSpeed();

    void setClientSpeed(int value);

    ArrayList<InvSlotOutput> getOutputSlots();

}
