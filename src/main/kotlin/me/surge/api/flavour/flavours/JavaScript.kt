package me.surge.api.flavour.flavours

import me.surge.api.flavour.Flavour

/**
 * @author surge
 * @since 16/02/2023
 */
object JavaScript : Flavour() {

    override val ALLOWED_SYMBOLS = super.ALLOWED_SYMBOLS + "."

    override fun mutable() = "let"
    override fun elif() = "else if"
    override fun method() = "function"
    override fun `in`() = "of"
    override fun container() = "class"
    override fun accessor() = "."

}