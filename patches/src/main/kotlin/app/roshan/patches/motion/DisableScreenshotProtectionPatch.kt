package app.roshan.patches.motion

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

/**
 * Motion 4.2.17 app-specific screenshot protection patch.
 *
 * Motion's ScreenshotPreventModule.b(Activity) enables Android FLAG_SECURE by
 * calling Window.setFlags(0x2000, 0x2000). We replace only that call with
 * Window.clearFlags(0x2000), leaving unrelated Window flag operations alone.
 */
private object MotionScreenshotEnableFingerprint : Fingerprint(
    definingClass = "Lcom/elearning/motion/ScreenshotPreventModule;",
    name = "b",
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;"),
    filters = listOf(
        methodCall(
            definingClass = "Landroid/view/Window;",
            name = "setFlags",
            parameters = listOf("I", "I")
        )
    )
)

@Suppress("unused")
val disableMotionScreenshotProtectionPatch = bytecodePatch(
    name = "Disable Motion screenshot protection",
    description = "Disables Motion 4.2.17's app-specific FLAG_SECURE enable call."
) {
    execute {
        val match = MotionScreenshotEnableFingerprint.instructionMatches.singleOrNull()
            ?: error("Motion screenshot protection: expected exactly one Window.setFlags call")

        val index = match.index
        val instruction = MotionScreenshotEnableFingerprint.method
            .getInstruction<FiveRegisterInstruction>(index)

        // invoke-virtual {vC, vD, vE}, Window.setFlags(II)V
        // C = Window object, D/E = the two int arguments.
        val windowRegister = instruction.registerC
        val flagRegister = instruction.registerD
        val maskRegister = instruction.registerE

        // The two arguments must be the same FLAG_SECURE value. The constant
        // itself is preserved in the method; only the call is changed.
        if (flagRegister != maskRegister) {
            error("Motion screenshot protection: setFlags arguments are not identical")
        }

        MotionScreenshotEnableFingerprint.method.replaceInstruction(
            index,
            "invoke-virtual { v$windowRegister, v$flagRegister }, Landroid/view/Window;->clearFlags(I)V"
        )
    }
}
