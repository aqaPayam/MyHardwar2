package hardwar.branch.prediction.judged.SAp;


import hardwar.branch.prediction.shared.*;
import hardwar.branch.prediction.shared.devices.*;

import java.util.Arrays;

public class SAp implements BranchPredictor {

    private final int branchInstructionSize;
    private final int KSize;
    private final ShiftRegister SC;
    private final RegisterBank PSBHR; // per set branch history register
    private final Cache<Bit[], Bit[]> PAPHT; // per address predication history table

    public SAp() {
        this(4, 2, 8, 4);
    }

    public SAp(int BHRSize, int SCSize, int branchInstructionSize, int KSize) {

        this.branchInstructionSize = branchInstructionSize;
        
        this.KSize = KSize;

        this.PSBHR = new RegisterBank(KSize, BHRSize);


        Bit[] defaultBlock = new Bit[SCSize];
        Arrays.fill(defaultBlock, Bit.ZERO);
        this.SC = new SIPORegister("SC", SCSize,null );

        // Initialize the PHT with a size of 2^size and each entry having a saturating counter of size "SCSize"
        int PHT_col =(1 << BHRSize);

        this.PAPHT = new PerAddressPredictionHistoryTable(this.branchInstructionSize,PHT_col,SCSize);

    }

    @Override
    public BranchResult predict(BranchInstruction branchInstruction) {
              PAPHT.putIfAbsent(getCacheEntry(branchInstruction.getInstructionAddress(), PSBHR.read(getRBAddressLine(branchInstruction.getInstructionAddress())).read()), getDefaultBlock());

        SC.load( PAPHT.get(getCacheEntry(branchInstruction.getInstructionAddress(), PSBHR.read(getRBAddressLine(branchInstruction.getInstructionAddress())).read() )));
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
        PAPHT.put(getCacheEntry(branchInstruction.getInstructionAddress(), PSBHR.read(getRBAddressLine(branchInstruction.getInstructionAddress())).read()), SC.read());
    
        if(actual== BranchResult.TAKEN){
            ShiftRegister bits =  PSBHR.read(getRBAddressLine(branchInstruction.getInstructionAddress()));
            bits.insert(Bit.ONE);
            PSBHR.write(getRBAddressLine(branchInstruction.getInstructionAddress()),bits.read());
        }
        else{
            ShiftRegister bits =  PSBHR.read(getRBAddressLine(branchInstruction.getInstructionAddress()));
            bits.insert(Bit.ZERO);
            PSBHR.write(getRBAddressLine(branchInstruction.getInstructionAddress()),bits.read());
        }
    }


    private Bit[] getRBAddressLine(Bit[] branchAddress) {
        // hash the branch address
        return hash(branchAddress);
    }

    private Bit[] getCacheEntry(Bit[] branchAddress, Bit[] BHRValue) {
        // Concatenate the branch address bits with the BHR bits
        Bit[] cacheEntry = new Bit[branchAddress.length + BHRValue.length];
        System.arraycopy(branchAddress, 0, cacheEntry, 0, branchInstructionSize);
        System.arraycopy(BHRValue, 0, cacheEntry, branchAddress.length, BHRValue.length);
        return cacheEntry;
    }


    /**
     * hash N bits to a K bit value
     *
     * @param bits program counter
     * @return hash value of fist M bits of `bits` in K bits
     */
    private Bit[] hash(Bit[] bits) {
        Bit[] hash = new Bit[KSize];

        // XOR the first M bits of the PC to produce the hash
        for (int i = 0; i < branchInstructionSize; i++) {
            int j = i % KSize;
            if (hash[j] == null) {
                hash[j] = bits[i];
            } else {
                Bit xorProduce = hash[j].getValue() ^ bits[i].getValue() ? Bit.ONE : Bit.ZERO;
                hash[j] = xorProduce;

            }
        }
        return hash;
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
