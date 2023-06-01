package hardwar.branch.prediction.judged.SAs;

import hardwar.branch.prediction.shared.*;
import hardwar.branch.prediction.shared.devices.*;

import java.util.Arrays;

public class SAs implements BranchPredictor {

    private final int branchInstructionSize;
    private final int KSize;
    private final ShiftRegister SC;
    private final RegisterBank PSBHR; // per set branch history register
    private final Cache<Bit[], Bit[]> PSPHT; // per set predication history table
    private final HashMode hashMode;

    public SAs() {
        this(4, 2, 8, 4, HashMode.XOR);
    }

    public SAs(int BHRSize, int SCSize, int branchInstructionSize, int KSize, HashMode hashMode) {

        this.branchInstructionSize = branchInstructionSize;
        
        this.KSize = KSize;

        this.PSBHR = new RegisterBank(KSize, BHRSize);


        Bit[] defaultBlock = new Bit[SCSize];
        Arrays.fill(defaultBlock, Bit.ZERO);
        this.SC = new SIPORegister("SC", SCSize,null );

        // Initialize the PHT with a size of 2^size and each entry having a saturating counter of size "SCSize"
        int PHT_col =(1 << BHRSize);

        this.PSPHT = new PerAddressPredictionHistoryTable(KSize,PHT_col,SCSize);
        
        this.hashMode = HashMode.XOR;

    }

    @Override
    public BranchResult predict(BranchInstruction branchInstruction) {
        PSPHT.putIfAbsent(getCacheEntry(branchInstruction.getInstructionAddress(), PSBHR.read(getAddressLine(branchInstruction.getInstructionAddress())).read()), getDefaultBlock());

        SC.load( PSPHT.get(getCacheEntry(branchInstruction.getInstructionAddress(), PSBHR.read(getAddressLine(branchInstruction.getInstructionAddress())).read() )));
        if (SC.read()[0] == Bit.ONE)
            return BranchResult.TAKEN;
        return BranchResult.NOT_TAKEN;
    }

    @Override
    public void update(BranchInstruction branchInstruction, BranchResult actual) {
        if(actual== BranchResult.TAKEN){
            SC.load(CombinationalLogic.count(SC.read(), true, CountMode.SATURATING));
        }
        else{
            SC.load(CombinationalLogic.count(SC.read(), false, CountMode.SATURATING));
        }
        PSPHT.put(getCacheEntry(branchInstruction.getInstructionAddress(), PSBHR.read(getAddressLine(branchInstruction.getInstructionAddress())).read()), SC.read());
    
        if(actual== BranchResult.TAKEN){
            ShiftRegister bits =  PSBHR.read(getAddressLine(branchInstruction.getInstructionAddress()));
            bits.insert(Bit.ONE);
            PSBHR.write(getAddressLine(branchInstruction.getInstructionAddress()),bits.read());
        }
        else{
            ShiftRegister bits =  PSBHR.read(getAddressLine(branchInstruction.getInstructionAddress()));
            bits.insert(Bit.ZERO);
            PSBHR.write(getAddressLine(branchInstruction.getInstructionAddress()),bits.read());
        }
    }


    private Bit[] getAddressLine(Bit[] branchAddress) {
        // hash the branch address
        return CombinationalLogic.hash(branchAddress, KSize, hashMode);
    }

    private Bit[] getCacheEntry(Bit[] branchAddress, Bit[] BHRValue) {
        // Concatenate the branch address bits with the BHR bits
        Bit[] cacheEntry = new Bit[branchAddress.length + BHRValue.length];
        System.arraycopy(branchAddress, 0, cacheEntry, 0, KSize);
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
        return null;
    }
}
