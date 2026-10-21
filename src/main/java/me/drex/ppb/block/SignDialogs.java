package me.drex.ppb.block;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.level.block.Block;

/**
 * Polymer's sign edit dialogs, found reflectively so this compiles and runs against a Polymer
 * without them, where signs keep the client's own editor. With them, a sign whose stand-in is
 * another wood opens a dialog instead of an editor drawing that wood.
 */
public final class SignDialogs {
    private static final @Nullable MethodHandle REGISTER;

    static {
        MethodHandle register;
        try {
            Class<?> editing = Class.forName("eu.pb4.polymer.core.api.block.PolymerSignEditing");
            register = MethodHandles.publicLookup().findStatic(editing, "register",
                    MethodType.methodType(void.class, Block.class));
        } catch (ReflectiveOperationException e) {
            register = null;
        }
        REGISTER = register;
    }

    private SignDialogs() {
    }

    /** Gives the sign Polymer's default dialog; no-op without it. */
    public static void register(Block sign) {
        if (REGISTER == null) {
            return;
        }
        try {
            REGISTER.invoke(sign);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
}
