package dev.lifeos.agent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EnforcerTest {
    @Test
    fun classifiesTheRealProcesses() {
        // Tal como aparecen en este PC (ps).
        assertEquals(Distraction.DISCORD, classify("/opt/vesktop/vesktop", listOf("--type=renderer")))
        assertEquals(Distraction.STEAM, classify("/home/u/.local/share/Steam/ubuntu12_32/steam", emptyList()))
        assertEquals(Distraction.STEAM, classify("/home/u/.local/share/Steam/ubuntu12_64/steamwebhelper", emptyList()))
        assertEquals(Distraction.LUTRIS, classify("/usr/bin/python3.14", listOf("/usr/bin/lutris")))
        assertEquals(Distraction.DISCORD, classify("/usr/bin/bwrap", listOf("--args", "41", "dev.vencord.Vesktop")))
        // Cosas que no se tocan.
        assertNull(classify("/usr/bin/python3", listOf("/home/u/proyecto/main.py")))
        assertNull(classify("/usr/lib/zen-browser/zen", emptyList()))
        assertNull(classify("/usr/bin/kitty", listOf("steam-notes.txt")))
        assertNull(classify(null, emptyList()))
    }
}
