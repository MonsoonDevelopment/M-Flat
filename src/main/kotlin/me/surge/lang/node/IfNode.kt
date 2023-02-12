package me.surge.lang.node

import me.surge.lang.parse.Parser

class IfNode(val cases: ArrayList<Parser.Case>, val elseCase: Parser.BaseCase?) : Node(cases[0].token.start, elseCase?.token?.end ?: cases.last().token.end) {

    override fun toString(): String {
        return "<If: ${this.cases}, ${this.elseCase}>"
    }

}