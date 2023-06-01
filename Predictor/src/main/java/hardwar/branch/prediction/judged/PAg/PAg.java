package hardwar.branch.prediction.judged.PAg;

import hardwar.branch.prediction.shared.*;
import hardwar.branch.prediction.shared.devices.*;

import java.util.Arrays;

public class PAg implements BranchPredictor {
    private final ShiftRegister SC; // saturating counter register
    private final RegisterBank PABHR; // per address branch history register
    private final Cache<Bit[], Bit[]> PHT; // page history table

    public PAg() {
        this(4, 2, 8);
    }

    /**
     * Creates a new PAg predictor with the given BHR register size and initializes the PABHR based on
     * the branch instruction size and BHR size
     *
     * @param BHRSize               the size of the BHR register
     * @param SCSize                the size of the register which hold the saturating counter value
     * @param branchInstructionSize the number of bits which is used for saving a branch instruction
     */
    public PAg(int BHRSize, int SCSize, int branchInstructionSize) {

        // TODO: complete the constructor
        // Initialize the PABHR with the given bhr and branch instruction size
        PABHR = new RegisterBank(branchInstructionSize, BHRSize);

        int PHT_col = 1 << BHRSize ;
        PHT = new PageHistoryTable(PHT_col, SCSize);

        Bit[] defaultBlock = new Bit[SCSize];
        Arrays.fill(defaultBlock, Bit.ZERO);
        SC = new SIPORegister("SC", SCSize,null );
    }

    /**
     * @param instruction the branch instruction
     * @return the predicted outcome of the branch instruction (taken or not taken)
     */
    @Override
    public BranchResult predict(BranchInstruction instruction) {
        PHT.putIfAbsent( PABHR.read(instruction.getInstructionAddress()).read() , getDefaultBlock());
        // TODO : complete Task 1
        SC.load(PHT.get(PABHR.read(instruction.getInstructionAddress()).read()));
        if (SC.read()[0] == Bit.ONE)
            return BranchResult.TAKEN;
        return BranchResult.NOT_TAKEN;
    }

    /**
     * @param instruction the branch instruction
     * @param actual      the actual result of branch (taken or not)
     */
    @Override
    public void update(BranchInstruction instruction, BranchResult actual) {
        if(actual== BranchResult.TAKEN){
            SC.load(CombinationalLogic.count(SC.read(), true, CountMode.SATURATING));
        }
        else{
            SC.load(CombinationalLogic.count(SC.read(), false, CountMode.SATURATING));
        }
        PHT.put(PABHR.read(instruction.getInstructionAddress()).read(), SC.read());

        if(actual== BranchResult.TAKEN){
            PABHR.read(instruction.getInstructionAddress()).insert(Bit.ONE);
        }
        else{
            PABHR.read(instruction.getInstructionAddress()).insert(Bit.ZERO);
        }
        
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
        return "PAg predictor snapshot: \n" + PABHR.monitor() + SC.monitor() + PHT.monitor();
    }
}
