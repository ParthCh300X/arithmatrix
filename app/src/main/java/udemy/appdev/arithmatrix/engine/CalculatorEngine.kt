package udemy.appdev.arithmatrix.engine

import java.util.Stack
import kotlin.math.*

class CalculatorEngine {

    fun evaluate(expression: String): Double {
        val sanitized = sanitize(expression)
        val tokens = tokenize(sanitized)
        return evaluateTokens(tokens)
    }

    fun formatResult(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "Error"
        return if (value == floor(value) && !value.isInfinite()) {
            value.toLong().toString()
        } else {
            "%.10g".format(value).trimEnd('0').trimEnd('.')
        }
    }

    private fun sanitize(raw: String): String {
        var s = raw
            .trim()
            .lowercase()
            .replace("×", "*")
            .replace("÷", "/")
            .replace("x²",  "^2")
            .replace("x^2", "^2")
            // √4 or √(4) → sqrt(4) — handles OCR output directly
            .replace(Regex("√\\(([^)]+)\\)")) { mr -> "sqrt(${mr.groupValues[1]})" }
            .replace(Regex("√(\\d+(?:\\.\\d+)?)")) { mr -> "sqrt(${mr.groupValues[1]})" }
            .replace("√", "sqrt")
            // standalone x → multiply
            .replace(Regex("\\bx\\b"), "*")
            // strip spaces
            .replace(" ", "")

        // strip OCR noise characters before a known function name
        // e.g. "olog98" → "log98", "asin45" → "sin45"
        // only do this when the expression is a single token (no operators)
        val hasOperator = s.any { it in setOf('+', '-', '*', '/', '^', '%') }
        if (!hasOperator) {
            val functions = listOf("sqrt", "sin", "cos", "tan", "log", "ln", "abs")
            for (fn in functions) {
                val idx = s.indexOf(fn)
                if (idx > 0) {
                    // there are chars before the function name — strip them
                    s = s.substring(idx)
                    break
                }
            }
        }

        // wrap bare function calls: sin75 → sin(75), log98 → log(98)
        // leave sin(75) untouched
        s = s.replace(
            Regex("(sin|cos|tan|sqrt|log|ln|abs)(\\d+(?:\\.\\d+)?)")
        ) { mr ->
            "${mr.groupValues[1]}(${mr.groupValues[2]})"
        }

        return s
    }

    private sealed class Token {
        data class Num(val value: Double) : Token()
        data class Op(val char: Char)     : Token()
        data class Fn(val name: String)   : Token()
        object LParen                     : Token()
        object RParen                     : Token()
    }

    private fun tokenize(expr: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0

        while (i < expr.length) {
            val c = expr[i]
            when {
                c.isDigit() || c == '.' -> {
                    val sb = StringBuilder()
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                        sb.append(expr[i++])
                    }
                    tokens.add(Token.Num(sb.toString().toDouble()))
                }

                c.isLetter() -> {
                    val sb = StringBuilder()
                    while (i < expr.length && expr[i].isLetter()) {
                        sb.append(expr[i++])
                    }
                    tokens.add(Token.Fn(sb.toString()))
                }

                c == '(' -> { tokens.add(Token.LParen); i++ }
                c == ')' -> { tokens.add(Token.RParen); i++ }

                c == '+' || c == '-' || c == '*' || c == '/' ||
                        c == '^' || c == '%' -> {
                    tokens.add(Token.Op(c))
                    i++
                }

                else -> i++
            }
        }

        return tokens
    }

    private fun evaluateTokens(tokens: List<Token>): Double {
        val values = Stack<Double>()
        val ops    = Stack<Any>()

        for (token in tokens) {
            when (token) {
                is Token.Num -> values.push(token.value)

                is Token.Fn -> ops.push(token.name)

                is Token.LParen -> ops.push('(')

                is Token.RParen -> {
                    while (ops.isNotEmpty() && ops.peek() != '(') {
                        applyTop(ops, values)
                    }
                    if (ops.isEmpty()) throw ArithmeticException("Mismatched parentheses")
                    ops.pop()

                    if (ops.isNotEmpty() && ops.peek() is String) {
                        val fn = ops.pop() as String
                        if (values.isEmpty()) throw ArithmeticException("Missing argument for $fn")
                        values.push(applyFunction(fn, values.pop()))
                    }
                }

                is Token.Op -> {
                    val c = token.char

                    if (c == '-' && (values.isEmpty() ||
                                (ops.isNotEmpty() && (ops.peek() == '(' ||
                                        (ops.peek() is Char && isOperator(ops.peek() as Char)))))) {
                        values.push(0.0)
                    }

                    if (c == '%') {
                        if (values.isNotEmpty()) {
                            val num = values.pop()
                            val base = if (ops.isNotEmpty() &&
                                ops.peek() is Char &&
                                (ops.peek() == '+' || ops.peek() == '-')) {
                                if (values.isNotEmpty()) values.peek() else num
                            } else 1.0
                            values.push(num * base / 100.0)
                        }
                    } else {
                        while (ops.isNotEmpty() &&
                            ops.peek() is Char &&
                            ops.peek() != '(' &&
                            hasPrecedence(c, ops.peek() as Char)) {
                            applyTop(ops, values)
                        }
                        ops.push(c)
                    }
                }
            }
        }

        while (ops.isNotEmpty()) {
            if (ops.peek() == '(') throw ArithmeticException("Mismatched parentheses")
            applyTop(ops, values)
        }

        if (values.isEmpty()) throw ArithmeticException("Invalid expression")
        return values.pop()
    }

    private fun applyTop(ops: Stack<Any>, values: Stack<Double>) {
        val op = ops.pop()
        if (op is Char) {
            if (values.size < 2) throw ArithmeticException("Invalid expression")
            val b = values.pop()
            val a = values.pop()
            values.push(applyOperator(op, a, b))
        }
    }

    private fun applyOperator(op: Char, a: Double, b: Double): Double {
        return when (op) {
            '+' -> a + b
            '-' -> a - b
            '*' -> a * b
            '/' -> {
                if (b == 0.0) throw ArithmeticException("Division by zero")
                a / b
            }
            '^' -> a.pow(b)
            else -> throw ArithmeticException("Unknown operator: $op")
        }
    }

    private fun applyFunction(name: String, arg: Double): Double {
        return when (name) {
            "sin"   -> sin(Math.toRadians(arg))
            "cos"   -> cos(Math.toRadians(arg))
            "tan"   -> tan(Math.toRadians(arg))
            "sqrt"  -> {
                if (arg < 0) throw ArithmeticException("sqrt of negative number")
                sqrt(arg)
            }
            "log"   -> {
                if (arg <= 0) throw ArithmeticException("log of non-positive number")
                log10(arg)
            }
            "ln"    -> {
                if (arg <= 0) throw ArithmeticException("ln of non-positive number")
                ln(arg)
            }
            "abs"   -> abs(arg)
            "ceil"  -> ceil(arg)
            "floor" -> floor(arg)
            else    -> throw ArithmeticException("Unknown function: $name")
        }
    }

    private fun hasPrecedence(op1: Char, op2: Char): Boolean {
        if (op2 == '(' || op2 == ')') return false
        if (op1 == '^' && op2 == '^') return false
        if ((op1 == '*' || op1 == '/') && (op2 == '+' || op2 == '-')) return false
        if (op1 == '^' && (op2 == '+' || op2 == '-' || op2 == '*' || op2 == '/')) return false
        return true
    }

    private fun isOperator(c: Char) = c in setOf('+', '-', '*', '/', '^', '%')
}