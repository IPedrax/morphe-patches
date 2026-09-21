import java.util.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.instruction.Instruction;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.instruction.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VerifyAdsDexTest {
    enum Change { NONE, ARGUMENT, RESULT, GETTER, FILTER, ITERATOR, CALLER, MISSING, DUPLICATE, CAPABILITY }

    @Test void acceptsExpectedHooks() { VerifyAdsDex.verifyHooks(fixture(Change.NONE), true); }

    @Test void rejectsBrokenHooks() {
        for (var change : Change.values()) if (change != Change.NONE) {
            assertThrows(AssertionError.class, () -> VerifyAdsDex.verifyHooks(fixture(change), true), change.name());
        }
    }

    @Test void rejectsHooksWhenPatchIsNotSelected() {
        assertThrows(AssertionError.class, () -> VerifyAdsDex.verifyHooks(fixture(Change.NONE), false));
    }

    @Test void acceptsNoExtensionWhenNotSelected() { VerifyAdsDex.verifyHooks(List.of(), false); }

    List<ImmutableClassDef> fixture(Change change) {
        var classes = new ArrayList<ImmutableClassDef>();
        for (var owner : List.of("Lp/jb20;", "Lp/vot;", "Lp/x7v0;")) {
            if (change == Change.MISSING && owner.equals("Lp/jb20;")) continue;
            boolean browse = owner.equals("Lp/x7v0;");
            String structure = browse ? "Lcom/spotify/browsita/v1/resolved/BrowseStructure;"
                    : "Lcom/spotify/casita/v1/resolved/HomeStructure;";
            var code = new ArrayList<Instruction>(List.of(
                new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 4, 0, 0, 0, 0,
                    new ImmutableMethodReference(structure, change == Change.GETTER ? "x" : browse ? "o" : "p", List.of(), "Lp/ih40;")),
                new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 4),
                new ImmutableInstruction3rc(Opcode.INVOKE_STATIC_RANGE, change == Change.ARGUMENT ? 5 : 4, 1,
                    new ImmutableMethodReference(VerifyAdsDex.HELPER, change == Change.FILTER ? "filter" : browse ? "browse" : "home",
                        List.of("Ljava/util/List;"), "Ljava/util/List;")),
                new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, change == Change.RESULT ? 5 : 4),
                new ImmutableInstruction10x(Opcode.NOP), new ImmutableInstruction10x(Opcode.NOP)));
            if (owner.equals("Lp/jb20;")) code.add(new ImmutableInstruction10x(Opcode.NOP));
            code.add(new ImmutableInstruction35c(Opcode.INVOKE_INTERFACE, 1, change == Change.ITERATOR ? 5 : 4, 0, 0, 0, 0,
                    new ImmutableMethodReference("Ljava/lang/Iterable;", "iterator", List.of(), "Ljava/util/Iterator;")));
            code.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 4));
            code.add(new ImmutableInstruction11x(Opcode.RETURN_OBJECT, 4));
            String actualOwner = change == Change.CALLER && owner.equals("Lp/jb20;") ? "Ltest/Other;" : owner;
            var definition = definition(actualOwner, VerifyAdsDex.CALLERS.get(owner), "Ljava/lang/Object;", code);
            classes.add(definition);
            if (change == Change.DUPLICATE && owner.equals("Lp/jb20;")) classes.add(definition);
        }
        classes.add(definition(VerifyAdsDex.INSTALLED, "hideBrandAds", "Z", List.of(
                new ImmutableInstruction11n(Opcode.CONST_4, 0, change == Change.CAPABILITY ? 0 : 1),
                new ImmutableInstruction11x(Opcode.RETURN, 0))));
        return classes;
    }

    ImmutableClassDef definition(String owner, String name, String result, List<? extends Instruction> code) {
        var method = new ImmutableMethod(owner, name, List.of(), result, 9, Set.of(), Set.of(),
                new ImmutableMethodImplementation(7, code, List.of(), List.of()));
        return new ImmutableClassDef(owner, 1, "Ljava/lang/Object;", List.of(), null, Set.of(), List.of(), List.of(method));
    }
}
