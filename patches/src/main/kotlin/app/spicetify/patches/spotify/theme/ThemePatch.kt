package app.spicetify.patches.spotify.theme

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.spicetify.patches.spotify.settings.NativeSettingsAbi
import app.spicetify.patches.spotify.settings.themeSettingsPatch
import app.spicetify.patches.spotify.spotifyCompatibility
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import java.util.Properties

private const val COLOR = "Lp/iae1;->g(J)J"
private const val MAP = "Lapp/spicetify/extension/spotify/theme/EncorePalette;->map(J)J"

// Stock Encore background and accent constants that the in-app theme replaces.
internal val paletteColors = listOf(
    0xFF121212L, 0xFF1F1F1FL, 0xFF2A2A2AL, 0xFF191919L,
    0xFF1ED760L, 0xFF3BE477L, 0xFF1ABC54L,
)

private val themeResourcesPatch = resourcePatch {
    execute { document("res/values/colors.xml").use(::requireThemeColorResources) }
}

@Suppress("unused")
val themePatch = bytecodePatch(
    name = "Theme colors",
    description = "Choose background and accent colors in Spicetify settings. Restart Spotify after changing them. " +
        "Some screens and hardcoded colors keep Spotify's colors.",
    default = false,
) {
    compatibleWith(spotifyCompatibility)
    dependsOn(themeSettingsPatch, themeResourcesPatch)

    execute {
        val snapshot = Properties().apply {
            NativeSettingsAbi::class.java.getResourceAsStream("/theme/palette-9.1.80.2221.properties")!!.use(::load)
        }
        for (type in snapshot.stringPropertyNames()) {
            val definition = classDefByOrNull(type) ?: throw PatchException("Spotify palette ABI changed: missing $type")
            if (NativeSettingsAbi.digest(definition) != snapshot.getProperty(type)) {
                throw PatchException("Spotify palette ABI changed: $type. Use the verified Spotify 9.1.80.2221 APK.")
            }
        }

        // Each pinned class is one of Encore's palette variants; hook every stock theme constant it loads.
        snapshot.stringPropertyNames().sorted().forEach(::hookPalette)
    }
}

private fun app.morphe.patcher.patch.BytecodePatchContext.hookPalette(type: String) {
    val initializer = mutableClassDefBy(type).methods.single { it.name == "<clinit>" }
    val instructions = initializer.implementation!!.instructions.toList()
    val sites = instructions.indices.filter {
        instructions[it].opcode == Opcode.CONST_WIDE && (instructions[it] as WideLiteralInstruction).wideLiteral in paletteColors
    }
    if (sites.isEmpty()) throw PatchException("Encore palette $type has no theme colors.")
    val colors = sites.map { (instructions[it] as WideLiteralInstruction).wideLiteral }
    if (colors.size != colors.toSet().size) throw PatchException("Encore palette $type repeats a theme color constant.")
    sites.forEach { index ->
        if (!feedsColor(instructions, index)) throw PatchException("Encore palette $type constant no longer feeds Color().")
    }
    sites.sortedDescending().forEach { index ->
        val register = initializer.getInstruction<OneRegisterInstruction>(index).registerA
        initializer.addInstructions(index + 1, """
            invoke-static/range {v$register .. v${register + 1}}, $MAP
            move-result-wide v$register
        """.trimIndent())
    }
}

/** True when the constant loaded at [index] reaches Color() within a few instructions, before its register is rewritten. */
private fun feedsColor(instructions: List<com.android.tools.smali.dexlib2.iface.instruction.Instruction>, index: Int): Boolean {
    val register = (instructions[index] as OneRegisterInstruction).registerA
    for (next in instructions.subList(index + 1, minOf(instructions.size, index + 9))) {
        val firstArgument = when (next) {
            is FiveRegisterInstruction -> next.registerC
            is RegisterRangeInstruction -> next.startRegister
            else -> -1
        }
        if ((next as? ReferenceInstruction)?.reference?.toString() == COLOR && firstArgument == register) return true
        if (next.opcode.setsRegister() && next is OneRegisterInstruction &&
            next.registerA in (register - 1)..(register + 1)
        ) return false
    }
    return false
}
