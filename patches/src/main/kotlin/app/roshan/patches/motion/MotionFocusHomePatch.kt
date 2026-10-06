package app.roshan.patches.motion

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

/**
 * Motion 4.2.17 home-screen cleanup.
 *
 * Hooks React Native's root view when it is attached and lets a small runtime
 * extension hide selected promotional/recommendation sections after they are
 * rendered. Core learning sections are intentionally untouched.
 */
private object MotionReactRootAttachFingerprint : Fingerprint(
    definingClass = "Lcom/facebook/react/ReactRootView;",
    name = "onAttachedToWindow",
    returnType = "V",
    parameters = emptyList()
)

@Suppress("unused")
val motionFocusHomePatch = bytecodePatch(
    name = "Motion Focus Home",
    description = "Hides selected promotional and recommendation sections from Motion 4.2.17's home screen."
) {
    compatibleWith("com.elearning.motion"("4.2.17"))
    extendWith("extensions/extension.mpe")

    execute {
        MotionReactRootAttachFingerprint.method.addInstructions(
            0,
            """
                invoke-static {p0}, Lapp/roshan/extension/MotionHomeCleaner;->schedule(Landroid/view/View;)V
            """
        )
    }
}
