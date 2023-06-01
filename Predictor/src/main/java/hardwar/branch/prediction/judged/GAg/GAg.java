package hardwar.branch.prediction.judged.GAg;

import hardwar.branch.prediction.shared.*;
import hardwar.branch.prediction.shared.devices.*;

import java.util.Arrays;

public class GAg implements BranchPredictor {
    private final ShiftRegister BHR; // branch history register
    private final Cache<Bit[], Bit[]> PHT; // page history table
    private final ShiftRegister SC; // saturated counter register

    public GAg() {
        this(4, 2);
    }

    /**
     * Creates a new GAg predictor with the given BHR register size and initializes the BHR and PHT.
     *
     * @param BHRSize the size of the BHR register
     * @param SCSize  the size of the register which hold the saturating counter value and the cache block size
     */
    public GAg(int BHRSize, int SCSize) {

        Bit[] defaultBlock = new Bit[BHRSize];
        Arrays.fill(defaultBlock, Bit.ZERO);
        this.BHR = new SIPORegister("BHR",BHRSize,null) ;
        //

        // TODO : complete the constructor
        // Initialize the BHR register with the given size and no default value
    

        // Initialize the PHT with a size of 2^size and each entry having a saturating counter of size "SCSize"
        PHT = new PageHistoryTable(2 << BHRSize, SCSize);

        // Initialize the SC register
        defaultBlock = new Bit[SCSize];
        Arrays.fill(defaultBlock, Bit.ZERO);
        SC = new SIPORegister("SC", SCSize,null );
    }

    /**
     * Predicts the result of a branch instruction based on the global branch history
     *
     * @param branchInstruction the branch instruction
     * @return the predicted outcome of the branch instruction (taken or not taken)
     */
    @Override
    public BranchResult predict(BranchInstruction branchInstruction) {
        // TODO : complete Task 1
        SC.load(PHT.get(BHR.read()));
        if (SC.read()[0] == Bit.ONE)
            return BranchResult.TAKEN;
        return BranchResult.NOT_TAKEN;
    }

    /**
     * Updates the values in the cache based on the actual branch result
     *
     * @param instruction the branch instruction
     * @param actual      the actual result of the branch condition
     */
    @Override
    public void update(BranchInstruction instruction, BranchResult actual) {
        // akahrin chizi ke predict shode ro update nishe kard faghat 
        //TODO OOOOOOOOOOOOOOOOOOOOO

        if(actual== BranchResult.TAKEN){
            SC.load(CombinationalLogic.count(SC.read(), true, CountMode.SATURATING));
        }
        else{
            SC.load(CombinationalLogic.count(SC.read(), false, CountMode.SATURATING));
        }
        PHT.put(BHR.read(), SC.read());

        if(actual== BranchResult.TAKEN){
            BHR.insert(Bit.ONE);
        }
        else{
            BHR.insert(Bit.ZERO);
        }
        
        // TODO: complete Task 2
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
        return "GAg predictor snapshot: \n" + BHR.monitor() + SC.monitor() + PHT.monitor();
    }

    public static void main(String[] args) {
        
    }
}
