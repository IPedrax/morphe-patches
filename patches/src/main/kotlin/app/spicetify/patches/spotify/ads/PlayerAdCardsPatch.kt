package app.spicetify.patches.spotify.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.spicetify.patches.spotify.settings.NativeSettingsAbi
import app.spicetify.patches.spotify.settings.enableSetting
import app.spicetify.patches.spotify.settings.settingsPatch
import app.spicetify.patches.spotify.spotifyCompatibility
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import java.util.Properties

@Suppress("unused")
val playerAdCardsPatch = bytecodePatch(
    name = "Hide player ad cards",
    description = "Hides image brand-ad cards in Now Playing. " +
        "Does not suppress audio ads or other player overlays. Experimental.",
    default = false,
) {
    compatibleWith(spotifyCompatibility)
    dependsOn(settingsPatch)

    execute {
        val snapshot = Properties().apply {
            NativeSettingsAbi::class.java.getResourceAsStream("/ads/player-9.1.80.2221.properties")!!.use(::load)
        }
        for (type in snapshot.stringPropertyNames()) {
            val definition = classDefByOrNull(type)
                ?: throw PatchException("Spotify player advertising ABI changed: missing $type")
            if (NativeSettingsAbi.digest(definition) != snapshot.getProperty(type)) {
                throw PatchException("Spotify player advertising ABI changed: $type. Use the verified Spotify 9.1.80.2221 APK.")
            }
        }

        val method = mutableClassDefBy("Lp/ja31;").methods.single {
            it.name == "invoke" && it.parameterTypes == listOf("Ljava/lang/Object;")
        }
        val instructions = method.implementation!!.instructions.toList()
        val getter = "Lcom/spotify/scrollsita/v1/Section;->o0()Z"
        val matches = instructions.indices.filter {
            (instructions[it] as? ReferenceInstruction)?.reference.toString() == getter
        }
        if (matches.size != 1 || instructions.getOrNull(matches.single() + 1)?.opcode != Opcode.MOVE_RESULT
            || instructions.getOrNull(matches.single() + 3)?.opcode != Opcode.IF_EQZ
        ) {
            throw PatchException("Expected one Now Playing image-ad mapper in Lp/ja31;->invoke.")
        }
        val result = matches.single() + 1
        val register = (instructions[result] as OneRegisterInstruction).registerA
        if ((instructions[result + 2] as OneRegisterInstruction).registerA != register) {
            throw PatchException("Now Playing image-ad branch changed its result register.")
        }
        method.addInstructions(result + 1, """
            invoke-static/range {v$register .. v$register}, Lapp/spicetify/extension/spotify/ads/PlayerAdCards;->showImageBrandAd(Z)Z
            move-result v$register
        """.trimIndent())
        enableSetting("hidePlayerAdCards")
    }
}
