package com.man11.client.screen;

import net.minecraft.client.Minecraft;

public final class C2cScreenHelper {
	private C2cScreenHelper() {
	}

	public static void close() {
		Minecraft.getInstance().setScreenAndShow(null);
	}
}
