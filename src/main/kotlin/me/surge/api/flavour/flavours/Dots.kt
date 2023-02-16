package me.surge.api.flavour.flavours

import me.surge.api.flavour.Flavour

/**
 * @author surge
 * @since 16/02/2023
 */
object Dots : Flavour() {

    override val ALLOWED_SYMBOLS = super.ALLOWED_SYMBOLS + "."
    override fun accessor() = "."

}