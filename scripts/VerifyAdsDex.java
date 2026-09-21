import java.io.File;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.ZipFile;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

class VerifyAdsDex {
    static final String HELPER = "Lapp/spicetify/extension/spotify/ads/BrandAds;";
    static final String INSTALLED = "Lapp/spicetify/extension/spotify/settings/InstalledPatches;";
    static final Map<String, String> CALLERS = Map.of("Lp/jb20;", "invoke", "Lp/vot;", "g", "Lp/x7v0;", "a");
    static final List<String> MODELS = List.of("Lp/ih40;",
            "Lcom/spotify/casita/v1/resolved/Section;", "Lcom/spotify/browsita/v1/resolved/Section;",
            "Lcom/spotify/casita/v1/resolved/HomeStructure;", "Lcom/spotify/browsita/v1/resolved/BrowseStructure;");

    static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static Map<String, ClassDef> load(String path) throws Exception {
        var classes = new HashMap<String, ClassDef>();
        var dex = DexFileFactory.loadDexContainer(new File(path), Opcodes.forApi(35));
        for (var entry : dex.getDexEntryNames()) for (var cls : dex.getEntry(entry).getDexFile().getClasses()) {
            require(classes.put(cls.getType(), cls) == null, "Duplicate class: " + cls.getType());
        }
        return classes;
    }

    static byte[] canonical(ClassDef definition) throws Exception {
        require(definition != null, "Missing expected class");
        var pool = new DexPool(Opcodes.forApi(35));
        pool.internClass(definition);
        var output = new MemoryDataStore();
        try {
            pool.writeTo(output);
            return output.getData();
        } finally {
            output.close();
        }
    }

    static String reference(Instruction instruction) {
        return instruction instanceof ReferenceInstruction ref ? ref.getReference().toString() : "";
    }

    static void verifyHooks(Collection<? extends ClassDef> classes, boolean enabled) {
        var seen = new HashSet<String>();
        int capabilities = 0;
        for (var cls : classes) for (var method : cls.getMethods()) {
            if (method.getImplementation() == null) continue;
            var code = new ArrayList<Instruction>();
            method.getImplementation().getInstructions().forEach(code::add);
            if (cls.getType().equals(INSTALLED) && method.getName().equals("hideBrandAds")) {
                capabilities++;
                require(method.getParameterTypes().isEmpty() && method.getReturnType().equals("Z")
                        && AccessFlags.PUBLIC.isSet(method.getAccessFlags()) && AccessFlags.STATIC.isSet(method.getAccessFlags())
                        && code.size() == 2 && code.get(0).getOpcode() == Opcode.CONST_4
                        && ((NarrowLiteralInstruction) code.get(0)).getNarrowLiteral() == (enabled ? 1 : 0)
                        && code.get(1).getOpcode() == Opcode.RETURN
                        && ((OneRegisterInstruction) code.get(0)).getRegisterA() == ((OneRegisterInstruction) code.get(1)).getRegisterA(),
                        "Brand-ad capability differs from patch selection");
            }
            for (int index = 0; index < code.size(); index++) {
                if (!(code.get(index) instanceof ReferenceInstruction ref)
                        || !(ref.getReference() instanceof MethodReference target)
                        || !target.getDefiningClass().equals(HELPER) || cls.getType().equals(HELPER)) continue;
                require(enabled && method.getName().equals(CALLERS.get(cls.getType())) && seen.add(cls.getType()),
                        "Unexpected or duplicate brand-ad caller");
                boolean browse = cls.getType().equals("Lp/x7v0;");
                String getter = browse ? "Lcom/spotify/browsita/v1/resolved/BrowseStructure;->o()Lp/ih40;"
                        : "Lcom/spotify/casita/v1/resolved/HomeStructure;->p()Lp/ih40;";
                require(index >= 2 && index + 1 < code.size()
                        && code.get(index - 2).getOpcode() == Opcode.INVOKE_VIRTUAL
                        && reference(code.get(index - 2)).equals(getter)
                        && code.get(index - 1).getOpcode() == Opcode.MOVE_RESULT_OBJECT,
                        "Brand-ad hook must follow the native section getter");
                int register = ((OneRegisterInstruction) code.get(index - 1)).getRegisterA();
                require(target.getName().equals(browse ? "browse" : "home")
                        && target.getParameterTypes().equals(List.of("Ljava/util/List;"))
                        && target.getReturnType().equals("Ljava/util/List;")
                        && code.get(index).getOpcode() == Opcode.INVOKE_STATIC_RANGE
                        && code.get(index) instanceof RegisterRangeInstruction call
                        && call.getRegisterCount() == 1 && call.getStartRegister() == register
                        && code.get(index + 1).getOpcode() == Opcode.MOVE_RESULT_OBJECT
                        && ((OneRegisterInstruction) code.get(index + 1)).getRegisterA() == register,
                        "Brand-ad hook must preserve the section list register and filter type");
                int iterator = index + (cls.getType().equals("Lp/jb20;") ? 5 : 4);
                require(iterator < code.size() && code.get(iterator).getOpcode() == Opcode.INVOKE_INTERFACE
                        && reference(code.get(iterator)).equals("Ljava/lang/Iterable;->iterator()Ljava/util/Iterator;")
                        && code.get(iterator) instanceof FiveRegisterInstruction call
                        && call.getRegisterCount() == 1 && call.getRegisterC() == register,
                        "Filtered list must reach the native iterator");
            }
        }
        require(seen.equals(enabled ? CALLERS.keySet() : Set.of()), "Missing brand-ad hooks");
        require(enabled ? capabilities == 1 : capabilities <= 1, "Missing or duplicate brand-ad capability");
    }

    public static void main(String[] args) throws Exception {
        require(args.length == 4 && Set.of("0", "1").contains(args[3]),
                "Usage: VerifyAdsDex.java STOCK PATCHED BUNDLE BRAND_ADS_ENABLED");
        var stock = load(args[0]);
        var patched = load(args[1]);
        boolean enabled = args[3].equals("1");
        verifyHooks(patched.values(), enabled);
        for (var type : MODELS) require(Arrays.equals(canonical(stock.get(type)), canonical(patched.get(type))),
                "Brand-ad model or protobuf list changed: " + type);
        if (patched.containsKey(HELPER)) {
            try (var bundle = new ZipFile(args[2])) {
                var entry = bundle.getEntry("extensions/spotify.mpe");
                require(entry != null, "Bundle lacks the extension");
                try (var input = bundle.getInputStream(entry)) {
                    var dex = new DexBackedDexFile(Opcodes.forApi(35), ByteBuffer.wrap(input.readAllBytes()));
                    var expected = dex.getClasses().stream().filter(c -> c.getType().equals(HELPER)).findFirst().orElseThrow();
                    require(Arrays.equals(canonical(expected), canonical(patched.get(HELPER))), "Brand-ad helper differs from bundle");
                }
            }
        } else require(!enabled, "Missing brand-ad helper");
        System.out.println("Brand ads verified: selected=" + enabled + ", native models unchanged");
    }
}
