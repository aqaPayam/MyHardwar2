package hardwar.branch.prediction.judged.PAp;


import hardwar.branch.prediction.shared.*;
import hardwar.branch.prediction.shared.devices.*;

import java.util.Arrays;

public class PAp implements BranchPredictor {

    private final int branchInstructionSize;

    private final ShiftRegister SC; // saturating counter register

    private final RegisterBank PABHR; // per address branch history register

    private final Cache<Bit[], Bit[]> PAPHT; // Per Address Predication History Table

    public PAp() {
        this(4, 2, 8);
    }

    public PAp(int BHRSize, int SCSize, int branchInstructionSize) {
        this.branchInstructionSize = branchInstructionSize;

        PABHR = new RegisterBank(branchInstructionSize, BHRSize);


        Bit[] defaultBlock = new Bit[SCSize];
        Arrays.fill(defaultBlock, Bit.ZERO);
        SC = new SIPORegister("SC", SCSize,null );

        // Initialize the PHT with a size of 2^size and each entry having a saturating counter of size "SCSize"
        int PHT_col =(1 << BHRSize);

        PAPHT = new PerAddressPredictionHistoryTable(this.branchInstructionSize,PHT_col,SCSize);
    
    }

    @Override
    public BranchResult predict(BranchInstruction branchInstruction) {
        PAPHT.putIfAbsent(getCacheEntry(branchInstruction.getInstructionAddress(), PABHR.read(branchInstruction.getInstructionAddress()).read()), getDefaultBlock());

        SC.load(PAPHT.get(PABHR.read(branchInstruction.getInstructionAddress()).read()));
        if (SC.read()[0] == Bit.ONE)
            return BranchResult.TAKEN;
        return BranchResult.NOT_TAKEN;
    }

    @Override
    public void update(BranchInstruction instruction, BranchResult actual) {
        if(actual== BranchResult.TAKEN){
            SC.load(CombinationalLogic.count(SC.read(), true, CountMode.SATURATING));
        }
        else{
            SC.load(CombinationalLogic.count(SC.read(), false, CountMode.SATURATING));
        }
        PAPHT.put(getCacheEntry(instruction.getInstructionAddress(), PABHR.read(instruction.getInstructionAddress()).read()), SC.read());
    
        if(actual== BranchResult.TAKEN){
            ShiftRegister bits =  PABHR.read(instruction.getInstructionAddress());
            bits.insert(Bit.ONE);
            PABHR.write(instruction.getInstructionAddress(),bits.read());
        }
        else{
            ShiftRegister bits =  PABHR.read(instruction.getInstructionAddress());
            bits.insert(Bit.ZERO);
            PABHR.write(instruction.getInstructionAddress(),bits.read());
        }
    }


    private Bit[] getCacheEntry(Bit[] branchAddress, Bit[] BHRValue) {
        // Concatenate the branch address bits with the BHR bits
        Bit[] cacheEntry = new Bit[branchAddress.length + BHRValue.length];
        System.arraycopy(branchAddress, 0, cacheEntry, 0, branchInstructionSize);
        System.arraycopy(BHRValue, 0, cacheEntry, branchAddress.length, BHRValue.length);
        return cacheEntry;
    }

    /**
     * @return a zero series of bits as default value of cache block
     */
    private Bit[] getDefaultBlock() {
        Bit[] defaultBlock = new Bit[SC.getLength()];
        Arrays.fill(defaultBlock, Bit.ZERO);
        return defaultBlock;
    }

    @Override
    public String monitor() {
        return "PAp predictor snapshot: \n" + PABHR.monitor() + SC.monitor() + PAPHT.monitor();
    }
}


