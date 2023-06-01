package hardwar.branch.prediction.judged.SAg;


import hardwar.branch.prediction.shared.*;
import hardwar.branch.prediction.shared.devices.*;

import java.util.Arrays;

public class SAg implements BranchPredictor {
    private final int branchInstructionSize;
    private final int KSize;
    private final ShiftRegister SC; // saturating counter register
    private final RegisterBank PSBHR; // per set branch history register
    private final Cache<Bit[], Bit[]> PHT; // page history table

    public SAg() {
        this(4, 2, 8, 4);
    }

    public SAg(int BHRSize, int SCSize, int branchInstructionSize, int KSize) {
        // TODO: complete the constructor
        this.branchInstructionSize = branchInstructionSize;
        this.KSize = KSize;

        // Initialize the PABHR with the given bhr and Ksize
        PSBHR = new RegisterBank(KSize, BHRSize);

        // Initialize the PHT with a size of 2^size and each entry having a saturating counter of size "SCSize"
        PHT = new PageHistoryTable(1 << BHRSize, SCSize);

        // Initialize the SC register
        SC = new SIPORegister("SC", SCSize, null);
    }

    @Override
    public BranchResult predict(BranchInstruction instruction) {
        PHT.putIfAbsent(PSBHR.read(getRBAddressLine(instruction.getInstructionAddress())).read() , getDefaultBlock());
        // TODO : complete Task 1
        SC.load(PHT.get(PSBHR.read(getRBAddressLine(instruction.getInstructionAddress())).read()));
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
        PHT.put(PSBHR.read(getRBAddressLine(branchInstruction.getInstructionAddress())).read(), SC.read());

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


        // TODO: complete Task 2
    }

    private Bit[] getRBAddressLine(Bit[] branchAddress) {
        // hash the branch address
        return hash(branchAddress);
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
